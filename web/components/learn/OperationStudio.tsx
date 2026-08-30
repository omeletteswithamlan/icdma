'use client';

import { useMemo, useState } from 'react';
import {
  analyzeEarthmoving, buildEarthmoving, simulate, mulberry32,
  type EarthmovingParams,
} from 'icdma-engine';

const fmtN = (x: number, d = 0) => x.toLocaleString('en-US', { maximumFractionDigits: d });

/* ---------------- ACD diagram (Takeaway 4) ---------------- */

function AcdDiagram({ counts, active }: {
  counts: Record<string, number>;
  active: Record<string, boolean>;
}) {
  const q = (cx: number, cy: number, id: string, label: string, dy = 46) => (
    <g>
      <circle cx={cx} cy={cy} r={24} fill="var(--surface)" stroke="var(--line)" strokeWidth={1.5} />
      <text x={cx} y={cy + 5} textAnchor="middle" fontSize={15} fontWeight={700}
        fontFamily="var(--font-display)" fill="var(--accent)">{counts[id] ?? 0}</text>
      <text x={cx} y={cy + dy} textAnchor="middle" fontSize={10.5} fill="var(--muted)">{label}</text>
    </g>
  );
  const act = (x: number, y: number, id: string, label: string) => (
    <g>
      <rect x={x - 42} y={y - 16} width={84} height={32} rx={4}
        fill={active[id] ? 'var(--wash-accent)' : 'var(--surface)'}
        stroke={active[id] ? 'var(--accent)' : 'var(--line)'} strokeWidth={active[id] ? 2 : 1.5} />
      <text x={x} y={y + 4} textAnchor="middle" fontSize={11.5} fontWeight={600}
        fontFamily="var(--font-display)" fill="var(--ink)">{label}</text>
    </g>
  );
  const arrow = (d: string) => (
    <path d={d} fill="none" stroke="var(--muted)" strokeWidth={1.4} markerEnd="url(#arr)" />
  );
  return (
    <svg viewBox="0 0 640 240" style={{ width: '100%', height: 'auto' }} role="img"
      aria-label="Activity cycle diagram: truck loop through load, haul, dump, return; excavator loop through load">
      <defs>
        <marker id="arr" viewBox="0 0 8 8" refX="7" refY="4" markerWidth="7" markerHeight="7" orient="auto">
          <path d="M0 0 L8 4 L0 8 z" fill="var(--muted)" />
        </marker>
      </defs>
      {arrow('M104 120 L166 120')}
      {arrow('M292 120 L354 120')}
      {arrow('M480 120 L530 120')}
      {arrow('M556 96 C 556 40, 420 30, 260 34')}
      {arrow('M208 96 C 208 60, 120 60, 92 94')}
      {arrow('M232 144 C 260 190, 180 200, 110 146')}
      {arrow('M110 178 C 160 214, 240 210, 226 148')}
      {q(80, 120, 'trucksIdle', 'Trucks waiting')}
      {act(208, 120, 'load', 'LOAD')}
      {q(80, 200, 'loaderIdle', 'Excavator idle', -34)}
      {q(330, 120, 'loaded', 'Loaded')}
      {act(418, 120, 'haul', 'HAUL')}
      {q(556, 120, 'atDump', 'At dump')}
      {act(418, 34, 'dumpReturn', 'DUMP · RETURN')}
    </svg>
  );
}

/* ---------------- balance chart (Takeaways 1 & 5) ---------------- */

