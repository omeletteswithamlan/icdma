/**
 * Stochastic-path validation against Java oracle fixtures obtained by
 * temporarily forcing rule probabilities in the restored database:
 * - A1: rule 46 (global weather) p=1, all others 0 → Java: identical to the
 *   no-event run (141 turns, $2,263,996.75) because its Productivity
 *   postcondition targets a local variable through a global lookup (silent
 *   no-op) and nothing reads Weather.
 * - A2: rule 82 (local, ID eq 276 → Productivity=0) p=1, others 0 → Java:
 *   activity 276 never progresses; all futures hit the 999-turn cap at
 *   exactly $7,618,661.06.
 * - A3: with real probabilities and a real RNG, overruns appear at roughly
 *   the rate Java showed (2/20), and a single effective firing reproduces
 *   Java's observed worst case $2,280,293.33.
 */
import { describe, it, expect } from 'vitest';
import { readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { fromRaw } from '../src/convert.js';
import { Engine } from '../src/engine.js';
import { mulberry32 } from '../src/session.js';
import type { Scenario } from '../src/schema.js';

const dir = join(dirname(fileURLToPath(import.meta.url)), '../../scenarios');
const base: Scenario = fromRaw(JSON.parse(readFileSync(join(dir, 'project-823-highway-timebased.json'), 'utf8')));

function withProbs(probs: Record<number, number>): Scenario {
  return {
    ...base,
    rules: base.rules.map((r) => ({ ...r, probability: probs[r.id] ?? 0 })),
  };
}

describe('stochastic path vs Java fixtures (823)', () => {
  it('A1: always-firing global weather rule is a no-op (scope bug parity)', () => {
    const e = new Engine(withProbs({ 46: 1 }), { variant: 'query', rng: () => 0.5 });
    const r = e.runDefaultFuture();
    expect(r.lastTurn).toBe(141);
    expect(r.finalCost).toBeCloseTo(2263996.75, 2);
  });

  it('A2: always-firing productivity-killer blocks activity 276 to the turn cap', () => {
    const e = new Engine(withProbs({ 82: 1 }), { variant: 'query', rng: () => 0.5 });
    const r = e.runDefaultFuture();
    expect(r.lastTurn).toBe(999);
    expect(r.finalCost).toBeCloseTo(7618661.06, 1);
  });

  it('A3: real-probability futures reproduce the Java overrun distribution', () => {
    const outcomes = new Map<string, number>();
    let overruns = 0;
    const N = 300;
    for (let i = 0; i < N; i++) {
      // seeded per-sample streams: statistically random, deterministic test
      const e = new Engine(base, { variant: 'query', rng: mulberry32(0xa3000 + i) });
      const r = e.runDefaultFuture();
      if (r.lastTurn > 141) overruns++;
      const key = `${r.lastTurn}:${r.finalCost.toFixed(2)}`;
      outcomes.set(key, (outcomes.get(key) ?? 0) + 1);
    }
    // Java saw 2/20 overruns; effective-firing odds ≈ p(0.001) x ready-turns.
    expect(overruns / N).toBeGreaterThan(0.02);
    expect(overruns / N).toBeLessThan(0.35);
    // Java's observed worst-case COST should be among the TS outcomes (a
    // single effective firing; it can land on day 141 or 142 depending on
    // whether the lost day is absorbed by float).
    const javaWorst = [...outcomes.keys()].some((k) => k.endsWith(':2280293.33'));
    console.log('outcomes:', [...outcomes.entries()].sort((a, b) => b[1] - a[1]).slice(0, 6));
    console.log('overrun fraction:', overruns / N);
    expect(javaWorst).toBe(true);
  });
});
