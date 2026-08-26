/**
 * Cost schedules: cumulative per-activity, per-day material/labor/indirect
 * series, and the as-planned/as-built projection loop. Faithful port of
 * engine-spec/network-and-costs.md §4.
 */
import type { Network } from './network.js';
import type { ProjectModel } from './model.js';

export class CostSchedule {
  private material = new Map<number, Map<number, number>>(); // actId -> day -> cumulative
  private labor = new Map<number, Map<number, number>>();
  private indirect = new Map<number, Map<number, number>>();
  private stockValue = new Map<number, number>();

  /** false while inside a Monte-Carlo future (legacy querymode): record nothing */
  recording = true;

  private get(map: Map<number, Map<number, number>>, actId: number, day: number): number {
    return map.get(actId)?.get(day) ?? 0;
  }

  private set(map: Map<number, Map<number, number>>, actId: number, day: number, v: number): void {
    if (!this.recording) return;
    let inner = map.get(actId);
    if (!inner) map.set(actId, (inner = new Map()));
    inner.set(day, v);
  }

  getMaterial(actId: number, day: number): number { return this.get(this.material, actId, day); }
  getLabor(actId: number, day: number): number { return this.get(this.labor, actId, day); }
  getIndirect(actId: number, day: number): number { return this.get(this.indirect, actId, day); }
  setMaterial(actId: number, day: number, v: number): void { this.set(this.material, actId, day, v); }
  setLabor(actId: number, day: number, v: number): void { this.set(this.labor, actId, day, v); }
  setIndirect(actId: number, day: number, v: number): void { this.set(this.indirect, actId, day, v); }
  addMaterial(actId: number, day: number, v: number): void {
    this.set(this.material, actId, day, v + this.getMaterial(actId, day));
  }
  addLabor(actId: number, day: number, v: number): void {
    this.set(this.labor, actId, day, v + this.getLabor(actId, day));
  }
  addIndirect(actId: number, day: number, v: number): void {
    this.set(this.indirect, actId, day, v + this.getIndirect(actId, day));
  }
  setStockValue(day: number, v: number): void {
    if (this.recording) this.stockValue.set(day, v);
  }
  getStockValue(day: number): number { return this.stockValue.get(day) ?? 0; }

  /** roll-ups iterate activities in id ASC order (float-order fidelity) */
  materialTotal(model: ProjectModel, day: number): number {
    let sum = 0;
    for (const a of model.activities) sum += this.getMaterial(a.id, day);
    return sum + this.getStockValue(day);
  }

  laborTotal(model: ProjectModel, day: number): number {
    let sum = 0;
    for (const a of model.activities) sum += this.getLabor(a.id, day);
    return sum;
  }

  indirectTotal(model: ProjectModel, day: number): number {
    let sum = 0;
    for (const a of model.activities) sum += this.getIndirect(a.id, day);
    return sum;
  }

  /**
   * Cumulative total per day, keys 1 .. lastTimeStep-1 (exclusive bound is
   * legacy behavior — for project 523 the lastKey is 13).
   */
  totalSeries(model: ProjectModel, lastTimeStep: number): Map<number, number> {
    const out = new Map<number, number>();
    for (let x = 1; x < lastTimeStep; x++) {
      out.set(x, this.materialTotal(model, x) + this.laborTotal(model, x) + this.indirectTotal(model, x));
    }
    return out;
  }
}

/**
 * Project cumulative daily costs over [firstDay, lastDay] from the current
 * network state (legacy AgentM.computeCost).
 */
export function computeCost(
  sched: CostSchedule,
  firstDay: number,
  lastDay: number,
  network: Network,
  model: ProjectModel,
): void {
  const material = new Map<number, number>();
  const labor = new Map<number, number>();
  const indirect = new Map<number, number>();
  for (const a of model.activities) {
    material.set(a.id, sched.getMaterial(a.id, firstDay - 1));
    labor.set(a.id, sched.getLabor(a.id, firstDay - 1));
    indirect.set(a.id, sched.getIndirect(a.id, firstDay - 1));
  }

  for (let x = firstDay; x <= lastDay; x++) {
    sched.setStockValue(x, 0);
    for (const anode of network.aNodes) {
      if (!anode.outPrimary) continue; // start nodes only
      const a = anode.activity!;
      const start = anode.earlyStart;
      const end = anode.outPrimary.head.outPrimary!.head.earlyStart;
      if (x >= start && x < end) {
        const mat = model.dailyMaterialCost(a);
        labor.set(a.id, labor.get(a.id)! + model.dailyLaborCost(a));
        material.set(a.id, material.get(a.id)! + mat);
        indirect.set(a.id, indirect.get(a.id)! + mat * model.overhead);
      }
    }
    for (const a of model.activities) {
      sched.setMaterial(a.id, x, material.get(a.id)!);
      sched.setLabor(a.id, x, labor.get(a.id)!);
      sched.setIndirect(a.id, x, indirect.get(a.id)!);
    }
  }
}
