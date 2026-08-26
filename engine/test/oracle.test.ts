/**
 * Acceptance tests against the restored 2011 Java engine ("the oracle").
 * Oracle values are documented in docs/engine-spec/*.md and runs/run-1745.
 */
import { describe, it, expect } from 'vitest';
import { readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { fromRaw } from '../src/convert.js';
import { Engine } from '../src/engine.js';
import { ProjectModel } from '../src/model.js';
import { Network } from '../src/network.js';

const scenarioDir = join(dirname(fileURLToPath(import.meta.url)), '../../scenarios');
const load = (f: string) => fromRaw(JSON.parse(readFileSync(join(scenarioDir, f), 'utf8')));

const s523 = load('project-523-roadconstruction.json');

describe('as-planned schedule (oracle: legacy SmokeTest + spec §3)', () => {
  const model = new ProjectModel(s523);
  const net = new Network(model);

  it('computes the ES/EF chain 1-3-6-10-13 / 3-6-10-13-14', () => {
    const es = model.activities.map((a) => net.earlyStart(a));
    const ef = model.activities.map((a) => net.earlyFinish(a));
    expect(es).toEqual([1, 3, 6, 10, 13]);
    expect(ef).toEqual([3, 6, 10, 13, 14]);
    expect(net.lastTimeStep()).toBe(14);
  });

  it('marks the whole chain critical', () => {
    const late = net.lateSchedule(1);
    for (const a of model.activities) expect(net.isCritical(a, late)).toBe(true);
  });

  it('reproduces per-activity totals to the cent', () => {
    const totals = model.activities.map((a) => model.total(a));
    const expected = [13901.6, 69717.36, 66275.2, 38667.36, 19789.12];
    totals.forEach((t, i) => expect(t).toBeCloseTo(expected[i], 8));
  });
});

describe('as-planned cost schedule (oracle: $208,350.64 at day 13)', () => {
  it('cumulative total series has lastKey 13 with the exact grand total', () => {
    const engine = new Engine(s523, { variant: 'interactive', rng: () => 0.99 });
    const series = engine.asPlanned.totalSeries(engine.model, engine.network.lastTimeStep());
    const lastKey = Math.max(...series.keys());
    expect(lastKey).toBe(13);
    expect(series.get(13)!).toBeCloseTo(208350.64, 6);
  });
});

describe('Monte-Carlo default-play future (oracle: 20/20 at day 18, $227,689.76)', () => {
  it('completes on turn 18 with the exact oracle cost', () => {
    const engine = new Engine(s523, { variant: 'query', rng: () => 0.99 });
    const { lastTurn, finalCost } = engine.runDefaultFuture();
    expect(lastTurn).toBe(18);
    expect(finalCost).toBeCloseTo(227689.76, 6);
  });

  it('is deterministic across rule draws (rules cannot affect this scenario)', () => {
    const a = new Engine(s523, { variant: 'query', rng: () => 0.0 }).runDefaultFuture();
    const b = new Engine(s523, { variant: 'query', rng: () => 0.99 }).runDefaultFuture();
    expect(a.lastTurn).toBe(b.lastTurn);
    expect(a.finalCost).toBeCloseTo(b.finalCost, 8);
  });
});

describe('all 12 recovered scenarios run to completion', () => {
  const files = [
    'project-7-steel.json', 'project-70-road-cpm.json', 'project-80-road-ls.json',
    'project-90-i69-as-planned.json', 'project-200-i69-cpm.json', 'project-300-i69-ls.json',
    'project-369-residential-house.json', 'project-523-roadconstruction.json',
    'project-777-steel-project.json', 'project-823-highway-timebased.json',
    'project-824-highway-productionbased.json', 'project-888-house-construction.json',
  ];
  // project-369 is an authoring stub in the recovered data: 7 activities with
  // no materials and no crews, so a zero-cost completed run is correct.
  const stubs = new Set(['project-369-residential-house.json']);
  for (const f of files) {
    it(`${f} finishes under the turn cap`, () => {
      const engine = new Engine(load(f), { variant: 'query', rng: () => 0.99 });
      const { lastTurn, finalCost } = engine.runDefaultFuture();
      expect(lastTurn).toBeLessThan(1000);
      if (!stubs.has(f)) expect(finalCost).toBeGreaterThan(0);
    });
  }
});
