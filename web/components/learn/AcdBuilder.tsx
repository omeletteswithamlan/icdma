'use client';

import { useEffect, useMemo, useRef, useState } from 'react';
import { simulate, mulberry32, type CycleModel, type SimResult } from 'icdma-engine';

/* ------------------------------------------------------------------ */
/* Graph model — what the student draws                                */
/* ------------------------------------------------------------------ */

export type Kind = 'queue' | 'combi' | 'normal' | 'counter';
export type QueueIcon = 'none' | 'truck' | 'excavator' | 'soil' | 'fill';
export type Carry = 'dot' | 'truckLoaded' | 'truckEmpty';

export interface GNode {
  id: string;
  kind: Kind;
  label: string;
  x: number;
  y: number;
  tokens?: number;      // queues: units at t=0
  fleet?: boolean;      // queues: the fleet the production plot sweeps
  resource?: boolean;   // queues: report utilization
  icon?: QueueIcon;     // queues/counters: how the units are drawn
  duration?: number;    // activities: minutes
  step?: number;        // activities: ± step for the on-symbol control
  carry?: Carry;        // activities: what travels while the activity runs
  stationary?: boolean; // activities: the unit stays at the box (loading, dumping)
  units?: number;       // counters: quantity per completion
  unitLabel?: string;
}

export interface GArc { from: string; to: string }
export interface Graph { nodes: GNode[]; arcs: GArc[] }

const isActivity = (k: Kind) => k === 'combi' || k === 'normal';

