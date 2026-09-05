'use client';

import { useMemo, useState } from 'react';
import TutorChat, { type TutorProblem } from './TutorChat';
import { TAKEAWAYS } from '../../lib/takeaways';

/**
 * Module 2, Part B: problems on rolling, grade and total resistance from the
 * course's Week 4 class example, practice problem, and Homework 1 Problem 2,
 * worked on a sheet the tool checks and the tutor can see.
 */

interface Field {
  id: string;
  label: string;
  unit: string;
  /** the correct value */
  target: number;
  /** relative tolerance (0.01 = 1 %) */
  tol: number;
  /** what to say when it is wrong — the formula, not the number */
  hint: string;
}

interface WorkProblem extends TutorProblem {
  source: string;
  focus: string;
  given: string[];
  fields: Field[];
  closing: string;
}

const HP_TERM = 375 * 310 * 0.8; // the synthetic hauler from Part A: rimpull = 93,000 ÷ mph

export const PROBLEMS: WorkProblem[] = [
  {
    id: 'three-segments',
    title: 'Three segments, one machine',
    source: 'Week 4 class example',
    focus: 'Rolling, grade and effective grade',
    statement: 'A machine with a gross weight of 86 tons hauls over a road in three stretches: 2,000 ft on the level, 2,000 ft up a 2% grade, then 5,000 ft down a 5% grade. The rolling resistance is 120 lb/ton throughout. For each stretch find the grade resistance, the effective grade, and the total resistance the machine must overcome.',
    given: ['GVW 86 tons', 'RR 120 lb/ton', 'Stretch 1: 2,000 ft, 0%', 'Stretch 2: 2,000 ft, +2%', 'Stretch 3: 5,000 ft, −5%'],
    fields: [
      { id: 'eg1', label: 'Stretch 1 — effective grade', unit: '%', target: 6, tol: 0.02, hint: 'Effective grade = grade % + RR ÷ 20 lb/ton per %. On the level the whole thing is the rolling part.' },
      { id: 'tr1', label: 'Stretch 1 — total resistance', unit: 'lb', target: 10320, tol: 0.01, hint: 'TR (lb) = (RR + GR) in lb/ton × GVW in tons. Check that you multiplied by tons, not pounds.' },
      { id: 'gr2', label: 'Stretch 2 — grade resistance', unit: 'lb/ton', target: 40, tol: 0.02, hint: 'GR = 20 lb/ton for every 1% of grade.' },
      { id: 'eg2', label: 'Stretch 2 — effective grade', unit: '%', target: 8, tol: 0.02, hint: 'Add the actual grade to RR ÷ 20.' },
      { id: 'tr2', label: 'Stretch 2 — total resistance', unit: 'lb', target: 13760, tol: 0.01, hint: '(RR + GR) × 86 tons.' },
      { id: 'gr3', label: 'Stretch 3 — grade resistance', unit: 'lb/ton', target: -100, tol: 0.02, hint: 'Downhill is a negative grade, so the grade resistance is negative: it helps.' },
      { id: 'eg3', label: 'Stretch 3 — effective grade', unit: '%', target: 1, tol: 0.05, hint: '−5% plus the rolling part. It is small but still positive: rolling resistance outweighs this downhill.' },
      { id: 'tr3', label: 'Stretch 3 — total resistance', unit: 'lb', target: 1720, tol: 0.02, hint: '(120 − 100) lb/ton × 86 tons.' },
    ],
    closing: 'The same 86 tons need 13,760 lb on the 2% climb and only 1,720 lb on the 5% descent: grade resistance is the lever, and rolling resistance never goes away.',
  },
  {
    id: 'min-traction',
    title: 'Minimum coefficient of traction',
    source: 'Week 4 practice problem',
    focus: 'Power required against usable power',
    statement: 'A wheel-tractor scraper must travel up a 12% grade with 8 inches of tire penetration. Fully loaded it weighs 126,589 lb. When full, 47% of its weight is on the rear (scraper) axle; the tractor’s front axle drives. What is the minimum coefficient of traction the road must provide?',
    given: ['Grade +12%', 'Tire penetration 8 in', 'Full weight 126,589 lb', '47% of weight on the rear axle when full', 'Drive axle: front'],
    fields: [
      { id: 'rr', label: 'Rolling resistance', unit: 'lb/ton', target: 280, tol: 0.01, hint: 'RR = 40 lb/ton + 30 lb/ton for every inch of penetration.' },
      { id: 'eg', label: 'Effective grade', unit: '%', target: 26, tol: 0.02, hint: 'Grade % + RR ÷ 20.' },
      { id: 'req', label: 'Power required', unit: 'lb', target: 32913, tol: 0.01, hint: 'Effective grade as a decimal × GVW in pounds (or lb/ton × tons — same thing).' },
      { id: 'drive', label: 'Weight on the driving wheels', unit: 'lb', target: 67092, tol: 0.01, hint: 'The front axle drives and carries what the rear axle does not: (1 − 0.47) × GVW.' },
      { id: 'coef', label: 'Minimum coefficient of traction', unit: '', target: 0.49, tol: 0.03, hint: 'Usable power = coefficient × weight on the drive wheels must at least equal the power required. Solve for the coefficient.' },
    ],
    closing: 'Firm earth (about 0.55) will carry it; loose sand (about 0.30) will not, no matter how much horsepower the tractor has.',
  },
  {
    id: 'cable',
    title: 'What the cable knows',
    source: 'Homework 1, Problem 2 (iv)',
    focus: 'Rolling resistance from a measurement',
    statement: 'An empty scraper weighing 73,789 lb is pulled up a road with a +4% slope at a uniform speed. The average tension in the tow cable is 8,766 lb. What is the rolling resistance of the road, in lb per ton?',
    given: ['Empty weight 73,789 lb', 'Grade +4%', 'Cable tension 8,766 lb, uniform speed'],
    fields: [
      { id: 'tons', label: 'Gross weight', unit: 'tons', target: 36.89, tol: 0.01, hint: '2,000 lb to the ton.' },
      { id: 'gr', label: 'Grade resistance', unit: 'lb/ton', target: 80, tol: 0.02, hint: '20 lb/ton per percent of grade.' },
      { id: 'grlb', label: 'Grade resistance', unit: 'lb', target: 2952, tol: 0.01, hint: 'lb/ton × tons.' },
      { id: 'rrlb', label: 'Rolling resistance', unit: 'lb', target: 5814, tol: 0.01, hint: 'At uniform speed the cable carries exactly the total resistance. What is left after the grade takes its share?' },
      { id: 'rr', label: 'Rolling resistance', unit: 'lb/ton', target: 157.6, tol: 0.02, hint: 'Pounds ÷ tons.' },
      { id: 'pen', label: 'Implied tire penetration', unit: 'in', target: 3.9, tol: 0.06, hint: 'Invert RR = 40 + 30 × inches.' },
    ],
    closing: 'A cable tension is a total-resistance reading. Take the grade out and the road’s own resistance is what remains — about a 4-inch rut.',
  },
  {
    id: 'hold-15',
    title: 'Hold 15 mph',
    source: 'Homework 1, Problem 2 (vi), on the Part A hauler',
    focus: 'Available power from the curve',
    statement: 'The Part A hauler (310 hp, 0.8 drivetrain efficiency, 90,000 lb loaded) is on level ground. Disregarding traction, what is the largest rolling resistance, in lb per ton, at which the fully loaded truck can still hold 15 mph?',
    given: ['310 hp, drivetrain efficiency 0.8', 'Loaded GVW 90,000 lb', 'Level grade', 'Speed to hold: 15 mph'],
    fields: [
      { id: 'rim', label: 'Rimpull available at 15 mph', unit: 'lb', target: HP_TERM / 15, tol: 0.01, hint: 'Rimpull = 375 × hp × efficiency ÷ mph.' },
      { id: 'tons', label: 'Gross weight', unit: 'tons', target: 45, tol: 0.01, hint: '2,000 lb to the ton.' },
      { id: 'rrmax', label: 'Largest rolling resistance', unit: 'lb/ton', target: HP_TERM / 15 / 45, tol: 0.01, hint: 'On the level all the resistance is rolling: rimpull ÷ tons.' },
      { id: 'pen', label: 'Equivalent tire penetration', unit: 'in', target: (HP_TERM / 15 / 45 - 40) / 30, tol: 0.06, hint: 'Invert RR = 40 + 30 × inches.' },
    ],
    closing: 'About 138 lb/ton — a rutted road with roughly 3 inches of penetration — is the most this truck can take and still hold 15 mph.',
  },
  {
    id: 'usable',
    title: 'Usable power on the haul',
    source: 'Homework 1, Problem 2 (i), on the Part A hauler',
    focus: 'The fundamental principle',
    statement: 'The Part A hauler carries its rated payload 4,000 ft up a 3% grade from the cut to the fill. The rolling resistance is 100 lb/ton and the road is firm earth (coefficient of traction 0.55). All wheels drive. Loaded it weighs 90,000 lb. What is the usable power on the haul, and how much of it is to spare?',
    given: ['Loaded GVW 90,000 lb, all-wheel drive', 'Grade +3%', 'RR 100 lb/ton', 'Firm earth, coefficient of traction 0.55'],
    fields: [
      { id: 'gr', label: 'Grade resistance', unit: 'lb/ton', target: 60, tol: 0.02, hint: '20 lb/ton per percent.' },
      { id: 'eg', label: 'Effective grade', unit: '%', target: 8, tol: 0.02, hint: 'Grade + RR ÷ 20.' },
      { id: 'req', label: 'Power required', unit: 'lb', target: 7200, tol: 0.01, hint: 'Effective grade (decimal) × GVW in pounds.' },
      { id: 'usable', label: 'Usable power', unit: 'lb', target: 49500, tol: 0.01, hint: 'Coefficient of traction × weight on the driving wheels — here every wheel drives.' },
      { id: 'spare', label: 'Power to spare', unit: 'lb', target: 42300, tol: 0.01, hint: 'Usable minus required.' },
    ],
    closing: 'Usable power exceeds power required by a wide margin, so this haul is governed by the engine and the road’s posted limit, not by the tires.',
  },
];

