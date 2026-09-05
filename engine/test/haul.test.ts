import { describe, it, expect } from 'vitest';
import {
  SYNTHETIC_HAULER as M, SURFACES, solveSegment, solveTrip, rimpullAvailable, gearAt, grossWeightLb,
  LB_PER_TON_PER_PCT, RIMPULL_CONST, FT_PER_MIN_PER_MPH, travelTimeCurves, type Segment,
} from '../src/haul.js';

describe('haul: resistance, rimpull, speed and time (Takeaway 6)', () => {
  it('effective grade is grade plus rolling resistance over 20 lb/ton', () => {
    const s = solveSegment(M, { lengthFt: 1000, gradePct: 4, surface: 'firm' }, true);
    expect(s.rollingLbPerTon).toBe(65);
    expect(s.gradeLbPerTon).toBe(80);
    expect(s.totalLbPerTon).toBe(145);
    expect(s.effectiveGradePct).toBeCloseTo(7.25, 6);
    expect(s.requiredLb).toBeCloseTo(145 * grossWeightLb(M, true) / 2000, 6);
  });

  it('rimpull falls as 1/speed and is capped by the converter', () => {
    expect(rimpullAvailable(M, 0.1)).toBe(M.maxRimpullLb);
    const v = 10;
    expect(rimpullAvailable(M, v)).toBeCloseTo((RIMPULL_CONST * M.ratedHp * M.drivetrainEff) / v, 6);
    expect(rimpullAvailable(M, 20)).toBeLessThan(rimpullAvailable(M, 10));
  });

  it('speed is the fastest at which rimpull still covers the resistance, and lands in a gear', () => {
    const s = solveSegment(M, { lengthFt: 2000, gradePct: 6, surface: 'firm' }, true);
    expect(s.limit).toBe('power');
    expect(s.availableLb).toBeCloseTo(s.requiredLb, 3);
    expect(s.gear.n).toBe(gearAt(M, s.mph).n);
    expect(s.minutes).toBeCloseTo(2000 / (s.mph * FT_PER_MIN_PER_MPH), 9);
  });

  it('a steeper grade means a lower gear and more time', () => {
    const easy = solveSegment(M, { lengthFt: 2000, gradePct: 2, surface: 'firm' }, true);
    const hard = solveSegment(M, { lengthFt: 2000, gradePct: 8, surface: 'firm' }, true);
    expect(hard.mph).toBeLessThan(easy.mph);
    expect(hard.gear.n).toBeLessThanOrEqual(easy.gear.n);
    expect(hard.minutes).toBeGreaterThan(easy.minutes);
  });

  it('downhill or flat on pavement runs at top speed', () => {
    const s = solveSegment(M, { lengthFt: 3000, gradePct: -4, surface: 'paved' }, false);
    expect(s.limit).toBe('top-speed');
    expect(s.mph).toBe(M.gears[M.gears.length - 1].maxMph);
  });

  it('on a soft surface traction, not power, is what runs out', () => {
    const s = solveSegment(M, { lengthFt: 500, gradePct: 12, surface: 'mud' }, true);
    expect(s.requiredLb).toBeGreaterThan(s.usableLb);
    expect(s.limit).toBe('traction');
    expect(s.minutes).toBe(Infinity);
  });

  it('the empty return runs the profile backwards, reversed grades, lighter machine, and is faster', () => {
    const profile: Segment[] = [
      { lengthFt: 1500, gradePct: 0, surface: 'firm' },
      { lengthFt: 2000, gradePct: 6, surface: 'rutted' },
      { lengthFt: 1000, gradePct: -2, surface: 'firm' },
    ];
    const haul = solveTrip(M, profile, true);
    const back = solveTrip(M, profile, false);
    expect(back.segments.map((s) => s.gradePct)).toEqual([2, -6, 0]);
    expect(back.segments[0].grossLb).toBe(M.emptyLb);
    expect(back.minutes).toBeLessThan(haul.minutes);
    expect(haul.minutes).toBeCloseTo(haul.segments.reduce((a, s) => a + s.minutes, 0), 9);
    expect(haul.lengthFt).toBe(4500);
  });

  it('a flat firm haul road of one mile takes a few minutes loaded', () => {
    const haul = solveTrip(M, [{ lengthFt: 5280, gradePct: 0, surface: 'firm' }], true);
    expect(haul.minutes).toBeGreaterThan(1.5);
    expect(haul.minutes).toBeLessThan(6);
  });

  it('travel-time curves are straight lines through the origin that steepen with resistance', () => {
    const curves = travelTimeCurves(M, true, [2, 8], 4000, 4);
    const [c2, c8] = curves;
    expect(c2.points[0][1]).toBe(0);
    expect(c2.points[4][1]).toBeCloseTo(2 * c2.points[2][1], 9);
    expect(c8.points[4][1]).toBeGreaterThan(c2.points[4][1]);
  });

  it('every surface has a positive rolling resistance and a traction coefficient under 1', () => {
    for (const s of SURFACES) { expect(s.rollingLbPerTon).toBeGreaterThan(0); expect(s.tractionCoef).toBeLessThan(1); }
    expect(LB_PER_TON_PER_PCT).toBe(20);
  });
});

describe('haul: a posted speed limit', () => {
  it('caps power-limited speed and is reported as the reason', () => {
    const free = solveSegment(M, { lengthFt: 5280, gradePct: 0, surface: 'firm' }, true);
    const capped = solveSegment(M, { lengthFt: 5280, gradePct: 0, surface: 'firm' }, true, { speedLimitMph: 25 });
    expect(free.mph).toBeGreaterThan(25);
    expect(capped.mph).toBe(25);
    expect(capped.limit).toBe('speed-limit');
    expect(capped.minutes).toBeCloseTo(5280 / (25 * FT_PER_MIN_PER_MPH), 9);
  });
  it('does not touch a segment that is already slower than the limit', () => {
    const s = solveSegment(M, { lengthFt: 1000, gradePct: 8, surface: 'rutted' }, true, { speedLimitMph: 25 });
    expect(s.limit).toBe('power');
    expect(s.mph).toBeLessThan(25);
  });
});
