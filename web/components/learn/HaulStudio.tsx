'use client';

import { useMemo, useRef, useState } from 'react';
import Link from 'next/link';
import {
  SURFACES, SYNTHETIC_HAULER, solveTrip, rimpullCurve, rimpullAvailable, gearAt, travelTimeCurves, grossWeightLb,
  LB_PER_TON_PER_PCT, RIMPULL_CONST, FT_PER_MIN_PER_MPH, topSpeedMph, type Segment, type SegmentSolution, type TripSolution,
} from 'icdma-engine';
import { Axes, niceTicks } from './AcdBuilder';
import PartHeader from './PartHeader';
import { TAKEAWAYS } from '../../lib/takeaways';

const M = SYNTHETIC_HAULER;

/** the haul from Module 1's cut to its fill, as a profile a student can redraw */
export const DEFAULT_PROFILE: Segment[] = [
  { label: 'Cut floor', lengthFt: 600, gradePct: 0, surface: 'rutted' },
  { label: 'Ramp out of the cut', lengthFt: 1200, gradePct: 6, surface: 'rutted' },
  { label: 'Haul road', lengthFt: 7500, gradePct: 1, surface: 'firm' },
  { label: 'Down onto the fill', lengthFt: 1000, gradePct: -4, surface: 'firm' },
  { label: 'Across the fill', lengthFt: 500, gradePct: 0, surface: 'loose' },
];
export const DEFAULT_LIMIT = 25;

const n0 = (v: number) => (Number.isFinite(v) ? v.toLocaleString(undefined, { maximumFractionDigits: 0 }) : '—');
const n1 = (v: number) => (Number.isFinite(v) ? v.toLocaleString(undefined, { minimumFractionDigits: 1, maximumFractionDigits: 1 }) : '—');
const n2 = (v: number) => (Number.isFinite(v) ? v.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '—');
const WHY: Record<SegmentSolution['limit'], string> = {
  power: 'power governs', traction: 'wheels spin — no traction', 'top-speed': 'top gear', 'speed-limit': 'posted limit', stalled: 'stalls',
};

const inputStyle: React.CSSProperties = { padding: '0.28rem 0.4rem', fontSize: '0.95rem', border: '1px solid var(--line)', borderRadius: 6, background: 'var(--surface)', color: 'var(--ink)' };

/* ------------------------------------------------------------------ */
/* The profile, drawn                                                   */
/* ------------------------------------------------------------------ */

function ProfileSvg({ profile, selected, onSelect }: { profile: Segment[]; selected: number; onSelect: (i: number) => void }) {
  const W = 920; const H = 300; const Mg = { l: 64, r: 20, t: 40, b: 46 };
  const pts: { x: number; y: number }[] = [{ x: 0, y: 0 }];
  for (const s of profile) { const last = pts[pts.length - 1]; pts.push({ x: last.x + s.lengthFt, y: last.y + (s.lengthFt * s.gradePct) / 100 }); }
  const totalFt = Math.max(1, pts[pts.length - 1].x);
  const ys = pts.map((p) => p.y); const yMin = Math.min(0, ...ys); const yMax = Math.max(10, ...ys);
  const xs = (ft: number) => Mg.l + (ft / totalFt) * (W - Mg.l - Mg.r);
  const yss = (el: number) => H - Mg.b - ((el - yMin) / Math.max(1, yMax - yMin)) * (H - Mg.t - Mg.b);
  const xTicks = niceTicks(totalFt, 6);
  const yTicks = niceTicks(yMax - yMin, 4).map((v) => v + yMin);
  return (
    <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: 'auto' }} role="img" aria-label="Haul road profile: elevation against distance from the cut">
      <text x={Mg.l} y={18} fontSize={14} fontFamily="var(--font-display)" fontWeight={700} fill="var(--ink)">The haul road, cut to fill (vertical scale exaggerated)</text>
      <Axes W={W} H={H} M={Mg} xTicks={xTicks} yTicks={yTicks} xs={xs} ys={yss} xLabel="Distance from the cut, feet" yLabel="Elevation, feet" fmtY={(v) => Math.round(v).toString()} />
      <path d={`M${xs(0)} ${H - Mg.b} ${pts.map((p) => `L${xs(p.x)} ${yss(p.y)}`).join(' ')} L${xs(totalFt)} ${H - Mg.b} Z`} fill="var(--caution)" opacity={0.18} />
      {profile.map((s, i) => {
        const a = pts[i]; const b = pts[i + 1];
        const mx = (xs(a.x) + xs(b.x)) / 2; const my = (yss(a.y) + yss(b.y)) / 2;
        const sel = i === selected;
        return (
          <g key={i} onClick={() => onSelect(i)} style={{ cursor: 'pointer' }}>
            <line x1={xs(a.x)} y1={yss(a.y)} x2={xs(b.x)} y2={yss(b.y)} stroke={sel ? 'var(--accent)' : 'var(--ink)'} strokeWidth={sel ? 4 : 2.5} strokeLinecap="round" />
            <line x1={xs(b.x)} y1={Mg.t} x2={xs(b.x)} y2={H - Mg.b} stroke="var(--line)" strokeDasharray="3 3" />
            <text x={mx} y={my - 12} textAnchor="middle" fontSize={11} fontWeight={600} fontFamily="var(--font-display)" fill={sel ? 'var(--accent)' : 'var(--ink)'}>
              {s.label ?? `Segment ${i + 1}`}
            </text>
            <text x={mx} y={my + 22} textAnchor="middle" fontSize={10.5} fill="var(--muted)" className="num">
              {n0(s.lengthFt)} ft · {s.gradePct > 0 ? '+' : ''}{s.gradePct}% · {SURFACES.find((x) => x.id === s.surface)?.rollingLbPerTon} lb/ton
            </text>
          </g>
        );
      })}
      <text x={xs(0)} y={yss(0) - 8} fontSize={10.5} fill="var(--muted)">cut</text>
      <text x={xs(totalFt)} y={yss(pts[pts.length - 1].y) - 8} textAnchor="end" fontSize={10.5} fill="var(--muted)">fill</text>
    </svg>
  );
}

