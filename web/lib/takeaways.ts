/**
 * The CE3332 Takeaways (Spring 2019) — the course's own statement of what a
 * student should be able to do. Modules are built backward from these; every
 * module page shows the takeaways it serves as objective chips.
 */

export interface Takeaway {
  n: number;
  text: string;
}

export const TAKEAWAYS: Takeaway[] = [
  { n: 1, text: 'Construct a relationship between equipment production rate, cycle time, operation duration and work to be done.' },
  { n: 2, text: 'Relate banked volume to loose volume and compacted volume using swell% and shrink% factors.' },
  { n: 3, text: 'Identify information from equipment data sheets to estimate production rates for standard excavating equipment.' },
  { n: 4, text: 'Identify the components to connect when drawing an activity cycle diagram for operations with interacting equipment — both material flow and equipment use.' },
  { n: 5, text: 'Apply the principle of continuous operation in designing an operation.' },
  { n: 6, text: 'For off-road hauling: explain rolling, grade, and effective grade resistance; power required; available power, gear, and speed from the rimpull curve; and usable power.' },
  { n: 7, text: 'Conduct quantity take-offs from geometry: pavements, CMU walls and bond beams, earthwork, strip foundations, CMU foundation walls.' },
  { n: 8, text: 'Use a unit-cost book to estimate production rates and cost for specific job items.' },
  { n: 9, text: 'Distinguish detailed from preliminary estimates; make time- and location-based cost adjustments.' },
  { n: 10, text: 'Explain the significance of critical path and float in network scheduling.' },
  { n: 11, text: 'Differentiate free float from total float, and how each applies to activities and paths.' },
  { n: 12, text: 'Construct a precedence network and apply CPM to solve for early/late starts and finishes, free float, and total float.' },
  { n: 13, text: 'State clearly the role of the critical path in a construction schedule.' },
  { n: 14, text: 'Calculate the net present value of owning a piece of equipment (P, A, F, n, i).' },
  { n: 15, text: 'Estimate equipment depreciation by straight-line, double-declining-balance, and MACRS methods.' },
  { n: 16, text: 'Conduct a net present worth analysis comparing two procurement alternatives.' },
  { n: 17, text: 'Conduct a cash flow analysis for a project.' },
  { n: 18, text: 'Identify the internal rate of return through project cash flow analysis.' },
  { n: 19, text: 'Describe the bidding process: participants, relationships, and the elements of a Notice to Bidders.' },
  { n: 20, text: 'Explain contracts, delivery systems, and the trade-offs that drive engineering decision-making.' },
];

export interface ModuleDef {
  slug: string;
  title: string;
  tagline: string;
  takeaways: number[];
  status: 'live' | 'soon';
}

export const MODULES: ModuleDef[] = [
  {
    slug: 'operations',
    title: 'Design the Operation',
    tagline: 'Cycles, fleets, and the balance point — a discrete-event simulation studio.',
    takeaways: [1, 2, 3, 4, 5],
    status: 'live',
  },
  {
    slug: 'haul',
    title: 'Move the Earth',
    tagline: 'Resistance, rimpull, and usable power on a haul profile you draw.',
    takeaways: [6],
    status: 'soon',
  },
  {
    slug: 'takeoff',
    title: 'Count the Work',
    tagline: 'Quantity take-off as geometry you can drag.',
    takeaways: [7],
    status: 'soon',
  },
  {
    slug: 'estimating',
    title: 'Price the Work',
    tagline: 'Unit costs, detailed vs preliminary, time and place.',
    takeaways: [8, 9],
    status: 'soon',
  },
  {
    slug: 'scheduling',
    title: 'Schedule the Job',
    tagline: 'CPM on the same engine that runs the simulation.',
    takeaways: [10, 11, 12, 13],
    status: 'soon',
  },
  {
    slug: 'money',
    title: 'Money and Machines',
    tagline: 'Ownership, depreciation, cash flow, and the rate of return.',
    takeaways: [14, 15, 16, 17, 18],
    status: 'soon',
  },
  {
    slug: 'business',
    title: 'The Business of Building',
    tagline: 'Bids, contracts, and delivery systems.',
    takeaways: [19, 20],
    status: 'soon',
  },
];
