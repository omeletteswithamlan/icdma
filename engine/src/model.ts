/**
 * Runtime project model: the scenario resolved into id-keyed structures with
 * the as-planned start relaxation applied. Faithful to the legacy semantics
 * documented in docs/engine-spec/network-and-costs.md §1.
 */
import type { Scenario } from './schema.js';

export interface RtConstraint {
  fromId: number;
  toId: number;
  length: number;
  soft: boolean;
}

export interface RtActivity {
  /** renumbered 1..n in original activity-id order (legacy step 13) */
  id: number;
  /** the original scenario/database id (what the "ID" variable reports) */
  realId: number;
  name: string;
  duration: number;
  /** as-planned start, after constraint relaxation; day 1 = first day */
  start: number;
  /** materialId -> per-DAY quantity */
  materialUse: Map<number, number>;
  crewIds: number[];
  drivingMaterials: number[];
  /** outgoing constraints (this activity is `from`) */
  links: RtConstraint[];
}

export interface RtMaterial {
  id: number;
  name: string;
  cost: number;
  size: number;
  perishable: boolean;
}

export class ProjectModel {
  readonly activities: RtActivity[] = []; // ordered by id ASC (1..n)
  readonly constraints: RtConstraint[] = [];
  readonly materials = new Map<number, RtMaterial>();
  readonly laborCost = new Map<number, number>(); // laborId -> daily unit cost
  readonly laborName = new Map<number, string>(); // needed by compareProductivity
  /** crewId -> members sorted by laborId ASC (float-order fidelity) */
  readonly crewMembers = new Map<number, { laborId: number; count: number }[]>();
  readonly overhead: number;
  readonly overstockPenalty: number;
  readonly space: number;
  readonly intervalDays: number;
  readonly startDate: Date;

  private byId = new Map<number, RtActivity>();

  constructor(readonly scenario: Scenario) {
    this.overhead = scenario.costs.overheadRate;
    this.overstockPenalty = scenario.site.overstockPenalty;
    this.space = scenario.site.space;
    this.intervalDays = scenario.time.intervalDays;
    this.startDate = new Date(scenario.time.startDate);

    for (const m of scenario.materials) {
      this.materials.set(m.id, {
        id: m.id, name: m.name, cost: m.unitCost, size: m.size, perishable: m.perishable,
      });
    }
    for (const l of scenario.labor) {
      this.laborCost.set(l.id, l.unitCost);
      this.laborName.set(l.id, l.name);
    }
    for (const c of scenario.crews) {
      this.crewMembers.set(
        c.id,
        [...c.members].sort((a, b) => a.laborId - b.laborId),
      );
    }

    // Activities in original-id order, renumbered 1..n; realId preserved.
    const sorted = [...scenario.activities].sort((a, b) => a.id - b.id);
    const realToNew = new Map<number, number>();
    sorted.forEach((a, i) => realToNew.set(a.id, i + 1));

    for (const a of sorted) {
      if (a.duration === undefined) {
        throw new Error(`activity ${a.id} has no duration (production mode not yet resolved)`);
      }
      const rt: RtActivity = {
        id: realToNew.get(a.id)!,
        realId: a.id,
        name: a.name,
        duration: a.duration,
        start: 1,
        materialUse: new Map(a.materialUse.map((m) => [m.materialId, m.quantity])),
        crewIds: [...a.crewUse],
        drivingMaterials: a.drivingMaterials ?? [],
        links: [],
      };
      this.activities.push(rt);
      this.byId.set(rt.id, rt);
    }

    for (const c of scenario.constraints) {
      const rt: RtConstraint = {
        fromId: realToNew.get(c.from)!,
        toId: realToNew.get(c.to)!,
        length: c.lagDays,
        soft: c.soft,
      };
      this.constraints.push(rt);
      this.byId.get(rt.fromId)!.links.push(rt);
    }

    // Monotone forward relaxation to fixed point (legacy Activity.setStart).
    let changed = true;
    while (changed) {
      changed = false;
      for (const c of this.constraints) {
        const from = this.byId.get(c.fromId)!;
        const to = this.byId.get(c.toId)!;
        const min = from.start + from.duration + c.length;
        if (to.start < min) {
          to.start = min;
          changed = true;
        }
      }
    }
  }

  activity(id: number): RtActivity {
    const a = this.byId.get(id);
    if (!a) throw new Error(`no activity ${id}`);
    return a;
  }

  /** Σ per-day quantity × material cost (materialUse iteration order) */
  dailyMaterialCost(a: RtActivity): number {
    let sum = 0;
    for (const [matId, qty] of a.materialUse) sum += qty * this.materials.get(matId)!.cost;
    return sum;
  }

  /** crew daily cost: members in laborId ASC order */
  crewDailyCost(crewId: number): number {
    let sum = 0;
    for (const m of this.crewMembers.get(crewId) ?? []) {
      sum += this.laborCost.get(m.laborId)! * m.count;
    }
    return sum;
  }

  dailyLaborCost(a: RtActivity): number {
    let sum = 0;
    for (const crewId of a.crewIds) sum += this.crewDailyCost(crewId);
    return sum;
  }

  dailyCost(a: RtActivity): number {
    return this.dailyMaterialCost(a) * (this.overhead + 1) + this.dailyLaborCost(a);
  }

  /** BAC / PV: as-planned total for one activity */
  total(a: RtActivity): number {
    return this.dailyCost(a) * a.duration;
  }

  totalMaterial(a: RtActivity): number {
    return this.dailyMaterialCost(a) * a.duration;
  }
}