/* ------------------------------------------------------------------ */
/* The rimpull curve, with the segments on it                            */
/* ------------------------------------------------------------------ */

function RimpullChart({ haul, back, selected, speedLimit }: { haul: TripSolution; back: TripSolution; selected: number; speedLimit: number }) {
  const W = 560; const H = 340; const Mg = { l: 84, r: 18, t: 36, b: 52 };
  const top = topSpeedMph(M);
  const xMax = Math.ceil(top / 5) * 5;
  const yTicks = niceTicks(M.maxRimpullLb * 1.05);
  const yMax = yTicks[yTicks.length - 1];
  const xs = (v: number) => Mg.l + (v / xMax) * (W - Mg.l - Mg.r);
  const ys = (v: number) => H - Mg.b - (Math.max(0, v) / yMax) * (H - Mg.t - Mg.b);
  const curve = useMemo(() => rimpullCurve(M, 160), []);
  const [hover, setHover] = useState<number | null>(null);
  const svgRef = useRef<SVGSVGElement>(null);
  const onMove = (e: React.MouseEvent) => {
    const r = svgRef.current!.getBoundingClientRect();
    const v = ((e.clientX - r.left) / r.width * W - Mg.l) / (W - Mg.l - Mg.r) * xMax;
    setHover(v > 0.3 && v <= top ? v : null);
  };
  const sel = haul.segments[selected];
  const selBack = back.segments[back.segments.length - 1 - selected];
  return (
    <svg ref={svgRef} viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: 'auto' }} role="img" aria-label="Rimpull against speed for the synthetic hauler, with each haul segment's operating point"
      onMouseMove={onMove} onMouseLeave={() => setHover(null)}>
      <text x={Mg.l} y={16} fontSize={14} fontFamily="var(--font-display)" fontWeight={700} fill="var(--ink)">Rimpull curve — {M.name}</text>
      <Axes W={W} H={H} M={Mg} xTicks={niceTicks(xMax, 8)} yTicks={yTicks} xs={xs} ys={ys} xLabel="Speed, miles per hour" yLabel="Rimpull available, lb" />
      {/* gear bands */}
      {M.gears.map((g, i) => {
        const x0 = i === 0 ? 0 : M.gears[i - 1].maxMph;
        return (
          <g key={g.n}>
            <line x1={xs(g.maxMph)} x2={xs(g.maxMph)} y1={Mg.t} y2={H - Mg.b} stroke="var(--line)" strokeDasharray="2 3" />
            <text x={xs((x0 + g.maxMph) / 2)} y={Mg.t + 12} textAnchor="middle" fontSize={10} fill="var(--muted)" fontFamily="var(--font-display)" fontWeight={600}>{g.n}</text>
          </g>
        );
      })}
      <text x={xs(0.3)} y={Mg.t + 24} fontSize={9.5} fill="var(--muted)">gear</text>
      {/* posted limit */}
      <line x1={xs(speedLimit)} x2={xs(speedLimit)} y1={Mg.t + 28} y2={H - Mg.b} stroke="var(--muted)" strokeWidth={1.2} strokeDasharray="6 3" />
      <text x={xs(speedLimit) + 4} y={Mg.t + 40} fontSize={10.5} fill="var(--muted)">posted limit {speedLimit} mph</text>
      {/* traction limit for the selected segment */}
      {sel && sel.usableLb < yMax && (
        <>
          <line x1={Mg.l} x2={W - Mg.r} y1={ys(sel.usableLb)} y2={ys(sel.usableLb)} stroke="var(--caution)" strokeDasharray="5 4" strokeWidth={1.3} />
          <text x={W - Mg.r - 4} y={ys(sel.usableLb) - 5} textAnchor="end" fontSize={10.5} fill="var(--caution)">usable rimpull, loaded on {sel.surface.label.toLowerCase().split(',')[0]}: {n0(sel.usableLb)} lb</text>
        </>
      )}
      {/* the curve */}
      <polyline fill="none" stroke="var(--ink)" strokeWidth={2.4} points={curve.map(([v, r]) => `${xs(v)},${ys(r)}`).join(' ')} />
      {/* selected segment crosshair */}
      {sel && sel.mph > 0 && (
        <g>
          <line x1={Mg.l} x2={xs(sel.mph)} y1={ys(sel.requiredLb)} y2={ys(sel.requiredLb)} stroke="var(--accent)" strokeDasharray="4 3" />
          <line x1={xs(sel.mph)} x2={xs(sel.mph)} y1={ys(sel.requiredLb)} y2={H - Mg.b} stroke="var(--accent)" strokeDasharray="4 3" />
        </g>
      )}
      {/* operating points: loaded filled, empty hollow */}
      {haul.segments.map((s, i) => s.mph > 0 && (
        <g key={`h${i}`}>
          <circle cx={xs(s.mph)} cy={ys(s.requiredLb)} r={i === selected ? 7 : 5} fill="var(--accent)" stroke="var(--surface)" strokeWidth={1.5} />
          <text x={xs(s.mph) + 8} y={ys(s.requiredLb) - 6} fontSize={10.5} fontWeight={600} fill="var(--accent)" className="num">{i + 1}</text>
        </g>
      ))}
      {back.segments.map((s, j) => s.mph > 0 && (
        <circle key={`b${j}`} cx={xs(s.mph)} cy={ys(s.requiredLb)} r={s === selBack ? 6 : 4.5} fill="var(--surface)" stroke="var(--caution)" strokeWidth={2} />
      ))}
      {/* hover readout */}
      {hover !== null && (
        <g>
          <line x1={xs(hover)} x2={xs(hover)} y1={Mg.t + 28} y2={H - Mg.b} stroke="var(--muted)" strokeWidth={1} />
          <circle cx={xs(hover)} cy={ys(rimpullAvailable(M, hover))} r={4} fill="var(--ink)" />
          <rect x={Math.min(xs(hover) + 8, W - Mg.r - 190)} y={H - Mg.b - 62} width={182} height={44} rx={4} fill="var(--surface)" stroke="var(--line)" />
          <text x={Math.min(xs(hover) + 14, W - Mg.r - 184)} y={H - Mg.b - 45} fontSize={11} fill="var(--ink)" className="num">{n1(hover)} mph → gear {gearAt(M, hover).n}</text>
          <text x={Math.min(xs(hover) + 14, W - Mg.r - 184)} y={H - Mg.b - 29} fontSize={11} fill="var(--ink)" className="num">{n0(rimpullAvailable(M, hover))} lb of rimpull</text>
        </g>
      )}
      <g transform={`translate(${W - Mg.r - 150}, ${H - Mg.b - 100})`}>
        <circle cx={6} cy={0} r={5} fill="var(--accent)" /><text x={16} y={4} fontSize={10.5} fill="var(--ink)">loaded, by segment</text>
        <circle cx={6} cy={16} r={4.5} fill="var(--surface)" stroke="var(--caution)" strokeWidth={2} /><text x={16} y={20} fontSize={10.5} fill="var(--ink)">empty return</text>
      </g>
    </svg>
  );
}

