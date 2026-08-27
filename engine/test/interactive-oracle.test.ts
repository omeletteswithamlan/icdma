/**
 * Interactive-variant (TONAE path) parity vs the headless Java oracle
 * (legacy/InteractiveOracle.java): default GUI play — default allocations,
 * full crews, OrderPanel default ordering advancing percentOrdered — with all
 * legacy-compat switches on (float32 overhead, America/New_York calendar
 * incl. DST weekday shift, Java HashMap iteration order).
 *
 * Headless default play deadlocks by design (no space-violation dialog to
 * roll ordering back), so these fixtures compare the STALL state at a turn
 * cap — full parity of network drift, and cost to the microdollar.
 */
import { describe, it, expect } from 'vitest';
import { readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { fromRaw } from '../src/convert.js';
import { Engine } from '../src/engine.js';

const dir = join(dirname(fileURLToPath(import.meta.url)), '../../scenarios');
const load = (f: string) => fromRaw(JSON.parse(readFileSync(join(dir, f), 'utf8')));
const compat = { floatOverhead: true, timezone: 'America/New_York', javaHashOrder: true };

function run(file: string, cap: number) {
  const e = new Engine(load(file), { variant: 'interactive', rng: () => 0.99, legacyCompat: compat });
  let turns = 0;
  while (!e.isFinished() && turns < cap) { e.playInteractiveDefaultTurn(); turns++; }
  const last = e.network.lastTimeStep();
  const lk = last - 1;
  const total = e.asBuilt.materialTotal(e.model, lk)
    + e.asBuilt.laborTotal(e.model, lk) + e.asBuilt.indirectTotal(e.model, lk);
  return { lastKey: lk, total, engine: e };
}

describe('interactive default play vs Java oracle', () => {
  it('523 stall state at 200 turns matches to the microdollar', () => {
    const { lastKey, total, engine } = run('project-523-roadconstruction.json', 200);
    expect(lastKey).toBe(209);
    expect(total).toBeCloseTo(1099488.341946, 5);
    // Java: Design Trusses complete, Cutting Members stalled at 84%
    expect(engine.network.pnodes.get(1)!.percentCompletion()).toBe(100);
    expect(engine.network.pnodes.get(2)!.percentCompletion()).toBe(84);
  });

  it('70 stall state at 400 turns matches to the microdollar (24 activities, shared crews, DST)', () => {
    const { lastKey, total } = run('project-70-road-cpm.json', 400);
    expect(lastKey).toBe(455);
    expect(total).toBeCloseTo(118106391.191821, 5);
  });
});
