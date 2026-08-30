/**
 * A playable session on top of the interactive engine: seeded RNG, a decision
 * log (so state can be reconstructed by deterministic replay), space-violation
 * preview/resolution, and mid-game Monte-Carlo futures.
 *
 * Futures here deliberately differ from the 2013 system: legacy forecasting
 * ran the Updater variant (different purchase/consumption rules than the game
 * the player experiences — spec divergences A-F) and deadlock-free only by
 * accident. This session forecasts with the SAME interactive physics as play,
 * driven by a demand-paced auto-player (see the AUTO-PLAY POLICY note below)
 * that resolves space violations with the default proportional cut, so the
 * forecast predicts the game you are playing — and completes it even on
 * scenarios where GUI-default ordering deadlocks.
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
  /**
   * per-material request overrides (the resource-request panel); defaults to
   * the activity's daily material use when absent
   */
  requested?: Map<number, number>;
  /**
   * per-crew staffing for this activity's crews — hire above the base
   * complement (diminishing returns) or fire below it (slower pace; losing
   * every foreman stops work). Defaults to the full complement when absent.
   */
  staffing?: { crewId: number; members: { laborId: number; count: number }[] }[];
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
        staffing: a.crewIds.map((crewId) => ({
          crewId,
          members: (engine.model.crewMembers.get(crewId) ?? [])
            .map((m) => ({ laborId: m.laborId, count: m.count })),
        })),
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
      for (const [matId, qty] of a.materialUse) {
        alloc.requested.set(matId, d.requested?.get(matId) ?? qty);
      }
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
    // crews granted this turn: full complements, overridden by any staffing
    // decisions (the hire/fire panel) — legacy semantics: staffing resets to
    // the full complement every turn, exactly as the Swing crew panel did
    const crews = engine.defaultCrews();
    const byCrew = new Map(crews.map((c) => [c.id, c]));
    for (const d of decisions) {
      for (const st of d.staffing ?? []) {
        const crew = byCrew.get(st.crewId);
        if (!crew) continue;
        crew.members = new Map(st.members.map((m) => [m.laborId, Math.max(0, Math.trunc(m.count))]));
      }
    }
    return engine.update(this.buildTurn(engine, decisions), crews);
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

  /*
   * AUTO-PLAY POLICY (clampQuota + autoDecisions + autoTurn), used only by
   * futures. The GUI-default order (100 − totalPercentOrdered) × duration
   * deadlocks on tight-space scenarios: ordered percent reaches 100 while
   * some purchased material never became consumable — rejected by the silent
   * Stock.add space clamp at delivery, destroyed above the trunc'd ordered
   * cap (purchases are ceil'd per turn while the cap truncates), or cleared
   * as perishable on zero-work days — and with orders stuck at 0 forever the
   * activity strands with work left. The auto-player instead re-derives each
   * turn's order from the per-material need/stock/quota ledger (demand-paced,
   * so every lost unit is re-ordered), keeps ordered quota minimal so the
   * engine's surplus-destruction clamp stays armed against dead stock, and
   * surge-requests surpluses so that clamp can actually reclaim site space.
   */

  /**
   * Roll ordered percent back to the tightest quota that still covers every
   * still-needed material's consumption this turn (used + one day's intent,
   * +1 unit for the trunc in orderedAmount) — never above what play already
   * ordered, and always ≤ ~100. This is the auto-player's analogue of the
   * legacy space-violation dialog's setOrdered rollback: session-owned
   * bookkeeping, not engine mechanics. Keeping quota minimal is what arms the
   * surplus-destruction clamp for every material whose need is met, so dead
   * stock is continuously destroyed instead of jamming the site forever.
   */
  private clampQuota(engine: Engine): void {
    for (const p of engine.network.readyList) {
      const a = p.activity!;
      let target = 0;
      for (const [matId, dailyUse] of a.materialUse) {
        if (dailyUse <= 0) continue;
        const need = engine.remainingNeed(a.id, matId);
        if (need <= 0) continue;
        const used = dailyUse * a.duration - need;
        const wanted = used + Math.min(dailyUse, need) + 1;
        target = Math.max(target, (100 * wanted) / (dailyUse * a.duration));
      }
      if (p.totalPercentOrdered > target) p.totalPercentOrdered = target;
    }
  }

  private autoDecisions(engine: Engine): PlayerDecision[] {
    const out: PlayerDecision[] = [];
    for (const p of engine.network.readyList) {
      const a = p.activity!;
      // Cost-weighted aggregate shortfall vs one day's consumable need, over
      // STILL-NEEDED materials only. "Usable" material must be on site, still
      // needed, AND covered by ordered quota — stock without quota is as good
      // as missing (the consumption cap ignores it), so ordering against the
      // gap also mints the quota that unlocks a shared pool. Materials whose
      // need is met contribute nothing: ordering them would grow their quota
      // headroom and disarm the surplus-destruction clamp — the engine's only
      // mechanism for clearing dead stock off a jammed site. The AGGREGATE
      // (not the max) matters because the order is one scalar applied to every
      // material: pacing it to the dollar-weighted gap keeps purchases in step
      // with actual consumption throughput, so no single under-consuming
      // material accumulates stock that would strand when the activity ends.
      let missing$ = 0;
      let daily$ = 0;
      // Surge-request any on-site surplus: requesting the full stock cycles
      // it through the availability check, where the ordered-quota clamp
      // destroys everything above one day's headroom — the engine's only way
      // to reclaim site space from over-delivered buffers (removal is capped
      // at the request, so the default daily request can never drain them).
      // Driving materials stay capped below 2× daily use so the "Driving
      // Material Available" state reads the same as under default requests.
      const requested = new Map<number, number>();
      for (const [matId, dailyUse] of a.materialUse) {
        if (dailyUse <= 0) continue;
        const cost = engine.model.materials.get(matId)!.cost;
        daily$ += dailyUse * cost;
        const stock = engine.stock.qty.get(matId) ?? 0;
        if (stock > dailyUse) {
          requested.set(matId, a.drivingMaterials.includes(matId)
            ? Math.min(stock, 2 * dailyUse - 1)
            : stock);
        }
        const need = engine.remainingNeed(a.id, matId);
        if (need <= 0) continue;
        const used = dailyUse * a.duration - need;
        const headroom = p.orderedAmount(dailyUse, a.duration) - used;
        const usable = Math.min(stock, need, Math.max(0, headroom));
        missing$ += Math.max(0, Math.min(dailyUse, need) - usable) * cost;
      }
      out.push({
        activityId: a.id,
        order: daily$ <= 0 ? 0 : Math.max(0, Math.min(100, (100 * missing$) / daily$)),
        workDays: 5,
        workHours: 8,
        wageIncentive: 1.0,
        requested,
      });
    }
    return out;
  }

  /**
   * Step one auto-resolved turn on an arbitrary engine (futures).
   * Ordering advances from the FINAL (post-cut) order at build time, which is
   * net-equivalent to the GUI's advance-then-rollback dialog flow — so the
   * cut alone reproduces the legacy dialog's bookkeeping.
   */
  private autoTurn(engine: Engine): void {
    this.clampQuota(engine);
    let d = this.autoDecisions(engine);
    const v = this.previewSpaceViolation(d, engine);
    if (v) d = this.applyProportionalCut(d, v);
    this.runTurn(engine, d);
  }

  /**
   * Monte-Carlo futures from the CURRENT state via deterministic replay:
   * rebuild an engine with the session seed, replay the decision log, then
   * continue with auto-resolved default play under a per-sample RNG.
   */
  queryFutures(samples: number, horizon: number = 1000): FuturesReport {
    const days: number[] = [];
    const costs: number[] = [];
    let finished = 0;
    for (let i = 0; i < samples; i++) {
      const e = this.fresh(this.seed);
      e.projectTail = false; // display-only; quadratic in drifted horizon
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
