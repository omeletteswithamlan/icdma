/**
 * A compact cycle-DES in the CYCLONE family, built for teaching operation
 * design (CE3332 Takeaways 1-5): queues hold resource tokens, combi
 * activities fire when every input queue can supply its tokens, normal
 * activities are pass-through delays, and a counter records production.
 *
 * Distinct from the TONAE project engine by design: TONAE simulates a
 * project's weeks; this simulates an operation's minutes — the modeling
 * layer the original research compared itself against (CYCLONE/STROBOSCOPE),
 * now serving as its teaching companion.
 */

export type Dist =
  | { kind: 'const'; value: number }
  | { kind: 'uniform'; low: number; high: number }
  | { kind: 'triangular'; low: number; mode: number; high: number };

export function sample(d: Dist, rng: () => number): number {
  switch (d.kind) {
    case 'const': return d.value;
    case 'uniform': return d.low + (d.high - d.low) * rng();
    case 'triangular': {
      const u = rng();
      const f = (d.mode - d.low) / (d.high - d.low);
      return u < f
        ? d.low + Math.sqrt(u * (d.high - d.low) * (d.mode - d.low))
        : d.high - Math.sqrt((1 - u) * (d.high - d.low) * (d.high - d.mode));
    }
  }
}

export interface QueueDef {
  id: string;
  label: string;
  /** tokens present at t=0 */
  initial: number;
  /** true if this queue's tokens are a resource whose idleness we report */
  resource?: boolean;
}

export interface ActivityDef {
  id: string;
  label: string;
  duration: Dist;
  /** tokens drawn when the activity starts (combi if more than one source) */
  takes: { queue: string; n: number }[];
  /** tokens deposited when the activity ends */
  gives: { queue: string; n: number }[];
  /** each completion adds this much to the production counter */
  produces?: number;
}

export interface CycleModel {
  queues: QueueDef[];
  activities: ActivityDef[];
}

export interface SimEvent {
  t: number;
  type: 'start' | 'end';
  activity: string;
}

export interface QueueSample { t: number; queue: string; count: number }

export interface SimResult {
  /** total simulated time (same unit as durations) */
  endTime: number;
  /** production counter total */
  produced: number;
  /** completions per activity */
  firings: Map<string, number>;
  /** fraction of time each resource queue's tokens spent away from home */
  utilization: Map<string, number>;
  /** average token count per queue */
  avgQueue: Map<string, number>;
  events: SimEvent[];
  queueTimeline: QueueSample[];
}

export interface SimOptions {
  /** stop after this much simulated time */
  horizon?: number;
  /** stop once the production counter reaches this */
  stopAtProduction?: number;
  rng?: () => number;
  /** cap on recorded events (timelines keep recording) */
  maxEvents?: number;
}

interface Pending { t: number; seq: number; activity: ActivityDef }

export function simulate(model: CycleModel, opts: SimOptions = {}): SimResult {
  const horizon = opts.horizon ?? Infinity;
  const stopAt = opts.stopAtProduction ?? Infinity;
  const rng = opts.rng ?? (() => 0.5);
  const maxEvents = opts.maxEvents ?? 20000;
  if (!Number.isFinite(horizon) && !Number.isFinite(stopAt)) {
    throw new Error('simulate needs a horizon or a production target');
  }

  const count = new Map<string, number>();
  const initial = new Map<string, number>();
  for (const q of model.queues) {
    count.set(q.id, q.initial);
    initial.set(q.id, q.initial);
  }

  const firings = new Map<string, number>();
  for (const a of model.activities) firings.set(a.id, 0);

  // time-weighted token integrals, for utilization and average queue length
  const integral = new Map<string, number>();
  for (const q of model.queues) integral.set(q.id, 0);
  let lastT = 0;

  const events: SimEvent[] = [];
  const timeline: QueueSample[] = [];
  const pending: Pending[] = [];
  let seq = 0;
  let produced = 0;
  let now = 0;

  const record = (e: SimEvent) => { if (events.length < maxEvents) events.push(e); };
  const snapshot = (t: number) => {
    if (timeline.length < maxEvents) {
      for (const q of model.queues) timeline.push({ t, queue: q.id, count: count.get(q.id)! });
    }
  };
  const advanceIntegrals = (t: number) => {
    const dt = t - lastT;
    if (dt > 0) for (const q of model.queues) integral.set(q.id, integral.get(q.id)! + count.get(q.id)! * dt);
    lastT = t;
  };

  const canStart = (a: ActivityDef) => a.takes.every((x) => (count.get(x.queue) ?? 0) >= x.n);

  const tryStarts = () => {
    // definition order, repeated until nothing else can fire (legacy scanning style)
    let started = true;
    while (started) {
      started = false;
      for (const a of model.activities) {
        if (canStart(a)) {
          for (const x of a.takes) count.set(x.queue, count.get(x.queue)! - x.n);
          const d = Math.max(0, sample(a.duration, rng));
          pending.push({ t: now + d, seq: seq++, activity: a });
          record({ t: now, type: 'start', activity: a.id });
          started = true;
        }
      }
    }
  };

  snapshot(0);
  tryStarts();
  snapshot(0);

  while (pending.length > 0 && now <= horizon && produced < stopAt) {
    pending.sort((x, y) => x.t - y.t || x.seq - y.seq);
    const next = pending.shift()!;
    if (next.t > horizon) { advanceIntegrals(horizon); now = horizon; break; }
    advanceIntegrals(next.t);
    now = next.t;
    const a = next.activity;
    for (const x of a.gives) count.set(x.queue, (count.get(x.queue) ?? 0) + x.n);
    firings.set(a.id, firings.get(a.id)! + 1);
    if (a.produces) produced += a.produces;
    record({ t: now, type: 'end', activity: a.id });
    tryStarts();
    snapshot(now);
  }
  if (pending.length === 0) advanceIntegrals(now);

  const endTime = Math.min(now, horizon);
  const utilization = new Map<string, number>();
  const avgQueue = new Map<string, number>();
  for (const q of model.queues) {
    const avg = endTime > 0 ? integral.get(q.id)! / endTime : q.initial;
    avgQueue.set(q.id, avg);
    if (q.resource && q.initial > 0) {
      utilization.set(q.id, Math.min(1, Math.max(0, 1 - avg / q.initial)));
    }
  }

  return { endTime, produced, firings, utilization, avgQueue, events, queueTimeline: timeline };
}

