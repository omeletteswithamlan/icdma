/**
 * The TONAE temporal network: activity start/end nodes joined by primary arcs,
 * a present node (PNode) spliced into each primary arc, and constraint arcs
 * between activities. Faithful port of engine-spec/network-and-costs.md §2-3.
 */
import type { ProjectModel, RtActivity } from './model.js';

export const MAXIMUM_DURATION = 10000;

export interface Arc {
  tail: TNode;
  head: TNode;
  lower: number;
  upper: number;
  threshold: number;
}

export class TNode {
  outPrimary: Arc | null = null;
  inPrimary: Arc | null = null;
  outConstraints: Arc[] = [];
  inConstraints: Arc[] = [];
  earlyStart = -1;
  timeOfResolution = -1;
  activity: RtActivity | null = null;
  label = '';
}

export class PNode extends TNode {
  isActive = false;
  totalPercentOrdered = 0; // percent, 0..100
  totalWork = 0; // dollars of material
  totalWorkLeft = 0;

  get start(): number { return this.inPrimary!.tail.earlyStart; }
  get end(): number { return this.outPrimary!.head.earlyStart; }

  percentCompletion(): number {
    return 100 - Math.trunc((this.totalWorkLeft / this.totalWork) * 100);
  }

  /** total orderable amount of material t covered by the current order percent */
  orderedAmount(perDayQty: number, duration: number): number {
    return Math.trunc((perDayQty * duration * this.totalPercentOrdered) / 100);
  }
}

export class Network {
  /** A-nodes in insertion order: (start, end) per activity, activity id ASC */
  readonly aNodes: TNode[] = [];
  /** activity id -> its PNode */
  readonly pnodes = new Map<number, PNode>();
  /** active PNodes, kept sorted by label (legacy TreeSet ordering) */
  readonly readyList: PNode[] = [];

  constructor(readonly model: ProjectModel) {
    // Primary structure per activity
    for (const a of model.activities) {
      const start = new TNode();
      start.label = `${a.name}: Start`;
      start.earlyStart = a.start;
      start.activity = a;
      const end = new TNode();
      end.label = `${a.name}: End`;
      end.earlyStart = a.start + a.duration;
      end.activity = a;

      const arc: Arc = {
        tail: start, head: end,
        lower: a.duration, upper: a.duration + MAXIMUM_DURATION,
        threshold: 0,
      };
      start.outPrimary = arc;
      end.inPrimary = arc;
      this.aNodes.push(start, end);
    }

    // Constraint arcs: latest node of `from` -> earliest node of `to`
    for (const c of model.constraints) {
      let tail: TNode | null = null;
      let head: TNode | null = null;
      for (const n of this.aNodes) {
        if (n.activity!.id === c.fromId && (tail === null || n.earlyStart > tail.earlyStart)) tail = n;
        if (n.activity!.id === c.toId && (head === null || n.earlyStart < head.earlyStart)) head = n;
      }
      if (!tail || !head) throw new Error('constraint endpoints not found');
      const arc: Arc = {
        tail, head,
        lower: c.length,
        upper: c.length + MAXIMUM_DURATION,
        // hard: threshold == upper; soft: threshold = MAXIMUM_DURATION
        threshold: c.soft ? MAXIMUM_DURATION : c.length + MAXIMUM_DURATION,
      };
      tail.outConstraints.push(arc);
      head.inConstraints.push(arc);
    }

    // Splice a PNode into every primary arc
    for (const n of [...this.aNodes]) {
      if (!n.outPrimary || n.outPrimary.head instanceof PNode) continue;
      const a = n.activity!;
      const p = new PNode();
      p.activity = a;
      p.totalWork = p.totalWorkLeft = model.totalMaterial(a);
      const current = n.outPrimary;
      const before: Arc = { tail: n, head: p, lower: 0, upper: 0, threshold: 0 };
      const after: Arc = {
        tail: p, head: current.head,
        lower: current.lower, upper: current.upper, threshold: 0,
      };
      n.outPrimary = before;
      p.inPrimary = before;
      p.outPrimary = after;
      current.head.inPrimary = after;
      p.earlyStart = n.earlyStart;
      p.label = `Y-${n.label}`;
      this.pnodes.set(a.id, p);
    }
  }

  startNode(a: RtActivity): TNode {
    for (const n of this.aNodes) if (n.outPrimary && n.activity!.id === a.id) return n;
    throw new Error(`no start node for activity ${a.id}`);
  }

  endNode(a: RtActivity): TNode {
    for (const n of this.aNodes) if (!n.outPrimary && n.activity!.id === a.id) return n;
    throw new Error(`no end node for activity ${a.id}`);
  }

