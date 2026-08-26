/**
 * Convert a legacy export (icdma-scenario-raw/0.1 — the faithful per-table
 * dump of a vcdb project) into the icdma-scenario/1 format.
 */
import type {
  Scenario, Activity, ComparatorAction, MutatorAction,
} from './schema.js';

// Shapes of the raw export (only the fields we consume).
interface RawScenario {
  format: string;
  source: string;
  project: {
    projectid: number; description: string; name: string; space: number;
    overstock_penalty: number; startdate: string; overhead: number; interval: number;
  };
  activities: { activityid: number; description: string; duration: number; csidivisionid: number; responsibilityid: number }[];
  constraints: { fromactivityid: number; toactivityid: number; length: number; soft: boolean }[];
  materialuse: { activityid: number; materialid: number; quantity: number }[];
  laboruse: { activityid: number; laborid: number; quantity_skilled: number; quantity_unskilled: number }[];
  laborcrewuse: { laborcrewid: number; activityid: number }[];
  driving_materials: { activityid: number; materialid: number }[];
  materials: { materialid: number; description: string; unitcost: number; area: number; perishable: boolean }[];
  labor: { laborid: number; description: string; unitcost: number }[];
  laborcrews: { laborcrewid: number; description: string; entries: { laborid: number; amount: number }[] }[];
  rules: {
    ruleid: number; description: string; message: string; probability: number; global: boolean;
    preconditions: { variableid: number; state: string; action: string }[];
    postconditions: { variableid: number; state: string; time: number; action: string }[];
  }[];
  variables: { variableid: number; label: string; global: boolean; initialstate: string; discreet: boolean }[];
}

const COMPARATORS: Record<string, ComparatorAction> = {
  eq: 'eq', neq: 'neq', lt: 'lt', lte: 'lte', gt: 'gt', gte: 'gte',
};
const MUTATORS: Record<string, MutatorAction> = { set: 'set', add: 'add', mul: 'mul' };

export function fromRaw(raw: RawScenario): Scenario {
  if (!raw.format?.startsWith('icdma-scenario-raw/0.1')) {
    throw new Error(`unexpected raw format: ${raw.format}`);
  }
  const p = raw.project;
  const knownMaterials = new Set(raw.materials.map((m) => m.materialid));

  const activities: Activity[] = raw.activities.map((a) => ({
    id: a.activityid,
    name: a.description,
    csiDivision: a.csidivisionid,
    responsibility: a.responsibilityid,
    duration: a.duration,
    // Drop rows referencing materials no longer in the material table — the
    // legacy loader's SQL join had the same effect (seen in the 2008 Steel data).
    materialUse: raw.materialuse
      .filter((m) => m.activityid === a.activityid && knownMaterials.has(m.materialid))
      .map((m) => ({ materialId: m.materialid, quantity: m.quantity })),
    laborUse: raw.laboruse
      .filter((l) => l.activityid === a.activityid)
      .map((l) => ({ laborId: l.laborid, skilled: l.quantity_skilled, unskilled: l.quantity_unskilled })),
    crewUse: raw.laborcrewuse
      .filter((c) => c.activityid === a.activityid)
      .map((c) => c.laborcrewid),
    drivingMaterials: raw.driving_materials
      .filter((d) => d.activityid === a.activityid)
      .map((d) => d.materialid),
  }));

  return {
    format: 'icdma-scenario/1',
    meta: {
      id: `vcdb-${p.projectid}`,
      name: p.description,
      description: p.name,
      source: raw.source,
    },
    site: { space: p.space, overstockPenalty: p.overstock_penalty },
    time: { startDate: p.startdate, intervalDays: p.interval },
    costs: { overheadRate: p.overhead },
    activities,
    constraints: raw.constraints.map((c) => ({
      from: c.fromactivityid, to: c.toactivityid, lagDays: c.length, soft: c.soft,
    })),
    labor: raw.labor.map((l) => ({ id: l.laborid, name: l.description, unitCost: l.unitcost })),
    crews: raw.laborcrews.map((c) => ({
      id: c.laborcrewid, name: c.description,
      members: c.entries.map((e) => ({ laborId: e.laborid, count: e.amount })),
    })),
    materials: raw.materials.map((m) => ({
      id: m.materialid, name: m.description, unitCost: m.unitcost, size: m.area, perishable: m.perishable,
    })),
    rules: raw.rules.map((r) => ({
      id: r.ruleid, name: r.description, message: r.message,
      probability: r.probability, global: r.global,
      preconditions: r.preconditions.map((c) => ({
        variableId: c.variableid, state: c.state,
        action: COMPARATORS[c.action] ?? (() => { throw new Error(`unknown comparator ${c.action}`); })(),
      })),
      postconditions: r.postconditions.map((c) => ({
        variableId: c.variableid, state: c.state, time: c.time,
        action: MUTATORS[c.action] ?? (() => { throw new Error(`unknown mutator ${c.action}`); })(),
      })),
    })),
    variables: raw.variables.map((v) => ({
      id: v.variableid, label: v.label, global: v.global,
      initialState: v.initialstate, discrete: v.discreet,
    })),
  };
}
