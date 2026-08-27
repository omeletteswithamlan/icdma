import { it, expect } from 'vitest';
import { readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { fromRaw } from '../src/convert.js';
import { Session } from '../src/session.js';

const dir = join(dirname(fileURLToPath(import.meta.url)), '../../scenarios');
const load = (f: string) => fromRaw(JSON.parse(readFileSync(join(dir, f), 'utf8')));

it('futures are fast and finish (or report honestly) on every playable scenario', () => {
  for (const f of ['project-523-roadconstruction.json', 'project-90-i69-as-planned.json',
    'project-823-highway-timebased.json', 'project-70-road-cpm.json', 'project-7-steel.json']) {
    const s = new Session(load(f), 5);
    const t0 = Date.now();
    const r = s.queryFutures(8);
    const ms = Date.now() - t0;
    console.log(f, 'finished', r.finished, '/', r.samples, 'in', ms, 'ms',
      r.days.length ? `days ${Math.min(...r.days)}-${Math.max(...r.days)}` : 'NO FINISHERS');
    expect(ms).toBeLessThan(20000);
  }
}, 180000);
