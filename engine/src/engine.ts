/**
 * The simulation engine: the per-turn loop of engine-spec/turn-loop.md.
 *
 * Two variants exist in the legacy system and are preserved here:
 *  - 'query'  (legacy Updater): the Monte-Carlo path — the verification oracle.
 *  - 'interactive' (legacy TONAE): the player path, with ordering caps.
 * Divergences are marked with the spec's divergence letters (A-M).
 */
import type { Scenario } from './schema.js';
import { ProjectModel, type RtActivity, type RtMaterial } from './model.js';
import { Network, type PNode } from './network.js';
import { CostSchedule, computeCost } from './costs.js';
import { Environment, applyRules, setState, type FiredRule } from './environment.js';

export type Variant = 'query' | 'interactive';

export interface EngineOptions {
  variant?: Variant;
  /** uniform [0,1) source for rule sampling; defaults to Math.random */
  rng?: () => number;
}

export interface GrantedCrew {
  id: number;
  members: Map<number, number>; // laborId -> count
}

export interface Allocation {
  activityId: number | null;
  requested: Map<number, number>; // materialId -> qty
  workDays: number;
  workHours: number;
  wageIncentive: number;
  /** percent used by the interactive purchase model (legacy default 1.0!) */
  order: number;
  /** bound by buildRequestList each turn */
  crews: GrantedCrew[];
}

export function makeAllocation(activityId: number | null): Allocation {
  return {
    activityId,
    requested: new Map(),
    workDays: 5,
    workHours: 8,
    wageIncentive: 1.0,
    order: 1.0,
    crews: [],
  };
}

class Stock {
  qty = new Map<number, number>();
  curamt = 0;
  constructor(readonly total: number) {}

  add(m: RtMaterial, q: number): number {
    if (q * m.size + this.curamt > this.total) {
      q = Math.trunc((this.total - this.curamt) / m.size);
    }
    this.qty.set(m.id, (this.qty.get(m.id) ?? 0) + q);
    this.curamt += q * m.size;
    return q;
  }

  remove(m: RtMaterial, q: number): number {
    const have = this.qty.get(m.id) ?? 0;
    if (q > have) q = have;
    this.qty.set(m.id, have - q);
    this.curamt -= q * m.size;
    return q;
  }

  availableSpace(): number { return this.total - this.curamt; }
  usedSpace(): number { return this.curamt; }

  value(materials: Map<number, RtMaterial>): number {
    let v = 0;
    for (const [id, q] of this.qty) v += q * materials.get(id)!.cost;
    return v;
  }

  clearPerishable(materials: Map<number, RtMaterial>): void {
    for (const [id, q] of this.qty) {
      const m = materials.get(id)!;
      if (m.perishable && q > 0) {
        this.curamt -= q * m.size;
        this.qty.set(id, 0);
      }
    }
  }

  clone(): Stock {
    const s = new Stock(this.total);
    s.qty = new Map(this.qty);
    s.curamt = this.curamt;
    return s;
  }
}

const SUNDAY = 0;
const SATURDAY = 6;

export interface TurnResult {
  time: number;
  fired: FiredRule[];
  turnCost: number;
}

export class Engine {
  readonly model: ProjectModel;
  readonly network: Network;
  readonly env: Environment;
  readonly asPlanned = new CostSchedule();
  readonly asBuilt = new CostSchedule();
  readonly stock: Stock;
  time = 1;
  /** cumulative project cost per completed turn (legacy query_futures_total_track) */
  readonly costTrack = new Map<number, number>();
  private purchased = new Map<number, number>();
  /** per-(activityId,materialId) consumption caps (legacy MaterialInfo) */
  private totalNeed = new Map<string, number>();
  private totalUsed = new Map<string, number>();
  private readonly variant: Variant;
  private readonly rng: () => number;
  private readonly varLabel = new Map<number, string>();

