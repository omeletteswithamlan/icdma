import Link from 'next/link';
import { TAKEAWAYS, MODULES } from '../../../lib/takeaways';
import AcdBuilder from '../../../components/learn/AcdBuilder';

export const metadata = { title: 'Design the Operation — iCDMA' };

const mod = MODULES.find((m) => m.slug === 'operations')!;

export default function OperationsModule() {
  return (
    <main style={{ maxWidth: '76rem', margin: '0 auto', padding: '1.6rem 1.2rem 3rem' }}>
      <div className="label">
        <Link href="/" style={{ color: 'inherit', textDecoration: 'none' }}>iCDMA</Link>
        {' '}· <Link href="/learn" style={{ color: 'inherit', textDecoration: 'none' }}>Learn</Link>
        {' '}· Module 1
      </div>
      <h1 style={{ fontSize: '1.6rem', margin: '0.25rem 0 0.4rem' }}>Design the Operation</h1>
      <details style={{ marginBottom: '0.9rem' }}>
        <summary style={{ fontSize: '0.85rem', color: 'var(--muted)', cursor: 'pointer' }}>
          After this module you should be able to… ({mod.takeaways.map((n) => `T${n}`).join(' · ')})
        </summary>
        <ol style={{ fontSize: '0.88rem', color: 'var(--muted)', margin: '0.4rem 0 0', paddingLeft: '1.2rem' }}>
          {mod.takeaways.map((n) => <li key={n} value={n}>{TAKEAWAYS[n - 1].text}</li>)}
        </ol>
      </details>

      <section className="card" style={{ marginBottom: '0.9rem' }}>
        <div style={{ display: 'grid', gap: '0.9rem', gridTemplateColumns: 'minmax(0, 3fr) minmax(0, 2fr)', alignItems: 'start' }} className="studio-grid">
          <img src="/media/jobsite-013.jpg" alt="A CAT 235C hydraulic excavator loading a tandem-axle dump truck on the I-69 reconstruction"
            style={{ width: '100%', borderRadius: 6, border: '1px solid var(--line)' }} />
          <div>
            <div className="label" style={{ marginBottom: '0.4rem' }}>The operation</div>
            <p style={{ fontSize: '0.95rem', margin: '0 0 0.6rem' }}>
              One excavator, a fleet of trucks, a bank of soil that has to become fill
              somewhere else. The excavator can only load when a truck is under its bucket;
              a truck can only be loaded when the excavator is free. Two cycles, one
              shared moment — that interaction is the whole subject of this module.
            </p>
            <p style={{ fontSize: '0.95rem', margin: 0 }}>
              Draw it as an activity cycle diagram below, give each step its time, and run
              it. Then resize the fleet and watch production plateau as trucks start
              waiting on the machine.
            </p>
            <img src="/media/jobsite-104.jpg" alt="The CAT 235C excavator, side view"
              style={{ width: '100%', borderRadius: 6, border: '1px solid var(--line)', marginTop: '0.7rem' }} />
          </div>
        </div>
      </section>

      <AcdBuilder />

      <section className="card" style={{ marginTop: '0.9rem' }}>
        <div style={{ display: 'grid', gap: '0.9rem', gridTemplateColumns: 'minmax(0,1fr) minmax(0,1fr)' }} className="studio-grid">
          <div>
            <div className="label" style={{ marginBottom: '0.4rem' }}>The lecture: equipment cycle time</div>
            <video controls preload="metadata"
              style={{ width: '100%', borderRadius: 6, border: '1px solid var(--line)' }}>
              <source src="/media/lecture-equipment-ct.mp4" type="video/mp4" />
            </video>
          </div>
          <div>
            <div className="label" style={{ marginBottom: '0.4rem' }}>Run it</div>
            <p style={{ fontSize: '0.92rem' }}>
              An operation is a morning&apos;s decision. A project is a hundred mornings,
              with weather, deliveries, and a budget. Take the production thinking you just
              practiced into the <Link href="/play/i69">I-69 simulation</Link> — the same
              project this excavator worked on.
            </p>
            <p style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>
              Photographs: Amlan Mukherjee, I-69 reconstruction, Michigan DOT.
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
