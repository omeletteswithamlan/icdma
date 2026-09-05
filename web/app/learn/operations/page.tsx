import Link from 'next/link';
import { TAKEAWAYS, MODULES } from '../../../lib/takeaways';
import AcdBuilder from '../../../components/learn/AcdBuilder';
import BuildStudio from '../../../components/learn/BuildStudio';

export const metadata = { title: 'Design the Operation — iCDMA' };

const mod = MODULES.find((m) => m.slug === 'operations')!;

function PartHeader({ part, title, blurb }: { part: string; title: string; blurb: string }) {
  return (
    <div style={{ margin: '1.6rem 0 0.7rem', borderTop: '2px solid var(--ink)', paddingTop: '0.7rem' }}>
      <div className="label">{part}</div>
      <h2 style={{ fontSize: '1.25rem', margin: '0.15rem 0 0.25rem' }}>{title}</h2>
      <p style={{ fontSize: '0.92rem', color: 'var(--muted)', margin: 0, maxWidth: '52rem' }}>{blurb}</p>
    </div>
  );
}

export default function OperationsModule() {
  return (
    <main style={{ maxWidth: '78rem', margin: '0 auto', padding: '1.6rem 1.2rem 3rem' }}>
      <div className="label">
        <Link href="/" style={{ color: 'inherit', textDecoration: 'none' }}>iCDMA</Link>
        {' '}· <Link href="/learn" style={{ color: 'inherit', textDecoration: 'none' }}>Learn</Link>
        {' '}· Module 1
      </div>
      <h1 style={{ fontSize: '1.6rem', margin: '0.25rem 0 0.8rem' }}>Design the Operation</h1>

      <section className="card" style={{ marginBottom: '0.9rem' }}>
        <div style={{ display: 'grid', gap: '1rem', gridTemplateColumns: 'minmax(0, 3fr) minmax(0, 2fr)', alignItems: 'start' }} className="studio-grid">
          <div>
            <div className="label" style={{ marginBottom: '0.4rem' }}>The problem</div>
            <p style={{ fontSize: '1.05rem', margin: '0 0 0.6rem', fontFamily: 'var(--font-display)', fontWeight: 600 }}>
              Design an earthwork operation to move 20,000 bank cubic yards of common earth
              from a cut on the I-69 reconstruction to a fill two miles down the alignment.
            </p>
            <p style={{ fontSize: '0.95rem', margin: '0 0 0.6rem' }}>
              You have one CAT 235C hydraulic excavator — a 2.0 LCY bucket cycling in about
              25 seconds — and can rent 16-LCY tandem-axle dump trucks. The soil swells 25%
              from bank to loose. Hauling to the fill takes about 8 minutes, dumping 1.5,
              and returning 6.
            </p>
            <p style={{ fontSize: '0.95rem', margin: 0 }}>
              <strong>Decide:</strong> how many trucks keep the excavator continuously busy,
              what production rate that fleet delivers in bank measure, and how many 8-hour
              shifts the job takes.
            </p>
            <details style={{ marginTop: '0.7rem' }}>
              <summary style={{ fontSize: '0.85rem', color: 'var(--muted)', cursor: 'pointer' }}>
                What this problem teaches ({mod.takeaways.map((n) => `T${n}`).join(' · ')})
              </summary>
              <ol style={{ fontSize: '0.88rem', color: 'var(--muted)', margin: '0.4rem 0 0', paddingLeft: '1.2rem' }}>
                {mod.takeaways.map((n) => <li key={n} value={n}>{TAKEAWAYS[n - 1].text}</li>)}
              </ol>
            </details>
          </div>
          <div>
            <img src="/media/jobsite-013.jpg" alt="A CAT 235C hydraulic excavator loading a tandem-axle dump truck on the I-69 reconstruction"
              style={{ width: '100%', borderRadius: 6, border: '1px solid var(--line)' }} />
            <p style={{ fontSize: '0.78rem', color: 'var(--muted)', margin: '0.35rem 0 0' }}>
              The 235C loading out on I-69. Photograph: Amlan Mukherjee, Michigan DOT project.
            </p>
          </div>
        </div>
      </section>

      <PartHeader part="Part A" title="Explore the operation"
        blurb="The problem, drawn as an activity cycle diagram. Press Simulate to watch it run. Use the − + controls on the symbols to change the fleet, the excavator's load time, or the haul — and watch the plots answer." />
      <AcdBuilder variant="explore" />

      <PartHeader part="Part B" title="Build your own operation"
        blurb="A blank canvas, a new problem, and a tutor who can see what you draw. Build the diagram yourself; ask when you are stuck." />
      <BuildStudio />

      <section className="card" style={{ marginTop: '1.2rem' }}>
        <div style={{ display: 'grid', gap: '0.9rem', gridTemplateColumns: 'minmax(0,1fr) minmax(0,1fr)' }} className="studio-grid">
          <div>
            <div className="label" style={{ marginBottom: '0.4rem' }}>The lecture: equipment cycle time</div>
            <video controls preload="metadata" style={{ width: '100%', borderRadius: 6, border: '1px solid var(--line)' }}>
              <source src="/media/lecture-equipment-ct.mp4" type="video/mp4" />
            </video>
          </div>
          <div>
            <div className="label" style={{ marginBottom: '0.4rem' }}>Run it</div>
            <p style={{ fontSize: '0.92rem' }}>
              An operation is a morning&apos;s decision. A project is a hundred mornings, with
              weather, deliveries, and a budget. Take the production thinking you just
              practiced into the <Link href="/play/i69">I-69 simulation</Link> — the same
              project this excavator worked on.
            </p>
            <p style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>
              The diagram notation is CYCLONE — Halpin, D. W. (1977), &ldquo;CYCLONE: Method for
              Modeling of Job Site Processes,&rdquo; <em>Journal of the Construction Division</em>,
              103(3) — as generalized by Julio Martinez&apos;s STROBOSCOPE: Martinez, J. C. (1996),
              <em> STROBOSCOPE: State and Resource Based Simulation of Construction Processes</em>,
              doctoral dissertation, University of Michigan; and Martinez, J. C., and Ioannou, P. G.
              (1999), &ldquo;General-Purpose Systems for Effective Construction Simulation,&rdquo;
              <em> Journal of Construction Engineering and Management</em>, 125(4), 265–276.
            </p>
          </div>
        </div>
      </section>
    </main>
  );
}
