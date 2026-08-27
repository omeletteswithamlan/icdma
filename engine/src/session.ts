/**
 * A playable session on top of the interactive engine: seeded RNG, a decision
 * log (so state can be reconstructed by deterministic replay), space-violation
 * preview/resolution, and mid-game Monte-Carlo futures.
 *
 * Futures here deliberately differ from the 2013 system: legacy forecasting
 * ran the Updater variant (different purchase/consumption rules than the game
 * the player experiences — spec divergences A-F) and deadlock-free only by
 * accident. This session forecasts with the SAME interactive physics as play,
 * resolving space violations with the default proportional cut (what a
 * reasonable player does), so the forecast predicts the game you are playing.
 */
import type { Scenario } from './schema.js';
import { Engine, makeAllocation, type Allocation, type TurnResult } from './engine.js';

/** deterministic PRNG (mulberry32) */
export function mulberry32(seed: number): () => number {
  let a = seed >>> 0;
  return () => {
    a |= 0; a = (a + 0x6D2B79F5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export interface PlayerDecision {
  activityId: number;
  /** daily order percent (the OrderPanel spinner) */
  order: number;
  workDays: number;
  workHours: number;
  wageIncentive: number;
}

export interface SpaceViolationInfo {
  spaceAvailable: number;
  spaceRequired: number;
  minimumCut: number;
  /** space units each activity's current order would occupy */
  perActivity: { activityId: number; name: string; space: number }[];
}

export interface TurnReport {
  turn: number;
  events: { name: string; message?: string; activities: number[] }[];
  finished: boolean;
}

export interface FuturesReport {
  samples: number;
  finished: number;
  days: number[]; // completion turn per finished sample
  costs: number[]; // final cost per finished sample
}

export class Session {
  engine: Engine;
  readonly log: PlayerDecision[][] = [];
  constructor(
    readonly scenario: Scenario,
    readonly seed: number = 42,
  ) {
    this.engine = this.fresh(seed);
  }

  private fresh(seed: number): Engine {
    return new Engine(this.scenario, { variant: 'interactive', rng: mulberry32(seed) });
  }

  /** the GUI-default decision for every ready activity */
  defaultDecisions(engine: Engine = this.engine): PlayerDecision[] {
    const out: PlayerDecision[] = [];
    for (const p of engine.network.readyList) {
      const a = p.activity!;
      const val = (100 - p.totalPercentOrdered) * a.duration;
      out.push({
        activityId: a.id,
        order: Math.max(0, Math.min(100, val)),
        workDays: 5,
        workHours: 8,
        wageIncentive: 1.0,
      });
    }
    return out;
  }

  /** space check for the coming delivery, before committing the turn */
  previewSpaceViolation(decisions: PlayerDecision[], engine: Engine = this.engine): SpaceViolationInfo | null {
    const model = engine.model;
    let required = 0;
    const perActivity: SpaceViolationInfo['perActivity'] = [];
    for (const d of decisions) {
      const a = model.activity(d.activityId);
      let s = 0;
      for (const [matId, qty] of a.materialUse) {
        const m = model.materials.get(matId)!;
        s += Math.ceil((qty * d.order) / 100) * m.size;
      }
      required += s;
      if (s > 0) perActivity.push({ activityId: a.id, name: a.name, space: s });
    }
    const available = engine.stock.availableSpace();
    if (required <= available) return null;
    return {
      spaceAvailable: available,
      spaceRequired: required,
      minimumCut: required - available,
      perActivity,
    };
  }

  /**
   * The default proportional cut (the space-violation dialog's default
   * option): scale every space-occupying order so the delivery fits.
   */
  applyProportionalCut(decisions: PlayerDecision[], v: SpaceViolationInfo, cut: number = v.minimumCut): PlayerDecision[] {
    const factor = v.spaceRequired <= 0 ? 0 : Math.max(0, 1 - cut / v.spaceRequired);
    const affected = new Set(v.perActivity.map((p) => p.activityId));
    return decisions.map((d) => (affected.has(d.activityId) ? { ...d, order: d.order * factor } : d));
  }

  private buildTurn(engine: Engine, decisions: PlayerDecision[]): Allocation[] {
    const byId = new Map(decisions.map((d) => [d.activityId, d]));
    const allocs: Allocation[] = [];
    for (const p of engine.network.readyList) {
      const a = p.activity!;
      const d = byId.get(a.id);
      if (!d) continue;
      const alloc = makeAllocation(a.id);
      for (const [matId, qty] of a.materialUse) alloc.requested.set(matId, qty);
      alloc.order = d.order;
      alloc.workDays = d.workDays;
      alloc.workHours = d.workHours;
      alloc.wageIncentive = d.wageIncentive;
      p.totalPercentOrdered += d.order / a.duration; // GUI allocate() semantics
      allocs.push(alloc);
    }
    return allocs;
  }

  private runTurn(engine: Engine, decisions: PlayerDecision[]): TurnResult {
    return engine.update(this.buildTurn(engine, decisions), engine.defaultCrews());
  }

  /** commit a turn with the given decisions (records them in the log) */
  playTurn(decisions: PlayerDecision[]): TurnReport {
    this.log.push(decisions);
    const r = this.runTurn(this.engine, decisions);
    return {
      turn: r.time,
      events: r.fired.map((f) => ({
        name: f.rule.name, message: f.rule.message, activities: f.activities,
      })),
      finished: this.engine.isFinished(),
    };
  }

  /**
   * Step one auto-resolved default turn on an arbitrary engine (futures).
   * Ordering advances from the FINAL (post-cut) order at build time, which is
   * net-equivalent to the GUI's advance-then-rollback dialog flow — so the
   * cut alone reproduces the legacy dialog's bookkeeping.
   */
  private autoTurn(engine: Engine): void {
    let d = this.defaultDecisions(engine);
    const v = this.previewSpaceViolation(d, engine);
    if (v) d = this.applyProportionalCut(d, v);
    this.runTurn(engine, d);
  }

  /**
   * Monte-Carlo futures from the CURRENT state via deterministic replay:
   * rebuild an engine with the session seed, replay the decision log, then
   * continue with auto-resolved default play under a per-sample RNG.
   */
  queryFutures(samples: number, horizon: number = 750): FuturesReport {
    const days: number[] = [];
    const costs: number[] = [];
    let finished = 0;
    for (let i = 0; i < samples; i++) {
      const e = this.fresh(this.seed);
      for (const decisions of this.log) this.runTurn(e, decisions);
      // continue under a different RNG stream per sample
      e.setRng(mulberry32(this.seed ^ (0x9e3779b9 + i)));
      let guard = 0;
      while (!e.isFinished() && guard < horizon) { this.autoTurn(e); guard++; }
      if (e.isFinished()) {
        finished++;
        const lastTurn = Math.max(...e.costTrack.keys());
        days.push(lastTurn);
        costs.push(e.costTrack.get(lastTurn)!);
      }
    }
    return { samples, finished, days, costs };
  }
}