/** the textbook earthmoving operation, as the student should draw it */
export function classicEarthmoving(): Graph {
  return {
    nodes: [
      { id: 'bank', kind: 'queue', label: 'Soil in bank', x: 100, y: 84, tokens: 120, unitLabel: 'loads', icon: 'soil' },
      { id: 'exc', kind: 'queue', label: 'Excavator idle', x: 100, y: 340, tokens: 1, resource: true, icon: 'excavator', unitLabel: 'excavator' },
      { id: 'trucks', kind: 'queue', label: 'Trucks waiting', x: 270, y: 340, tokens: 4, fleet: true, resource: true, unitLabel: 'trucks', icon: 'truck' },
      { id: 'load', kind: 'combi', label: 'LOAD', x: 270, y: 200, duration: 3.3, step: 0.3, carry: 'truckLoaded', stationary: true },
      { id: 'haul', kind: 'normal', label: 'HAUL', x: 470, y: 200, duration: 8, step: 1, carry: 'truckLoaded' },
      { id: 'dump', kind: 'normal', label: 'DUMP', x: 660, y: 200, duration: 1.5, step: 0.5, carry: 'truckLoaded', stationary: true },
      { id: 'fill', kind: 'counter', label: 'Fill placed', x: 820, y: 84, units: 16, unitLabel: 'LCY', icon: 'fill' },
      { id: 'return', kind: 'normal', label: 'RETURN', x: 660, y: 340, duration: 6, step: 1, carry: 'truckEmpty' },
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

export interface Compiled { model: CycleModel; errors: string[]; fleetQueue?: string; outNode: Map<string, string | null> }

export function compile(g: Graph): Compiled {
  const errors: string[] = [];
  const byId = new Map(g.nodes.map((n) => [n.id, n]));
  const model: CycleModel = { queues: [], activities: [] };
  const outNode = new Map<string, string | null>();

  for (const a of g.arcs) {
    const f = byId.get(a.from); const t = byId.get(a.to);
    if (!f || !t) continue;
    const ok = (f.kind === 'queue' && isActivity(t.kind))
      || (isActivity(f.kind) && (t.kind === 'queue' || t.kind === 'counter' || isActivity(t.kind)));
    if (!ok) errors.push(`An arrow from "${f.label}" to "${t.label}" is not allowed — arrows run queue → activity → queue (or counter).`);
  }
  for (const n of g.nodes) if (n.kind === 'queue') model.queues.push({ id: n.id, label: n.label, initial: n.tokens ?? 0, resource: n.resource });
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
      if (a.to === n.id) { const f = byId.get(a.from)!; takes.push({ queue: f.kind === 'queue' ? f.id : `_${a.from}_${a.to}`, n: 1 }); }
      if (a.from === n.id) {
        const t = byId.get(a.to)!;
        if (t.kind === 'queue') { gives.push({ queue: t.id, n: 1 }); if (!primaryOut || !t.resource) primaryOut = t.id; }
        else if (t.kind === 'counter') produces = (produces ?? 0) + (t.units ?? 1);
        else { gives.push({ queue: `_${a.from}_${a.to}`, n: 1 }); primaryOut = primaryOut ?? t.id; }
      }
    }
    outNode.set(n.id, primaryOut);
    if (takes.length === 0) errors.push(`"${n.label}" has no incoming arrow — nothing can start it.`);
    if (n.kind === 'combi' && takes.length < 2) errors.push(`"${n.label}" is a COMBI but draws from only one queue — a COMBI combines two or more resources.`);
    if (n.kind === 'normal' && takes.length > 1) errors.push(`"${n.label}" is a NORMAL activity but draws from ${takes.length} sources — that makes it a COMBI.`);
    model.activities.push({ id: n.id, label: n.label, duration: { kind: 'const', value: n.duration ?? 1 }, takes, gives, produces });
  }
  if (!g.nodes.some((n) => n.kind === 'counter')) errors.push('Add a COUNTER and connect an activity to it so production can be measured.');
  const fleet = g.nodes.find((n) => n.kind === 'queue' && n.fleet)?.id;
  return { model, errors, fleetQueue: fleet, outNode };
}

/* ------------------------------------------------------------------ */
/* Playback helpers                                                     */
/* ------------------------------------------------------------------ */

interface Instance { activity: string; start: number; end: number }

function instancesOf(r: SimResult): Instance[] {
  const open = new Map<string, number[]>();
  const out: Instance[] = [];
  for (const e of r.events) {
    if (e.type === 'start') (open.get(e.activity) ?? open.set(e.activity, []).get(e.activity)!).push(e.t);
    else { const q = open.get(e.activity); out.push({ activity: e.activity, start: q && q.length ? q.shift()! : e.t, end: e.t }); }
  }
  for (const [activity, starts] of open) for (const s of starts) out.push({ activity, start: s, end: r.endTime });
  return out;
}

function countsAt(r: SimResult, t: number): Map<string, number> {
  const m = new Map<string, number>();
  for (const s of r.queueTimeline) if (s.t <= t) m.set(s.queue, s.count);
  return m;
}

/* ------------------------------------------------------------------ */
/* Pictograms                                                           */
/* ------------------------------------------------------------------ */

function Truck({ x, y, loaded, scale = 1, flip = false }: { x: number; y: number; loaded: boolean; scale?: number; flip?: boolean }) {
  return (
    <g transform={`translate(${x},${y}) scale(${flip ? -scale : scale},${scale})`}>
      <rect x={-14} y={-9} width={19} height={9} fill="var(--surface)" stroke="var(--ink)" strokeWidth={1.2} />
      {loaded && <path d="M-13 -9 Q-5 -17 4 -9 Z" fill="var(--caution)" stroke="var(--ink)" strokeWidth={0.8} />}
      <path d="M5 -8 h7 l3 4 v4 h-10 z" fill="var(--accent)" stroke="var(--ink)" strokeWidth={1.2} />
      <circle cx={-9} cy={1.5} r={2.6} fill="var(--ink)" />
      <circle cx={-2} cy={1.5} r={2.6} fill="var(--ink)" />
      <circle cx={10} cy={1.5} r={2.6} fill="var(--ink)" />
    </g>
  );
}

function Excavator({ x, y, scale = 1 }: { x: number; y: number; scale?: number }) {
  return (
    <g transform={`translate(${x},${y}) scale(${scale})`}>
      <rect x={-13} y={0} width={26} height={6} rx={3} fill="var(--ink)" />
      <rect x={-9} y={-9} width={14} height={9} rx={1.5} fill="var(--caution)" stroke="var(--ink)" strokeWidth={1} />
      <path d="M2 -6 L14 -16 L22 -8" fill="none" stroke="var(--ink)" strokeWidth={2} strokeLinecap="round" />
      <path d="M22 -8 l2 6 l-6 1 z" fill="var(--caution)" stroke="var(--ink)" strokeWidth={0.8} />
    </g>
  );
}

function Mound({ x, y, w, h, tone }: { x: number; y: number; w: number; h: number; tone: 'soil' | 'fill' }) {
  return (
    <path d={`M${x - w / 2} ${y} Q${x} ${y - h * 2} ${x + w / 2} ${y} Z`}
      fill={tone === 'soil' ? 'var(--caution)' : 'var(--good)'} opacity={0.85} stroke="var(--ink)" strokeWidth={0.8} />
  );
}

/* ------------------------------------------------------------------ */
/* Symbols (CYCLONE conventions)                                        */
/* ------------------------------------------------------------------ */

function StepButtons({ onMinus, onPlus, y }: { onMinus: () => void; onPlus: () => void; y: number }) {
  const btn = (x: number, label: string, fn: () => void) => (
    <g transform={`translate(${x},${y})`} onClick={(e) => { e.stopPropagation(); fn(); }} onPointerDown={(e) => e.stopPropagation()}
      style={{ cursor: 'pointer' }} role="button" aria-label={label}>
      <rect x={-11} y={-11} width={22} height={22} rx={4} fill="var(--surface)" stroke="var(--accent)" strokeWidth={1.2} />
      <text y={4.5} textAnchor="middle" fontSize={15} fontWeight={700} fill="var(--accent)" fontFamily="var(--font-display)">{label}</text>
    </g>
  );
  return <>{btn(-24, '−', onMinus)}{btn(24, '+', onPlus)}</>;
}

function Symbol({ n, selected, live, inFlight, busy, fired, produced, producedMax, editable, onStep, onPointerDown, onClick }: {
  n: GNode; selected: boolean;
  live?: number; inFlight?: number; busy?: number; fired?: number; produced?: number; producedMax?: number;
  editable: boolean; onStep: (delta: number) => void;
  onPointerDown: (e: React.PointerEvent) => void; onClick: () => void;
}) {
  const stroke = selected ? 'var(--accent)' : 'var(--ink)';
  const sw = selected ? 2.2 : 1.4;
  const common = { onPointerDown, onClick, style: { cursor: 'grab' } as React.CSSProperties };

  if (n.kind === 'queue') {
    const shown = live ?? n.tokens ?? 0;
    const icon = n.icon ?? 'none';
    return (
      <g transform={`translate(${n.x},${n.y})`} {...common}>
        <circle r={34} fill="var(--surface)" stroke={stroke} strokeWidth={sw} />
        {icon === 'truck' && (
          <g>
            {Array.from({ length: Math.min(shown, 3) }).map((_, i) => <Truck key={i} x={-14 + i * 14} y={-4 - i * 3} loaded={false} scale={0.55} />)}
            <text y={17} textAnchor="middle" fontSize={13} fontWeight={700} fontFamily="var(--font-display)" fill={live !== undefined ? 'var(--accent)' : 'var(--ink)'} className="num">{shown}</text>
          </g>
        )}
        {icon === 'excavator' && (
          <g>
            <Excavator x={-4} y={2} scale={0.9} />
            <text y={22} textAnchor="middle" fontSize={11} fontWeight={700} fontFamily="var(--font-display)" fill={live !== undefined ? 'var(--accent)' : 'var(--ink)'} className="num">{shown > 0 ? 'idle' : 'working'}</text>
          </g>
        )}
        {icon === 'soil' && (
          <g>
            <Mound x={0} y={8} w={46} h={8 + 12 * Math.min(1, shown / Math.max(1, n.tokens ?? 1))} tone="soil" />
            <text y={22} textAnchor="middle" fontSize={13} fontWeight={700} fontFamily="var(--font-display)" fill={live !== undefined ? 'var(--accent)' : 'var(--ink)'} className="num">{shown}</text>
          </g>
        )}
        {(icon === 'none' || icon === 'fill') && (
          <>
            <text y={-1} textAnchor="middle" fontSize={16} fontWeight={700} fontFamily="var(--font-display)" fill={live !== undefined ? 'var(--accent)' : 'var(--ink)'} className="num">{shown}</text>
            <text y={13} textAnchor="middle" fontSize={8.5} fill="var(--muted)">{n.unitLabel ?? (shown === 1 ? 'unit' : 'units')}</text>
          </>
        )}
        <text y={50} textAnchor="middle" fontSize={11} fill="var(--ink)">{n.label}</text>
        {busy !== undefined && <text y={63} textAnchor="middle" fontSize={9.5} fill="var(--muted)" className="num">busy {Math.round(busy * 100)}%</text>}
        {n.fleet && <text y={busy !== undefined ? 75 : 63} textAnchor="middle" fontSize={9} fill="var(--accent)" fontFamily="var(--font-display)" fontWeight={600}>FLEET</text>}
        {editable && <StepButtons y={-46} onMinus={() => onStep(-1)} onPlus={() => onStep(1)} />}
      </g>
    );
  }
  if (n.kind === 'counter') {
    const frac = producedMax ? Math.min(1, (produced ?? 0) / producedMax) : 0;
    return (
      <g transform={`translate(${n.x},${n.y})`} {...common}>
        <circle r={30} fill="var(--surface)" stroke={stroke} strokeWidth={sw} />
        <line x1={-30} x2={30} y1={12} y2={12} stroke={stroke} strokeWidth={1.4} />
        {n.icon === 'fill'
          ? <Mound x={0} y={11} w={44} h={3 + 14 * frac} tone="fill" />
          : <text y={-2} textAnchor="middle" fontSize={9} fontFamily="var(--font-display)" fontWeight={600} fill="var(--ink)">COUNT</text>}
        <text y={24} textAnchor="middle" fontSize={11} fontWeight={700} fontFamily="var(--font-display)" fill={produced !== undefined ? 'var(--accent)' : 'var(--ink)'} className="num">
          {produced !== undefined ? Math.round(produced).toLocaleString() : ''}
        </text>
        <text y={46} textAnchor="middle" fontSize={11} fill="var(--ink)">{n.label}</text>
        <text y={59} textAnchor="middle" fontSize={9.5} fill="var(--muted)" className="num">{n.unitLabel ?? ''} · +{n.units ?? 1} per load</text>
      </g>
    );
  }
  const w = 100; const h = 42;
  return (
    <g transform={`translate(${n.x},${n.y})`} {...common}>
      <rect x={-w / 2} y={-h / 2} width={w} height={h} rx={2} fill={inFlight ? 'var(--wash-accent)' : 'var(--surface)'} stroke={inFlight ? 'var(--accent)' : stroke} strokeWidth={inFlight ? 2 : sw} />
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
  const len = Math.hypot(dx, dy) || 1; const ux = dx / len; const uy = dy / len;
  if (n.kind === 'queue') return { x: n.x + ux * 35, y: n.y + uy * 35 };
  if (n.kind === 'counter') return { x: n.x + ux * 31, y: n.y + uy * 31 };
  const hw = 50; const hh = 21;
  const tx = Math.abs(ux) > 1e-6 ? hw / Math.abs(ux) : Infinity;
  const ty = Math.abs(uy) > 1e-6 ? hh / Math.abs(uy) : Infinity;
  const t = Math.min(tx, ty) + 1;
  return { x: n.x + ux * t, y: n.y + uy * t };
}

function arcPath(f: GNode, t: GNode) {
  const p1 = anchor(f, t); const p2 = anchor(t, f);
  const mx = (p1.x + p2.x) / 2; const my = (p1.y + p2.y) / 2;
  const c = { x: mx - (p2.y - p1.y) * 0.12, y: my + (p2.x - p1.x) * 0.12 };
  return { d: `M${p1.x} ${p1.y} Q${c.x} ${c.y} ${p2.x} ${p2.y}`, p1, p2, c };
}
const onQuad = (p1: { x: number; y: number }, c: { x: number; y: number }, p2: { x: number; y: number }, s: number) => ({
  x: (1 - s) ** 2 * p1.x + 2 * (1 - s) * s * c.x + s * s * p2.x,
  y: (1 - s) ** 2 * p1.y + 2 * (1 - s) * s * c.y + s * s * p2.y,
});

/* ------------------------------------------------------------------ */
/* Charts — axes marked the way students must learn to mark them        */
/* ------------------------------------------------------------------ */

function Axes({ W, H, M, xTicks, yTicks, xLabel, yLabel, xs, ys, fmtX, fmtY }: {
  W: number; H: number; M: { l: number; r: number; t: number; b: number };
  xTicks: number[]; yTicks: number[]; xLabel: string; yLabel: string;
  xs: (v: number) => number; ys: (v: number) => number;
  fmtX?: (v: number) => string; fmtY?: (v: number) => string;
}) {
  const fx = fmtX ?? ((v) => String(v)); const fy = fmtY ?? ((v) => Math.round(v).toLocaleString());
  return (
    <g>
      {yTicks.map((v) => <line key={`g${v}`} x1={M.l} x2={W - M.r} y1={ys(v)} y2={ys(v)} stroke="var(--line)" strokeWidth={0.7} />)}
      <line x1={M.l} x2={W - M.r} y1={H - M.b} y2={H - M.b} stroke="var(--ink)" strokeWidth={1.2} />
      <line x1={M.l} x2={M.l} y1={M.t} y2={H - M.b} stroke="var(--ink)" strokeWidth={1.2} />
      {xTicks.map((v) => (
        <g key={`x${v}`}>
          <line x1={xs(v)} x2={xs(v)} y1={H - M.b} y2={H - M.b + 5} stroke="var(--ink)" strokeWidth={1} />
          <text x={xs(v)} y={H - M.b + 18} textAnchor="middle" fontSize={12} fill="var(--ink)" className="num">{fx(v)}</text>
        </g>
      ))}
      {yTicks.map((v) => (
        <g key={`y${v}`}>
          <line x1={M.l - 5} x2={M.l} y1={ys(v)} y2={ys(v)} stroke="var(--ink)" strokeWidth={1} />
          <text x={M.l - 8} y={ys(v) + 4} textAnchor="end" fontSize={12} fill="var(--ink)" className="num">{fy(v)}</text>
        </g>
      ))}
      <text x={(M.l + W - M.r) / 2} y={H - 4} textAnchor="middle" fontSize={13} fontWeight={600} fontFamily="var(--font-display)" fill="var(--ink)">{xLabel}</text>
      <text transform={`translate(14,${(M.t + H - M.b) / 2}) rotate(-90)`} textAnchor="middle" fontSize={13} fontWeight={600} fontFamily="var(--font-display)" fill="var(--ink)">{yLabel}</text>
    </g>
  );
}

function niceTicks(max: number, n = 4): number[] {
  if (max <= 0) return [0];
  const raw = max / n; const pow = 10 ** Math.floor(Math.log10(raw));
  const step = [1, 2, 2.5, 5, 10].map((m) => m * pow).find((s) => s >= raw) ?? raw;
  const out: number[] = []; for (let v = 0; v <= max + 1e-9; v += step) out.push(Number(v.toFixed(6))); return out;
}

function LiveProduction({ series, t, endTime, unit, plannedRate }: { series: [number, number][]; t: number; endTime: number; unit: string; plannedRate: number }) {
  const W = 560; const H = 300; const M = { l: 74, r: 18, t: 30, b: 52 };
  const maxY = Math.max(1, series.length ? series[series.length - 1][1] : 1, plannedRate * endTime / 60) * 1.05;
  const yTicks = niceTicks(maxY); const xTicks = niceTicks(endTime, 5);
  const xs = (v: number) => M.l + (v / Math.max(1, endTime)) * (W - M.l - M.r);
  const ys = (v: number) => H - M.b - (v / (yTicks[yTicks.length - 1] || 1)) * (H - M.t - M.b);
  const shown = series.filter(([tt]) => tt <= t);
  const last = shown.length ? shown[shown.length - 1] : [0, 0];
  return (
    <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: 'auto' }} role="img" aria-label="Cumulative production versus time for this run">
      <text x={M.l} y={16} fontSize={14} fontFamily="var(--font-display)" fontWeight={700} fill="var(--ink)">Cumulative production, this run</text>
      <Axes W={W} H={H} M={M} xTicks={xTicks} yTicks={yTicks} xs={xs} ys={ys} xLabel="Time, minutes" yLabel={`Production, ${unit}`} />
      <line x1={xs(0)} y1={ys(0)} x2={xs(endTime)} y2={ys(plannedRate * endTime / 60)} stroke="var(--muted)" strokeDasharray="5 4" strokeWidth={1.3} />
      <text x={xs(endTime * 0.55)} y={ys(plannedRate * endTime * 0.55 / 60) - 8} fontSize={11} fill="var(--muted)">excavator never waiting</text>
      <polyline fill="none" stroke="var(--accent)" strokeWidth={2.6} points={[[0, 0], ...shown].map(([tt, v]) => `${xs(tt)},${ys(v)}`).join(' ')} />
      {shown.length > 0 && <circle cx={xs(Math.min(t, endTime))} cy={ys(last[1])} r={5} fill="var(--accent)" />}
    </svg>
  );
}