  constructor(scenario: Scenario, opts: EngineOptions = {}) {
    this.variant = opts.variant ?? 'query';
    this.rng = opts.rng ?? Math.random;
    this.model = new ProjectModel(scenario);
    this.network = new Network(this.model);
    this.env = new Environment(scenario, this.model);
    this.stock = new Stock(this.model.space);
    for (const v of scenario.variables) this.varLabel.set(v.id, v.label);

    for (const a of this.model.activities) {
      for (const [matId, qty] of a.materialUse) {
        this.totalNeed.set(`${a.id}:${matId}`, qty * a.duration);
        this.totalUsed.set(`${a.id}:${matId}`, 0);
      }
    }

    // In query mode the cost books record nothing (legacy querymode).
    if (this.variant === 'query') {
      this.asBuilt.recording = false;
      this.asPlanned.recording = false;
    }

    this.network.startActivities(1);
    const last = this.network.lastTimeStep();
    const wasRecording = this.asPlanned.recording;
    this.asPlanned.recording = true;
    this.asBuilt.recording = true;
    computeCost(this.asPlanned, 1, last, this.network, this.model);
    computeCost(this.asBuilt, 1, last, this.network, this.model);
    this.asPlanned.recording = wasRecording;
    this.asBuilt.recording = wasRecording;
  }

  isFinished(): boolean {
    return this.time >= this.network.lastTimeStep();
  }

  /** day-of-week of the current turn, 0=Sunday..6=Saturday */
  private dayOfWeek(): number {
    const ms = this.model.startDate.getTime()
      + (this.time - 1) * this.model.intervalDays * 86400000;
    return new Date(ms).getUTCDay();
  }

  defaultAllocations(): Allocation[] {
    const allocs: Allocation[] = [makeAllocation(null)]; // the "stock" allocation
    for (const p of this.network.readyList) {
      const a = p.activity!;
      const alloc = makeAllocation(a.id);
      for (const [matId, qty] of a.materialUse) alloc.requested.set(matId, qty);
      allocs.push(alloc);
    }
    return allocs;
  }

  defaultCrews(): GrantedCrew[] {
    const crews: GrantedCrew[] = [];
    for (const [id, members] of this.model.crewMembers) {
      crews.push({ id, members: new Map(members.map((m) => [m.laborId, m.count])) });
    }
    return crews;
  }

  /** legacy LaborCrew.compareProductivity(base=this, granted=o) */
  private compareProductivity(baseCrewId: number, granted: GrantedCrew): number {
    const base = this.model.crewMembers.get(baseCrewId) ?? [];
    const name = (id: number) => this.model.laborName.get(id) ?? '';
    let needsCrane = 0; let needsOiler = 0; let hasCrane = 0; let hasOiler = 0;
    for (const m of base) {
      if (name(m.laborId).includes('Crane')) needsCrane += m.count;
      if (name(m.laborId).includes('Oiler')) needsOiler += m.count;
    }
    let foremen = 0;
    for (const [laborId, count] of granted.members) {
      if (name(laborId).includes('Crane')) hasCrane += count;
      if (name(laborId).includes('Oiler')) hasOiler += count;
      if (name(laborId).includes('Foreman')) foremen += count;
    }
    const maxLaborers = 10 * foremen;
    let cmp = -1;
    for (const m of base) {
      const n = name(m.laborId);
      if (n.includes('Foreman') || n.includes('Crane') || n.includes('Oiler')) continue;
      if (maxLaborers > 0) { // legacy numlaborers counter is dead: this is a foreman-presence test
        const r = (granted.members.get(m.laborId) ?? 0) / m.count;
        cmp = cmp === -1 ? r : Math.min(cmp, r);
      }
    }
    if (cmp === -1) cmp = 0;
    if (hasCrane < needsCrane || hasOiler < needsOiler) cmp = 0;
    return cmp;
  }

  /** legacy ResourceAllocation.computeWorkQuantityMultiplier */
  private workMultiplier(alloc: Allocation, a: RtActivity, dow: number): number {
    let hourfactor = (alloc.workHours * alloc.workDays) / 40.0;
    const wagefactor = 2 - 1.0 / alloc.wageIncentive;
    if (hourfactor > 1) hourfactor = 1 + (hourfactor - 1) * 0.5; // overtime half-productive
    let work = -1;
    for (const baseCrewId of a.crewIds) {
      for (const o of alloc.crews) {
        if (o.id === baseCrewId) {
          let perc = this.compareProductivity(baseCrewId, o);
          if (perc > 1) perc = 1 + (perc - 1) * 0.8; // congestion
          perc *= hourfactor;
          work = work === -1 ? perc : Math.min(work, perc);
        }
      }
    }
    if (this.model.intervalDays === 1
      && ((dow === SATURDAY && alloc.workDays <= 5) || (dow === SUNDAY && alloc.workDays <= 6))) {
      return 0; // the weekend gate — before wagefactor, regardless of crews
    }
    return work === -1 ? 0 : work * wagefactor;
  }