  earlyStart(a: RtActivity): number { return this.startNode(a).earlyStart; }
  earlyFinish(a: RtActivity): number { return this.endNode(a).earlyStart; }

  lastTimeStep(): number {
    let max = 0;
    for (const n of this.aNodes) if (n.earlyStart > max) max = n.earlyStart;
    return max;
  }

  /** activate PNodes whose start node resolves at time t (legacy startActivities) */
  startActivities(t: number): void {
    for (const n of this.aNodes) {
      if (n.earlyStart === t && n.timeOfResolution === -1 && n.outPrimary) {
        n.timeOfResolution = t;
        const p = n.outPrimary.head as PNode;
        p.earlyStart = t;
        p.timeOfResolution = t;
        p.isActive = true;
        this.readyList.push(p);
      }
    }
    this.readyList.sort((x, y) => (x.label < y.label ? -1 : x.label > y.label ? 1 : 0));
  }

  /** retire PNodes whose end has been reached (legacy endActivities; runs before time++) */
  endActivities(t: number): void {
    for (let i = this.readyList.length - 1; i >= 0; i--) {
      const p = this.readyList[i];
      if (p.outPrimary!.head.earlyStart <= t + 1) {
        p.timeOfResolution = t;
        p.isActive = false;
        this.readyList.splice(i, 1);
      }
    }
  }

  incrementPresentNodes(): void {
    for (const p of this.readyList) {
      p.earlyStart += 1;
      if (p.inPrimary) { p.inPrimary.lower += 1; p.inPrimary.upper += 1; }
      if (p.outPrimary) { p.outPrimary.lower -= 1; p.outPrimary.upper -= 1; }
    }
  }

  /**
   * Push an activity's end and relax the not-yet-started network (legacy
   * delayActivity). `floor` is tNow+1 on the interactive path and tNow on the
   * query path (spec divergence D); the freeze condition uses tNow in both.
   */
  delayActivity(p: PNode, delay: number, tNow: number, floor: number = tNow + 1): void {
    const start = p.inPrimary!.tail;
    const end = p.outPrimary!.head;
    end.earlyStart += delay;
    end.earlyStart = Math.max(end.earlyStart, floor, start.earlyStart);

    let changed = true;
    while (changed) {
      changed = false;
      for (const endNode of this.aNodes) {
        if (endNode.outPrimary) continue; // end nodes only
        const startNode = endNode.inPrimary!.tail.inPrimary!.tail;
        if (startNode.earlyStart <= tNow) continue; // already started: frozen
        let latest = floor;
        for (const carc of startNode.inConstraints) {
          const lastEnd = carc.tail;
          const soft = carc.upper !== carc.threshold;
          let suggested = lastEnd.earlyStart + carc.lower;
          if (soft && endNode.earlyStart < lastEnd.earlyStart) {
            suggested = lastEnd.earlyStart;
          } else if (soft && endNode.earlyStart >= lastEnd.earlyStart
            && endNode.earlyStart < suggested) {
            suggested = endNode.earlyStart;
          }
          if (suggested > latest) latest = suggested;
        }
        if (startNode.earlyStart !== latest) {
          const dur = endNode.earlyStart - startNode.earlyStart;
          startNode.earlyStart = latest;
          endNode.earlyStart = latest + dur;
          changed = true;
        }
      }
    }
  }

  /** legacy getLateSchedule: reverse relaxation over outgoing constraints */
  lateSchedule(tNow: number): Map<number, { ls: number; lf: number }> {
    const lastday = this.lastTimeStep();
    const sched = new Map<number, { ls: number; lf: number }>();
    while (sched.size !== this.model.activities.length) {
      let changed = false;
      for (const anode of this.aNodes) {
        if (!anode.outPrimary) continue;
        const a = anode.activity!;
        if (sched.has(a.id)) continue;
        const endNode = anode.outPrimary.head.outPrimary!.head;

        let min = lastday;
        let noadd = false;
        for (const c of a.links) {
          const succ = sched.get(c.toId);
          if (succ) min = Math.min(min, succ.ls - c.length);
          else noadd = true;
        }
        if (noadd) continue;

        let stime = min - (endNode.earlyStart - anode.earlyStart);
        if (anode.earlyStart <= tNow) stime = anode.earlyStart;
        if (endNode.earlyStart <= tNow) min = endNode.earlyStart;
        sched.set(a.id, { ls: stime, lf: min });
        changed = true;
      }
      if (!changed) throw new Error('late-schedule relaxation stalled');
    }
    return sched;
  }

  isCritical(a: RtActivity, late: Map<number, { ls: number; lf: number }>): boolean {
    const l = late.get(a.id)!;
    return this.earlyStart(a) === l.ls && this.earlyFinish(a) === l.lf;
  }
}
