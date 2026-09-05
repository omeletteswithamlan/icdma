'use client';

import { useMemo, useRef, useState } from 'react';
import { simulate, mulberry32, type CycleModel, type SimResult } from 'icdma-engine';

/* ------------------------------------------------------------------ */
/* Graph model — what the student draws                                */
/* ------------------------------------------------------------------ */

type Kind = 'queue' | 'combi' | 'normal' | 'counter';

interface GNode {
  id: string;
  kind: Kind;
  label: string;
  x: number;
  y: number;
  /** queues: tokens at t=0 */
  tokens?: number;
  /** queues: this is the fleet whose size the production plot sweeps */
  fleet?: boolean;
  /** queues: report utilization of these tokens */
  resource?: boolean;
  /** activities: duration in minutes */
  duration?: number;
  /** counters: quantity added per completion of the feeding activity */
  units?: number;
  unitLabel?: string;
}

interface GArc { from: string; to: string }

interface Graph { nodes: GNode[]; arcs: GArc[] }

const isActivity = (k: Kind) => k === 'combi' || k === 'normal';

/** the textbook earthmoving operation, as the student should draw it */
function classicEarthmoving(): Graph {
  return {
    nodes: [
      { id: 'bank', kind: 'queue', label: 'Soil in bank', x: 70, y: 70, tokens: 1560, unitLabel: 'loads' },
      { id: 'exc', kind: 'queue', label: 'Excavator idle', x: 70, y: 300, tokens: 1, resource: true },
      { id: 'trucks', kind: 'queue', label: 'Trucks waiting', x: 210, y: 300, tokens: 4, fleet: true, resource: true },
      { id: 'load', kind: 'combi', label: 'LOAD', x: 220, y: 180, duration: 3.3 },
      { id: 'haul', kind: 'normal', label: 'HAUL', x: 400, y: 180, duration: 8 },
      { id: 'dump', kind: 'normal', label: 'DUMP', x: 580, y: 180, duration: 1.5 },
      { id: 'fill', kind: 'counter', label: 'Fill placed', x: 760, y: 70, units: 16, unitLabel: 'LCY' },
      { id: 'return', kind: 'normal', label: 'RETURN', x: 580, y: 320, duration: 6 },
    ],
    arcs: [
      { from: 'bank', to: 'load' }, { from: 'exc', to: 'load' }, { from: 'trucks', to: 'load' },
      { from: 'load', to: 'exc' }, { from: 'load', to: 'haul' },
      { from: 'haul', to: 'dump' }, { from: 'dump', to: 'fill' }, { from: 'dump', to: 'return' },
      { from: 'return', to: 'trucks' },
    ],
  };
}

/* ------------------------------------------------------------------ */
/* Graph → engine model                                                 */
/* ------------------------------------------------------------------ */

interface Compiled { model: CycleModel; errors: string[]; fleetQueue?: string }

