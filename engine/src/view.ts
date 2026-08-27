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
