/**
 * Off-road hauling (CE3332 Takeaway 6): rolling, grade and effective-grade
 * resistance on a haul profile; power required; available power, gear and
 * speed from a rimpull curve; usable (traction-limited) power; and the travel
 * time that falls out — which is the haul and return time Module 1's cycle
 * model needs.
 *
 * The machine is a SYNTHETIC parametric hauler. Manufacturer rimpull charts
 * are copyrighted and stay out of the platform until permission arrives; the
 * relationships taught here are the textbook ones (Peurifoy & Schexnayder,
 * Nunnally) and do not depend on any particular manufacturer's numbers.
 *
 * Conventions: US customary. Rimpull and resistance in pounds, speed in
 * miles per hour, distance in feet, weight in pounds, grade in percent
 * (positive = uphill in the direction of travel).
 */

export interface Surface {
  id: string;
  label: string;
  /** rolling resistance, lb per ton of gross weight */
  rollingLbPerTon: number;
  /** coefficient of traction on this surface (rubber tires) */
  tractionCoef: number;
}

/** textbook rolling-resistance and traction ranges, not any manufacturer's table */
export const SURFACES: Surface[] = [
  { id: 'paved', label: 'Concrete or asphalt', rollingLbPerTon: 40, tractionCoef: 0.9 },
  { id: 'firm', label: 'Firm, smooth earth, maintained', rollingLbPerTon: 65, tractionCoef: 0.55 },
  { id: 'rutted', label: 'Dirt, rutted, 1–2 in tire penetration', rollingLbPerTon: 100, tractionCoef: 0.5 },
  { id: 'rutted4', label: 'Rutted dirt, 4 in penetration', rollingLbPerTon: 150, tractionCoef: 0.45 },
  { id: 'loose', label: 'Loose sand or gravel', rollingLbPerTon: 200, tractionCoef: 0.3 },
  { id: 'mud', label: 'Soft, muddy', rollingLbPerTon: 300, tractionCoef: 0.25 },
];

export interface Gear { n: number; maxMph: number }

export interface HaulMachine {
  name: string;
  emptyLb: number;
  payloadLb: number;
  /** flywheel horsepower */
  ratedHp: number;
  /** drivetrain efficiency applied to rated power (0–1) */
  drivetrainEff: number;
  /** converter-stall / low-gear cap on rimpull, lb */
  maxRimpullLb: number;
  gears: Gear[];
  /** fraction of gross weight carried by the driving wheels (1 for all-wheel drive) */
  driveWeightFraction: number;
}

/** a made-up but plausible 16-LCY articulated hauler, matched to Module 1's trucks */
export const SYNTHETIC_HAULER: HaulMachine = {
  name: 'Synthetic 16-LCY articulated hauler',
  emptyLb: 48000,
  payloadLb: 42000,
  ratedHp: 310,
  drivetrainEff: 0.8,
  maxRimpullLb: 32000,
  gears: [
    { n: 1, maxMph: 4.5 }, { n: 2, maxMph: 7 }, { n: 3, maxMph: 10.5 }, { n: 4, maxMph: 15 },
    { n: 5, maxMph: 20 }, { n: 6, maxMph: 26 }, { n: 7, maxMph: 32 }, { n: 8, maxMph: 38 },
  ],
  driveWeightFraction: 1,
};

/** 1 % of grade costs 20 lb of resistance per ton of gross weight */
export const LB_PER_TON_PER_PCT = 20;
/** rimpull (lb) = 375 × hp / mph, before efficiency */
export const RIMPULL_CONST = 375;
/** 1 mph = 88 ft/min */
export const FT_PER_MIN_PER_MPH = 88;

export const grossWeightLb = (m: HaulMachine, loaded: boolean) => m.emptyLb + (loaded ? m.payloadLb : 0);

/** rimpull the engine can deliver at a road speed, capped by the converter */
export function rimpullAvailable(m: HaulMachine, mph: number): number {
  if (mph <= 0) return m.maxRimpullLb;
  return Math.min(m.maxRimpullLb, (RIMPULL_CONST * m.ratedHp * m.drivetrainEff) / mph);
}

/** the most rimpull the tires can put on the ground */
export const rimpullUsable = (m: HaulMachine, loaded: boolean, surface: Surface) =>
  surface.tractionCoef * m.driveWeightFraction * grossWeightLb(m, loaded);

export const topSpeedMph = (m: HaulMachine) => m.gears[m.gears.length - 1].maxMph;

/** which gear a road speed falls in (lowest gear whose top speed covers it) */
export function gearAt(m: HaulMachine, mph: number): Gear {
  for (const g of m.gears) if (mph <= g.maxMph + 1e-9) return g;
  return m.gears[m.gears.length - 1];
}

export interface Segment {
  lengthFt: number;
  /** percent, positive uphill in the loaded (haul) direction */
  gradePct: number;
  surface: string; // Surface id
  label?: string;
}

export interface HaulOptions {
  /** posted haul-road limit; the machine never exceeds it even when power would allow */
  speedLimitMph?: number;
}

export type SpeedLimit = 'power' | 'traction' | 'top-speed' | 'speed-limit' | 'stalled';

