/**
 * Simulation variables and event rules. Faithful port of
 * engine-spec/turn-loop.md §2 (Environment, Rule.apply, Condition semantics).
 */
import type { Scenario, EventRule } from './schema.js';
import type { ProjectModel, RtActivity } from './model.js';
import type { Network } from './network.js';

export interface SimVariable {
  label: string;
  discrete: boolean;
  state: string | number;
  defstate: string | number;
  timespan: number;
}

function makeVar(label: string, discrete: boolean, initial: string): SimVariable {
  const state = discrete ? initial : Number(initial);
  return { label, discrete, state, defstate: state, timespan: 0 };
}

export function setState(v: SimVariable, value: string | number, time: number): void {
  v.state = value;
  v.timespan = time;
  if (v.discrete && time === 0) v.defstate = value; // permanent + rewrites default
}

export class Environment {
  readonly globals = new Map<string, SimVariable>();
  /** activityId -> label -> variable (each non-global var cloned per activity) */
  readonly locals = new Map<number, Map<string, SimVariable>>();

  constructor(scenario: Scenario, model: ProjectModel) {
    const defs = [...scenario.variables];
    // Synthetic fallbacks (legacy DatabaseInterpreter): only when label absent.
    const labels = new Set(defs.map((v) => v.label));
    if (!labels.has('ID')) defs.push({ id: -1, label: 'ID', global: false, initialState: '0', discrete: true });
    if (!labels.has('ActivityTime')) defs.push({ id: -2, label: 'ActivityTime', global: false, initialState: '0', discrete: true });
    if (!labels.has('Productivity')) defs.push({ id: -3, label: 'Productivity', global: false, initialState: '1.0', discrete: false });

    for (const a of model.activities) this.locals.set(a.id, new Map());
    for (const d of defs) {
      if (d.global) {
        this.globals.set(d.label, makeVar(d.label, d.discrete, d.initialState));
      } else {
        for (const a of model.activities) {
          this.locals.get(a.id)!.set(d.label, makeVar(d.label, d.discrete, d.initialState));
        }
      }
    }
  }

  getGlobal(label: string): SimVariable | undefined {
    return this.globals.get(label);
  }

  /** local first, then global fallback (legacy Environment.getVariable) */
  get(actId: number, label: string): SimVariable | undefined {
    return this.locals.get(actId)?.get(label) ?? this.globals.get(label);
  }

  /**
   * Start-of-turn variable maintenance (legacy Environment.update).
   * Material-linked continuous variables (matassoc) are not carried by the
   * scenario format yet — none of the recovered scenarios use them.
   */
  update(t: number, network: Network, model: ProjectModel): void {
    const all: SimVariable[] = [...this.globals.values()];
    for (const m of this.locals.values()) all.push(...m.values());
    for (const v of all) {
      v.timespan -= 1;
      if (v.timespan === 0) v.state = v.defstate;
    }
    for (const a of model.activities) {
      const locals = this.locals.get(a.id)!;
      const id = locals.get('ID');
      if (id?.discrete) setState(id, String(a.realId), 0);
      const at = locals.get('ActivityTime');
      if (at?.discrete) {
        const start = network.startNode(a).earlyStart;
        const end = network.endNode(a).earlyStart;
        setState(at, t < start || t > end ? '-1' : String(t - start), 0);
      }
    }
  }
}

function conditionMet(v: SimVariable, state: string, action: string): boolean {
  if (v.discrete) {
    // legacy: gte/lte/eq collapse to equality, gt/lt/neq to inequality
    const eq = String(v.state) === state;
    return action === 'gt' || action === 'lt' || action === 'neq' ? !eq : eq;
  }
  const s = Number(v.state);
  const x = Number(state);
  switch (action) {
    case 'eq': return s === x;
    case 'neq': return s !== x;
    case 'lt': return s < x;
    case 'lte': return s <= x;
    case 'gt': return s > x;
    case 'gte': return s >= x;
    default: return false;
  }
}

function applyPostcondition(v: SimVariable, state: string, time: number, action: string): void {
  if (v.discrete) {
    setState(v, state, time); // set/add/mul all behave as set for discrete
  } else {
    const x = Number(state);
    const cur = Number(v.state);
    setState(v, action === 'add' ? cur + x : action === 'mul' ? cur * x : x, time);
  }
}

export interface FiredRule {
  rule: EventRule;
  /** empty for a global firing; activity ids for local firings */
  activities: number[];
}

/**
 * Apply all rules for the turn (legacy step 8). Probability is sampled BEFORE
 * preconditions; global rules resolve conditions in global scope only (a local
 * target silently no-ops — load-bearing legacy behavior); local rules draw once
 * per ready activity in ready-list order.
 */
export function applyRules(
  rules: EventRule[],
  env: Environment,
  readyActivities: RtActivity[],
  varLabel: (variableId: number) => string,
  rng: () => number,
): FiredRule[] {
  const fired: FiredRule[] = [];
  for (const rule of rules) {
    if (rule.global) {
      if (rng() >= rule.probability) continue;
      let met = true;
      for (const c of rule.preconditions) {
        const v = env.getGlobal(varLabel(c.variableId));
        if (!v || !conditionMet(v, c.state, c.action)) { met = false; break; }
      }
      if (!met) continue;
      for (const c of rule.postconditions) {
        const v = env.getGlobal(varLabel(c.variableId));
        if (v) applyPostcondition(v, c.state, c.time, c.action); // silent no-op when local
      }
      fired.push({ rule, activities: [] });
    } else {
      const hit: number[] = [];
      for (const a of readyActivities) {
        if (rng() >= rule.probability) continue;
        let met = true;
        for (const c of rule.preconditions) {
          const v = env.get(a.id, varLabel(c.variableId));
          if (!v || !conditionMet(v, c.state, c.action)) { met = false; break; }
        }
        if (!met) continue;
        for (const c of rule.postconditions) {
          const v = env.get(a.id, varLabel(c.variableId));
          if (v) applyPostcondition(v, c.state, c.time, c.action);
        }
        hit.push(a.id);
      }
      if (hit.length > 0) fired.push({ rule, activities: hit });
    }
  }
  return fired;
}