function BalanceChart({ params }: { params: EarthmovingParams }) {
  const pts = useMemo(() => {
    const out: { n: number; sim: number }[] = [];
    for (let n = 1; n <= 10; n++) {
      const r = simulate(buildEarthmoving({ ...params, trucks: n, jitter: 0 }), { horizon: 16 * 60 });
      out.push({ n, sim: (r.produced / r.endTime) * 60 });
    }
    return out;
  }, [params.loaders, params.truckCapacityLcy, params.passesPerTruck, params.secondsPerPass,
    params.haulMin, params.dumpMin, params.returnMin]);

  const a = analyzeEarthmoving({ ...params, trucks: 1 });
  const W = 560; const H = 230; const M = { l: 52, r: 14, t: 12, b: 34 };
  const maxY = Math.max(a.loaderRateLcyHr, ...pts.map((p) => p.sim)) * 1.15;
  const x = (n: number) => M.l + ((n - 1) / 9) * (W - M.l - M.r);
  const y = (v: number) => H - M.b - (v / maxY) * (H - M.t - M.b);

  return (
    <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: 'auto' }} role="img"
      aria-label="Production rate versus number of trucks: rising truck-limited line meets the flat excavator ceiling at the balance point">
      {[0.25, 0.5, 0.75, 1].map((f) => (
        <line key={f} x1={M.l} x2={W - M.r} y1={y(maxY * f)} y2={y(maxY * f)} stroke="var(--line)" strokeWidth={0.7} />
      ))}
      <line x1={M.l} x2={W - M.r} y1={y(a.loaderRateLcyHr)} y2={y(a.loaderRateLcyHr)}
        stroke="var(--caution)" strokeWidth={2} strokeDasharray="6 4" />
      <text x={W - M.r} y={y(a.loaderRateLcyHr) - 6} textAnchor="end" fontSize={11}
        fill="var(--caution)" fontFamily="var(--font-display)" fontWeight={600}>excavator ceiling</text>
      <line x1={x(1)} y1={y(a.truckRateLcyHr)} x2={x(10)} y2={y(10 * a.truckRateLcyHr)}
        stroke="var(--good)" strokeWidth={2} strokeDasharray="6 4" />
      <text x={x(3.1)} y={y(3.1 * a.truckRateLcyHr) - 8} fontSize={11} fill="var(--good)"
        fontFamily="var(--font-display)" fontWeight={600}>n × one truck</text>
      <polyline fill="none" stroke="var(--accent)" strokeWidth={2.5}
        points={pts.map((p) => `${x(p.n)},${y(p.sim)}`).join(' ')} />
      {pts.map((p) => (
        <circle key={p.n} cx={x(p.n)} cy={y(p.sim)} r={p.n === params.trucks ? 6 : 3.5}
          fill={p.n === params.trucks ? 'var(--accent)' : 'var(--surface)'}
          stroke="var(--accent)" strokeWidth={1.8} />
      ))}
      <line x1={x(a.balancePoint)} y1={y(0)} x2={x(a.balancePoint)} y2={M.t + 8}
        stroke="var(--muted)" strokeWidth={1} strokeDasharray="3 3" />
      <text x={x(a.balancePoint)} y={M.t + 6} textAnchor="middle" fontSize={11} fill="var(--muted)">
        balance {a.balancePoint.toFixed(1)}
      </text>
      {[1, 4, 7, 10].map((n) => (
        <text key={n} x={x(n)} y={H - 12} textAnchor="middle" fontSize={11} fill="var(--muted)">{n}</text>
      ))}
      <text x={(M.l + W - M.r) / 2} y={H - 1} textAnchor="middle" fontSize={11} fill="var(--muted)">trucks in the fleet</text>
      <text x={12} y={M.t + 8} fontSize={11} fill="var(--muted)">LCY/hr</text>
      {[0.5, 1].map((f) => (
        <text key={f} x={M.l - 6} y={y(maxY * f) + 4} textAnchor="end" fontSize={10.5}
          fill="var(--muted)" className="num">{fmtN(maxY * f)}</text>
      ))}
    </svg>
  );
}

/* ---------------- self-checks (Takeaways 1, 2, 4, 5) ---------------- */

interface Problem {
  prompt: string;
  answer: number;
  unit: string;
  tolerance: number;
  explain: string;
}

