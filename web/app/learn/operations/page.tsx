import Link from 'next/link';
import { TAKEAWAYS, MODULES } from '../../../lib/takeaways';
import OperationStudio from '../../../components/learn/OperationStudio';

export const metadata = { title: 'Design the Operation — iCDMA' };

const mod = MODULES.find((m) => m.slug === 'operations')!;

export default function OperationsModule() {
  return (
    <main style={{ maxWidth: '72rem', margin: '0 auto', padding: '1.6rem 1.2rem 3rem' }}>
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
        <div style={{ display: 'grid', gap: '0.9rem', gridTemplateColumns: 'minmax(0, 3fr) minmax(0, 2fr)' }} className="studio-grid">
          <div>
            <div className="label" style={{ marginBottom: '0.4rem' }}>See it: the I-69 reconstruction</div>
            <video controls preload="metadata" poster="/media/jobsite-108.jpg"
              style={{ width: '100%', borderRadius: 6, border: '1px solid var(--line)' }}>
              <source src="/media/jobsite-120.mp4" type="video/mp4" />
            </video>
            <p style={{ fontSize: '0.82rem', color: 'var(--muted)', margin: '0.4rem 0 0' }}>
              Watch the loading cycle: excavator swing, truck exchange, the pause when no
              truck is waiting. Count seconds per pass — then find those numbers in the
              model on this page. Every clip and photo here is from a real Michigan DOT
              highway job.
            </p>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <img src="/media/jobsite-104.jpg" alt="Excavator loading a haul truck on the I-69 reconstruction"
              style={{ width: '100%', borderRadius: 6, border: '1px solid var(--line)' }} />
            <img src="/media/jobsite-115.jpg" alt="Haul trucks staged along the I-69 grade"
              style={{ width: '100%', borderRadius: 6, border: '1px solid var(--line)' }} />
          </div>
        </div>
      </section>

      <OperationStudio />

      <section className="card" style={{ marginTop: '0.9rem' }}>
        <div style={{ display: 'grid', gap: '0.9rem', gridTemplateColumns: 'minmax(0,1fr) minmax(0,1fr)' }} className="studio-grid">
          <div>
            <div className="label" style={{ marginBottom: '0.4rem' }}>The lecture, if you want it</div>
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
              practiced into the <Link href="/play/i69">I-69 simulation</Link> —
              the same project these photographs came from — and see what continuous
              operation is worth over a season.
            </p>
            <p style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>
              Photos and video: Amlan Mukherjee, I-69 reconstruction, Michigan DOT project.
            </p>
          </div>
        </div>
      </section>
    </main>
  );
}