/* ------------------------------------------------------------------ */
/* Travel time against distance, a family of effective grades           */
/* ------------------------------------------------------------------ */

function TravelTimeChart({ haul, speedLimit }: { haul: TripSolution; speedLimit: number }) {
  const W = 560; const H = 340; const Mg = { l: 74, r: 44, t: 36, b: 52 };
  const grades = [0, 2, 4, 6, 8, 10, 12, 15];
  const maxFt = Math.max(4000, Math.ceil(Math.max(...haul.segments.map((s) => s.lengthFt)) / 1000) * 1000);
  const curves = useMemo(() => travelTimeCurves(M, true, grades, maxFt, 2, { speedLimitMph: speedLimit }), [maxFt, speedLimit]); // eslint-disable-line react-hooks/exhaustive-deps
  const finiteMax = Math.max(1, ...curves.map((c) => c.points[2][1]).filter(Number.isFinite), ...haul.segments.map((s) => s.minutes).filter(Number.isFinite));
  const yTicks = niceTicks(Math.min(finiteMax, 30) * 1.05);
  const yMax = yTicks[yTicks.length - 1];
  const xs = (ft: number) => Mg.l + (ft / maxFt) * (W - Mg.l - Mg.r);
  const ys = (min: number) => H - Mg.b - (Math.min(min, yMax) / yMax) * (H - Mg.t - Mg.b);
  return (
    <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: 'auto' }} role="img" aria-label="Travel time against distance for a range of effective grades, loaded">
      <text x={Mg.l} y={16} fontSize={14} fontFamily="var(--font-display)" fontWeight={700} fill="var(--ink)">Travel time, loaded, by effective grade</text>
      <Axes W={W} H={H} M={Mg} xTicks={niceTicks(maxFt, 5)} yTicks={yTicks} xs={xs} ys={ys} xLabel="Distance, feet" yLabel="Travel time, minutes" fmtY={(v) => v.toFixed(0)} />
      {curves.map((c) => {
        if (!Number.isFinite(c.points[2][1])) return null;
        const endMin = c.points[2][1]; const clipped = endMin > yMax;
        const endFt = clipped ? (yMax / endMin) * maxFt : maxFt;
        return (
          <g key={c.effectiveGradePct}>
            <line x1={xs(0)} y1={ys(0)} x2={xs(endFt)} y2={ys(Math.min(endMin, yMax))} stroke="var(--muted)" strokeWidth={1.3} />
            <text x={xs(endFt) + 4} y={ys(Math.min(endMin, yMax)) + 4} fontSize={10.5} fill="var(--muted)" className="num">{c.effectiveGradePct}%</text>
          </g>
        );
      })}
      {haul.segments.map((s, i) => Number.isFinite(s.minutes) && (
        <g key={i}>
          <circle cx={xs(s.lengthFt)} cy={ys(s.minutes)} r={5.5} fill="var(--accent)" stroke="var(--surface)" strokeWidth={1.5} />
          <text x={xs(s.lengthFt) + 8} y={ys(s.minutes) - 6} fontSize={10.5} fontWeight={600} fill="var(--accent)" className="num">{i + 1}</text>
        </g>
      ))}
      <text x={W - Mg.r} y={Mg.t + 12} textAnchor="end" fontSize={10.5} fill="var(--muted)">lines: effective grade · dots: your loaded segments</text>
    </svg>
  );
}

