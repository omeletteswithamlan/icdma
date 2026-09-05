'use client';

import { useEffect, useMemo, useRef, useState } from 'react';
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
  tokens?: number;      // queues: units at t=0
  fleet?: boolean;      // queues: the fleet the production plot sweeps
  resource?: boolean;   // queues: report utilization
  duration?: number;    // activities: minutes
  step?: number;        // activities: ± step for the on-symbol control
  units?: number;       // counters: quantity per completion
  unitLabel?: string;
}

interface GArc { from: string; to: string }
interface Graph { nodes: GNode[]; arcs: GArc[] }

const isActivity = (k: Kind) => k === 'combi' || k === 'normal';

/** the textbook earthmoving operation, as the student should draw it */
function classicEarthmoving(): Graph {
  return {
    nodes: [
      { id: 'bank', kind: 'queue', label: 'Soil in bank', x: 90, y: 80, tokens: 120, unitLabel: 'loads' },
      { id: 'exc', kind: 'queue', label: 'Excavator idle', x: 90, y: 330, tokens: 1, resource: true },
      { id: 'trucks', kind: 'queue', label: 'Trucks waiting', x: 250, y: 330, tokens: 4, fleet: true, resource: true, unitLabel: 'trucks' },
      { id: 'load', kind: 'combi', label: 'LOAD', x: 250, y: 190, duration: 3.3, step: 0.3 },
      { id: 'haul', kind: 'normal', label: 'HAUL', x: 450, y: 190, duration: 8, step: 1 },
      { id: 'dump', kind: 'normal', label: 'DUMP', x: 640, y: 190, duration: 1.5, step: 0.5 },
      { id: 'fill', kind: 'counter', label: 'Fill placed', x: 800, y: 80, units: 16, unitLabel: 'LCY' },
      { id: 'return', kind: 'normal', label: 'RETURN', x: 640, y: 330, duration: 6, step: 1 },
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

interface Compiled { model: CycleModel; errors: string[]; fleetQueue?: string; outQueue: Map<string, string | null> }

function compile(g: Graph): Compiled {
  const errors: string[] = [];
  const byId = new Map(g.nodes.map((n) => [n.id, n]));
  const model: CycleModel = { queues: [], activities: [] };
  const outQueue = new Map<string, string | null>();

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
  for (const a of g.arcs) {
    const f = byId.get(a.from); const t = byId.get(a.to);
    if (f && t && isActivity(f.kind) && isActivity(t.kind)) model.queues.push({ id: `_${a.from}_${a.to}`, label: '', initial: 0 });
  }
  for (const n of g.nodes) {
    if (!isActivity(n.kind)) continue;
    const takes: { queue: string; n: number }[] = [];
    const gives: { queue: string; n: number }[] = [];
    let produces: number | undefined;
    let primaryOut: string | null = null;
    for (const a of g.arcs) {
      if (a.to === n.id) {
        const f = byId.get(a.from)!;
        takes.push({ queue: f.kind === 'queue' ? f.id : `_${a.from}_${a.to}`, n: 1 });
      }
      if (a.from === n.id) {
        const t = byId.get(a.to)!;
        if (t.kind === 'queue') { gives.push({ queue: t.id, n: 1 }); if (!primaryOut || !t.resource) primaryOut = t.id; }
        else if (t.kind === 'counter') produces = (produces ?? 0) + (t.units ?? 1);
        else { gives.push({ queue: `_${a.from}_${a.to}`, n: 1 }); primaryOut = primaryOut ?? t.id; }
      }
    }
    outQueue.set(n.id, primaryOut);
    if (takes.length === 0) errors.push(`"${n.label}" has no incoming arrow — nothing can start it.`);
    if (n.kind === 'combi' && takes.length < 2) errors.push(`"${n.label}" is a COMBI but draws from only one queue — a COMBI combines two or more resources.`);
    if (n.kind === 'normal' && takes.length > 1) errors.push(`"${n.label}" is a NORMAL activity but draws from ${takes.length} sources — that makes it a COMBI.`);
    model.activities.push({ id: n.id, label: n.label, duration: { kind: 'const', value: n.duration ?? 1 }, takes, gives, produces });
  }
  if (!g.nodes.some((n) => n.kind === 'counter')) errors.push('Add a COUNTER and connect an activity to it so production can be measured.');
  const fleet = g.nodes.find((n) => n.kind === 'queue' && n.fleet)?.id;
  return { model, errors, fleetQueue: fleet, outQueue };
}

/* ------------------------------------------------------------------ */
/* Playback: the run as a movie                                          */
/* ------------------------------------------------------------------ */

interface Instance { activity: string; start: number; end: number }

function instancesOf(r: SimResult): Instance[] {
  const open = new Map<string, number[]>();
  const out: Instance[] = [];
  for (const e of r.events) {
    if (e.type === 'start') { (open.get(e.activity) ?? open.set(e.activity, []).get(e.activity)!).push(e.t); }
    else {
      const q = open.get(e.activity);
      const s = q && q.length ? q.shift()! : e.t;
      out.push({ activity: e.activity, start: s, end: e.t });
    }
  }
  for (const [activity, starts] of open) for (const s of starts) out.push({ activity, start: s, end: r.endTime });
  return out;
}

function countsAt(r: SimResult, t: number): Map<string, number> {
  const m = new Map<string, number>();
  for (const s of r.queueTimeline) { if (s.t <= t) m.set(s.queue, s.count); }
  return m;
}

/* ------------------------------------------------------------------ */
/* Symbols (CYCLONE conventions)                                        */
/* ------------------------------------------------------------------ */

function StepButtons({ onMinus, onPlus, y }: { onMinus: () => void; onPlus: () => void; y: number }) {
  const btn = (x: number, label: string, fn: () => void) => (
    <g transform={`translate(${x},${y})`} onClick={(e) => { e.stopPropagation(); fn(); }}
      onPointerDown={(e) => e.stopPropagation()} style={{ cursor: 'pointer' }} role="button" aria-label={label}>
      <rect x={-11} y={-11} width={22} height={22} rx={4} fill="var(--surface)" stroke="var(--accent)" strokeWidth={1.2} />
      <text y={4.5} textAnchor="middle" fontSize={15} fontWeight={700} fill="var(--accent)" fontFamily="var(--font-display)">{label}</text>
    </g>
  );
  return <>{btn(-24, '−', onMinus)}{btn(24, '+', onPlus)}</>;
}

function Symbol({ n, selected, live, inFlight, busy, fired, produced, editable, onStep, onPointerDown, onClick }: {
  n: GNode; selected: boolean;
  live?: number; inFlight?: number; busy?: number; fired?: number; produced?: number;
  editable: boolean; onStep: (delta: number) => void;
  onPointerDown: (e: React.PointerEvent) => void; onClick: () => void;
}) {
  const stroke = selected ? 'var(--accent)' : 'var(--ink)';
  const sw = selected ? 2.2 : 1.4;
  const common = { onPointerDown, onClick, style: { cursor: 'grab' } as React.CSSProperties };

  if (n.kind === 'queue') {
    const shown = live ?? n.tokens ?? 0;
    return (
      <g transform={`translate(${n.x},${n.y})`} {...common}>
        <circle r={30} fill="var(--surface)" stroke={stroke} strokeWidth={sw} />
        <text y={-1} textAnchor="middle" fontSize={16} fontWeight={700} fontFamily="var(--font-display)" fill={live !== undefined ? 'var(--accent)' : 'var(--ink)'} className="num">{shown}</text>
        <text y={13} textAnchor="middle" fontSize={8.5} fill="var(--muted)">{n.unitLabel ?? (shown === 1 ? 'unit' : 'units')}</text>
        <text y={46} textAnchor="middle" fontSize={11} fill="var(--ink)">{n.label}</text>
        {busy !== undefined && <text y={59} textAnchor="middle" fontSize={9.5} fill="var(--muted)" className="num">busy {Math.round(busy * 100)}%</text>}
        {n.fleet && <text y={busy !== undefined ? 71 : 59} textAnchor="middle" fontSize={9} fill="var(--accent)" fontFamily="var(--font-display)" fontWeight={600}>FLEET</text>}
        {editable && <StepButtons y={-42} onMinus={() => onStep(-1)} onPlus={() => onStep(1)} />}
      </g>
    );
  }
  if (n.kind === 'counter') {
    return (
      <g transform={`translate(${n.x},${n.y})`} {...common}>
        <circle r={26} fill="var(--surface)" stroke={stroke} strokeWidth={sw} />
        <line x1={-26} x2={26} y1={10} y2={10} stroke={stroke} strokeWidth={1.4} />
        <text y={0} textAnchor="middle" fontSize={produced !== undefined ? 11 : 9} fontFamily="var(--font-display)" fontWeight={700} fill={produced !== undefined ? 'var(--accent)' : 'var(--ink)'} className="num">
          {produced !== undefined ? Math.round(produced).toLocaleString() : 'COUNT'}
        </text>
        <text y={42} textAnchor="middle" fontSize={11} fill="var(--ink)">{n.label}</text>
        <text y={55} textAnchor="middle" fontSize={9.5} fill="var(--muted)" className="num">{n.unitLabel ?? ''} · +{n.units ?? 1} per load</text>
      </g>
    );
  }
  const w = 100; const h = 42;
  return (
    <g transform={`translate(${n.x},${n.y})`} {...common}>
      <rect x={-w / 2} y={-h / 2} width={w} height={h} rx={2}
        fill={inFlight ? 'var(--wash-accent)' : 'var(--surface)'} stroke={inFlight ? 'var(--accent)' : stroke} strokeWidth={inFlight ? 2 : sw} />
      {n.kind === 'combi' && <path d={`M${-w / 2} ${-h / 2 + 12} L${-w / 2 + 12} ${-h / 2}`} stroke={inFlight ? 'var(--accent)' : stroke} strokeWidth={sw} fill="none" />}
      <text y={-4} textAnchor="middle" fontSize={11.5} fontWeight={700} fontFamily="var(--font-display)" fill="var(--ink)">{n.label}</text>
      <text y={11} textAnchor="middle" fontSize={10} fill="var(--muted)" className="num">{(n.duration ?? 0).toFixed(1)} min</text>
      <text y={h / 2 + 13} textAnchor="middle" fontSize={9} fill="var(--muted)">
        {n.kind === 'combi' ? 'COMBI' : 'NORMAL'}{inFlight ? ` · ${inFlight} in progress` : fired !== undefined ? ` · ran ${fired}×` : ''}
      </text>
      {editable && <StepButtons y={-h / 2 - 16} onMinus={() => onStep(-(n.step ?? 0.5))} onPlus={() => onStep(n.step ?? 0.5)} />}
    </g>
  );
}

function anchor(n: GNode, towards: { x: number; y: number }): { x: number; y: number } {
  const dx = towards.x - n.x; const dy = towards.y - n.y;
  const len = Math.hypot(dx, dy) || 1;
  const ux = dx / len; const uy = dy / len;
  if (n.kind === 'queue') return { x: n.x + ux * 31, y: n.y + uy * 31 };
  if (n.kind === 'counter') return { x: n.x + ux * 27, y: n.y + uy * 27 };
  const hw = 50; const hh = 21;
  const tx = Math.abs(ux) > 1e-6 ? hw / Math.abs(ux) : Infinity;
  const ty = Math.abs(uy) > 1e-6 ? hh / Math.abs(uy) : Infinity;
  const t = Math.min(tx, ty) + 1;
  return { x: n.x + ux * t, y: n.y + uy * t };
}

function arcPath(f: GNode, t: GNode) {
  const p1 = anchor(f, t); const p2 = anchor(t, f);
  const mx = (p1.x + p2.x) / 2; const my = (p1.y + p2.y) / 2;
  const cx = mx - (p2.y - p1.y) * 0.12; const cy = my + (p2.x - p1.x) * 0.12;
  return { d: `M${p1.x} ${p1.y} Q${cx} ${cy} ${p2.x} ${p2.y}`, p1, p2, c: { x: cx, y: cy } };
}
const onQuad = (p1: { x: number; y: number }, c: { x: number; y: number }, p2: { x: number; y: number }, s: number) => ({
  x: (1 - s) * (1 - s) * p1.x + 2 * (1 - s) * s * c.x + s * s * p2.x,
  y: (1 - s) * (1 - s) * p1.y + 2 * (1 - s) * s * c.y + s * s * p2.y,
});

/* ------------------------------------------------------------------ */
/* Charts                                                               */
/* ------------------------------------------------------------------ */

function LiveProduction({ series, t, endTime, unit, plannedRate }: {
  series: [number, number][]; t: number; endTime: number; unit: string; plannedRate: number;
}) {
  const W = 520; const H = 200; const M = { l: 56, r: 14, t: 16, b: 30 };
  const maxY = Math.max(1, series.length ? series[series.length - 1][1] : 1) * 1.1;
  const x = (v: number) => M.l + (v / Math.max(1, endTime)) * (W - M.l - M.r);
  const y = (v: number) => H - M.b - (v / maxY) * (H - M.t - M.b);
  const shown = series.filter(([tt]) => tt <= t);
  const last = shown.length ? shown[shown.length - 1] : [0, 0];
  return (
    <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: 'auto' }} role="img" aria-label="Cumulative production as the operation runs">
      <text x={M.l} y={11} fontSize={11} fontFamily="var(--font-display)" fontWeight={600} fill="var(--ink)">Production as it happens</text>
      {[0.5, 1].map((f) => <line key={f} x1={M.l} x2={W - M.r} y1={y(maxY * f)} y2={y(maxY * f)} stroke="var(--line)" strokeWidth={0.7} />)}
      {[0.5, 1].map((f) => <text key={f} x={M.l - 6} y={y(maxY * f) + 4} textAnchor="end" fontSize={10} fill="var(--muted)" className="num">{Math.round(maxY * f).toLocaleString()}</text>)}
      <line x1={x(0)} y1={y(0)} x2={x(endTime)} y2={y(plannedRate * endTime / 60)} stroke="var(--muted)" strokeDasharray="4 4" strokeWidth={1} />
      <text x={W - M.r} y={y(plannedRate * endTime / 60) - 5} textAnchor="end" fontSize={9.5} fill="var(--muted)">excavator, never waiting</text>
      <polyline fill="none" stroke="var(--accent)" strokeWidth={2.4}
        points={[[0, 0], ...shown].map(([tt, v]) => `${x(tt)},${y(v)}`).join(' ')} />
      {shown.length > 0 && <circle cx={x(Math.min(t, endTime))} cy={y(last[1])} r={5} fill="var(--accent)" />}
      <line x1={x(Math.min(t, endTime))} x2={x(Math.min(t, endTime))} y1={M.t + 6} y2={H - M.b} stroke="var(--accent)" strokeWidth={0.8} strokeDasharray="2 3" />
      {[0, 0.5, 1].map((f) => <text key={f} x={x(endTime * f)} y={H - 12} textAnchor="middle" fontSize={10} fill="var(--muted)" className="num">{Math.round(endTime * f)}</text>)}
      <text x={(M.l + W - M.r) / 2} y={H - 1} textAnchor="middle" fontSize={10} fill="var(--muted)">minutes</text>
      <text x={W - M.r} y={11} textAnchor="end" fontSize={10} fill="var(--muted)">{unit}</text>
    </svg>
  );
}

function Sweep({ g, compiled, unit }: { g: Graph; compiled: Compiled; unit: string }) {
  const data = useMemo(() => {
    if (compiled.errors.length || !compiled.fleetQueue) return null;
    const out: { n: number; rate: number; waiting: number }[] = [];
    for (let n = 1; n <= 12; n++) {
      const model: CycleModel = { ...compiled.model, queues: compiled.model.queues.map((q) => (q.id === compiled.fleetQueue ? { ...q, initial: n } : q)) };
      const r = simulate(model, { horizon: 480, rng: mulberry32(1) });
      out.push({ n, rate: (r.produced / r.endTime) * 60, waiting: r.avgQueue.get(compiled.fleetQueue) ?? 0 });
    }
    return out;
  }, [compiled]);
  if (!data) return null;
  const fleetNow = g.nodes.find((n) => n.id === compiled.fleetQueue)?.tokens ?? 0;
  let balance = data.length;
  for (let i = 1; i < data.length; i++) if (data[i].rate < data[i - 1].rate * 1.03) { balance = data[i - 1].n; break; }
  const W = 520; const H = 200; const M = { l: 56, r: 14, t: 16, b: 30 };
  const x = (n: number) => M.l + ((n - 1) / 11) * (W - M.l - M.r);
  const chart = (title: string, key: 'rate' | 'waiting', color: string, label: string) => {
    const max = Math.max(key === 'rate' ? 1 : 0.5, ...data.map((d) => d[key])) * 1.12;
    const y = (v: number) => H - M.b - (v / max) * (H - M.t - M.b);
    return (
      <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: 'auto' }} role="img" aria-label={title}>
        <text x={M.l} y={11} fontSize={11} fontFamily="var(--font-display)" fontWeight={600} fill="var(--ink)">{title}</text>
        {[0.5, 1].map((f) => <line key={f} x1={M.l} x2={W - M.r} y1={y(max * f)} y2={y(max * f)} stroke="var(--line)" strokeWidth={0.7} />)}
        {[0.5, 1].map((f) => <text key={f} x={M.l - 6} y={y(max * f) + 4} textAnchor="end" fontSize={10} fill="var(--muted)" className="num">{(max * f).toFixed(key === 'rate' ? 0 : 1)}</text>)}
        <line x1={x(balance)} x2={x(balance)} y1={M.t + 14} y2={H - M.b} stroke="var(--muted)" strokeDasharray="3 3" strokeWidth={1} />
        <text x={x(balance)} y={M.t + 26} textAnchor="middle" fontSize={10} fill="var(--muted)">balance ≈ {balance}</text>
        <polyline fill="none" stroke={color} strokeWidth={2.4} points={data.map((d) => `${x(d.n)},${y(d[key])}`).join(' ')} />
        {data.map((d) => <circle key={d.n} cx={x(d.n)} cy={y(d[key])} r={d.n === fleetNow ? 5.5 : 3} fill={d.n === fleetNow ? color : 'var(--surface)'} stroke={color} strokeWidth={1.6} />)}
        {[1, 4, 8, 12].map((n) => <text key={n} x={x(n)} y={H - 12} textAnchor="middle" fontSize={10} fill="var(--muted)">{n}</text>)}
        <text x={(M.l + W - M.r) / 2} y={H - 1} textAnchor="middle" fontSize={10} fill="var(--muted)">units in the fleet queue</text>
        <text x={W - M.r} y={11} textAnchor="end" fontSize={10} fill="var(--muted)">{label}</text>
      </svg>
    );
  };
  return (
    <>
      <div>{chart('Production rate vs fleet size', 'rate', 'var(--accent)', `${unit}/hr`)}</div>
      <div>{chart('Fleet units waiting vs fleet size', 'waiting', 'var(--caution)', 'avg waiting')}</div>
    </>
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
  const [t, setT] = useState(0);             // playback clock, sim minutes
  const [playing, setPlaying] = useState(false);
  const [speed, setSpeed] = useState(10);    // sim minutes per real second
  const drag = useRef<{ id: string; dx: number; dy: number } | null>(null);
  const svgRef = useRef<SVGSVGElement>(null);
  const nextId = useRef(1);

  const compiled = useMemo(() => compile(g), [g]);
  const byId = useMemo(() => new Map(g.nodes.map((n) => [n.id, n])), [g]);
  const counter = g.nodes.find((n) => n.kind === 'counter');
  const unit = counter?.unitLabel ?? 'units';
  const sel = selected ? byId.get(selected) ?? null : null;

  const instances = useMemo(() => (result ? instancesOf(result) : []), [result]);
  const production = useMemo<[number, number][]>(() => {
    if (!result) return [];
    const produces = new Map(compiled.model.activities.map((a) => [a.id, a.produces ?? 0]));
    let cum = 0; const out: [number, number][] = [];
    for (const e of result.events) if (e.type === 'end' && produces.get(e.activity)) { cum += produces.get(e.activity)!; out.push([e.t, cum]); }
    return out;
  }, [result, compiled]);
  const loadAct = compiled.model.activities.find((a) => a.takes.length >= 2);
  const plannedRate = loadAct && counter ? (60 / Math.max(0.01, loadAct.duration.kind === 'const' ? loadAct.duration.value : 1)) * (counter.units ?? 1) : 0;

  // playback clock — a timer rather than requestAnimationFrame so the run
  // keeps advancing (throttled) when the tab is in the background
  useEffect(() => {
    if (!playing || !result) return;
    let last = performance.now();
    const id = window.setInterval(() => {
      const now = performance.now();
      const dt = Math.min(0.5, (now - last) / 1000); last = now;
      setT((prev) => Math.min(result.endTime, prev + dt * speed));
    }, 33);
    return () => window.clearInterval(id);
  }, [playing, result, speed]);
  useEffect(() => {
    if (playing && result && t >= result.endTime) setPlaying(false);
  }, [t, playing, result]);

  const liveCounts = useMemo(() => (result ? countsAt(result, t) : null), [result, t]);
  const liveInFlight = useMemo(() => {
    const m = new Map<string, Instance[]>();
    if (!result) return m;
    for (const i of instances) if (i.start <= t && t < i.end) (m.get(i.activity) ?? m.set(i.activity, []).get(i.activity)!).push(i);
    return m;
  }, [instances, t, result]);
  const liveProduced = useMemo(() => {
    let v = 0; for (const [tt, c] of production) if (tt <= t) v = c; return v;
  }, [production, t]);
  const finished = !!result && t >= result.endTime - 1e-9;

  const mutate = (fn: (s: Graph) => Graph) => { setG(fn); setResult(null); setPlaying(false); setT(0); };
  const update = (id: string, patch: Partial<GNode>) => mutate((s) => ({ ...s, nodes: s.nodes.map((n) => (n.id === id ? { ...n, ...patch } : n)) }));
  const stepNode = (n: GNode, delta: number) => {
    if (n.kind === 'queue') update(n.id, { tokens: Math.max(0, (n.tokens ?? 0) + delta) });
    else if (isActivity(n.kind)) update(n.id, { duration: Math.max(0.1, Math.round(((n.duration ?? 1) + delta) * 10) / 10) });
  };

  const svgPoint = (e: React.PointerEvent) => {
    const svg = svgRef.current!; const pt = svg.createSVGPoint(); pt.x = e.clientX; pt.y = e.clientY;
    const p = pt.matrixTransform(svg.getScreenCTM()!.inverse()); return { x: p.x, y: p.y };
  };
  const onNodePointerDown = (n: GNode) => (e: React.PointerEvent) => {
    if (mode !== 'move') return;
    const p = svgPoint(e); drag.current = { id: n.id, dx: n.x - p.x, dy: n.y - p.y };
    (e.currentTarget as Element).setPointerCapture?.(e.pointerId);
  };
  const onPointerMove = (e: React.PointerEvent) => {
    if (!drag.current) return;
    const p = svgPoint(e); const { id, dx, dy } = drag.current;
    setG((s) => ({ ...s, nodes: s.nodes.map((n) => (n.id === id ? { ...n, x: Math.round(p.x + dx), y: Math.round(p.y + dy) } : n)) }));
  };
  const onPointerUp = () => { drag.current = null; };
  const onNodeClick = (n: GNode) => () => {
    if (mode === 'move') { setSelected(n.id); return; }
    if (mode === 'delete') { mutate((s) => ({ nodes: s.nodes.filter((x) => x.id !== n.id), arcs: s.arcs.filter((a) => a.from !== n.id && a.to !== n.id) })); setSelected(null); return; }
    if (!connectFrom) { setConnectFrom(n.id); return; }
    if (connectFrom !== n.id && !g.arcs.some((a) => a.from === connectFrom && a.to === n.id)) mutate((s) => ({ ...s, arcs: [...s.arcs, { from: connectFrom, to: n.id }] }));
    setConnectFrom(null);
  };
  const add = (kind: Kind) => {
    const id = `${kind}${nextId.current++}`;
    const base: GNode = { id, kind, label: kind === 'queue' ? 'New queue' : kind === 'counter' ? 'Counter' : kind.toUpperCase(), x: 140 + (nextId.current % 5) * 70, y: 440 };
    if (kind === 'queue') base.tokens = 1;
    if (isActivity(kind)) { base.duration = 2; base.step = 0.5; }
    if (kind === 'counter') { base.units = 1; base.unitLabel = 'units'; }
    mutate((s) => ({ ...s, nodes: [...s.nodes, base] })); setSelected(id); setMode('move');
  };

  const run = () => {
    if (compiled.errors.length) return;
    const r = simulate(compiled.model, { horizon, rng: mulberry32(1) });
    setResult(r); setT(0); setPlaying(true);
  };

  const tool = (label: string, active: boolean, onClick: () => void) => (
    <button className="ghost" onClick={onClick} style={active ? { borderColor: 'var(--accent)', color: 'var(--accent)', background: 'var(--wash-accent)' } : undefined}>{label}</button>
  );
  const field = (label: string, value: number | string | undefined, onChange: (v: string) => void, type: 'number' | 'text' = 'number', step?: number) => (
    <label style={{ display: 'block', fontSize: '0.78rem', color: 'var(--muted)', marginTop: '0.45rem' }}>
      {label}
      <input type={type} value={value ?? ''} step={step} onChange={(e) => onChange(e.target.value)}
        style={{ display: 'block', width: '100%', padding: '0.3rem 0.45rem', fontSize: '0.95rem', marginTop: '0.1rem', border: '1px solid var(--line)', borderRadius: 6, background: 'var(--surface)', color: 'var(--ink)' }} />
    </label>
  );

  const rate = result && finished ? (result.produced / result.endTime) * 60 : null;

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
          {tool(connectFrom ? 'Connect: now pick the target…' : 'Connect arrows', mode === 'connect', () => { setMode('connect'); setConnectFrom(null); })}
          {tool('Delete', mode === 'delete', () => { setMode('delete'); setConnectFrom(null); })}
          <span style={{ width: 1, height: 22, background: 'var(--line)', margin: '0 0.3rem' }} />
          {tool('Load the classic example', false, () => { mutate(() => classicEarthmoving()); setSelected(null); })}
          {tool('Start blank', false, () => { mutate(() => ({ nodes: [], arcs: [] })); setSelected(null); })}
        </div>

        <div style={{ display: 'grid', gap: '0.8rem', gridTemplateColumns: 'minmax(0, 4fr) minmax(14rem, 1.15fr)' }} className="studio-grid">
          <svg ref={svgRef} viewBox="0 0 900 480" onPointerMove={onPointerMove} onPointerUp={onPointerUp} onPointerLeave={onPointerUp}
            style={{ width: '100%', height: 'auto', background: 'var(--bg)', border: '1px solid var(--line)', borderRadius: 6, touchAction: 'none' }}
            role="img" aria-label="Activity cycle diagram canvas">
            <defs>
              <marker id="acd-arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
                <path d="M0 0 L10 5 L0 10 z" fill="var(--ink)" />
              </marker>
            </defs>
            {g.arcs.map((a) => {
              const f = byId.get(a.from); const tt = byId.get(a.to);
              if (!f || !tt) return null;
              const { d } = arcPath(f, tt);
              return (
                <path key={`${a.from}-${a.to}`} d={d} fill="none" stroke="var(--ink)" strokeWidth={1.4} markerEnd="url(#acd-arrow)"
                  onClick={() => { if (mode === 'delete') mutate((s) => ({ ...s, arcs: s.arcs.filter((x) => !(x.from === a.from && x.to === a.to)) })); }}
                  style={{ cursor: mode === 'delete' ? 'pointer' : 'default' }} />
              );
            })}
            {/* units in motion: each in-progress instance travels its activity's outgoing arc */}
            {result && !finished && [...liveInFlight.entries()].flatMap(([actId, list]) => {
              const a = byId.get(actId); const outId = compiled.outQueue.get(actId); const o = outId ? byId.get(outId) : null;
              if (!a || !o) return [];
              const { p1, p2, c } = arcPath(a, o);
              return list.map((inst, k) => {
                const s = Math.min(1, Math.max(0, (t - inst.start) / Math.max(1e-6, inst.end - inst.start)));
                const p = onQuad(p1, c, p2, s);
                return <circle key={`${actId}-${k}`} cx={p.x} cy={p.y} r={6} fill="var(--caution)" stroke="var(--surface)" strokeWidth={1.5} />;
              });
            })}
            {g.nodes.map((n) => (
              <Symbol key={n.id} n={n} selected={n.id === selected || n.id === connectFrom}
                live={result && liveCounts ? liveCounts.get(n.id) ?? n.tokens : undefined}
                inFlight={result && !finished ? liveInFlight.get(n.id)?.length : undefined}
                busy={result && finished && n.resource ? result.utilization.get(n.id) : undefined}
                fired={result && finished ? result.firings.get(n.id) : undefined}
                produced={result && n.kind === 'counter' ? liveProduced : undefined}
                editable={mode === 'move' && (n.kind === 'queue' || isActivity(n.kind)) && !(n.kind === 'queue' && n.resource && !n.fleet && (n.tokens ?? 0) <= 1 && n.id === 'exc')}
                onStep={(d) => stepNode(n, d)}
                onPointerDown={onNodePointerDown(n)} onClick={onNodeClick(n)} />
            ))}
            {g.nodes.length === 0 && <text x={450} y={240} textAnchor="middle" fontSize={14} fill="var(--muted)">Add queues and activities, then connect them with arrows.</text>}
            {result && (
              <text x={890} y={470} textAnchor="end" fontSize={12} fill="var(--muted)" className="num">
                t = {Math.round(t)} min{finished ? ' · done' : ''}
              </text>
            )}
          </svg>

          <div>
            {sel ? (
              <div>
                <div className="label">Selected: {sel.kind}</div>
                {field('Label', sel.label, (v) => update(sel.id, { label: v }), 'text')}
                {sel.kind === 'queue' && field('Units at the start', sel.tokens, (v) => update(sel.id, { tokens: Math.max(0, Math.trunc(Number(v) || 0)) }))}
                {sel.kind === 'queue' && field('What the units are', sel.unitLabel ?? '', (v) => update(sel.id, { unitLabel: v || undefined }), 'text')}
                {isActivity(sel.kind) && field('Duration, minutes', sel.duration, (v) => update(sel.id, { duration: Math.max(0.1, Number(v) || 0) }), 'number', 0.1)}
                {sel.kind === 'counter' && field('Quantity per completion', sel.units, (v) => update(sel.id, { units: Math.max(0, Number(v) || 0) }), 'number', 0.5)}
                {sel.kind === 'counter' && field('Unit (LCY, loads…)', sel.unitLabel ?? '', (v) => update(sel.id, { unitLabel: v || undefined }), 'text')}
                {sel.kind === 'queue' && <label style={{ display: 'block', fontSize: '0.8rem', marginTop: '0.5rem' }}><input type="checkbox" checked={!!sel.resource} onChange={(e) => update(sel.id, { resource: e.target.checked })} /> Report how busy these units are</label>}
                {sel.kind === 'queue' && <label style={{ display: 'block', fontSize: '0.8rem', marginTop: '0.3rem' }}><input type="checkbox" checked={!!sel.fleet} onChange={(e) => mutate((s) => ({ ...s, nodes: s.nodes.map((n) => ({ ...n, fleet: n.id === sel.id ? e.target.checked : false })) }))} /> This is the fleet to size</label>}
                {isActivity(sel.kind) && <button className="ghost" style={{ marginTop: '0.5rem' }} onClick={() => update(sel.id, { kind: sel.kind === 'combi' ? 'normal' : 'combi' })}>Make it {sel.kind === 'combi' ? 'NORMAL' : 'COMBI'}</button>}
                <button className="ghost" style={{ marginTop: '0.5rem', marginLeft: '0.4rem' }} onClick={() => setSelected(null)}>Done</button>
              </div>
            ) : (
              <div style={{ fontSize: '0.82rem', color: 'var(--muted)' }}>
                <div className="label">How to read it</div>
                <p style={{ margin: '0.4rem 0' }}><strong>Circle</strong> — a queue: units waiting idle (trucks, the excavator, soil in the bank).</p>
                <p style={{ margin: '0.4rem 0' }}><strong>Box with a cut corner</strong> — COMBI: starts only when every feeding queue can supply a unit.</p>
                <p style={{ margin: '0.4rem 0' }}><strong>Plain box</strong> — NORMAL: starts as soon as its unit arrives.</p>
                <p style={{ margin: '0.4rem 0' }}><strong>Barred circle</strong> — counter: tallies production.</p>
                <p style={{ margin: '0.4rem 0' }}>Use <strong>− +</strong> on a symbol to change trucks, load time, haul time. Click a symbol for everything else; drag to arrange.</p>
              </div>
            )}
          </div>
        </div>

        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.6rem', alignItems: 'center', marginTop: '0.7rem' }}>
          <button className="primary" onClick={run} disabled={compiled.errors.length > 0}>Simulate</button>
          {result && (
            <>
              <button className="ghost" onClick={() => setPlaying((p) => !p)} disabled={finished}>{playing ? 'Pause' : 'Play'}</button>
              <button className="ghost" onClick={() => { setT(0); setPlaying(true); }}>Replay</button>
              <span style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>speed</span>
              {[5, 10, 30].map((s) => <button key={s} className="ghost" onClick={() => setSpeed(s)} style={speed === s ? { borderColor: 'var(--accent)', color: 'var(--accent)' } : undefined}>{s}×</button>)}
            </>
          )}
          <label style={{ fontSize: '0.8rem', color: 'var(--muted)', marginLeft: 'auto' }}>
            run for{' '}
            <input type="number" value={horizon} step={60} min={60} onChange={(e) => setHorizon(Math.max(60, Number(e.target.value) || 60))}
              style={{ width: '5rem', padding: '0.2rem 0.4rem', border: '1px solid var(--line)', borderRadius: 6, background: 'var(--surface)', color: 'var(--ink)' }} />
            {' '}min
          </label>
        </div>
        {rate !== null && result && (
          <div style={{ fontSize: '0.92rem', marginTop: '0.5rem' }}>
            <strong className="num">{Math.round(result.produced).toLocaleString()} {unit}</strong> in {(result.endTime / 60).toFixed(1)} h
            {' '}= <strong className="num">{rate.toFixed(0)} {unit}/hr</strong>
            {result.endTime < horizon - 1e-6 && <span style={{ color: 'var(--muted)' }}> · the bank ran out at {Math.round(result.endTime)} min — that is the operation&apos;s duration</span>}
          </div>
        )}
        {compiled.errors.length > 0 && (
          <ul style={{ margin: '0.5rem 0 0', paddingLeft: '1.1rem', fontSize: '0.82rem', color: 'var(--caution)' }}>
            {compiled.errors.slice(0, 4).map((e) => <li key={e}>{e}</li>)}
          </ul>
        )}
      </section>

      <section className="card">
        <div className="label" style={{ marginBottom: '0.3rem' }}>What the diagram produces</div>
        <p style={{ fontSize: '0.82rem', color: 'var(--muted)', margin: '0 0 0.5rem' }}>
          Left: this run, drawn as it plays — the dashed line is what the excavator would produce if a truck were always waiting.
          Right: the drawn diagram re-run with the fleet resized 1 to 12; past the balance point the rate flattens while trucks queue.
          Change trucks, load time, or haul time on the diagram and both respond.
        </p>
        <div style={{ display: 'grid', gap: '0.6rem', gridTemplateColumns: 'repeat(3, minmax(0,1fr))' }} className="studio-grid">
          <div>
            {result
              ? <LiveProduction series={production} t={t} endTime={result.endTime} unit={unit} plannedRate={plannedRate} />
              : <div style={{ fontSize: '0.85rem', color: 'var(--muted)', padding: '2rem 0', textAlign: 'center' }}>Press Simulate to see production accumulate.</div>}
          </div>
          {compiled.errors.length === 0 && compiled.fleetQueue
            ? <Sweep g={g} compiled={compiled} unit={unit} />
            : <div style={{ fontSize: '0.85rem', color: 'var(--muted)' }}>Finish a valid diagram and mark one queue as the fleet to see the sweep.</div>}
        </div>
      </section>
    </div>
  );
}