function makeProblem(kind: number, rng: () => number): Problem {
  if (kind === 0) {
    const rate = 120 + Math.round(rng() * 20) * 10;
    const bank = 10000 + Math.round(rng() * 8) * 2500;
    const hours = 8;
    return {
      prompt: `An operation produces ${fmtN(rate)} BCY/hr and works ${hours}-hour days. How many full working days does a ${fmtN(bank)} BCY job take?`,
      answer: Math.ceil(bank / (rate * hours)),
      unit: 'days', tolerance: 0.4,
      explain: `Duration = work ÷ production rate: ${fmtN(bank)} ÷ ${fmtN(rate)} = ${fmtN(bank / rate, 1)} hr; ÷ ${hours} hr/day and round up.`,
    };
  }
  if (kind === 1) {
    const bank = 4000 + Math.round(rng() * 10) * 800;
    const swell = 15 + Math.round(rng() * 4) * 5;
    return {
      prompt: `You must excavate ${fmtN(bank)} BCY. With ${swell}% swell, how many loose cubic yards will your trucks actually carry?`,
      answer: bank * (1 + swell / 100),
      unit: 'LCY', tolerance: 0.01,
      explain: `Loose = bank × (1 + swell%): ${fmtN(bank)} × ${(1 + swell / 100).toFixed(2)}.`,
    };
  }
  const load = 2 + Math.round(rng() * 3);
  const rest = 12 + Math.round(rng() * 5) * 2;
  return {
    prompt: `Loading a truck takes ${load} min; haul + dump + return take ${rest} min. How many trucks keep one excavator continuously busy? (Round up.)`,
    answer: Math.ceil((load + rest) / load),
    unit: 'trucks', tolerance: 0.4,
    explain: `Balance = truck cycle ÷ load time = (${load} + ${rest}) ÷ ${load} = ${((load + rest) / load).toFixed(2)}, round up for continuous operation.`,
  };
}

function SelfCheck() {
  const [seed, setSeed] = useState(1);
  const [kind, setKind] = useState(0);
  const [entry, setEntry] = useState('');
  const [state, setState] = useState<'open' | 'right' | 'wrong'>('open');
  const problem = useMemo(() => makeProblem(kind, mulberry32(seed * 7 + kind)), [seed, kind]);

  const grade = () => {
    const v = Number(entry);
    if (!Number.isFinite(v)) return;
    const ok = Math.abs(v - problem.answer) <= problem.tolerance * Math.max(1, Math.abs(problem.answer)) * 0.05 + problem.tolerance;
    setState(ok ? 'right' : 'wrong');
  };

  return (
    <div>
      <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap', marginBottom: '0.6rem' }}>
        {['Rate → duration', 'Bank → loose', 'Balance point'].map((label, i) => (
          <button key={label} className="ghost"
            style={i === kind ? { borderColor: 'var(--accent)', color: 'var(--accent)' } : undefined}
            onClick={() => { setKind(i); setEntry(''); setState('open'); }}>
            {label}
          </button>
        ))}
      </div>
      <p style={{ fontSize: '0.95rem', margin: '0.4rem 0' }}>{problem.prompt}</p>
      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
        <input
          type="number" value={entry} inputMode="decimal"
          onChange={(e) => { setEntry(e.target.value); setState('open'); }}
          onKeyDown={(e) => e.key === 'Enter' && grade()}
          style={{ width: '9rem', padding: '0.35rem 0.5rem', fontSize: '1rem',
            border: '1px solid var(--line)', borderRadius: 6, background: 'var(--surface)', color: 'var(--ink)' }}
          aria-label={`answer in ${problem.unit}`}
        />
        <span style={{ color: 'var(--muted)', fontSize: '0.9rem' }}>{problem.unit}</span>
        <button className="primary" onClick={grade}>Check</button>
        <button className="ghost" onClick={() => { setSeed((s) => s + 1); setEntry(''); setState('open'); }}>
          New numbers
        </button>
      </div>
      {state === 'right' && (
        <p style={{ color: 'var(--good)', fontSize: '0.92rem', marginTop: '0.5rem' }}>
          Right — {problem.explain}
        </p>
      )}
      {state === 'wrong' && (
        <p style={{ color: 'var(--caution)', fontSize: '0.92rem', marginTop: '0.5rem' }}>
          Not yet. {problem.explain} Expected about <strong className="num">{fmtN(problem.answer, 1)} {problem.unit}</strong>.
        </p>
      )}
    </div>
  );
}

/* ---------------- the studio ---------------- */

