import Link from 'next/link';
import { notFound } from 'next/navigation';
import HaulStudio from '../../../components/learn/HaulStudio';
import { MODULES } from '../../../lib/takeaways';

export const metadata = { title: 'Move the Earth — iCDMA' };

export default function HaulModule() {
  // Under construction: reachable on localhost, a 404 in production until the
  // module is flipped to 'live' in lib/takeaways.ts.
  if (process.env.NODE_ENV === 'production' && MODULES.find((m) => m.slug === 'haul')?.status !== 'live') notFound();
  return (
    <main style={{ maxWidth: '92rem', margin: '0 auto', padding: '1.6rem 1.2rem 3rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.9rem', flexWrap: 'wrap' }}>
        <Link href="/learn" className="ghost"
          style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', borderRadius: 999, padding: '0.3rem 0.85rem 0.3rem 0.7rem', textDecoration: 'none', fontSize: '0.85rem' }}>
          <span aria-hidden>←</span> Back to the syllabus
        </Link>
        <div className="label">
          <Link href="/" style={{ color: 'inherit', textDecoration: 'none' }}>iCDMA</Link>
          {' '}· Fundamentals of Construction · Module 2
        </div>
      </div>
      <h1 style={{ fontSize: '1.6rem', margin: '0.5rem 0 0.8rem' }}>Move the Earth</h1>

      <HaulStudio />

      <section className="card" style={{ marginTop: '1.2rem' }}>
        <div style={{ display: 'grid', gap: '0.9rem', gridTemplateColumns: 'minmax(0,1fr) minmax(0,1fr)' }} className="studio-grid">
          <div>
            <div className="label" style={{ marginBottom: '0.4rem' }}>The lecture: reading gradability, speed and rimpull curves</div>
            <video controls preload="metadata" style={{ width: '100%', borderRadius: 6, border: '1px solid var(--line)' }}>
              <source src="/media/lecture-rimpull-curves.mp4" type="video/mp4" />
            </video>
            <p style={{ fontSize: '0.78rem', color: 'var(--muted)', margin: '0.35rem 0 0' }}>
              The lecture reads a manufacturer&apos;s chart: gross weight down to the total-resistance line, across to the
              curve, down to the speed. The chart on this page is the same reading made from a synthetic machine.
            </p>
          </div>
          <div>
            <div className="label" style={{ marginBottom: '0.4rem' }}>About the machine on this page</div>
            <p style={{ fontSize: '0.92rem' }}>
              Manufacturers&apos; rimpull and travel-time charts are copyrighted, so the hauler here is a synthetic one:
              a plausible 16-LCY articulated truck whose curve is generated from its horsepower, drivetrain efficiency and
              gear speeds. The relationships are the textbook ones and do not depend on any particular make; when a
              manufacturer&apos;s permission arrives, its published curve can replace the synthetic one without changing
              the lesson.
            </p>
            <p style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>
              Method after Peurifoy, R. L., Schexnayder, C. J., Schmitt, R. L., and Shapira, A., <em>Construction Planning,
              Equipment, and Methods</em> (McGraw-Hill), and Nunnally, S. W., <em>Construction Methods and Management</em>
              (Pearson): rolling resistance in lb/ton, 20 lb/ton per percent of grade, rimpull = 375 × hp × efficiency ÷ mph,
              usable rimpull = coefficient of traction × weight on the driving wheels.
            </p>
          </div>
        </div>
      </section>
    </main>
  );
}