  /** legacy overtime-normalized labor time fraction (§3.9) */
  private laborTime(alloc: Allocation, dow: number): number {
    const interval = this.model.intervalDays;
    if (interval === 1) {
      let time = alloc.workHours;
      if ((alloc.workDays > 5 && dow === SATURDAY) || (alloc.workDays > 6 && dow === SUNDAY)) {
        time *= 2.0;
      } else if (dow === SATURDAY || dow === SUNDAY) {
        time = 0;
      } else if (time > 8) {
        time = (time - 8) * 2.0 + 8;
      }
      return time / 8;
    }
    if (interval === 7) {
      let time = alloc.workHours * alloc.workDays;
      if (time > 40) time = (time - 40) * 2.0 + 40;
      return time / 40;
    }
    if (interval === 28) {
      let time = alloc.workHours * alloc.workDays * 4;
      if (time > 160) time = (time - 160) * 2.0 + 160;
      return time / 160;
    }
    throw new Error('unsupported interval');
  }

  private purchase(matId: number, amt: number): void {
    if (amt > 0) this.purchased.set(matId, (this.purchased.get(matId) ?? 0) + amt);
  }

  /** legacy buildPurchaceList — divergence A */
  private buildPurchaseList(allocs: Allocation[]): void {
    if (this.variant === 'query') {
      const cs = this.stock.clone();
      for (const alloc of allocs) {
        for (const m of this.model.materials.values()) {
          const req = alloc.requested.get(m.id) ?? 0;
          const amt = cs.remove(m, req);
          this.purchase(m.id, req - amt); // top up to full request
        }
      }
    } else {
      for (const alloc of allocs) {
        if (alloc.activityId === null) continue;
        const a = this.model.activity(alloc.activityId);
        for (const m of this.model.materials.values()) {
          const dailyUse = a.materialUse.get(m.id);
          const amount = dailyUse === undefined ? 0 : Math.ceil((dailyUse * alloc.order) / 100);
          this.purchase(m.id, amount);
        }
      }
    }
  }

  /** legacy buildRequestList: crew binding, first-come-first-served */
  private bindCrews(allocs: Allocation[], crews: (GrantedCrew | null)[]): void {
    for (const alloc of allocs) {
      if (alloc.activityId === null) continue;
      alloc.crews = [];
      const a = this.model.activity(alloc.activityId);
      for (const baseCrewId of a.crewIds) {
        for (let x = 0; x < crews.length; x++) {
          const c = crews[x];
          if (c !== null && c.id === baseCrewId) {
            alloc.crews.push(c);
            crews[x] = null; // shared crews: earlier allocation wins
          }
        }
      }
    }
  }

  /** legacy checkLabor (with the restoration-patch guard for absent variables) */
  private checkLabor(allocs: Allocation[]): void {
    for (const alloc of allocs) {
      if (alloc.activityId === null) continue;
      const avail = this.env.locals.get(alloc.activityId)?.get('Labor Available');
      const low = this.env.locals.get(alloc.activityId)?.get('Low Labor');
      if (!avail || !low) continue;
      if (String(avail.state) === 'False') {
        for (const c of alloc.crews) c.members.clear();
      } else if (String(low.state) === 'True') {
        const pool: number[] = [];
        for (const c of alloc.crews) for (const [id, n] of c.members) for (let i = 0; i < n; i++) pool.push(id);
        if (pool.length > 0) {
          const victim = pool[Math.trunc(this.rng() * pool.length)];
          for (const c of alloc.crews) {
            const n = c.members.get(victim) ?? 0;
            if (n > 0) { c.members.set(victim, n - 1); break; }
          }
        } else {
          setState(low, 'False', 1);
        }
      }
    }
  }

