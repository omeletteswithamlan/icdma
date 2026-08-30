import Link from 'next/link';
import { MODULES, TAKEAWAYS } from '../../lib/takeaways';

export const metadata = { title: 'Learn — iCDMA' };

export default function LearnPage() {
  return (
    <main style={{ maxWidth: '46rem', margin: '0 auto', padding: '2.5rem 1.2rem 4rem' }}>
      <div className="label">
        <Link href="/" style={{ color: 'inherit', textDecoration: 'none' }}>iCDMA</Link> · Learn
      </div>
      <h1 style={{ fontSize: '1.7rem', margin: '0.3rem 0 0.6rem' }}>
        Construction engineering, one takeaway at a time
      </h1>
      <p style={{ color: 'var(--muted)' }}>
        Interactive modules built from a course taught for twenty years — each designed
        backward from the capabilities its students were promised, photographed and
        filmed on a real highway reconstruction, and computed by the same engine that
        runs the <Link href="/">simulation</Link>.
      </p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.8rem', marginTop: '1.4rem' }}>
        {MODULES.map((m, i) => {
          const inner = (
            <div className="card" style={{ opacity: m.status === 'live' ? 1 : 0.65 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: '0.8rem' }}>
                <strong style={{ fontFamily: 'var(--font-display)', fontSize: '1.02rem' }}>
                  {i + 1}. {m.title}
                </strong>
                <span className="num" style={{ fontSize: '0.72rem', color: m.status === 'live' ? 'var(--good)' : 'var(--muted)', fontFamily: 'var(--font-display)', fontWeight: 600, letterSpacing: '0.06em', textTransform: 'uppercase' }}>
                  {m.status === 'live' ? 'Open' : 'Coming'}
                </span>
              </div>
              <div style={{ fontSize: '0.9rem', color: 'var(--muted)', marginTop: '0.2rem' }}>{m.tagline}</div>
              <div style={{ display: 'flex', gap: '0.3rem', flexWrap: 'wrap', marginTop: '0.5rem' }}>
                {m.takeaways.map((n) => (
                  <span key={n} title={TAKEAWAYS[n - 1].text} className="num"
                    style={{ fontSize: '0.7rem', border: '1px solid var(--line)', borderRadius: 99, padding: '0.05rem 0.45rem', color: 'var(--muted)' }}>
                    T{n}
                  </span>
                ))}
              </div>
            </div>
          );
          return m.status === 'live'
            ? <Link key={m.slug} href={`/learn/${m.slug}`} style={{ textDecoration: 'none', color: 'inherit' }}>{inner}</Link>
            : <div key={m.slug}>{inner}</div>;
        })}
      </div>
      <p style={{ fontSize: '0.8rem', color: 'var(--muted)', marginTop: '1.6rem' }}>
        The chips are the course&apos;s own Takeaways — the twenty things a student of
        CE3332 should be able to do. Hover any chip to read it.
      </p>
    </main>
  );
}