const n = (v: number) => (Math.abs(v) >= 100 ? v.toLocaleString(undefined, { maximumFractionDigits: 0 }) : v.toLocaleString(undefined, { maximumFractionDigits: 2 }));

type Status = 'blank' | 'correct' | 'incorrect' | 'unchecked';

export default function ResistanceStudio() {
  const [problemId, setProblemId] = useState(PROBLEMS[0].id);
  const problem = PROBLEMS.find((p) => p.id === problemId) ?? PROBLEMS[0];
  const [entries, setEntries] = useState<Record<string, string>>({});
  const [checked, setChecked] = useState<Record<string, Status>>({});
  const [reveal, setReveal] = useState(false);
  const [attempts, setAttempts] = useState(0);

  const pick = (id: string) => { setProblemId(id); setEntries({}); setChecked({}); setReveal(false); setAttempts(0); };

  const status = (f: Field): Status => checked[f.id] ?? (entries[f.id]?.trim() ? 'unchecked' : 'blank');
  const check = () => {
    const next: Record<string, Status> = {};
    for (const f of problem.fields) {
      const raw = entries[f.id]?.trim();
      if (!raw) { next[f.id] = 'blank'; continue; }
      const v = Number(raw.replace(/,/g, ''));
      const ok = Number.isFinite(v) && Math.abs(v - f.target) <= Math.max(Math.abs(f.target) * f.tol, 1e-9);
      next[f.id] = ok ? 'correct' : 'incorrect';
    }
    setChecked(next); setAttempts((a) => a + 1);
  };
  const right = problem.fields.filter((f) => status(f) === 'correct').length;
  const allRight = right === problem.fields.length;

  // what the tutor sees: the sheet with targets, never shown to the student
  const work = useMemo(() => ({
    source: problem.source,
    given: problem.given,
    entries: problem.fields.map((f) => ({ id: f.id, label: f.label, unit: f.unit, student: entries[f.id] ?? '', status: status(f), target: Math.round(f.target * 1000) / 1000 })),
    correct: right, total: problem.fields.length, checks: attempts,
  }), [problem, entries, checked, attempts]); // eslint-disable-line react-hooks/exhaustive-deps

  const mark = (s: Status) => s === 'correct' ? <span style={{ color: 'var(--good)', fontWeight: 700 }}>✓</span>
    : s === 'incorrect' ? <span style={{ color: 'var(--caution)', fontWeight: 700 }}>✗</span>
    : <span style={{ color: 'var(--line)' }}>·</span>;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem' }}>
      <section className="card">
        <div className="label" style={{ marginBottom: '0.4rem' }}>Pick a problem</div>
        <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap', marginBottom: '0.6rem' }}>
          {PROBLEMS.map((p) => (
            <button key={p.id} className="ghost" onClick={() => pick(p.id)}
              style={p.id === problemId ? { borderColor: 'var(--accent)', color: 'var(--accent)', background: 'var(--wash-accent)' } : undefined}>
              {p.title}
            </button>
          ))}
        </div>
        <p style={{ fontSize: '0.95rem', margin: '0 0 0.4rem' }}>{problem.statement}</p>
        <p style={{ fontSize: '0.8rem', color: 'var(--muted)', margin: 0 }}>
          <span title={TAKEAWAYS[5].text} style={{ fontFamily: 'var(--font-display)', fontWeight: 600, color: 'var(--accent)', background: 'var(--wash-accent)', borderRadius: 4, padding: '0.05rem 0.4rem', marginRight: '0.45rem' }}>T6 · {problem.focus}</span>
          {problem.source}
        </p>
      </section>

      <div style={{ display: 'grid', gap: '0.9rem', gridTemplateColumns: 'minmax(0, 3fr) minmax(18rem, 1fr)', alignItems: 'start' }} className="studio-grid">
        <section className="card">
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'baseline', gap: '0.6rem', marginBottom: '0.5rem' }}>
            <div className="label">Your worksheet</div>
            <span style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>Given: {problem.given.join(' · ')}</span>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) auto auto', gap: '0.4rem 0.7rem', alignItems: 'center' }}>
            {problem.fields.map((f, i) => {
              const s = status(f);
              return (
                <div key={f.id} style={{ display: 'contents' }}>
                  <label htmlFor={`w-${f.id}`} style={{ fontSize: '0.9rem' }}>
                    <span className="num" style={{ color: 'var(--muted)', marginRight: '0.4rem' }}>{i + 1}.</span>{f.label}
                    {s === 'incorrect' && <div style={{ fontSize: '0.8rem', color: 'var(--caution)', marginTop: '0.15rem' }}>{f.hint}</div>}
                    {reveal && s !== 'correct' && <div style={{ fontSize: '0.8rem', color: 'var(--muted)', marginTop: '0.15rem' }}>Answer: <strong className="num">{n(f.target)} {f.unit}</strong></div>}
                  </label>
                  <span style={{ display: 'flex', alignItems: 'baseline', gap: '0.3rem' }}>
                    <input id={`w-${f.id}`} inputMode="decimal" value={entries[f.id] ?? ''} placeholder="?"
                      onChange={(e) => { setEntries((prev) => ({ ...prev, [f.id]: e.target.value })); setChecked((prev) => { const c = { ...prev }; delete c[f.id]; return c; }); }}
                      className="num"
                      style={{ width: '7rem', padding: '0.3rem 0.45rem', fontSize: '1rem', textAlign: 'right', border: `1px solid ${s === 'correct' ? 'var(--good)' : s === 'incorrect' ? 'var(--caution)' : 'var(--line)'}`, borderRadius: 6, background: 'var(--surface)', color: 'var(--ink)' }} />
                    <span style={{ fontSize: '0.8rem', color: 'var(--muted)', minWidth: '3.2rem' }}>{f.unit}</span>
                  </span>
                  <span style={{ width: '1.2rem', textAlign: 'center' }}>{mark(s)}</span>
                </div>
              );
            })}
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', alignItems: 'center', marginTop: '0.8rem' }}>
            <button className="primary" onClick={check}>Check my work</button>
            <button className="ghost" onClick={() => { setEntries({}); setChecked({}); setReveal(false); }}>Clear</button>
            {attempts >= 2 && !allRight && <button className="ghost" onClick={() => setReveal(true)}>Show the answers</button>}
            <span className="num" style={{ marginLeft: 'auto', fontSize: '0.85rem', color: allRight ? 'var(--good)' : 'var(--muted)' }}>
              {attempts > 0 ? `${right} of ${problem.fields.length} right` : `${problem.fields.length} entries`}
            </span>
          </div>
          {allRight && (
            <p style={{ margin: '0.7rem 0 0', padding: '0.6rem 0.8rem', border: '1px solid var(--good)', borderRadius: 6, fontSize: '0.92rem' }}>
              <strong>All right.</strong> {problem.closing}
            </p>
          )}
          <p style={{ fontSize: '0.8rem', color: 'var(--muted)', margin: '0.7rem 0 0' }}>
            Formulas as taught: RR = 40 + 30 × inches of penetration (lb/ton) · GR = 20 lb/ton per % · effective grade = grade + RR ÷ 20 ·
            TR = (RR + GR) × tons = effective grade × GVW · rimpull = 375 × hp × efficiency ÷ mph · usable power = coefficient of traction × weight on the driving wheels.
          </p>
        </section>
        <section className="card" style={{ position: 'sticky', top: '0.8rem' }}>
          <TutorChat problem={problem} mode="haul" work={work}
            intro={<>Work the sheet, press <strong>Check my work</strong>, then ask. I can see your entries and which ones are off — ask &ldquo;why is 3 wrong?&rdquo;, &ldquo;what goes in the effective grade?&rdquo;, or &ldquo;which units?&rdquo;</>}
            quick={['Where do I start?', 'Why is my entry wrong?', 'Which units?']} />
        </section>
      </div>
    </div>
  );
}