/* ------------------------------------------------------------------ */
/* The studio                                                           */
/* ------------------------------------------------------------------ */

export default function HaulStudio() {
  const [profile, setProfile] = useState<Segment[]>(DEFAULT_PROFILE);
  const [speedLimit, setSpeedLimit] = useState(DEFAULT_LIMIT);
  const [selected, setSelected] = useState(1);

  const haul = useMemo(() => solveTrip(M, profile, true, { speedLimitMph: speedLimit }), [profile, speedLimit]);
  const back = useMemo(() => solveTrip(M, profile, false, { speedLimitMph: speedLimit }), [profile, speedLimit]);
  const sel = haul.segments[Math.min(selected, haul.segments.length - 1)];
  const changed = JSON.stringify(profile) !== JSON.stringify(DEFAULT_PROFILE) || speedLimit !== DEFAULT_LIMIT;

  const update = (i: number, patch: Partial<Segment>) => setProfile((p) => p.map((s, k) => (k === i ? { ...s, ...patch } : s)));
  const remove = (i: number) => { setProfile((p) => p.filter((_, k) => k !== i)); setSelected(0); };
  const add = () => { setProfile((p) => [...p, { label: `Segment ${p.length + 1}`, lengthFt: 1000, gradePct: 0, surface: 'firm' }]); setSelected(profile.length); };

  const tag = (name: string) => (
    <span style={{ display: 'inline-block', fontSize: '0.7rem', fontFamily: 'var(--font-display)', fontWeight: 600, color: 'var(--accent)', background: 'var(--wash-accent)', borderRadius: 4, padding: '0.05rem 0.4rem', marginRight: '0.45rem', verticalAlign: 'middle', whiteSpace: 'nowrap' }}
      title={TAKEAWAYS[5].text}>T6 · {name}</span>
  );
  const num = (s: string) => <strong className="num">{s}</strong>;
  const tons = grossWeightLb(M, true) / 2000; const tonsE = grossWeightLb(M, false) / 2000;
  const hp = RIMPULL_CONST * M.ratedHp * M.drivetrainEff;
  const haulMin = Math.round(haul.minutes * 10) / 10; const backMin = Math.round(back.minutes * 10) / 10;

  const trip = (t: TripSolution, title: string, offset: (i: number) => number) => (
    <div style={{ overflowX: 'auto' }}>
      <table style={{ borderCollapse: 'collapse', width: '100%', fontSize: '0.82rem' }}>
        <thead>
          <tr style={{ textAlign: 'left', color: 'var(--muted)' }}>
            <th style={{ padding: '0.25rem 0.4rem' }}>{title}</th>
            <th style={{ padding: '0.25rem 0.4rem' }} title="rolling + grade, lb per ton">lb/ton</th>
            <th style={{ padding: '0.25rem 0.4rem' }}>eff. grade</th>
            <th style={{ padding: '0.25rem 0.4rem' }} title="total resistance × gross tons">required</th>
            <th style={{ padding: '0.25rem 0.4rem' }}>speed</th>
            <th style={{ padding: '0.25rem 0.4rem' }}>gear</th>
            <th style={{ padding: '0.25rem 0.4rem' }}>min</th>
            <th style={{ padding: '0.25rem 0.4rem' }}>why</th>
          </tr>
        </thead>
        <tbody>
          {t.segments.map((s, i) => {
            const idx = offset(i);
            return (
              <tr key={i} onClick={() => setSelected(idx)} style={{ cursor: 'pointer', background: idx === selected ? 'var(--wash-accent)' : undefined, borderTop: '1px solid var(--line)' }}>
                <td style={{ padding: '0.25rem 0.4rem' }}><span className="num" style={{ color: 'var(--muted)' }}>{idx + 1}</span> {s.label} <span className="num" style={{ color: 'var(--muted)' }}>{s.gradePct > 0 ? '+' : ''}{s.gradePct}%</span></td>
                <td className="num" style={{ padding: '0.25rem 0.4rem' }}>{n0(s.totalLbPerTon)}</td>
                <td className="num" style={{ padding: '0.25rem 0.4rem' }}>{n1(s.effectiveGradePct)}%</td>
                <td className="num" style={{ padding: '0.25rem 0.4rem' }}>{n0(s.requiredLb)} lb</td>
                <td className="num" style={{ padding: '0.25rem 0.4rem' }}>{s.mph > 0 ? `${n1(s.mph)} mph` : '—'}</td>
                <td className="num" style={{ padding: '0.25rem 0.4rem' }}>{s.mph > 0 ? s.gear.n : '—'}</td>
                <td className="num" style={{ padding: '0.25rem 0.4rem', fontWeight: 600 }}>{n2(s.minutes)}</td>
                <td style={{ padding: '0.25rem 0.4rem', color: s.limit === 'traction' || s.limit === 'stalled' ? 'var(--caution)' : 'var(--muted)' }}>{WHY[s.limit]}</td>
              </tr>
            );
          })}
          <tr style={{ borderTop: '2px solid var(--ink)', fontWeight: 600 }}>
            <td style={{ padding: '0.3rem 0.4rem' }}>{n0(t.lengthFt)} ft</td>
            <td colSpan={3} />
            <td className="num" style={{ padding: '0.3rem 0.4rem' }}>{t.stalled ? '—' : `${n1(t.avgMph)} mph avg`}</td>
            <td />
            <td className="num" style={{ padding: '0.3rem 0.4rem' }}>{n2(t.minutes)}</td>
            <td />
          </tr>
        </tbody>
      </table>
    </div>
  );

  return (
    <>
      {/* ---------------------------------------------------------------- the problem */}
      <section className="card" style={{ marginBottom: '0.9rem' }}>
        <div style={{ display: 'grid', gap: '1rem', gridTemplateColumns: 'minmax(0, 3fr) minmax(0, 2fr)', alignItems: 'start' }} className="studio-grid">
          <div>
            <div className="label" style={{ marginBottom: '0.4rem' }}>The problem</div>
            <p style={{ fontSize: '1.05rem', margin: '0 0 0.6rem', fontFamily: 'var(--font-display)', fontWeight: 600 }}>
              Size the haul. Module 1 assumed the trucks take 8 minutes to reach the fill and 6 to come back.
              Where do those numbers come from?
            </p>
            <p style={{ fontSize: '0.95rem', margin: '0 0 0.6rem' }}>
              The loaded trucks leave the cut floor, climb a ramp, run the haul road along the alignment, drop onto
              the fill, and cross it to the dump. Each stretch has a length, a grade, and a surface. The machine is a
              {' '}{M.name.toLowerCase()}: {n0(M.emptyLb)} lb empty, {n0(M.payloadLb)} lb of payload, {M.ratedHp} hp,
              {' '}{M.gears.length} gears to {topSpeedMph(M)} mph, on a haul road posted at {speedLimit} mph.
            </p>
            <p style={{ fontSize: '0.95rem', margin: 0 }}>
              <strong>Decide:</strong> the resistance on each segment, the rimpull the truck needs, the gear and speed the
              curve allows, whether the tires can use that power, and the travel time loaded and empty. Then hand the
              times to Module 1.
            </p>
          </div>
          <div>
            <img src="/media/haul-107.jpg" alt="A loaded tandem-axle dump truck on the haul road of the I-69 reconstruction"
              style={{ width: '100%', borderRadius: 6, border: '1px solid var(--line)' }} />
            <p style={{ fontSize: '0.78rem', color: 'var(--muted)', margin: '0.35rem 0 0' }}>
              On the haul on I-69. Photograph: Amlan Mukherjee, Michigan DOT project.
            </p>
          </div>
        </div>

        <div style={{ borderTop: '1px solid var(--line)', marginTop: '1rem', paddingTop: '0.9rem' }}>
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'baseline', gap: '0.6rem', marginBottom: '0.5rem' }}>
            <div className="label">Worked out, step by step</div>
            <span style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>
              For segment {selected + 1}, &ldquo;{sel.label}&rdquo; — click any segment to work it instead.
              {changed && <span style={{ color: 'var(--accent)' }}> The numbers follow the profile as you have drawn it.</span>}
            </span>
          </div>
          <ol style={{ margin: 0, paddingLeft: '1.3rem', fontSize: '0.92rem', lineHeight: 1.55, columns: '2 22rem', columnGap: '2rem' }}>
            <li style={{ breakInside: 'avoid', marginBottom: '0.55rem', paddingRight: '0.5rem' }}>
              {tag('Resistances')}<strong>Rolling, grade, effective grade.</strong> On {sel.surface.label.toLowerCase()} the tires cost
              {' '}{num(`${sel.rollingLbPerTon} lb/ton`)}. A {sel.gradePct}% grade costs {LB_PER_TON_PER_PCT} lb/ton per percent = {num(`${n0(sel.gradeLbPerTon)} lb/ton`)}.
              Total {num(`${n0(sel.totalLbPerTon)} lb/ton`)}, which is an effective grade of {n0(sel.totalLbPerTon)} ÷ {LB_PER_TON_PER_PCT} = {num(`${n1(sel.effectiveGradePct)}%`)}:
              the flat-road equivalent of this stretch.
            </li>
            <li style={{ breakInside: 'avoid', marginBottom: '0.55rem', paddingRight: '0.5rem' }}>
              {tag('Power required')}<strong>Rimpull the truck needs.</strong> Loaded it weighs {n0(M.emptyLb)} + {n0(M.payloadLb)} = {num(`${n0(grossWeightLb(M, true))} lb`)} = {n1(tons)} tons,
              so it must pull {n1(tons)} × {n0(sel.totalLbPerTon)} = {num(`${n0(sel.requiredLb)} lb`)}{sel.requiredLb <= 0 ? ' — downhill, gravity does the work' : ''}.
            </li>
            <li style={{ breakInside: 'avoid', marginBottom: '0.55rem', paddingRight: '0.5rem' }}>
              {tag('Available power')}<strong>Gear and speed from the curve.</strong> Rimpull = 375 × {M.ratedHp} hp × {M.drivetrainEff} ÷ speed = {n0(hp)} ÷ speed.
              {sel.requiredLb > 0
                ? <> Setting that equal to {n0(sel.requiredLb)} lb gives {num(`${n1(hp / sel.requiredLb)} mph`)}{sel.limit === 'speed-limit' ? <>, more than the posted {speedLimit} mph, so the truck runs at {num(`${speedLimit} mph`)}</> : sel.limit === 'top-speed' ? <>, beyond top gear, so it runs at {num(`${topSpeedMph(M)} mph`)}</> : null}
                  {sel.mph > 0 && <>, in gear {num(`${sel.gear.n}`)}</>}.</>
                : <> With nothing to pull it runs at the road&apos;s limit, {num(`${n1(sel.mph)} mph`)}, gear {sel.gear.n}.</>}
            </li>
            <li style={{ breakInside: 'avoid', marginBottom: '0.55rem', paddingRight: '0.5rem' }}>
              {tag('Usable power')}<strong>Can the tires use it?</strong> Traction on {sel.surface.label.toLowerCase().split(',')[0]} is {sel.surface.tractionCoef} × {n0(grossWeightLb(M, true))} = {num(`${n0(sel.usableLb)} lb`)}.
              {sel.requiredLb > sel.usableLb
                ? <> That is less than the {n0(sel.requiredLb)} lb required: the wheels spin and the truck does not climb. More horsepower would not help; a better surface or a flatter ramp would.</>
                : <> More than the {n0(Math.max(0, sel.requiredLb))} lb required, so power, not traction, sets the speed here.</>}
            </li>
            <li style={{ breakInside: 'avoid', marginBottom: '0.55rem', paddingRight: '0.5rem' }}>
              {tag('Travel time')}<strong>Minutes per segment, then the trip.</strong> {n0(sel.lengthFt)} ft ÷ ({n1(sel.mph)} mph × {FT_PER_MIN_PER_MPH} ft/min per mph) = {num(`${n2(sel.minutes)} min`)}.
              Summed over the profile the loaded haul takes {num(`${n1(haul.minutes)} min`)}; the empty return, {n1(tonsE)} tons with the grades reversed, takes {num(`${n1(back.minutes)} min`)}.
            </li>
            <li style={{ breakInside: 'avoid', marginBottom: '0.55rem', paddingRight: '0.5rem' }}>
              {tag('Into the cycle')}<strong>Back to Module 1.</strong> Haul {n1(haul.minutes)} and return {n1(back.minutes)} replace the 8 and 6 that were assumed; the
              truck cycle, the balance point and the fleet all follow.
            </li>
          </ol>
        </div>
      </section>

      <PartHeader part="Part A" title="Draw the haul road"
        blurb="Each segment is a length, a grade (uphill positive in the loaded direction) and a surface. Edit the table or click a segment on the profile; every number, the curve, and the travel times follow." />

      <section className="card" style={{ marginBottom: '0.9rem' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'baseline', gap: '0.6rem', marginBottom: '0.5rem' }}>
          <div className="label">Segments, cut to fill</div>
          <button className="ghost" style={{ fontSize: '0.78rem' }} onClick={add}>+ Segment</button>
          {changed && <button className="ghost" style={{ fontSize: '0.78rem' }} onClick={() => { setProfile(DEFAULT_PROFILE); setSpeedLimit(DEFAULT_LIMIT); setSelected(1); }}>Reset to the problem as given</button>}
          <label style={{ marginLeft: 'auto', fontSize: '0.8rem', color: 'var(--muted)' }}>
            posted limit{' '}
            <input type="number" value={speedLimit} min={5} max={60} step={5} onChange={(e) => setSpeedLimit(Math.max(5, Math.min(60, Number(e.target.value) || 5)))} className="num" style={{ ...inputStyle, width: '4.2rem' }} /> mph
          </label>
        </div>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ borderCollapse: 'collapse', width: '100%', fontSize: '0.85rem' }}>
            <thead>
              <tr style={{ textAlign: 'left', color: 'var(--muted)' }}>
                <th style={{ padding: '0.2rem 0.4rem' }}>#</th><th style={{ padding: '0.2rem 0.4rem' }}>Segment</th>
                <th style={{ padding: '0.2rem 0.4rem' }}>Length, ft</th><th style={{ padding: '0.2rem 0.4rem' }}>Grade, %</th>
                <th style={{ padding: '0.2rem 0.4rem' }}>Surface</th><th style={{ padding: '0.2rem 0.4rem' }}>Rolling, lb/ton</th><th />
              </tr>
            </thead>
            <tbody>
              {profile.map((s, i) => (
                <tr key={i} style={{ background: i === selected ? 'var(--wash-accent)' : undefined }} onClick={() => setSelected(i)}>
                  <td className="num" style={{ padding: '0.2rem 0.4rem', color: 'var(--muted)' }}>{i + 1}</td>
                  <td style={{ padding: '0.2rem 0.4rem' }}><input value={s.label ?? ''} onChange={(e) => update(i, { label: e.target.value })} style={{ ...inputStyle, width: '11rem' }} /></td>
                  <td style={{ padding: '0.2rem 0.4rem' }}><input type="number" value={s.lengthFt} min={50} step={100} className="num" onChange={(e) => update(i, { lengthFt: Math.max(50, Number(e.target.value) || 50) })} style={{ ...inputStyle, width: '6rem' }} /></td>
                  <td style={{ padding: '0.2rem 0.4rem' }}><input type="number" value={s.gradePct} min={-20} max={20} step={0.5} className="num" onChange={(e) => update(i, { gradePct: Math.max(-20, Math.min(20, Number(e.target.value) || 0)) })} style={{ ...inputStyle, width: '5rem' }} /></td>
                  <td style={{ padding: '0.2rem 0.4rem' }}>
                    <select value={s.surface} onChange={(e) => update(i, { surface: e.target.value })} style={inputStyle}>
                      {SURFACES.map((x) => <option key={x.id} value={x.id}>{x.label}</option>)}
                    </select>
                  </td>
                  <td className="num" style={{ padding: '0.2rem 0.4rem' }}>{SURFACES.find((x) => x.id === s.surface)?.rollingLbPerTon}</td>
                  <td style={{ padding: '0.2rem 0.4rem' }}>{profile.length > 1 && <button className="ghost" style={{ fontSize: '0.75rem' }} onClick={(e) => { e.stopPropagation(); remove(i); }} aria-label="Remove segment">×</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <ProfileSvg profile={profile} selected={selected} onSelect={setSelected} />
      </section>

      <div className="sim-grid" style={{ display: 'grid', gap: '0.9rem', gridTemplateColumns: 'minmax(0, 1.1fr) minmax(0, 1fr)', alignItems: 'start' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem' }}>
          <section className="card">
            <div className="label" style={{ marginBottom: '0.4rem' }}>What each segment costs</div>
            {trip(haul, 'Loaded, cut → fill', (i) => i)}
            <div style={{ height: '0.8rem' }} />
            {trip(back, 'Empty, fill → cut', (i) => back.segments.length - 1 - i)}
          </section>
          <section className="card" style={{ borderColor: 'var(--accent)' }}>
            <div className="label" style={{ color: 'var(--accent)' }}>The answer, and where it goes</div>
            <p style={{ fontSize: '1rem', margin: '0.3rem 0 0.5rem' }}>
              Haul, loaded: <strong className="num">{haul.stalled ? 'the truck cannot make it' : `${n1(haul.minutes)} min`}</strong>
              {!haul.stalled && <span className="num" style={{ color: 'var(--muted)' }}> · {n1(haul.avgMph)} mph average</span>}<br />
              Return, empty: <strong className="num">{back.stalled ? 'the truck cannot make it' : `${n1(back.minutes)} min`}</strong>
              {!back.stalled && <span className="num" style={{ color: 'var(--muted)' }}> · {n1(back.avgMph)} mph average</span>}
            </p>
            {!haul.stalled && !back.stalled && (
              <Link href={`/learn/operations?haul=${haulMin}&return=${backMin}`} className="primary" style={{ display: 'inline-block', textDecoration: 'none' }}>
                Use {n1(haul.minutes)} and {n1(back.minutes)} min in Module 1 →
              </Link>
            )}
          </section>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem' }}>
          <section className="card">
            <div className="label" style={{ marginBottom: '0.35rem' }}>How to play the simulation</div>
            <p style={{ fontSize: '0.84rem', margin: '0 0 0.4rem', lineHeight: 1.4 }}>
              Change a length, grade or surface in the table, or click a segment on the profile to work it in the solution
              above. Hover the rimpull curve to read gear and speed at any point.
            </p>
            <ol style={{ margin: 0, paddingLeft: '1.2rem', fontSize: '0.84rem', lineHeight: 1.4 }}>
              <li style={{ marginBottom: '0.25rem' }}>{tag('Resistances')}<strong>Steepen the ramp.</strong> Take segment 2 from 6% to 10% and watch the effective grade, the required rimpull, and the gear it forces.</li>
              <li style={{ marginBottom: '0.25rem' }}>{tag('Resistances')}<strong>Let the road go.</strong> Change the haul road from firm to rutted: a flat road just became 1.75% steeper without moving.</li>
              <li style={{ marginBottom: '0.25rem' }}>{tag('Available power')}<strong>Read the curve.</strong> Loaded points sit on the curve when power governs and below it when the posted limit does. Raise the limit to 35 and see which segments move.</li>
              <li style={{ marginBottom: '0.25rem' }}>{tag('Usable power')}<strong>Find the spin.</strong> Set the ramp to soft, muddy at 12%: the required rimpull passes the traction line and the truck stops climbing.</li>
              <li>{tag('Into the cycle')}<strong>Close the loop.</strong> Send your haul and return times to Module 1 and see what they do to the fleet.</li>
            </ol>
          </section>
          <section className="card"><RimpullChart haul={haul} back={back} selected={selected} speedLimit={speedLimit} /></section>
          <section className="card"><TravelTimeChart haul={haul} speedLimit={speedLimit} /></section>
        </div>
      </div>
    </>
  );
}
