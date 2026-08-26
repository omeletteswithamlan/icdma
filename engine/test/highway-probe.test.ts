import { it, expect } from 'vitest';
import { readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { fromRaw } from '../src/convert.js';
import { Engine } from '../src/engine.js';

const dir = join(dirname(fileURLToPath(import.meta.url)), '../../scenarios');

// Java oracle (legacy SmokeTest, project 823, 20 futures): best case 141 turns
// at $2,263,996.75; worst 142 at $2,280,293.33 (rules fired in 2/20 runs).
it('highway 823: no-event default future matches the Java oracle to the cent', () => {
  const s = fromRaw(JSON.parse(readFileSync(join(dir, 'project-823-highway-timebased.json'), 'utf8')));
  const r = new Engine(s, { variant: 'query', rng: () => 0.99 }).runDefaultFuture();
  expect(r.lastTurn).toBe(141);
  expect(r.finalCost).toBeCloseTo(2263996.75, 2);
});