function SweepChart({ data, fleetNow, balance, metric, title, yLabel, color }: {
  data: { n: number; rate: number; waiting: number }[]; fleetNow: number; balance: number;
  metric: 'rate' | 'waiting'; title: string; yLabel: string; color: string;
}) {
  const W = 560; const H = 300; const M = { l: 74, r: 18, t: 30, b: 52 };
  const yTicks = niceTicks(Math.max(metric === 'rate' ? 1 : 0.5, ...data.map((d) => d[metric])) * 1.05);
  const yMax = yTicks[yTicks.length - 1] || 1;
  const xs = (n: number) => M.l + ((n - 1) / 11) * (W - M.l - M.r);
  const ys = (v: number) => H - M.b - (v / yMax) * (H - M.t - M.b);
  return (
    <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: 'auto' }} role="img" aria-label={title}>
      <text x={M.l} y={16} fontSize={14} fontFamily="var(--font-display)" fontWeight={700} fill="var(--ink)">{title}</text>
      <Axes W={W} H={H} M={M} xTicks={[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]} yTicks={yTicks} xs={xs} ys={ys}
        xLabel="Number of units in the fleet" yLabel={yLabel} fmtY={(v) => (metric === 'rate' ? Math.round(v).toLocaleString() : v.toFixed(1))} />
      <line x1={xs(balance)} x2={xs(balance)} y1={M.t + 6} y2={H - M.b} stroke="var(--muted)" strokeDasharray="3 3" strokeWidth={1} />
      <text x={xs(balance)} y={M.t + 2} textAnchor="middle" fontSize={11} fill="var(--muted)">balance ≈ {balance}</text>
      <polyline fill="none" stroke={color} strokeWidth={2.6} points={data.map((d) => `${xs(d.n)},${ys(d[metric])}`).join(' ')} />
      {data.map((d) => <circle key={d.n} cx={xs(d.n)} cy={ys(d[metric])} r={d.n === fleetNow ? 6 : 3.5} fill={d.n === fleetNow ? color : 'var(--surface)'} stroke={color} strokeWidth={1.8} />)}
      {data.filter((d) => d.n === fleetNow).map((d) => (
        <text key="now" x={xs(d.n)} y={ys(d[metric]) - 12} textAnchor="middle" fontSize={11} fontWeight={600} fill={color} className="num">
          {metric === 'rate' ? Math.round(d.rate).toLocaleString() : d.waiting.toFixed(1)}
        </text>
      ))}
    </svg>
  );
}

