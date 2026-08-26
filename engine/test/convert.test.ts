import { describe, it, expect } from 'vitest';
import { readFileSync, readdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { fromRaw } from '../src/convert.js';

const scenarioDir = join(dirname(fileURLToPath(import.meta.url)), '../../scenarios');

describe('raw scenario conversion', () => {
  const files = readdirSync(scenarioDir).filter((f) => f.endsWith('.json'));

  it('finds all 12 recovered scenarios', () => {
    expect(files.length).toBe(12);
  });

  for (const f of files) {
    it(`converts ${f}`, () => {
      const raw = JSON.parse(readFileSync(join(scenarioDir, f), 'utf8'));
      const s = fromRaw(raw);
      expect(s.format).toBe('icdma-scenario/1');
      expect(s.activities.length).toBeGreaterThan(0);
      for (const a of s.activities) expect(a.duration).toBeGreaterThan(0);
    });
  }

  it('round-trips project 523 exactly', () => {
    const raw = JSON.parse(readFileSync(join(scenarioDir, 'project-523-roadconstruction.json'), 'utf8'));
    const s = fromRaw(raw);
    expect(s.activities.length).toBe(5);
    expect(s.site.space).toBe(2500);
    expect(s.time.intervalDays).toBe(1);
    const cutting = s.activities.find((a) => a.name === '2_Cutting Members')!;
    expect(cutting.duration).toBe(3);
    expect(cutting.materialUse.find((m) => m.quantity === 6000)).toBeTruthy();
    expect(s.rules.length).toBe(2);
    expect(s.rules[0].postconditions[0].action).toBe('set');
  });
});
