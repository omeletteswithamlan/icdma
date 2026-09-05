'use client';

import { useState } from 'react';
import AcdBuilder, { DEFAULT_PARAMS, derive, type OperationParams } from './AcdBuilder';
import PartHeader from './PartHeader';
import { TAKEAWAYS, MODULES } from '../../lib/takeaways';

const mod = MODULES.find((m) => m.slug === 'operations')!;
const n0 = (v: number) => v.toLocaleString(undefined, { maximumFractionDigits: 0 });
const n1 = (v: number) => v.toLocaleString(undefined, { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const n2 = (v: number) => v.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/* ------------------------------------------------------------------ */
/* One editable parameter — a numeric field, never a slider             */
/* ------------------------------------------------------------------ */

function Param({ label, unit, value, step, min, max, onChange, changed }: {
  label: string; unit: string; value: number; step: number; min: number; max?: number; onChange: (v: number) => void; changed: boolean;
}) {
  return (
    <label style={{ display: 'block', fontSize: '0.74rem', color: 'var(--muted)', lineHeight: 1.2 }}>
      <span style={{ display: 'block', minHeight: '2.2em' }}>{label}</span>
      <span style={{ display: 'flex', alignItems: 'baseline', gap: '0.3rem', marginTop: '0.15rem' }}>
        <input type="number" value={value} step={step} min={min} max={max}
          onChange={(e) => { const v = Number(e.target.value); if (Number.isFinite(v)) onChange(Math.max(min, max !== undefined ? Math.min(max, v) : v)); }}
          className="num"
          style={{ width: '5.2rem', padding: '0.3rem 0.4rem', fontSize: '1.02rem', fontWeight: 600, border: `1px solid ${changed ? 'var(--accent)' : 'var(--line)'}`, borderRadius: 6, background: 'var(--surface)', color: changed ? 'var(--accent)' : 'var(--ink)' }} />
        <span style={{ fontSize: '0.78rem' }}>{unit}</span>
      </span>
    </label>
  );
}

/* ------------------------------------------------------------------ */
/* Part A: the problem, its parameters, the worked solution, the model  */
/* ------------------------------------------------------------------ */

export default function ExploreOperation() {
  const [p, setP] = useState<OperationParams>(DEFAULT_PARAMS);
  const d = derive(p);
  const set = <K extends keyof OperationParams>(k: K) => (v: number) => setP((prev) => ({ ...prev, [k]: v }));
  const diff = <K extends keyof OperationParams>(k: K) => p[k] !== DEFAULT_PARAMS[k];
  const changed = (Object.keys(DEFAULT_PARAMS) as (keyof OperationParams)[]).some(diff);

  const tag = (n: number, name: string) => (
    <span style={{ display: 'inline-block', fontSize: '0.7rem', fontFamily: 'var(--font-display)', fontWeight: 600, color: 'var(--accent)', background: 'var(--wash-accent)', borderRadius: 4, padding: '0.05rem 0.4rem', marginRight: '0.45rem', verticalAlign: 'middle', whiteSpace: 'nowrap' }}
      title={TAKEAWAYS[n - 1].text}>T{n} · {name}</span>
  );
  const num = (s: string) => <strong className="num">{s}</strong>;

  const steps: { tag: React.ReactNode; body: React.ReactNode }[] = [
    {
      tag: tag(3, 'Equipment data'),
      body: <>
        The excavator&apos;s data sheet gives a {num(`${p.bucketLcy} LCY`)} bucket and a {num(`${p.bucketCycleS} s`)} cycle. Filling a {num(`${p.truckLcy}-LCY`)} truck
        takes ⌈{p.truckLcy} ÷ {p.bucketLcy}⌉ = {num(`${d.passes} passes`)} × {p.bucketCycleS} s = {num(`${n2(d.loadMin)} min`)}. While it is loading, the
        excavator produces {p.truckLcy} LCY ÷ {n2(d.loadMin)} min = {num(`${n0(d.excLcyHr)} LCY/h`)}, loose.
      </>,
    },
    {
      tag: tag(2, 'Bank to loose'),
      body: <>
        The {num(`${n0(p.quantityBcy)} BCY`)} in the ground swells {p.swellPct}% in the trucks: {n0(p.quantityBcy)} × {n2(d.swell)} = {num(`${n0(d.looseLcy)} LCY`)} to haul,
        which is {n0(d.looseLcy)} ÷ {p.truckLcy} = {num(`${n0(d.loads)} truckloads`)}. The excavator&apos;s {n0(d.excLcyHr)} LCY/h is {n0(d.excLcyHr)} ÷ {n2(d.swell)} = {num(`${n0(d.excBcyHr)} BCY/h`)} in bank measure — the number the estimate is written in.
      </>,
    },
    {
      tag: tag(4, 'Components to draw'),
      body: <>
        Two cycles meet at LOAD. The excavator&apos;s is short: idle → LOAD → idle. The trucks&apos; is long: LOAD → HAUL → DUMP → RETURN → waiting.
        Material flows across both: bank → LOAD → HAUL → DUMP → fill. LOAD is a COMBI because it needs a truck, the excavator, and soil at the same
        moment; every other activity is NORMAL. That is the diagram below.
      </>,
    },
    {
      tag: tag(1, 'Cycle time'),
      body: <>
        A truck&apos;s cycle is load {n2(d.loadMin)} + haul {p.haulMin} + dump {p.dumpMin} + return {p.returnMin} = {num(`${n2(d.truckCycleMin)} min`)}, so
        one truck delivers {p.truckLcy} LCY every {n2(d.truckCycleMin)} min = {num(`${n0(d.perTruckLcyHr)} LCY/h`)}.
      </>,
    },
    {
      tag: tag(5, 'Continuous operation'),
      body: <>
        The excavator finishes a truck every {n2(d.loadMin)} min; each truck is back every {n2(d.truckCycleMin)} min. Keeping the excavator busy needs
        {' '}{n2(d.truckCycleMin)} ÷ {n2(d.loadMin)} = {num(n2(d.balance))} → {num(`${d.trucksToBalance} trucks`)}. With {d.trucksToBalance} the excavator governs at {n0(d.excLcyHr)} LCY/h and trucks
        queue briefly; with {d.trucksToBalance - 1} the trucks govern at {d.trucksToBalance - 1} × {n0(d.perTruckLcyHr)} = {n0((d.trucksToBalance - 1) * d.perTruckLcyHr)} LCY/h and the excavator waits.
        Your fleet of {num(`${p.trucks}`)} gives min({p.trucks} × {n0(d.perTruckLcyHr)}, {n0(d.excLcyHr)}) = {num(`${n0(d.fleetLcyHr)} LCY/h`)} — the {d.excavatorLimited ? 'excavator' : 'trucks'} govern{d.excavatorLimited ? 's' : ''}.
      </>,
    },
    {
      tag: tag(1, 'Duration'),
      body: <>
        At {n0(d.fleetLcyHr)} LCY/h ({n0(d.fleetBcyHr)} BCY/h) the {n0(d.looseLcy)} LCY take {n0(d.looseLcy)} ÷ {n0(d.fleetLcyHr)} = {num(`${n1(d.hours)} h`)} = {num(`${d.shifts} shifts`)} of {p.shiftHours} h.
        This assumes every minute is productive; the simulation below makes the same assumption, and a 50-minute hour would add 20% to it.
      </>,
    },
  ];

  return (
    <>
      <section className="card" style={{ marginBottom: '0.9rem' }}>
        <div style={{ display: 'grid', gap: '1rem', gridTemplateColumns: 'minmax(0, 3fr) minmax(0, 2fr)', alignItems: 'start' }} className="studio-grid">
          <div>
            <div className="label" style={{ marginBottom: '0.4rem' }}>The problem</div>
            <p style={{ fontSize: '1.05rem', margin: '0 0 0.6rem', fontFamily: 'var(--font-display)', fontWeight: 600 }}>
              Design an earthwork operation to move 20,000 bank cubic yards of common earth
              from a cut on the I-69 reconstruction to a fill two miles down the alignment.
            </p>
            <p style={{ fontSize: '0.95rem', margin: '0 0 0.6rem' }}>
              You have one CAT 235C hydraulic excavator — a 2.0 LCY bucket cycling in about
              25 seconds — and can rent 16-LCY tandem-axle dump trucks. The soil swells 25%
              from bank to loose. Hauling to the fill takes about 8 minutes, dumping 1.5,
              and returning 6.
            </p>
            <p style={{ fontSize: '0.95rem', margin: 0 }}>
              <strong>Decide:</strong> how many trucks keep the excavator continuously busy,
              what production rate that fleet delivers in bank measure, and how many 8-hour
              shifts the job takes.
            </p>
          </div>
          <div>
            <img src="/media/jobsite-013.jpg" alt="A CAT 235C hydraulic excavator loading a tandem-axle dump truck on the I-69 reconstruction"
              style={{ width: '100%', borderRadius: 6, border: '1px solid var(--line)' }} />
            <p style={{ fontSize: '0.78rem', color: 'var(--muted)', margin: '0.35rem 0 0' }}>
              The 235C loading out on I-69. Photograph: Amlan Mukherjee, Michigan DOT project.
            </p>
          </div>
        </div>

        <div style={{ borderTop: '1px solid var(--line)', marginTop: '1rem', paddingTop: '0.9rem' }}>
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'baseline', gap: '0.6rem', marginBottom: '0.5rem' }}>
            <div className="label">Worked out, step by step</div>
            <span style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>
              Each step is one of the module&apos;s learning objectives ({mod.takeaways.map((n) => `T${n}`).join(' · ')}); hover a tag to read it.
              {changed && <span style={{ color: 'var(--accent)' }}> The numbers follow the parameters as you have set them below.</span>}
            </span>
          </div>
          <ol style={{ margin: 0, paddingLeft: '1.3rem', fontSize: '0.92rem', lineHeight: 1.55, columns: '2 22rem', columnGap: '2rem' }}>
            {steps.map((s, i) => (
              <li key={i} style={{ breakInside: 'avoid', marginBottom: '0.55rem', paddingRight: '0.5rem' }}>{s.tag}{s.body}</li>
            ))}
          </ol>
        </div>
      </section>

      <PartHeader part="Part A" title="Explore the operation"
        blurb="The problem's parameters, the activity cycle diagram they describe, and what it produces. Change a number here or with the − + controls on the diagram; the worked solution above, the diagram, and every plot follow." />

      <section className="card" style={{ marginBottom: '0.9rem' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'baseline', gap: '0.6rem', marginBottom: '0.6rem' }}>
          <div className="label">Parameters</div>
          {changed && <button className="ghost" style={{ fontSize: '0.78rem' }} onClick={() => setP(DEFAULT_PARAMS)}>Reset to the problem as given</button>}
        </div>
        <div style={{ display: 'grid', gap: '0.7rem 1rem', gridTemplateColumns: 'repeat(auto-fill, minmax(8.6rem, 1fr))' }}>
          <Param label="Earth to move" unit="BCY" value={p.quantityBcy} step={1000} min={1000} onChange={set('quantityBcy')} changed={diff('quantityBcy')} />
          <Param label="Swell, bank → loose" unit="%" value={p.swellPct} step={5} min={0} max={100} onChange={set('swellPct')} changed={diff('swellPct')} />
          <Param label="Excavator bucket" unit="LCY" value={p.bucketLcy} step={0.5} min={0.5} onChange={set('bucketLcy')} changed={diff('bucketLcy')} />
          <Param label="Bucket cycle" unit="s" value={p.bucketCycleS} step={1} min={5} onChange={set('bucketCycleS')} changed={diff('bucketCycleS')} />
          <Param label="Truck capacity" unit="LCY" value={p.truckLcy} step={1} min={1} onChange={set('truckLcy')} changed={diff('truckLcy')} />
          <Param label="Trucks in the fleet" unit="" value={p.trucks} step={1} min={1} max={12} onChange={set('trucks')} changed={diff('trucks')} />
          <Param label="Haul, loaded" unit="min" value={p.haulMin} step={0.5} min={0.5} onChange={set('haulMin')} changed={diff('haulMin')} />
          <Param label="Dump" unit="min" value={p.dumpMin} step={0.5} min={0.5} onChange={set('dumpMin')} changed={diff('dumpMin')} />
          <Param label="Return, empty" unit="min" value={p.returnMin} step={0.5} min={0.5} onChange={set('returnMin')} changed={diff('returnMin')} />
          <Param label="Shift length" unit="h" value={p.shiftHours} step={1} min={1} max={24} onChange={set('shiftHours')} changed={diff('shiftHours')} />
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem 1.4rem', marginTop: '0.7rem', fontSize: '0.82rem', color: 'var(--muted)' }}>
          <span>Load time <strong className="num" style={{ color: 'var(--ink)' }}>{n2(d.loadMin)} min</strong></span>
          <span>Truck cycle <strong className="num" style={{ color: 'var(--ink)' }}>{n2(d.truckCycleMin)} min</strong></span>
          <span>Balance <strong className="num" style={{ color: 'var(--ink)' }}>{n2(d.balance)} → {d.trucksToBalance} trucks</strong></span>
          <span>Excavator <strong className="num" style={{ color: 'var(--ink)' }}>{n0(d.excLcyHr)} LCY/h · {n0(d.excBcyHr)} BCY/h</strong></span>
          <span>Fleet of {p.trucks} <strong className="num" style={{ color: 'var(--ink)' }}>{n0(d.fleetLcyHr)} LCY/h · {n0(d.fleetBcyHr)} BCY/h</strong></span>
          <span>Duration <strong className="num" style={{ color: 'var(--ink)' }}>{n1(d.hours)} h · {d.shifts} shifts</strong></span>
        </div>
      </section>

      <AcdBuilder variant="explore" params={p} onParams={setP} aside={
        <section className="card">
          <div className="label" style={{ marginBottom: '0.35rem' }}>How to use this simulation</div>
          <p style={{ fontSize: '0.84rem', margin: '0 0 0.4rem', lineHeight: 1.4 }}>
            <strong>Simulate</strong>, <strong>Pause</strong> at any moment, change a parameter (above, or − + on the diagram),
            <strong> Apply and continue</strong>. The dotted ghost is the path you left; the gap is what the decision was worth.
          </p>
          <ol style={{ margin: 0, paddingLeft: '1.2rem', fontSize: '0.84rem', lineHeight: 1.4 }}>
            <li style={{ marginBottom: '0.25rem' }}>
              {tag(5, 'Continuous operation')}<strong>Find the balance.</strong> Add a truck whenever the excavator reads
              &ldquo;idle&rdquo;; you should land at {d.trucksToBalance}, where the sweep plot bends.
            </li>
            <li style={{ marginBottom: '0.25rem' }}>
              {tag(1, 'Rate is a slope')}<strong>Read the slope.</strong> The line climbs {n0(d.perTruckLcyHr)} LCY/h per truck until
              the excavator governs at {n0(d.excLcyHr)} (dashed); every decision leaves a kink.
            </li>
            <li style={{ marginBottom: '0.25rem' }}>
              {tag(3, 'Equipment data')}<strong>Change the excavator.</strong> Cut the bucket cycle to 20 s and the balance point
              moves right: a faster loader needs more trucks.
            </li>
            <li style={{ marginBottom: '0.25rem' }}>
              {tag(2, 'Bank vs loose')}<strong>Mind the measure.</strong> The counter fills in loose yards; set swell to 35% and
              the shifts change while nothing on the diagram moves faster.
            </li>
            <li>
              {tag(4, 'What LOAD waits for')}<strong>Watch the COMBI.</strong> Pause with a truck waiting, then with the excavator
              idle: the same LOAD box, starved by a different queue.
            </li>
          </ol>
        </section>
      } />
    </>
  );
}