function compile(g: Graph): Compiled {
  const errors: string[] = [];
  const byId = new Map(g.nodes.map((n) => [n.id, n]));
  const model: CycleModel = { queues: [], activities: [] };

  for (const a of g.arcs) {
    const f = byId.get(a.from); const t = byId.get(a.to);
    if (!f || !t) continue;
    const ok = (f.kind === 'queue' && isActivity(t.kind))
      || (isActivity(f.kind) && (t.kind === 'queue' || t.kind === 'counter' || isActivity(t.kind)));
    if (!ok) errors.push(`An arrow from "${f.label}" to "${t.label}" is not allowed — arrows run queue → activity → queue (or counter).`);
  }

  for (const n of g.nodes) {
    if (n.kind === 'queue') model.queues.push({ id: n.id, label: n.label, initial: n.tokens ?? 0, resource: n.resource });
  }
  // activity → activity arrows get an implicit hand-off queue (CYCLONE normal-after-normal)
  for (const a of g.arcs) {
    const f = byId.get(a.from); const t = byId.get(a.to);
    if (f && t && isActivity(f.kind) && isActivity(t.kind)) {
      model.queues.push({ id: `_${a.from}_${a.to}`, label: '', initial: 0 });
    }
  }
  for (const n of g.nodes) {
    if (!isActivity(n.kind)) continue;
    const takes: { queue: string; n: number }[] = [];
    const gives: { queue: string; n: number }[] = [];
    let produces: number | undefined;
    for (const a of g.arcs) {
      if (a.to === n.id) {
        const f = byId.get(a.from)!;
        takes.push({ queue: f.kind === 'queue' ? f.id : `_${a.from}_${a.to}`, n: 1 });
      }
      if (a.from === n.id) {
        const t = byId.get(a.to)!;
        if (t.kind === 'queue') gives.push({ queue: t.id, n: 1 });
        else if (t.kind === 'counter') produces = (produces ?? 0) + (t.units ?? 1);
        else gives.push({ queue: `_${a.from}_${a.to}`, n: 1 });
      }
    }
    if (takes.length === 0) errors.push(`"${n.label}" has no incoming arrow — nothing can start it.`);
    if (n.kind === 'combi' && takes.length < 2) errors.push(`"${n.label}" is a COMBI but draws from only one queue — a COMBI combines two or more resources.`);
    if (n.kind === 'normal' && takes.length > 1) errors.push(`"${n.label}" is a NORMAL activity but draws from ${takes.length} sources — that makes it a COMBI.`);
    model.activities.push({
      id: n.id, label: n.label,
      duration: { kind: 'const', value: n.duration ?? 1 },
      takes, gives, produces,
    });
  }
  if (!g.nodes.some((n) => n.kind === 'counter')) errors.push('Add a COUNTER and connect an activity to it so production can be measured.');
  const fleet = g.nodes.find((n) => n.kind === 'queue' && n.fleet)?.id;
  return { model, errors, fleetQueue: fleet };
}

/* ------------------------------------------------------------------ */
/* Symbols (Halpin's CYCLONE conventions)                               */
/* ------------------------------------------------------------------ */

function Symbol({ n, selected, result, onPointerDown, onClick }: {
  n: GNode; selected: boolean; result?: SimResult | null;
  onPointerDown: (e: React.PointerEvent) => void; onClick: () => void;
}) {
  const stroke = selected ? 'var(--accent)' : 'var(--ink)';
  const sw = selected ? 2.2 : 1.4;
  const util = result && n.resource ? result.utilization.get(n.id) : undefined;
  const avg = result ? result.avgQueue.get(n.id) : undefined;
  const fired = result ? result.firings.get(n.id) : undefined;
  const common = { onPointerDown, onClick, style: { cursor: 'grab' } as React.CSSProperties };

  if (n.kind === 'queue') {
    return (
      <g transform={`translate(${n.x},${n.y})`} {...common}>
        <circle r={28} fill="var(--surface)" stroke={stroke} strokeWidth={sw} />
        <text y={-2} textAnchor="middle" fontSize={14} fontWeight={700} fontFamily="var(--font-display)" fill="var(--ink)">
          {n.tokens ?? 0}
        </text>
        <text y={13} textAnchor="middle" fontSize={8.5} fill="var(--muted)">{n.unitLabel ?? (n.tokens === 1 ? 'unit' : 'units')}</text>
        <text y={44} textAnchor="middle" fontSize={11} fill="var(--ink)">{n.label}</text>
        {n.fleet && <text y={57} textAnchor="middle" fontSize={9} fill="var(--accent)" fontFamily="var(--font-display)" fontWeight={600}>FLEET · swept</text>}
        {avg !== undefined && (
          <text y={n.fleet ? 69 : 57} textAnchor="middle" fontSize={9.5} fill="var(--muted)" className="num">
            avg waiting {avg.toFixed(1)}{util !== undefined ? ` · busy ${Math.round(util * 100)}%` : ''}
          </text>
        )}
      </g>
    );
  }
  if (n.kind === 'counter') {
    return (
      <g transform={`translate(${n.x},${n.y})`} {...common}>
        <circle r={24} fill="var(--surface)" stroke={stroke} strokeWidth={sw} />
        <line x1={-24} x2={24} y1={9} y2={9} stroke={stroke} strokeWidth={1.4} />
        <text y={-2} textAnchor="middle" fontSize={9} fontFamily="var(--font-display)" fontWeight={600} fill="var(--ink)">COUNT</text>
        <text y={40} textAnchor="middle" fontSize={11} fill="var(--ink)">{n.label}</text>
        <text y={53} textAnchor="middle" fontSize={9.5} fill="var(--muted)" className="num">
          {result ? `${Math.round(result.produced).toLocaleString()} ${n.unitLabel ?? ''}` : `+${n.units ?? 1} ${n.unitLabel ?? ''} each`}
        </text>
      </g>
    );
  }
  // activities
  const w = 96; const h = 40;
  return (
    <g transform={`translate(${n.x},${n.y})`} {...common}>
      <rect x={-w / 2} y={-h / 2} width={w} height={h} rx={2} fill="var(--surface)" stroke={stroke} strokeWidth={sw} />
      {n.kind === 'combi' && (
        <path d={`M${-w / 2} ${-h / 2 + 12} L${-w / 2 + 12} ${-h / 2}`} stroke={stroke} strokeWidth={sw} fill="none" />
      )}
      <text y={-3} textAnchor="middle" fontSize={11.5} fontWeight={700} fontFamily="var(--font-display)" fill="var(--ink)">{n.label}</text>
      <text y={11} textAnchor="middle" fontSize={9.5} fill="var(--muted)" className="num">{n.duration ?? 0} min</text>
      <text y={h / 2 + 13} textAnchor="middle" fontSize={9} fill="var(--muted)">
        {n.kind === 'combi' ? 'COMBI' : 'NORMAL'}{fired !== undefined ? ` · ran ${fired}×` : ''}
      </text>
    </g>
  );
}

