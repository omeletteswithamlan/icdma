import { describe, it, expect } from 'vitest';
import { simulate, buildEarthmoving, analyzeEarthmoving, type EarthmovingParams } from '../src/des.js';
import { mulberry32 } from '../src/session.js';

const base: EarthmovingParams = {
  trucks: 4, loaders: 1,
  truckCapacityLcy: 16, passesPerTruck: 8, secondsPerPass: 25,
  haulMin: 8, dumpMin: 1.5, returnMin: 6, swellPct: 25,
};

describe('cycle DES: canonical earthmoving operation', () => {
  it('deterministic sim matches the closed-form system rate (truck-limited)', () => {
    const a = analyzeEarthmoving(base); // cycle 18.83, balance ~5.65 trucks
    expect(a.balancePoint).toBeCloseTo(a.truckCycleMin / a.loadMin, 6);
    const r = simulate(buildEarthmoving(base), { horizon: 8 * 60 });
    const simRate = (r.produced / r.endTime) * 60;
    expect(simRate).toBeGreaterThan(a.systemRateLcyHr * 0.93); // warm-up loss only
    expect(simRate).toBeLessThanOrEqual(a.systemRateLcyHr * 1.02);
  });

  it('above the balance point the loader is the bottleneck and stays busy', () => {
    const p = { ...base, trucks: 8 }; // balance ~5.65
    const a = analyzeEarthmoving(p);
    expect(a.systemRateLcyHr).toBeCloseTo(a.loaderRateLcyHr, 6);
    const r = simulate(buildEarthmoving(p), { horizon: 8 * 60 });
    expect(r.utilization.get('loaderIdle')!).toBeGreaterThan(0.95);
    const simRate = (r.produced / r.endTime) * 60;
    expect(simRate).toBeGreaterThan(a.loaderRateLcyHr * 0.93);
  });

  it('below the balance point trucks stay busy and the loader idles', () => {
    const p = { ...base, trucks: 2 };
    const r = simulate(buildEarthmoving(p), { horizon: 8 * 60 });
    expect(r.utilization.get('trucksIdle')!).toBeGreaterThan(0.9);
    expect(r.utilization.get('loaderIdle')!).toBeLessThan(0.75);
  });

  it('production curve vs trucks bends at the balance point', () => {
    const rates: number[] = [];
    for (let n = 1; n <= 9; n++) {
      const r = simulate(buildEarthmoving({ ...base, trucks: n }), { horizon: 16 * 60 });
      rates.push((r.produced / r.endTime) * 60);
    }
    for (let i = 1; i < 5; i++) expect(rates[i]).toBeGreaterThan(rates[i - 1] * 1.1); // rising
    expect(rates[8]).toBeLessThan(rates[5] * 1.08); // flat past balance (~5.65)
  });

  it('stops at a production target and reports duration (Takeaway 1)', () => {
    const p = { ...base, trucks: 6 };
    const a = analyzeEarthmoving(p);
    const targetLcy = 20000 * (1 + p.swellPct / 100); // 20,000 BCY job
    const r = simulate(buildEarthmoving(p), { stopAtProduction: targetLcy });
    const hours = r.endTime / 60;
    expect(hours).toBeCloseTo(targetLcy / a.systemRateLcyHr, -1); // within ~5 hours
    expect(r.produced).toBeGreaterThanOrEqual(targetLcy);
  });

  it('stochastic runs are seed-deterministic and near the deterministic rate', () => {
    const p = { ...base, jitter: 0.3 };
    const r1 = simulate(buildEarthmoving(p), { horizon: 8 * 60, rng: mulberry32(11) });
    const r2 = simulate(buildEarthmoving(p), { horizon: 8 * 60, rng: mulberry32(11) });
    expect(r1.produced).toBe(r2.produced);
    const det = simulate(buildEarthmoving(base), { horizon: 8 * 60 });
    expect(Math.abs(r1.produced - det.produced) / det.produced).toBeLessThan(0.15);
  });

  it('event log is animation-ready: ordered, balanced starts/ends', () => {
    const r = simulate(buildEarthmoving(base), { horizon: 120 });
    for (let i = 1; i < r.events.length; i++) expect(r.events[i].t).toBeGreaterThanOrEqual(r.events[i - 1].t);
    const starts = r.events.filter((e) => e.type === 'start').length;
    const ends = r.events.filter((e) => e.type === 'end').length;
    expect(starts - ends).toBeLessThanOrEqual(base.trucks + 1);
  });
});

describe('cycle DES: resuming from a saved state (situational play)', () => {
  const p: EarthmovingParams = { trucks: 4, loaders: 1, truckCapacityLcy: 16, passesPerTruck: 8, secondsPerPass: 25, haulMin: 8, dumpMin: 1.5, returnMin: 6, swellPct: 25 };
  const ends = (r: { events: { t: number; type: string; activity: string }[] }) =>
    r.events.filter((e) => e.type === 'end').map((e) => `${e.t.toFixed(6)}:${e.activity}`);

  it('a run cut at t=200 and resumed reproduces the uninterrupted run', () => {
    const model = buildEarthmoving(p);
    const whole = simulate(model, { horizon: 480 });
    const first = simulate(model, { horizon: 200 });
    const rest = simulate(model, { horizon: 480, from: first.finalState });
    expect(first.finalState.t).toBe(200);
    expect(rest.produced).toBe(whole.produced);
    expect(rest.endTime).toBe(whole.endTime);
    expect([...ends(first), ...ends(rest)]).toEqual(ends(whole));
  });

  it('adding trucks at the cut raises production; retiring them lowers it as they return', () => {
    const model = buildEarthmoving(p);
    const first = simulate(model, { horizon: 200 });
    const s = first.finalState;
    const more = simulate(model, { horizon: 480, from: { ...s, counts: { ...s.counts, trucksIdle: (s.counts.trucksIdle ?? 0) + 2 } } });
    const fewer = simulate(model, { horizon: 480, from: { ...s, retire: { trucksIdle: 2 } } });
    const same = simulate(model, { horizon: 480, from: s });
    expect(more.produced).toBeGreaterThan(same.produced);
    expect(fewer.produced).toBeLessThan(same.produced);
    // the retired trucks are gone from the system by the end
    const inSystem = Object.values(fewer.finalState.counts).reduce((a, b) => a + b, 0) - (fewer.finalState.counts.loaderIdle ?? 0) + fewer.finalState.pending.length;
    expect(inSystem).toBe(2);
  });
});
