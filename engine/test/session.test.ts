import { describe, it, expect } from 'vitest';
import { readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { fromRaw } from '../src/convert.js';
import { Session } from '../src/session.js';
import { scheduleRows, costView, statusView } from '../src/view.js';

const dir = join(dirname(fileURLToPath(import.meta.url)), '../../scenarios');
const load = (f: string) => fromRaw(JSON.parse(readFileSync(join(dir, f), 'utf8')));

describe('playable session (bridge scenario)', () => {
  it('auto-resolved default play COMPLETES the project (unlike raw default play)', () => {
    const s = new Session(load('project-523-roadconstruction.json'), 7);
    let guard = 0;
    while (!s.engine.isFinished() && guard++ < 60) {
      let d = s.defaultDecisions();
      const v = s.previewSpaceViolation(d);
      if (v) d = s.applyProportionalCut(d, v);
      s.playTurn(d);
    }
    expect(s.engine.isFinished()).toBe(true);
    const status = statusView(s.engine);
    // run 1745 (human, dialog cuts): ~20 turns, $227,828.88 — same ballpark
    expect(status.turn).toBeGreaterThan(14);
    expect(status.turn).toBeLessThan(30);
    const cv = costView(s.engine);
    expect(cv.builtToDate).toBeGreaterThan(215000);
    expect(cv.builtToDate).toBeLessThan(245000);
  });

  it('mid-game futures replay the log and finish', () => {
    const s = new Session(load('project-523-roadconstruction.json'), 7);
    for (let i = 0; i < 3; i++) {
      let d = s.defaultDecisions();
      const v = s.previewSpaceViolation(d);
      if (v) d = s.applyProportionalCut(d, v);
      s.playTurn(d);
    }
    const f = s.queryFutures(15);
    expect(f.finished).toBe(15);
    for (const c of f.costs) { expect(c).toBeGreaterThan(200000); expect(c).toBeLessThan(260000); }
    expect(Math.min(...f.days)).toBeGreaterThan(14);
  });

  it('view models are coherent', () => {
    const s = new Session(load('project-90-i69-as-planned.json'), 3);
    s.playTurn(s.defaultDecisions());
    const rows = scheduleRows(s.engine);
    expect(rows.length).toBe(14);
    expect(rows.some((r) => r.active)).toBe(true);
    const cv = costView(s.engine);
    expect(cv.planned.length).toBeGreaterThan(5);
  });
});