function anchor(n: GNode, towards: GNode): { x: number; y: number } {
  const dx = towards.x - n.x; const dy = towards.y - n.y;
  const len = Math.hypot(dx, dy) || 1;
  const ux = dx / len; const uy = dy / len;
  if (n.kind === 'queue') return { x: n.x + ux * 29, y: n.y + uy * 29 };
  if (n.kind === 'counter') return { x: n.x + ux * 25, y: n.y + uy * 25 };
  // rectangle 96x40: intersect ray with box
  const hw = 48; const hh = 20;
  const tx = Math.abs(ux) > 1e-6 ? hw / Math.abs(ux) : Infinity;
  const ty = Math.abs(uy) > 1e-6 ? hh / Math.abs(uy) : Infinity;
  const t = Math.min(tx, ty) + 1;
  return { x: n.x + ux * t, y: n.y + uy * t };
}

/* ------------------------------------------------------------------ */
/* Production plot, swept from the diagram                               */
/* ------------------------------------------------------------------ */

function Sweep({ g, compiled, counterUnit }: { g: Graph; compiled: Compiled; counterUnit: string }) {
  const data = useMemo(() => {
    if (compiled.errors.length || !compiled.fleetQueue) return null;
    const out: { n: number; rate: number; waiting: number }[] = [];
    for (let n = 1; n <= 12; n++) {
      const model: CycleModel = {
        ...compiled.model,
        queues: compiled.model.queues.map((q) => (q.id === compiled.fleetQueue ? { ...q, initial: n } : q)),
      };
      const r = simulate(model, { horizon: 480, rng: mulberry32(1) });
      out.push({ n, rate: (r.produced / r.endTime) * 60, waiting: r.avgQueue.get(compiled.fleetQueue) ?? 0 });
    }
    return out;
  }, [compiled]);
  if (!data) return null;
  const fleetNow = g.nodes.find((n) => n.id === compiled.fleetQueue)?.tokens ?? 0;
  // balance: first n where the next unit adds less than 3%
  let balance = data.length;
  for (let i = 1; i < data.length; i++) if (data[i].rate < data[i - 1].rate * 1.03) { balance = data[i - 1].n; break; }

  const W = 520; const H = 200; const M = { l: 54, r: 14, t: 14, b: 30 };
  const maxR = Math.max(...data.map((d) => d.rate)) * 1.12;
  const maxW = Math.max(1, ...data.map((d) => d.waiting)) * 1.15;
  const x = (n: number) => M.l + ((n - 1) / 11) * (W - M.l - M.r);
  const chart = (title: string, key: 'rate' | 'waiting', max: number, color: string, unit: string) => (
    <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: 'auto' }} role="img" aria-label={title}>
      <text x={M.l} y={10} fontSize={11} fontFamily="var(--font-display)" fontWeight={600} fill="var(--ink)">{title}</text>
      {[0.5, 1].map((f) => (
        <line key={f} x1={M.l} x2={W - M.r} y1={H - M.b - f * (H - M.t - M.b)} y2={H - M.b - f * (H - M.t - M.b)} stroke="var(--line)" strokeWidth={0.7} />
      ))}
      {[0.5, 1].map((f) => (
        <text key={f} x={M.l - 6} y={H - M.b - f * (H - M.t - M.b) + 4} textAnchor="end" fontSize={10} fill="var(--muted)" className="num">
          {(f * max).toFixed(key === 'rate' ? 0 : 1)}
        </text>
      ))}
      <line x1={x(balance)} x2={x(balance)} y1={M.t + 14} y2={H - M.b} stroke="var(--muted)" strokeDasharray="3 3" strokeWidth={1} />
      <text x={x(balance)} y={M.t + 26} textAnchor="middle" fontSize={10} fill="var(--muted)">balance ≈ {balance}</text>
      <polyline fill="none" stroke={color} strokeWidth={2.4}
        points={data.map((d) => `${x(d.n)},${H - M.b - (d[key] / max) * (H - M.t - M.b)}`).join(' ')} />
      {data.map((d) => (
        <circle key={d.n} cx={x(d.n)} cy={H - M.b - (d[key] / max) * (H - M.t - M.b)}
          r={d.n === fleetNow ? 5.5 : 3} fill={d.n === fleetNow ? color : 'var(--surface)'} stroke={color} strokeWidth={1.6} />
      ))}
      {[1, 4, 8, 12].map((n) => <text key={n} x={x(n)} y={H - 12} textAnchor="middle" fontSize={10} fill="var(--muted)">{n}</text>)}
      <text x={(M.l + W - M.r) / 2} y={H - 1} textAnchor="middle" fontSize={10} fill="var(--muted)">units in the fleet queue</text>
      <text x={W - M.r} y={10} textAnchor="end" fontSize={10} fill="var(--muted)">{unit}</text>
    </svg>
  );
  return (
    <div style={{ display: 'grid', gap: '0.6rem', gridTemplateColumns: 'minmax(0,1fr) minmax(0,1fr)' }} className="studio-grid">
      {chart('Production rate', 'rate', maxR, 'var(--accent)', `${counterUnit}/hr`)}
      {chart('Fleet units waiting in queue', 'waiting', maxW, 'var(--caution)', 'avg waiting')}
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* The builder                                                          */
/* ------------------------------------------------------------------ */

type Mode = 'move' | 'connect' | 'delete';

export default function AcdBuilder() {
  const [g, setG] = useState<Graph>(() => classicEarthmoving());
  const [mode, setMode] = useState<Mode>('move');
  const [selected, setSelected] = useState<string | null>(null);
  const [connectFrom, setConnectFrom] = useState<string | null>(null);
  const [result, setResult] = useState<SimResult | null>(null);
  const [horizon, setHorizon] = useState(480);
  const drag = useRef<{ id: string; dx: number; dy: number } | null>(null);
  const svgRef = useRef<SVGSVGElement>(null);
  const nextId = useRef(1);

  const compiled = useMemo(() => compile(g), [g]);
  const counter = g.nodes.find((n) => n.kind === 'counter');
  const counterUnit = counter?.unitLabel ?? 'units';
  const sel = g.nodes.find((n) => n.id === selected) ?? null;

  const update = (id: string, patch: Partial<GNode>) => {
    setG((s) => ({ ...s, nodes: s.nodes.map((n) => (n.id === id ? { ...n, ...patch } : n)) }));
    setResult(null);
  };

  const svgPoint = (e: React.PointerEvent) => {
    const svg = svgRef.current!;
    const pt = svg.createSVGPoint(); pt.x = e.clientX; pt.y = e.clientY;
    const p = pt.matrixTransform(svg.getScreenCTM()!.inverse());
    return { x: p.x, y: p.y };
  };

  const onNodePointerDown = (n: GNode) => (e: React.PointerEvent) => {
    if (mode !== 'move') return;
    const p = svgPoint(e);
    drag.current = { id: n.id, dx: n.x - p.x, dy: n.y - p.y };
    (e.target as Element).setPointerCapture?.(e.pointerId);
  };
  const onPointerMove = (e: React.PointerEvent) => {
    if (!drag.current) return;
    const p = svgPoint(e);
    const { id, dx, dy } = drag.current;
    setG((s) => ({ ...s, nodes: s.nodes.map((n) => (n.id === id ? { ...n, x: Math.round(p.x + dx), y: Math.round(p.y + dy) } : n)) }));
  };
  const onPointerUp = () => { drag.current = null; };

  const onNodeClick = (n: GNode) => () => {
    if (mode === 'move') { setSelected(n.id); return; }
    if (mode === 'delete') {
      setG((s) => ({ nodes: s.nodes.filter((x) => x.id !== n.id), arcs: s.arcs.filter((a) => a.from !== n.id && a.to !== n.id) }));
      setSelected(null); setResult(null); return;
    }
    if (mode === 'connect') {
      if (!connectFrom) { setConnectFrom(n.id); return; }
      if (connectFrom !== n.id && !g.arcs.some((a) => a.from === connectFrom && a.to === n.id)) {
        setG((s) => ({ ...s, arcs: [...s.arcs, { from: connectFrom, to: n.id }] }));
        setResult(null);
      }
      setConnectFrom(null);
    }
  };

  const add = (kind: Kind) => {
    const id = `${kind}${nextId.current++}`;
    const base: GNode = { id, kind, label: kind === 'queue' ? 'New queue' : kind === 'counter' ? 'Counter' : kind.toUpperCase(), x: 120 + (nextId.current % 5) * 60, y: 420 };
    if (kind === 'queue') { base.tokens = 1; }
    if (isActivity(kind)) { base.duration = 2; }
    if (kind === 'counter') { base.units = 1; base.unitLabel = 'units'; }
    setG((s) => ({ ...s, nodes: [...s.nodes, base] }));
    setSelected(id); setMode('move'); setResult(null);
  };

  const run = () => {
    if (compiled.errors.length) return;
    setResult(simulate(compiled.model, { horizon, rng: mulberry32(1) }));
  };

  const byId = new Map(g.nodes.map((n) => [n.id, n]));
  const tool = (label: string, active: boolean, onClick: () => void) => (
    <button className="ghost" onClick={onClick}
      style={active ? { borderColor: 'var(--accent)', color: 'var(--accent)', background: 'var(--wash-accent)' } : undefined}>
      {label}
    </button>
  );
  const field = (label: string, value: number | string | undefined, onChange: (v: string) => void, type: 'number' | 'text' = 'number', step?: number) => (
    <label style={{ display: 'block', fontSize: '0.78rem', color: 'var(--muted)', marginTop: '0.45rem' }}>
      {label}
      <input type={type} value={value ?? ''} step={step} onChange={(e) => onChange(e.target.value)}
        style={{ display: 'block', width: '100%', padding: '0.3rem 0.45rem', fontSize: '0.95rem', marginTop: '0.1rem',
          border: '1px solid var(--line)', borderRadius: 6, background: 'var(--surface)', color: 'var(--ink)' }} />
    </label>
  );
  const rate = result ? (result.produced / result.endTime) * 60 : null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem' }}>
      <section className="card">
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem', alignItems: 'center', marginBottom: '0.5rem' }}>
          <span className="label" style={{ marginRight: '0.4rem' }}>Draw the operation</span>
          {tool('+ Queue', false, () => add('queue'))}
          {tool('+ COMBI', false, () => add('combi'))}
          {tool('+ NORMAL', false, () => add('normal'))}
          {tool('+ Counter', false, () => add('counter'))}
          <span style={{ width: 1, height: 22, background: 'var(--line)', margin: '0 0.3rem' }} />
          {tool('Move / edit', mode === 'move', () => { setMode('move'); setConnectFrom(null); })}
          {tool(connectFrom ? 'Connect: pick the target…' : 'Connect arrows', mode === 'connect', () => { setMode('connect'); setConnectFrom(null); })}
          {tool('Delete', mode === 'delete', () => { setMode('delete'); setConnectFrom(null); })}
          <span style={{ width: 1, height: 22, background: 'var(--line)', margin: '0 0.3rem' }} />
          {tool('Load the classic example', false, () => { setG(classicEarthmoving()); setResult(null); setSelected(null); })}
          {tool('Start blank', false, () => { setG({ nodes: [], arcs: [] }); setResult(null); setSelected(null); })}
        </div>
        <div style={{ display: 'grid', gap: '0.8rem', gridTemplateColumns: 'minmax(0, 4fr) minmax(14rem, 1.2fr)' }} className="studio-grid">
          <svg ref={svgRef} viewBox="0 0 860 470" onPointerMove={onPointerMove} onPointerUp={onPointerUp} onPointerLeave={onPointerUp}
            style={{ width: '100%', height: 'auto', background: 'var(--bg)', border: '1px solid var(--line)', borderRadius: 6, touchAction: 'none' }}
            role="img" aria-label="Activity cycle diagram canvas">
            <defs>
              <marker id="acd-arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
                <path d="M0 0 L10 5 L0 10 z" fill="var(--ink)" />
              </marker>
            </defs>
            {g.arcs.map((a) => {
              const f = byId.get(a.from); const t = byId.get(a.to);
              if (!f || !t) return null;
              const p1 = anchor(f, t); const p2 = anchor(t, f);
              const mx = (p1.x + p2.x) / 2; const my = (p1.y + p2.y) / 2;
              // a gentle curve so opposite arrows don't overlap
              const nx = -(p2.y - p1.y) * 0.12; const ny = (p2.x - p1.x) * 0.12;
              return (
                <path key={`${a.from}-${a.to}`} d={`M${p1.x} ${p1.y} Q${mx + nx} ${my + ny} ${p2.x} ${p2.y}`}
                  fill="none" stroke="var(--ink)" strokeWidth={1.4} markerEnd="url(#acd-arrow)"
                  onClick={() => { if (mode === 'delete') { setG((s) => ({ ...s, arcs: s.arcs.filter((x) => !(x.from === a.from && x.to === a.to)) })); setResult(null); } }}
                  style={{ cursor: mode === 'delete' ? 'pointer' : 'default' }} />
              );
            })}
            {g.nodes.map((n) => (
              <Symbol key={n.id} n={n} selected={n.id === selected || n.id === connectFrom} result={result}
                onPointerDown={onNodePointerDown(n)} onClick={onNodeClick(n)} />
            ))}
            {g.nodes.length === 0 && (
              <text x={430} y={235} textAnchor="middle" fontSize={14} fill="var(--muted)">
                Add queues and activities, then connect them with arrows.
              </text>
            )}
          </svg>
          <div>
            {sel ? (
              <div>
                <div className="label">Selected: {sel.kind}</div>
                {field('Label', sel.label, (v) => update(sel.id, { label: v }), 'text')}
                {sel.kind === 'queue' && field('Units at the start', sel.tokens, (v) => update(sel.id, { tokens: Math.max(0, Math.trunc(Number(v) || 0)) }))}
                {sel.kind === 'queue' && field('What the units are (e.g. trucks, loads)', sel.unitLabel ?? '', (v) => update(sel.id, { unitLabel: v || undefined }), 'text')}
                {isActivity(sel.kind) && field('Duration, minutes', sel.duration, (v) => update(sel.id, { duration: Math.max(0.01, Number(v) || 0) }), 'number', 0.1)}
                {sel.kind === 'counter' && field('Quantity counted per completion', sel.units, (v) => update(sel.id, { units: Math.max(0, Number(v) || 0) }), 'number', 0.5)}
                {sel.kind === 'counter' && field('Unit (e.g. LCY, loads)', sel.unitLabel ?? '', (v) => update(sel.id, { unitLabel: v || undefined }), 'text')}
                {sel.kind === 'queue' && (
                  <label style={{ display: 'block', fontSize: '0.8rem', marginTop: '0.5rem' }}>
                    <input type="checkbox" checked={!!sel.resource} onChange={(e) => update(sel.id, { resource: e.target.checked })} /> Report how busy these units are
                  </label>
                )}
                {sel.kind === 'queue' && (
                  <label style={{ display: 'block', fontSize: '0.8rem', marginTop: '0.3rem' }}>
                    <input type="checkbox" checked={!!sel.fleet} onChange={(e) => setG((s) => ({ ...s, nodes: s.nodes.map((n) => ({ ...n, fleet: n.id === sel.id ? e.target.checked : false })) }))} /> This is the fleet to size (production plot sweeps it)
                  </label>
                )}
                {isActivity(sel.kind) && (
                  <button className="ghost" style={{ marginTop: '0.5rem' }}
                    onClick={() => update(sel.id, { kind: sel.kind === 'combi' ? 'normal' : 'combi' })}>
                    Make it {sel.kind === 'combi' ? 'NORMAL' : 'COMBI'}
                  </button>
                )}
              </div>
            ) : (
              <div style={{ fontSize: '0.82rem', color: 'var(--muted)' }}>
                <div className="label">Symbols</div>
                <p style={{ margin: '0.4rem 0' }}><strong>Circle</strong> — a queue: units waiting idle (trucks, the excavator, soil in the bank).</p>
                <p style={{ margin: '0.4rem 0' }}><strong>Box with a cut corner</strong> — a COMBI activity: starts only when every queue feeding it can supply a unit.</p>
                <p style={{ margin: '0.4rem 0' }}><strong>Plain box</strong> — a NORMAL activity: starts as soon as the unit arrives.</p>
                <p style={{ margin: '0.4rem 0' }}><strong>Barred circle</strong> — a counter: tallies production.</p>
                <p style={{ margin: '0.4rem 0' }}>Click a symbol to edit it. Drag to arrange.</p>
              </div>
            )}
          </div>
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.6rem', alignItems: 'center', marginTop: '0.7rem' }}>
          <button className="primary" onClick={run} disabled={compiled.errors.length > 0}>Run the operation</button>
          <label style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>
            for{' '}
            <input type="number" value={horizon} step={60} min={60}
              onChange={(e) => setHorizon(Math.max(60, Number(e.target.value) || 60))}
              style={{ width: '5rem', padding: '0.2rem 0.4rem', border: '1px solid var(--line)', borderRadius: 6, background: 'var(--surface)', color: 'var(--ink)' }} />
            {' '}minutes
          </label>
          {rate !== null && result && (
            <span style={{ fontSize: '0.92rem' }}>
              → <strong className="num">{Math.round(result.produced).toLocaleString()} {counterUnit}</strong> in {(result.endTime / 60).toFixed(1)} h
              {' '}= <strong className="num">{rate.toFixed(0)} {counterUnit}/hr</strong>
              {result.endTime < horizon - 1e-6 && <span style={{ color: 'var(--muted)' }}> · the bank ran out — that is the operation&apos;s duration</span>}
            </span>
          )}
        </div>
        {compiled.errors.length > 0 && (
          <ul style={{ margin: '0.5rem 0 0', paddingLeft: '1.1rem', fontSize: '0.82rem', color: 'var(--caution)' }}>
            {compiled.errors.slice(0, 4).map((e) => <li key={e}>{e}</li>)}
          </ul>
        )}
      </section>

      <section className="card">
        <div className="label" style={{ marginBottom: '0.3rem' }}>Production from the diagram you drew</div>
        <p style={{ fontSize: '0.82rem', color: 'var(--muted)', margin: '0 0 0.5rem' }}>
          The fleet queue is re-sized from 1 to 12 units and the operation re-run each time.
          Past the balance point the rate goes flat while the queue keeps growing — every
          extra unit waits instead of works.
        </p>
        {compiled.errors.length === 0 && compiled.fleetQueue
          ? <Sweep g={g} compiled={compiled} counterUnit={counterUnit} />
          : <div style={{ fontSize: '0.85rem', color: 'var(--muted)' }}>Finish a valid diagram and mark one queue as the fleet to see this plot.</div>}
      </section>
    </div>
  );
}