export default function OperationStudio() {
  const [params, setParams] = useState<EarthmovingParams>({
    trucks: 4, loaders: 1,
    truckCapacityLcy: 16, passesPerTruck: 8, secondsPerPass: 25,
    haulMin: 8, dumpMin: 1.5, returnMin: 6, swellPct: 25, jitter: 0,
  });
  const [targetBcy, setTargetBcy] = useState(20000);
  const set = (patch: Partial<EarthmovingParams>) => setParams((p) => ({ ...p, ...patch }));

  const analysis = useMemo(() => analyzeEarthmoving(params), [params]);
  const sim = useMemo(() => simulate(buildEarthmoving(params), {
    horizon: 8 * 60, rng: mulberry32(3),
  }), [params]);

  const simRate = (sim.produced / sim.endTime) * 60;
  const simRateBcy = simRate / (1 + params.swellPct / 100);
  const jobHours = targetBcy / Math.max(1e-6, simRateBcy);
  const truckUtil = sim.utilization.get('trucksIdle') ?? 0;
  const loaderUtil = sim.utilization.get('loaderIdle') ?? 0;

  const finalCounts: Record<string, number> = {};
  for (const q of ['trucksIdle', 'loaderIdle', 'loaded', 'atDump', 'returning']) {
    const avg = sim.avgQueue.get(q) ?? 0;
    finalCounts[q] = Math.round(avg * 10) / 10;
  }
  const acdCounts = {
    trucksIdle: finalCounts.trucksIdle, loaderIdle: finalCounts.loaderIdle,
    loaded: finalCounts.loaded, atDump: finalCounts.atDump + finalCounts.returning,
  } as Record<string, number>;

  const slider = (label: string, value: number, min: number, max: number, step: number,
    onChange: (v: number) => void, unit = '') => (
    <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--muted)', marginTop: '0.5rem' }}>
      {label}: <strong className="num" style={{ color: 'var(--ink)' }}>{value}{unit}</strong>
      <input type="range" min={min} max={max} step={step} value={value}
        onChange={(e) => onChange(Number(e.target.value))}
        style={{ width: '100%', display: 'block', marginTop: '0.1rem' }} />
    </label>
  );

  const util = (label: string, v: number) => (
    <div style={{ marginTop: '0.45rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', color: 'var(--muted)' }}>
        <span>{label}</span><span className="num" style={{ color: 'var(--ink)' }}>{Math.round(v * 100)}%</span>
      </div>
      <div style={{ height: 6, borderRadius: 4, background: 'var(--line)' }}>
        <div style={{ width: `${Math.round(v * 100)}%`, height: 6, borderRadius: 4,
          background: v > 0.9 ? 'var(--good)' : v > 0.6 ? 'var(--accent)' : 'var(--caution)' }} />
      </div>
    </div>
  );

  return (
    <div style={{ display: 'grid', gap: '0.9rem', gridTemplateColumns: 'minmax(0, 3fr) minmax(16rem, 2fr)' }}
      className="studio-grid">
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem', minWidth: 0 }}>
        <section className="card">
          <div className="label" style={{ marginBottom: '0.4rem' }}>The operation, as a cycle diagram</div>
          <AcdDiagram counts={acdCounts} active={{ load: loaderUtil > 0.5, haul: true, dumpReturn: true }} />
          <p style={{ fontSize: '0.82rem', color: 'var(--muted)', margin: '0.4rem 0 0' }}>
            Circles are queues (average tokens waiting over the shift); boxes are activities.
            LOAD needs a truck <em>and</em> the excavator — that double draw is what makes
            this a system, not a sum.
          </p>
        </section>
        <section className="card">
          <div className="label" style={{ marginBottom: '0.4rem' }}>Production vs fleet size</div>
          <BalanceChart params={params} />
          <p style={{ fontSize: '0.82rem', color: 'var(--muted)', margin: '0.4rem 0 0' }}>
            Below the balance point, every truck you add buys production. Above it, trucks
            queue behind a busy excavator — the classic continuous-operation trade-off.
          </p>
        </section>
        <section className="card">
          <div className="label" style={{ marginBottom: '0.4rem' }}>Check yourself</div>
          <SelfCheck />
        </section>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem', minWidth: 0 }}>
        <section className="card">
          <div className="label">Your fleet</div>
          {slider('Trucks', params.trucks, 1, 10, 1, (v) => set({ trucks: v }))}
          {slider('Truck capacity', params.truckCapacityLcy, 8, 30, 1, (v) => set({ truckCapacityLcy: v }), ' LCY')}
          {slider('Bucket passes to fill', params.passesPerTruck, 4, 14, 1, (v) => set({ passesPerTruck: v }))}
          {slider('Seconds per pass', params.secondsPerPass, 15, 45, 1, (v) => set({ secondsPerPass: v }), ' s')}
          {slider('Haul', params.haulMin, 2, 20, 0.5, (v) => set({ haulMin: v }), ' min')}
          {slider('Dump', params.dumpMin, 0.5, 5, 0.5, (v) => set({ dumpMin: v }), ' min')}
          {slider('Return', params.returnMin, 2, 20, 0.5, (v) => set({ returnMin: v }), ' min')}
          {slider('Swell', params.swellPct, 5, 40, 1, (v) => set({ swellPct: v }), '%')}
        </section>
        <section className="card">
          <div className="label">What the shift produced</div>
          <div style={{ fontSize: '1.5rem', fontFamily: 'var(--font-display)', fontWeight: 700, marginTop: '0.3rem' }}>
            {fmtN(simRateBcy)} <span style={{ fontSize: '0.85rem', color: 'var(--muted)', fontWeight: 500 }}>BCY/hr</span>
          </div>
          <div className="num" style={{ fontSize: '0.82rem', color: 'var(--muted)' }}>
            {fmtN(simRate)} LCY/hr · load {analysis.loadMin.toFixed(1)} min · truck cycle {analysis.truckCycleMin.toFixed(1)} min
            · balance point {analysis.balancePoint.toFixed(1)} trucks
          </div>
          {util('Excavator working', loaderUtil)}
          {util('Trucks working', truckUtil)}
          <div style={{ borderTop: '1px solid var(--line)', marginTop: '0.7rem', paddingTop: '0.55rem' }}>
            <label style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>
              Job size: <strong className="num" style={{ color: 'var(--ink)' }}>{fmtN(targetBcy)} BCY</strong>
              <input type="range" min={5000} max={60000} step={1000} value={targetBcy}
                onChange={(e) => setTargetBcy(Number(e.target.value))}
                style={{ width: '100%', display: 'block', marginTop: '0.1rem' }} />
            </label>
            <div style={{ fontSize: '0.9rem', marginTop: '0.3rem' }}>
              At this rate: <strong className="num">{fmtN(jobHours, 1)} hours</strong>
              {' '}≈ <strong className="num">{fmtN(Math.ceil(jobHours / 8))} working days</strong>
            </div>
          </div>
        </section>
        <section className="card">
          <div className="label" style={{ marginBottom: '0.4rem' }}>Equipment data sheet</div>
          <div style={{ fontSize: '0.82rem', lineHeight: 1.55 }}>
            <strong style={{ fontFamily: 'var(--font-display)' }}>AM-320 hydraulic excavator</strong>
            <span style={{ color: 'var(--muted)' }}> (a synthetic machine — the reading skill is the point)</span>
            <table style={{ width: '100%', fontSize: '0.8rem', marginTop: '0.3rem', borderCollapse: 'collapse' }}>
              <tbody>
                <tr><td style={{ color: 'var(--muted)', padding: '0.12rem 0' }}>Heaped bucket</td><td className="num">2.0 LCY</td></tr>
                <tr><td style={{ color: 'var(--muted)', padding: '0.12rem 0' }}>Cycle @ 90° swing</td><td className="num">25 s</td></tr>
                <tr><td style={{ color: 'var(--muted)', padding: '0.12rem 0' }}>Fill factor, common earth</td><td className="num">1.00</td></tr>
                <tr><td style={{ color: 'var(--muted)', padding: '0.12rem 0' }}>Hauler pairing</td><td className="num">16 LCY → 8 passes</td></tr>
              </tbody>
            </table>
          </div>
          <p style={{ fontSize: '0.78rem', color: 'var(--muted)', margin: '0.45rem 0 0' }}>
            Takeaway 3 in practice: bucket size and cycle time from the sheet become
            passes-per-truck and seconds-per-pass in your model.
          </p>
        </section>
      </div>
    </div>
  );
}
