/**
 * icdma-scenario/1 — the scenario file format for the modernized engine.
 *
 * Design goals:
 * - Round-trips the 12 recovered vcdb scenarios (icdma-scenario-raw/0.1)
 *   losslessly, so the port can be verified against the legacy oracle.
 * - Extends the 2013 model where CE3332-grade realism needs it:
 *   equipment as a first-class resource, and production-based activities
 *   whose durations emerge from quantities and fleet production rates
 *   instead of being fixed inputs.
 */

export interface Scenario {
  format: 'icdma-scenario/1';
  meta: {
    id: string;
    name: string;
    description?: string;
    /** provenance, e.g. "vcdb project 523 (MTU backup, May 2024)" */
    source?: string;
  };
  site: {
    /** storage space available on site (space units) */
    space: number;
    /** fraction of value refunded when returned; legacy overstock_penalty */
    overstockPenalty: number;
  };
  time: {
    /** ISO date of project day 1 */
    startDate: string;
    /** days per simulation turn (legacy project.interval) */
    intervalDays: number;
  };
  costs: {
    /** indirect (overhead) rate; legacy project.overhead */
    overheadRate: number;
  };
  activities: Activity[];
  constraints: Constraint[];
  labor: LaborType[];
  crews: Crew[];
  materials: Material[];
  /** NEW in v1: equipment fleet types (absent from the 2013 model) */
  equipment?: EquipmentType[];
  rules: EventRule[];
  variables: VariableDef[];
}

export interface Activity {
  id: number;
  name: string;
  /** CSI division / responsibility, carried through from legacy data */
  csiDivision?: number;
  responsibility?: number;
  /**
   * Classic mode: fixed duration in days (the 2013 model).
   * Exactly one of `duration` or `work` must be present.
   */
  duration?: number;
  /**
   * NEW in v1 — production mode: duration emerges from quantity and the
   * production rate of the assigned resources (equipment fleet and/or crews),
   * the way CE3332 teaches it (e.g. 20000 BCY moved by an excavator-truck
   * fleet at its balanced production rate).
   */
  work?: {
    quantity: number;
    /** e.g. "BCY", "LCY", "SF", "LF", "tons" */
    unit: string;
  };
  materialUse: { materialId: number; quantity: number }[];
  laborUse: { laborId: number; skilled: number; unskilled: number }[];
  crewUse: number[]; // crew ids
  /** NEW in v1 */
  equipmentUse?: { equipmentId: number; count: number }[];
  /** materials whose availability gates progress (legacy driving_material) */
  drivingMaterials?: number[];
}

export interface Constraint {
  from: number; // activity id
  to: number;
  /** lag in days (legacy constraints.length) */
  lagDays: number;
  /** soft constraints can be violated at a penalty; hard ones cannot */
  soft: boolean;
}

export interface LaborType {
  id: number;
  name: string;
  /** wage per person per day (legacy labor.unitcost semantics per spec) */
  unitCost: number;
}

export interface Crew {
  id: number;
  name: string;
  members: { laborId: number; count: number }[];
}

export interface Material {
  id: number;
  name: string;
  unitCost: number;
  /** space units occupied per unit stored on site (legacy material.area) */
  size: number;
  perishable: boolean;
}

/** NEW in v1 — equipment with CE3332 cost & production structure */
export interface EquipmentType {
  id: number;
  name: string;
  /** production per equipment-hour in the activity's work unit */
  productionRatePerHour?: number;
  /** optional cycle model (production derived: capacity * 60/cycleTimeMin * efficiency) */
  cycle?: {
    capacity: number; // per cycle, in work units (e.g. LCY heaped)
    cycleTimeMinutes: number;
    efficiencyMinutesPerHour: number; // e.g. 50-min hour
  };
  ownershipCostPerHour: number;
  operatingCostPerHour: number;
}

/** Event rule: <precondition, effect, probability> triple (2013 model, kept) */
export interface EventRule {
  id: number;
  name: string;
  /** message shown to the player when the rule fires */
  message?: string;
  probability: number;
  global: boolean;
  preconditions: ConditionRef[];
  postconditions: PostconditionRef[];
}

export interface ConditionRef {
  variableId: number;
  state: string;
  action: ComparatorAction;
}
export type ComparatorAction = 'eq' | 'neq' | 'lt' | 'lte' | 'gt' | 'gte';

export interface PostconditionRef {
  variableId: number;
  state: string;
  /** turns the effect lasts (legacy postcondition.time) */
  time: number;
  action: MutatorAction;
}
export type MutatorAction = 'set' | 'add' | 'mul';

export interface VariableDef {
  id: number;
  label: string;
  global: boolean;
  initialState: string;
  discrete: boolean;
}