/* ------------------------------------------------------------------ */
/* The canonical earthmoving operation (Takeaways 1-5)                  */
/* ------------------------------------------------------------------ */

export interface EarthmovingParams {
  trucks: number;
  loaders: number;
  /** heaped truck capacity, LCY */
  truckCapacityLcy: number;
  /** bucket passes to fill a truck (from capacity / bucket size, rounded up) */
  passesPerTruck: number;
  /** seconds per bucket pass (excavator cycle time) */
  secondsPerPass: number;
  /** one-way haul, dump, and return times in minutes */
  haulMin: number;
  dumpMin: number;
  returnMin: number;
  /** percent swell from bank to loose */
  swellPct: number;
  /** optional variability: fraction of each duration, triangular */
  jitter?: number;
}

export function buildEarthmoving(p: EarthmovingParams): CycleModel {
  const loadMin = (p.passesPerTruck * p.secondsPerPass) / 60;
  const d = (value: number): Dist => {
    if (!p.jitter) return { kind: 'const', value };
    return { kind: 'triangular', low: value * (1 - p.jitter), mode: value, high: value * (1 + p.jitter) };
  };
  return {
    queues: [
      { id: 'trucksIdle', label: 'Trucks waiting to load', initial: p.trucks, resource: true },
      { id: 'loaderIdle', label: 'Excavator idle', initial: p.loaders, resource: true },
      { id: 'loaded', label: 'Loaded trucks', initial: 0 },
      { id: 'atDump', label: 'Trucks at dump', initial: 0 },
      { id: 'returning', label: 'Trucks returning', initial: 0 },
    ],
    activities: [
      {
        id: 'load', label: 'Load truck', duration: d(loadMin),
        takes: [{ queue: 'trucksIdle', n: 1 }, { queue: 'loaderIdle', n: 1 }],
        gives: [{ queue: 'loaded', n: 1 }, { queue: 'loaderIdle', n: 1 }],
      },
      {
        id: 'haul', label: 'Haul', duration: d(p.haulMin),
        takes: [{ queue: 'loaded', n: 1 }],
        gives: [{ queue: 'atDump', n: 1 }],
      },
      {
        id: 'dump', label: 'Dump', duration: d(p.dumpMin),
        takes: [{ queue: 'atDump', n: 1 }],
        gives: [{ queue: 'returning', n: 1 }],
        produces: p.truckCapacityLcy,
      },
      {
        id: 'return', label: 'Return', duration: d(p.returnMin),
        takes: [{ queue: 'returning', n: 1 }],
        gives: [{ queue: 'trucksIdle', n: 1 }],
      },
    ],
  };
}

export interface EarthmovingAnalysis {
  loadMin: number;
  truckCycleMin: number;
  /** LCY/hr the loader can produce if never starved */
  loaderRateLcyHr: number;
  /** LCY/hr one truck can deliver if never queued */
  truckRateLcyHr: number;
  /** analytic system rate: min(loader, n x truck) */
  systemRateLcyHr: number;
  systemRateBcyHr: number;
  /** trucks needed so the loader never idles (continuous operation) */
  balancePoint: number;
}

/** the closed-form relationships the classwork teaches (Takeaways 1, 2, 5) */
export function analyzeEarthmoving(p: EarthmovingParams): EarthmovingAnalysis {
  const loadMin = (p.passesPerTruck * p.secondsPerPass) / 60;
  const truckCycleMin = loadMin + p.haulMin + p.dumpMin + p.returnMin;
  const loaderRateLcyHr = (60 / loadMin) * p.truckCapacityLcy * p.loaders;
  const truckRateLcyHr = (60 / truckCycleMin) * p.truckCapacityLcy;
  const systemRateLcyHr = Math.min(loaderRateLcyHr, p.trucks * truckRateLcyHr);
  const systemRateBcyHr = systemRateLcyHr / (1 + p.swellPct / 100);
  return {
    loadMin,
    truckCycleMin,
    loaderRateLcyHr,
    truckRateLcyHr,
    systemRateLcyHr,
    systemRateBcyHr,
    balancePoint: (truckCycleMin / loadMin) * p.loaders,
  };
}