  /** one turn (legacy Updater.update / TONAE.update) */
  update(allocs: Allocation[], crews: GrantedCrew[]): TurnResult {
    const t = this.time;
    const model = this.model;
    const net = this.network;

    // 1. purchase list
    this.buildPurchaseList(allocs);
    // 2. environment maintenance
    this.env.update(t, net, model);
    // 4-7. driving materials / labor compliment / month / day — driving materials:
    for (const a of model.activities) {
      let min = 50000;
      if (a.drivingMaterials.length === 0) min = 100;
      for (const dm of a.drivingMaterials) {
        const alloc = allocs.find((x) => x.activityId === a.id);
        if (!alloc) continue;
        const base = a.materialUse.get(dm);
        if (base === undefined) continue;
        const available = alloc.requested.get(dm) ?? 0;
        const temp = base === 0 ? 0 : Math.trunc(available / base) * 100; // integer division first
        if (temp < min) min = temp;
      }
      const v = this.env.locals.get(a.id)?.get('Driving Material Available');
      if (v) setState(v, String(Math.trunc(min / 10)), 1);
    }
    const month = this.env.getGlobal('Month');
    const day = this.env.getGlobal('Day');
    const dow = this.dayOfWeek();
    if (month) setState(month, ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'][new Date(this.model.startDate.getTime() + (t - 1) * this.model.intervalDays * 86400000).getUTCMonth()], 1);
    if (day) setState(day, String(dow + 1), 1); // Java: 1=Sunday..7=Saturday

    // 8. rules
    const ready = net.readyList.map((p) => p.activity!);
    const fired = applyRules(
      this.model.scenario.rules, this.env, ready,
      (id) => this.varLabel.get(id) ?? '', this.rng,
    );

    // 11-13. labor check, crew binding, material check
    this.checkLabor(allocs);
    this.bindCrews(allocs, [...crews]);
    const matAvail = this.env.getGlobal('Material Available');
    if (matAvail && String(matAvail.state) === 'False') {
      this.purchased.clear();
      if (this.variant === 'interactive') {
        for (const p of net.readyList) {
          const alloc = allocs.find((x) => x.activityId === p.activity!.id);
          if (alloc) p.totalPercentOrdered -= alloc.order / p.activity!.duration; // divergence F
        }
      }
    }

    // 15. manageResources
    const turnCost = this.manageResources(allocs, dow);

    // 19-23. lifecycle + clock
    net.endActivities(t);
    this.time = t + 1;
    net.startActivities(this.time);
    this.stock.clearPerishable(model.materials);
    this.purchased.clear();

    return { time: t, fired, turnCost };
  }

  private manageResources(allocs: Allocation[], dow: number): number {
    const model = this.model;
    const net = this.network;
    const day = this.time;
    const sched = this.asBuilt;
    let cost = 0;

    // deliver purchases into stock (space clamp is the query path's throttle — divergence E)
    for (const [matId, qty] of this.purchased) {
      this.stock.add(model.materials.get(matId)!, qty);
    }

    if (this.variant === 'interactive') {
      for (const a of model.activities) {
        sched.setMaterial(a.id, day, sched.getMaterial(a.id, day - 1));
        sched.setLabor(a.id, day, sched.getLabor(a.id, day - 1));
        sched.setIndirect(a.id, day, sched.getIndirect(a.id, day - 1));
      }
    }

    for (const p of net.readyList) {
      const a = p.activity!;
      const alloc = allocs.find((x) => x.activityId === a.id);
      if (!alloc) continue;

      const baseworkrate = this.workMultiplier(alloc, a, dow);
      const productivity = this.env.get(a.id, 'Productivity');
      const workrate = baseworkrate * Number(productivity?.state ?? 1);

      // material availability
      const avail = new Map<number, number>();
      let materialrate = -1;
      let materialrate2 = 0;
      let dailyCost = 0;
      for (const [matId, dailyUse] of a.materialUse) {
        const m = model.materials.get(matId)!;
        const available = this.stock.remove(m, alloc.requested.get(matId) ?? 0);
        avail.set(matId, available);
        const perc = available / dailyUse;
        materialrate = materialrate === -1 ? perc : Math.min(materialrate, perc);
        materialrate2 += available * m.cost;
        dailyCost += dailyUse * m.cost;
      }
      if (materialrate === -1) materialrate = 0;

      if (this.variant === 'query') {
        // divergence C: min-ratio availability; divergence B: truncated, uncapped
        const rate = Math.min(workrate, materialrate);
        for (const [matId, dailyUse] of a.materialUse) {
          const m = model.materials.get(matId)!;
          let available = avail.get(matId)!;
          const getAmt = Math.trunc(dailyUse * rate);
          const amtused = Math.min(getAmt, available);
          available -= amtused;
          p.totalWorkLeft -= m.cost * amtused;
          sched.addMaterial(a.id, day, amtused * m.cost);
          cost += amtused * m.cost;
          this.stock.add(m, available); // full remainder returned
        }
      } else {
        // TONAE: cost-weighted availability, ceil + four caps, two passes
        materialrate2 = dailyCost === 0 ? 0 : materialrate2 / dailyCost;
        const newRate = Math.min(materialrate2, workrate);
        let materialcost = newRate * dailyCost;
        for (const [matId, dailyUse] of a.materialUse) {
          const m = model.materials.get(matId)!;
          let available = avail.get(matId)!;
          const key = `${a.id}:${matId}`;
          let newAmt = Math.ceil(dailyUse * newRate);
          const need = this.totalNeed.get(key)! - this.totalUsed.get(key)!;
          if (newAmt > need) newAmt = need;
          if (newAmt > available) newAmt = available;
          const test = p.orderedAmount(dailyUse, a.duration) - this.totalUsed.get(key)!;
          if (newAmt > test) newAmt = test;
          if (newAmt > 0) {
            available -= newAmt;
            p.totalWorkLeft -= m.cost * newAmt;
            this.totalUsed.set(key, this.totalUsed.get(key)! + newAmt);
            sched.addMaterial(a.id, day, newAmt * m.cost);
            cost += newAmt * m.cost;
            materialcost -= newAmt * m.cost;
          }
          if (available > 0) {
            if (available > test) available = test; // surplus above ordered cap is destroyed
            if (available > 0) this.stock.add(m, available);
          }
          avail.set(matId, Math.max(0, available));
        }
        if (materialcost > 1) {
          for (const [matId, dailyUse] of a.materialUse) {
            if (materialcost <= 0) break;
            const m = model.materials.get(matId)!;
            let available = avail.get(matId)!;
            const key = `${a.id}:${matId}`;
            let need = this.totalNeed.get(key)! - this.totalUsed.get(key)!;
            const test = p.orderedAmount(dailyUse, a.duration) - this.totalUsed.get(key)!;
            if (need > test) need = test;
            let newAmt = Math.min(need, available);
            const newMax = Math.trunc(materialcost / dailyUse); // legacy defect kept: ÷ quantity
            if (newMax < newAmt) newAmt = newMax + 1;
            if (newAmt > 0) {
              available -= newAmt;
              this.stock.remove(m, newAmt);
              p.totalWorkLeft -= m.cost * newAmt;
              this.totalUsed.set(key, this.totalUsed.get(key)! + newAmt);
              sched.addMaterial(a.id, day, newAmt * m.cost);
              cost += newAmt * m.cost;
              materialcost -= newAmt * m.cost;
            }
            if (available > 0) {
              const cap = p.orderedAmount(dailyUse, a.duration) - this.totalUsed.get(key)!;
              if (available > cap) available = cap;
              if (available > 0) this.stock.add(m, available);
            }
          }
        }
      }

      // duration recomputation and delay
      const baseWork = model.dailyMaterialCost(a);
      if (baseWork > 0) {
        const newDuration = Math.ceil(p.totalWorkLeft / baseWork - 0.01);
        const currentDuration = p.outPrimary!.head.earlyStart - p.earlyStart;
        const delay = newDuration - currentDuration + 1;
        if (delay !== 0) {
          // divergence D: query floor t_now, interactive floor t_now+1
          net.delayActivity(p, delay, day, this.variant === 'query' ? day : day + 1);
        }
      }

      // labor + indirect
      const time = this.laborTime(alloc, dow);
      for (const c of alloc.crews) {
        let crewDaily = 0;
        const sorted = [...c.members.entries()].sort((x, y) => x[0] - y[0]);
        for (const [laborId, count] of sorted) crewDaily += model.laborCost.get(laborId)! * count;
        const charge = time * crewDaily * alloc.wageIncentive;
        sched.addLabor(a.id, day, charge);
        cost += charge;
      }
      const ind = model.dailyMaterialCost(a) * model.overhead;
      sched.addIndirect(a.id, day, ind);
      cost += ind;
    }

    net.incrementPresentNodes();
    sched.setStockValue(day, this.stock.value(model.materials));

    const cum = (this.costTrack.get(day - 1) ?? 0) + cost;
    this.costTrack.set(day, cum);

    if (this.variant === 'interactive') {
      computeCost(sched, day + 1, net.lastTimeStep(), net, model);
    }
    return cost;
  }

  /** run to completion with default (as-planned) play — one Monte-Carlo future */
  runDefaultFuture(): { lastTurn: number; finalCost: number; track: Map<number, number> } {
    while (!this.isFinished() && this.time < 1000) {
      this.update(this.defaultAllocations(), this.defaultCrews());
    }
    const lastTurn = Math.max(...this.costTrack.keys());
    return { lastTurn, finalCost: this.costTrack.get(lastTurn)!, track: this.costTrack };
  }
}