function useSweep(compiled: Compiled) {
  return useMemo(() => {
    if (compiled.errors.length || !compiled.fleetQueue) return null;
    const out: { n: number; rate: number; waiting: number }[] = [];
    for (let n = 1; n <= 12; n++) {
      const model: CycleModel = { ...compiled.model, queues: compiled.model.queues.map((q) => (q.id === compiled.fleetQueue ? { ...q, initial: n } : q)) };
      const r = simulate(model, { horizon: 480, rng: mulberry32(1) });
      out.push({ n, rate: (r.produced / r.endTime) * 60, waiting: r.avgQueue.get(compiled.fleetQueue) ?? 0 });
    }
    return out;
  }, [compiled]);
}

/* ------------------------------------------------------------------ */
/* The builder                                                          */
/* ------------------------------------------------------------------ */

type Mode = 'move' | 'connect' | 'delete';

export default function AcdBuilder({ variant, initial, onSnapshot }: {
  /** explore: the classic example with on-symbol controls only; build: the full drawing tool */
  variant: 'explore' | 'build';
  initial?: Graph;
  onSnapshot?: (graph: Graph, errors: string[]) => void;
}) {
  const [g, setG] = useState<Graph>(() => initial ?? (variant === 'explore' ? classicEarthmoving() : { nodes: [], arcs: [] }));
  const [mode, setMode] = useState<Mode>('move');
  const [selected, setSelected] = useState<string | null>(null);
  const [connectFrom, setConnectFrom] = useState<string | null>(null);
  const [result, setResult] = useState<SimResult | null>(null);
  const [horizon, setHorizon] = useState(480);
  const [t, setT] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [speed, setSpeed] = useState(10);
  const drag = useRef<{ id: string; dx: number; dy: number } | null>(null);
  const svgRef = useRef<SVGSVGElement>(null);
  const nextId = useRef(1);

  const compiled = useMemo(() => compile(g), [g]);
  const byId = useMemo(() => new Map(g.nodes.map((n) => [n.id, n])), [g]);
  const counter = g.nodes.find((n) => n.kind === 'counter');
  const unit = counter?.unitLabel ?? 'units';
  const sel = selected ? byId.get(selected) ?? null : null;
  const sweep = useSweep(compiled);

  useEffect(() => { onSnapshot?.(g, compiled.errors); }, [g, compiled, onSnapshot]);

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
  const producedMax = result ? result.produced : undefined;

  useEffect(() => {
    if (!playing || !result) return;
    let last = performance.now();
    const id = window.setInterval(() => {
      const now = performance.now(); const dt = Math.min(0.5, (now - last) / 1000); last = now;
      setT((prev) => Math.min(result.endTime, prev + dt * speed));
    }, 33);
    return () => window.clearInterval(id);
  }, [playing, result, speed]);
  useEffect(() => { if (playing && result && t >= result.endTime) setPlaying(false); }, [t, playing, result]);

  const liveCounts = useMemo(() => (result ? countsAt(result, t) : null), [result, t]);
  const liveInFlight = useMemo(() => {
    const m = new Map<string, Instance[]>();
    if (!result) return m;
    for (const i of instances) if (i.start <= t && t < i.end) (m.get(i.activity) ?? m.set(i.activity, []).get(i.activity)!).push(i);
    return m;
  }, [instances, t, result]);
  const liveProduced = useMemo(() => { let v = 0; for (const [tt, c] of production) if (tt <= t) v = c; return v; }, [production, t]);
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
    const base: GNode = { id, kind, label: kind === 'queue' ? 'New queue' : kind === 'counter' ? 'Counter' : kind.toUpperCase(), x: 140 + (nextId.current % 5) * 80, y: 430 };
    if (kind === 'queue') base.tokens = 1;
    if (isActivity(kind)) { base.duration = 2; base.step = 0.5; }
    if (kind === 'counter') { base.units = 1; base.unitLabel = 'units'; }
    mutate((s) => ({ ...s, nodes: [...s.nodes, base] })); setSelected(id); setMode('move');
  };
  const run = () => { if (compiled.errors.length) return; setResult(simulate(compiled.model, { horizon, rng: mulberry32(1) })); setT(0); setPlaying(true); };

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
  const select = (label: string, value: string, opts: [string, string][], onChange: (v: string) => void) => (
    <label style={{ display: 'block', fontSize: '0.78rem', color: 'var(--muted)', marginTop: '0.45rem' }}>
      {label}
      <select value={value} onChange={(e) => onChange(e.target.value)} style={{ display: 'block', width: '100%', marginTop: '0.1rem', padding: '0.3rem', border: '1px solid var(--line)', borderRadius: 6, background: 'var(--surface)', color: 'var(--ink)' }}>
        {opts.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
      </select>
    </label>
  );

  const rate = result && finished ? (result.produced / result.endTime) * 60 : null;
  const fleetNow = g.nodes.find((n) => n.id === compiled.fleetQueue)?.tokens ?? 0;
  let balance = 12;
  if (sweep) for (let i = 1; i < sweep.length; i++) if (sweep[i].rate < sweep[i - 1].rate * 1.03) { balance = sweep[i - 1].n; break; }

  const editableNode = (n: GNode) => mode === 'move' && (n.kind === 'queue' || isActivity(n.kind)) && !(variant === 'explore' && n.kind === 'queue' && n.icon === 'excavator');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem' }}>
      <section className="card">
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem', alignItems: 'center', marginBottom: '0.5rem' }}>
          <span className="label" style={{ marginRight: '0.4rem' }}>{variant === 'explore' ? 'The operation' : 'Draw the operation'}</span>
          {variant === 'build' && (
            <>
              {tool('+ Queue', false, () => add('queue'))}
              {tool('+ COMBI', false, () => add('combi'))}
              {tool('+ NORMAL', false, () => add('normal'))}
              {tool('+ Counter', false, () => add('counter'))}
              <span style={{ width: 1, height: 22, background: 'var(--line)', margin: '0 0.3rem' }} />
              {tool('Move / edit', mode === 'move', () => { setMode('move'); setConnectFrom(null); })}
              {tool(connectFrom ? 'Connect: now pick the target…' : 'Connect arrows', mode === 'connect', () => { setMode('connect'); setConnectFrom(null); })}
              {tool('Delete', mode === 'delete', () => { setMode('delete'); setConnectFrom(null); })}
              <span style={{ width: 1, height: 22, background: 'var(--line)', margin: '0 0.3rem' }} />
              {tool('Clear', false, () => { mutate(() => ({ nodes: [], arcs: [] })); setSelected(null); })}
            </>
          )}
          {variant === 'explore' && tool('Reset the example', false, () => { mutate(() => classicEarthmoving()); setSelected(null); })}
        </div>

        <div style={{ display: 'grid', gap: '0.8rem', gridTemplateColumns: variant === 'build' ? 'minmax(0, 4fr) minmax(13rem, 1.1fr)' : '1fr' }} className="studio-grid">
          <svg ref={svgRef} viewBox="0 0 920 470" onPointerMove={onPointerMove} onPointerUp={onPointerUp} onPointerLeave={onPointerUp}
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
              return (
                <path key={`${a.from}-${a.to}`} d={arcPath(f, tt).d} fill="none" stroke="var(--ink)" strokeWidth={1.4} markerEnd="url(#acd-arrow)"
                  onClick={() => { if (mode === 'delete') mutate((s) => ({ ...s, arcs: s.arcs.filter((x) => !(x.from === a.from && x.to === a.to)) })); }}
                  style={{ cursor: mode === 'delete' ? 'pointer' : 'default' }} />
              );
            })}
            {g.nodes.map((n) => (
              <Symbol key={n.id} n={n} selected={n.id === selected || n.id === connectFrom}
                live={result && liveCounts ? liveCounts.get(n.id) ?? n.tokens : undefined}
                inFlight={result && !finished ? liveInFlight.get(n.id)?.length : undefined}
                busy={result && finished && n.resource ? result.utilization.get(n.id) : undefined}
                fired={result && finished ? result.firings.get(n.id) : undefined}
                produced={result && n.kind === 'counter' ? liveProduced : undefined}
                producedMax={producedMax}
                editable={editableNode(n)} onStep={(d) => stepNode(n, d)}
                onPointerDown={onNodePointerDown(n)} onClick={onNodeClick(n)} />
            ))}
            {/* units in motion, drawn as what they are */}
            {result && !finished && [...liveInFlight.entries()].flatMap(([actId, list]) => {
              const a = byId.get(actId); const outId = compiled.outNode.get(actId); const o = outId ? byId.get(outId) : null;
              if (!a) return [];
              const carry = a.carry ?? 'dot';
              return list.map((inst, k) => {
                const s = Math.min(1, Math.max(0, (t - inst.start) / Math.max(1e-6, inst.end - inst.start)));
                let p = { x: a.x + (k - (list.length - 1) / 2) * 16, y: a.y + 40 };
                if (!a.stationary && o) { const { p1, p2, c } = arcPath(a, o); p = onQuad(p1, c, p2, s); p.y -= 8; }
                if (carry === 'dot') return <circle key={`${actId}-${k}`} cx={p.x} cy={p.y} r={6} fill="var(--caution)" stroke="var(--surface)" strokeWidth={1.5} />;
                return <Truck key={`${actId}-${k}`} x={p.x} y={p.y} loaded={carry === 'truckLoaded'} scale={0.9} flip={!a.stationary && o ? o.x < a.x : false} />;
              });
            })}
            {g.nodes.length === 0 && <text x={460} y={235} textAnchor="middle" fontSize={14} fill="var(--muted)">Add queues and activities, then connect them with arrows.</text>}
            {result && <text x={910} y={460} textAnchor="end" fontSize={12} fill="var(--muted)" className="num">t = {Math.round(t)} min{finished ? ' · done' : ''}</text>}
          </svg>

          {variant === 'build' && (
            <div>
              {sel ? (
                <div>
                  <div className="label">Selected: {sel.kind}</div>
                  {field('Label', sel.label, (v) => update(sel.id, { label: v }), 'text')}
                  {sel.kind === 'queue' && field('Units at the start', sel.tokens, (v) => update(sel.id, { tokens: Math.max(0, Math.trunc(Number(v) || 0)) }))}
                  {sel.kind === 'queue' && field('What the units are', sel.unitLabel ?? '', (v) => update(sel.id, { unitLabel: v || undefined }), 'text')}
                  {sel.kind === 'queue' && select('Draw the units as', sel.icon ?? 'none', [['none', 'a number'], ['truck', 'trucks'], ['excavator', 'an excavator'], ['soil', 'a soil bank']], (v) => update(sel.id, { icon: v as QueueIcon }))}
                  {isActivity(sel.kind) && field('Duration, minutes', sel.duration, (v) => update(sel.id, { duration: Math.max(0.1, Number(v) || 0) }), 'number', 0.1)}
                  {isActivity(sel.kind) && select('While it runs, show', sel.carry ?? 'dot', [['dot', 'a moving unit'], ['truckLoaded', 'a loaded truck'], ['truckEmpty', 'an empty truck']], (v) => update(sel.id, { carry: v as Carry }))}
                  {isActivity(sel.kind) && <label style={{ display: 'block', fontSize: '0.8rem', marginTop: '0.4rem' }}><input type="checkbox" checked={!!sel.stationary} onChange={(e) => update(sel.id, { stationary: e.target.checked })} /> The unit stays put here (loading, dumping)</label>}
                  {sel.kind === 'counter' && field('Quantity per completion', sel.units, (v) => update(sel.id, { units: Math.max(0, Number(v) || 0) }), 'number', 0.5)}
                  {sel.kind === 'counter' && field('Unit (LCY, loads…)', sel.unitLabel ?? '', (v) => update(sel.id, { unitLabel: v || undefined }), 'text')}
                  {sel.kind === 'counter' && select('Draw it as', sel.icon ?? 'none', [['none', 'a count'], ['fill', 'a growing fill']], (v) => update(sel.id, { icon: v as QueueIcon }))}
                  {sel.kind === 'queue' && <label style={{ display: 'block', fontSize: '0.8rem', marginTop: '0.5rem' }}><input type="checkbox" checked={!!sel.resource} onChange={(e) => update(sel.id, { resource: e.target.checked })} /> Report how busy these units are</label>}
                  {sel.kind === 'queue' && <label style={{ display: 'block', fontSize: '0.8rem', marginTop: '0.3rem' }}><input type="checkbox" checked={!!sel.fleet} onChange={(e) => mutate((s) => ({ ...s, nodes: s.nodes.map((n) => ({ ...n, fleet: n.id === sel.id ? e.target.checked : false })) }))} /> This is the fleet to size</label>}
                  {isActivity(sel.kind) && <button className="ghost" style={{ marginTop: '0.5rem' }} onClick={() => update(sel.id, { kind: sel.kind === 'combi' ? 'normal' : 'combi' })}>Make it {sel.kind === 'combi' ? 'NORMAL' : 'COMBI'}</button>}
                  <button className="ghost" style={{ marginTop: '0.5rem', marginLeft: '0.4rem' }} onClick={() => setSelected(null)}>Done</button>
                </div>
              ) : (
                <div style={{ fontSize: '0.82rem', color: 'var(--muted)' }}>
                  <div className="label">How to read it</div>
                  <p style={{ margin: '0.4rem 0' }}><strong>Circle</strong> — a queue: units waiting idle.</p>
                  <p style={{ margin: '0.4rem 0' }}><strong>Box with a cut corner</strong> — COMBI: starts only when every feeding queue can supply a unit.</p>
                  <p style={{ margin: '0.4rem 0' }}><strong>Plain box</strong> — NORMAL: starts as soon as its unit arrives.</p>
                  <p style={{ margin: '0.4rem 0' }}><strong>Barred circle</strong> — counter: tallies production.</p>
                  <p style={{ margin: '0.4rem 0' }}>Click a symbol to edit it; drag to arrange; <strong>− +</strong> changes counts and times in place.</p>
                </div>
              )}
            </div>
          )}
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
        {compiled.errors.length > 0 && variant === 'build' && (
          <ul style={{ margin: '0.5rem 0 0', paddingLeft: '1.1rem', fontSize: '0.82rem', color: 'var(--caution)' }}>
            {compiled.errors.slice(0, 4).map((e) => <li key={e}>{e}</li>)}
          </ul>
        )}
      </section>

      {(result || sweep) && (
        <section className="card">
          <div className="label" style={{ marginBottom: '0.3rem' }}>What the diagram produces</div>
          <p style={{ fontSize: '0.85rem', color: 'var(--muted)', margin: '0 0 0.6rem' }}>
            The run you just watched, then the same diagram re-run with the fleet resized 1 to 12. Past the
            balance point the rate flattens while units wait in the queue. Change trucks, load time, or haul
            time on the diagram and every plot responds.
          </p>
          <div style={{ display: 'grid', gap: '0.8rem', gridTemplateColumns: 'repeat(auto-fit, minmax(19rem, 1fr))' }}>
            {result
              ? <LiveProduction series={production} t={t} endTime={result.endTime} unit={unit} plannedRate={plannedRate} />
              : <div style={{ fontSize: '0.85rem', color: 'var(--muted)', alignSelf: 'center', textAlign: 'center' }}>Press Simulate to draw this run.</div>}
            {sweep && <SweepChart data={sweep} fleetNow={fleetNow} balance={balance} metric="rate" title="Production rate vs fleet size" yLabel={`Production rate, ${unit} per hour`} color="var(--accent)" />}
            {sweep && <SweepChart data={sweep} fleetNow={fleetNow} balance={balance} metric="waiting" title="Units waiting vs fleet size" yLabel="Average units waiting in queue" color="var(--caution)" />}
          </div>
        </section>
      )}
    </div>
  );
}
