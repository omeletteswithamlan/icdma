import Link from 'next/link';
import ExploreOperation from '../../../components/learn/ExploreOperation';
import BuildStudio from '../../../components/learn/BuildStudio';
import PartHeader from '../../../components/learn/PartHeader';

export const metadata = { title: 'Design the Operation — iCDMA' };

export default function OperationsModule() {
  return (
    <main style={{ maxWidth: '92rem', margin: '0 auto', padding: '1.6rem 1.2rem 3rem' }}>
      <div className="label">
        <Link href="/" style={{ color: 'inherit', textDecoration: 'none' }}>iCDMA</Link>
        {' '}· <Link href="/learn" style={{ color: 'inherit', textDecoration: 'none' }}>Learn</Link>
        {' '}· Module 1
      </div>
      <h1 style={{ fontSize: '1.6rem', margin: '0.25rem 0 0.8rem' }}>Design the Operation</h1>

      <ExploreOperation />

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