export interface SegmentSolution {
  label?: string;
  lengthFt: number;
  gradePct: number;
  surface: Surface;
  grossLb: number;
  rollingLbPerTon: number;
  gradeLbPerTon: number;
  totalLbPerTon: number;
  effectiveGradePct: number;
  /** total resistance the machine must overcome, lb (negative downhill) */
  requiredLb: number;
  /** traction limit on this surface, lb */
  usableLb: number;
  /** rimpull at the operating speed, lb */
  availableLb: number;
  mph: number;
  gear: Gear;
  minutes: number;
  /** why the speed is what it is */
  limit: SpeedLimit;
}

export const surfaceById = (id: string): Surface => SURFACES.find((s) => s.id === id) ?? SURFACES[1];

/** the fastest speed at which the machine still overcomes a resistance, before any road limit */
export function speedFor(m: HaulMachine, requiredLb: number, usableLb: number): { mph: number; limit: SpeedLimit } {
  const top = topSpeedMph(m);
  if (requiredLb <= 0) return { mph: top, limit: 'top-speed' };
  if (requiredLb > usableLb) return { mph: 0, limit: 'traction' };
  if (requiredLb > m.maxRimpullLb) return { mph: 0, limit: 'stalled' };
  // rimpull falls as 1/speed, so the fastest speed that still overcomes the resistance is:
  const v = (RIMPULL_CONST * m.ratedHp * m.drivetrainEff) / requiredLb;
  return v >= top ? { mph: top, limit: 'top-speed' } : { mph: v, limit: 'power' };
}

/** solve one segment: resistance → required rimpull → speed → time */
export function solveSegment(m: HaulMachine, seg: Segment, loaded: boolean, opts: HaulOptions = {}): SegmentSolution {
  const surface = surfaceById(seg.surface);
  const grossLb = grossWeightLb(m, loaded);
  const tons = grossLb / 2000;
  const rollingLbPerTon = surface.rollingLbPerTon;
  const gradeLbPerTon = LB_PER_TON_PER_PCT * seg.gradePct;
  const totalLbPerTon = rollingLbPerTon + gradeLbPerTon;
  const effectiveGradePct = totalLbPerTon / LB_PER_TON_PER_PCT;
  const requiredLb = totalLbPerTon * tons;
  const usableLb = rimpullUsable(m, loaded, surface);
  let { mph, limit } = speedFor(m, requiredLb, usableLb);
  if (opts.speedLimitMph && mph > opts.speedLimitMph) { mph = opts.speedLimitMph; limit = 'speed-limit'; }
  const gear = gearAt(m, mph);
  const minutes = mph > 0 ? seg.lengthFt / (mph * FT_PER_MIN_PER_MPH) : Infinity;
  return {
    label: seg.label, lengthFt: seg.lengthFt, gradePct: seg.gradePct, surface, grossLb, rollingLbPerTon, gradeLbPerTon,
    totalLbPerTon, effectiveGradePct, requiredLb, usableLb, availableLb: rimpullAvailable(m, mph), mph, gear, minutes, limit,
  };
}

export interface TripSolution {
  loaded: boolean;
  segments: SegmentSolution[];
  minutes: number;
  lengthFt: number;
  /** distance-weighted average speed */
  avgMph: number;
  stalled: boolean;
}

/** the loaded haul runs the profile as drawn; the empty return runs it backwards with grades reversed */
export function solveTrip(m: HaulMachine, profile: Segment[], loaded: boolean, opts: HaulOptions & { reverse?: boolean } = {}): TripSolution {
  const reverse = opts.reverse ?? !loaded;
  const segs = reverse ? [...profile].reverse().map((s) => ({ ...s, gradePct: s.gradePct === 0 ? 0 : -s.gradePct })) : profile;
  const segments = segs.map((s) => solveSegment(m, s, loaded, opts));
  const minutes = segments.reduce((a, s) => a + s.minutes, 0);
  const lengthFt = segments.reduce((a, s) => a + s.lengthFt, 0);
  const stalled = segments.some((s) => !Number.isFinite(s.minutes));
  return { loaded, segments, minutes, lengthFt, avgMph: stalled || minutes === 0 ? 0 : lengthFt / (minutes * FT_PER_MIN_PER_MPH), stalled };
}

/** sample the machine's rimpull curve for plotting: [mph, lb] pairs up to top speed */
export function rimpullCurve(m: HaulMachine, points = 120): [number, number][] {
  const top = topSpeedMph(m);
  const out: [number, number][] = [];
  for (let i = 1; i <= points; i++) {
    const v = (top * i) / points;
    out.push([v, rimpullAvailable(m, v)]);
  }
  return out;
}

/**
 * The "travel time vs distance" family of curves manufacturers print: for a set
 * of total resistances (percent effective grade), minutes to cover a distance.
 */
export function travelTimeCurves(m: HaulMachine, loaded: boolean, effectiveGrades: number[], maxFt: number, steps = 20, opts: HaulOptions = {}) {
  const tons = grossWeightLb(m, loaded) / 2000;
  return effectiveGrades.map((eg) => {
    // total resistance is expressed as an effective grade, so rolling resistance is already inside it
    const required = LB_PER_TON_PER_PCT * eg * tons;
    let { mph } = speedFor(m, required, Infinity);
    if (opts.speedLimitMph && mph > opts.speedLimitMph) mph = opts.speedLimitMph;
    const pts: [number, number][] = [];
    for (let i = 0; i <= steps; i++) {
      const ft = (maxFt * i) / steps;
      pts.push([ft, mph > 0 ? ft / (mph * FT_PER_MIN_PER_MPH) : Infinity]);
    }
    return { effectiveGradePct: eg, mph, points: pts };
  });
}
