import { fromRaw, type Scenario } from 'icdma-engine';

import p523 from '../../scenarios/project-523-roadconstruction.json';
import p888 from '../../scenarios/project-888-house-construction.json';
import p90 from '../../scenarios/project-90-i69-as-planned.json';
import p70 from '../../scenarios/project-70-road-cpm.json';
import p823 from '../../scenarios/project-823-highway-timebased.json';
import p824 from '../../scenarios/project-824-highway-productionbased.json';
import p7 from '../../scenarios/project-7-steel.json';
import p777 from '../../scenarios/project-777-steel-project.json';

export interface ScenarioEntry {
  slug: string;
  title: string;
  blurb: string;
  scenario: Scenario;
}

/* eslint-disable @typescript-eslint/no-explicit-any */
const raw = (j: unknown) => fromRaw(j as any);

export const SCENARIOS: ScenarioEntry[] = [
  {
    slug: 'bridge',
    title: 'Truss Bridge',
    blurb: '5 activities, 14 planned days. Tight site storage forces delivery decisions. The place to learn the loop.',
    scenario: raw(p523),
  },
  {
    slug: 'i69',
    title: 'I-69 Reconstruction',
    blurb: '14 activities from a real Michigan DOT highway job — the project the research was validated against.',
    scenario: raw(p90),
  },
  {
    slug: 'highway',
    title: 'Highway (time-based)',
    blurb: '24 activities, ~140 planned days, stochastic material failures and high water tables.',
    scenario: raw(p823),
  },
  {
    slug: 'highway-production',
    title: 'Highway (production-based)',
    blurb: 'The same highway staged by production sections instead of time windows.',
    scenario: raw(p824),
  },
  {
    slug: 'road-cpm',
    title: 'Road (CPM staging)',
    blurb: '24 activities with shared crews and scarce site space — parallel work competing for the same deliveries.',
    scenario: raw(p70),
  },
  {
    slug: 'steel',
    title: 'Steel Erection',
    blurb: '18 activities of structural steel: cranes, oilers, welders — miss a key trade and nothing moves.',
    scenario: raw(p7),
  },
  {
    slug: 'steel-2',
    title: 'Steel Project',
    blurb: 'A second steel-erection staging of the same scope.',
    scenario: raw(p777),
  },
  {
    slug: 'house',
    title: 'House Construction',
    blurb: 'A compact residential build.',
    scenario: raw(p888),
  },
];

export function findScenario(slug: string): ScenarioEntry | undefined {
  return SCENARIOS.find((s) => s.slug === slug);
}
