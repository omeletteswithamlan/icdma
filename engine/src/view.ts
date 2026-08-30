/**
 * Read-only view models for a UI — the modern equivalent of the read-API list
 * in the legacy TODO file ("GanttChartPanel needs: start, finish, completion
 * amount, name…").
 */
import type { Engine } from './engine.js';

export interface ScheduleRow {
  id: number;
  name: string;
  /** as-planned window (static) */
  plannedStart: number;
  plannedEnd: number;
  /** live network window */
  start: number;
  end: number;
  lateStart: number;
  lateFinish: number;
  critical: boolean;
  active: boolean;
  percentComplete: number;
}

export function scheduleRows(engine: Engine): ScheduleRow[] {
  const late = engine.network.lateSchedule(engine.time);
  return engine.model.activities.map((a) => {
    const p = engine.network.pnodes.get(a.id)!;
    const l = late.get(a.id)!;
    return {
      id: a.id,
      name: a.name,
      plannedStart: a.start,
      plannedEnd: a.start + a.duration,
      start: engine.network.earlyStart(a),
      end: engine.network.earlyFinish(a),
      lateStart: l.ls,
      lateFinish: l.lf,
      critical: engine.network.isCritical(a, late),
      active: p.isActive,
      percentComplete: p.percentCompletion(),
    };
  });
}

export interface CostView {
  /** cumulative as-planned total per day (the baseline curve) */
  planned: [number, number][];
  /** cumulative as-built per day: actuals through `today`, projection after */
  built: [number, number][];
  today: number;
  plannedTotal: number;
  builtToDate: number;
}

export function costView(engine: Engine): CostView {
  const last = engine.network.lastTimeStep();
  // the baseline was computed once at load — read it over its own horizon,
  // not the drifted one (where it has no entries and would read as zero)
  const planned = [...engine.asPlanned.totalSeries(engine.model, engine.initialLastTimeStep).entries()];
  const built = [...engine.asBuilt.totalSeries(engine.model, last).entries()];
  const today = engine.time - 1; // last completed turn
  const plannedTotal = planned.length ? planned[planned.length - 1][1] : 0;
  const builtRow = built.filter(([d]) => d <= today).pop();
  return {
    planned,
    built,
    today,
    plannedTotal,
    builtToDate: builtRow ? builtRow[1] : 0,
  };
}

export interface StatusView {
  turn: number;
  dateISO: string;
  lastTimeStep: number;
  weather: string | null;
  spaceUsed: number;
  spaceTotal: number;
  finished: boolean;
}

export function statusView(engine: Engine): StatusView {
  const ms = Date.parse(engine.model.scenario.time.startDate.slice(0, 10) + 'T00:00:00Z')
    + (engine.time - 1) * engine.model.intervalDays * 86400000;
  return {
    turn: engine.time,
    dateISO: new Date(ms).toISOString().slice(0, 10),
    lastTimeStep: engine.network.lastTimeStep(),
    weather: (engine.env.getGlobal('Weather')?.state as string) ?? null,
    spaceUsed: engine.stock.usedSpace(),
    spaceTotal: engine.model.space,
    finished: engine.isFinished(),
  };
}

export interface CrewMemberView {
  laborId: number;
  name: string;
  unitCost: number;
  baseCount: number;
  count: number;
  isEquipment: boolean;
  isSupervision: boolean;
}

export interface CrewStaffView {
  crewId: number;
  name: string;
  members: CrewMemberView[];
  dailyCost: number;
  /** pace multiplier this staffing produces (before hours/incentive factors) */
  pace: number;
  warnings: string[];
}

/**
 * Staffing preview for one activity: the pace ratio and wage bill the
 * player's hire/fire choices produce, with the engine's hard rules surfaced
 * as warnings (foreman required; crane/oiler must be fully staffed;
 * extra hands above the complement are 80% effective).
 */
export function staffingView(
  engine: Engine,
  activityId: number,
  staffing: { crewId: number; members: { laborId: number; count: number }[] }[] | undefined,
): CrewStaffView[] {
  const model = engine.model;
  const a = model.activity(activityId);
  const crewName = (id: number) => engine.model.scenario.crews.find((c) => c.id === id)?.name ?? `Crew ${id}`;
  const out: CrewStaffView[] = [];
  for (const crewId of a.crewIds) {
    const base = model.crewMembers.get(crewId) ?? [];
    const chosen = staffing?.find((s) => s.crewId === crewId);
    const counts = new Map(chosen?.members.map((m) => [m.laborId, m.count])
      ?? base.map((m) => [m.laborId, m.count]));
    const members: CrewMemberView[] = base.map((m) => {
      const name = model.laborName.get(m.laborId) ?? `#${m.laborId}`;
      return {
        laborId: m.laborId,
        name: name.replace(/^\((equi|labor)\)\s*/, '').trim(),
        unitCost: model.laborCost.get(m.laborId) ?? 0,
        baseCount: m.count,
        count: counts.get(m.laborId) ?? m.count,
        isEquipment: name.startsWith('(equi)'),
        isSupervision: name.includes('Foreman'),
      };
    });
    let dailyCost = 0;
    for (const m of members) dailyCost += m.unitCost * m.count;
    const granted = { id: crewId, members: new Map(members.map((m) => [m.laborId, m.count])) };
    let pace = engine.compareProductivity(crewId, granted);
    const warnings: string[] = [];
    const foremen = members.filter((m) => m.isSupervision).reduce((s, m) => s + m.count, 0);
    if (foremen === 0) warnings.push('No foreman on this crew — no work will be done.');
    const rawName = (id: number) => model.laborName.get(id) ?? '';
    for (const m of members) {
      const rn = rawName(m.laborId);
      if ((rn.includes('Crane') || rn.includes('Oiler')) && m.count < m.baseCount) {
        warnings.push(`${m.name} below complement — this crew cannot work.`);
      }
    }
    if (pace > 1) {
      pace = 1 + (pace - 1) * 0.8; // congestion, as the engine applies it
      warnings.push('Extra hands above the complement are 80% effective.');
    }
    out.push({ crewId, name: crewName(crewId), members, dailyCost, pace, warnings });
  }
  return out;
}
