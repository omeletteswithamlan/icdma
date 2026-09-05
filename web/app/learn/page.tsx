import Link from 'next/link';
import { MODULES, TAKEAWAYS } from '../../lib/takeaways';

export const metadata = {
  title: 'Fundamentals of Construction — iCDMA',
  description: 'CE3332, Fundamentals of Construction Engineering, rebuilt as interactive modules: twenty takeaways, seven modules, one simulation engine.',
};

const first = MODULES[0];

export default function LearnPage() {
  return (
    <main style={{ maxWidth: '50rem', margin: '0 auto', padding: '2.5rem 1.2rem 4rem' }}>
      <div className="label">
        <Link href="/" style={{ color: 'inherit', textDecoration: 'none' }}>iCDMA</Link> · Syllabus
      </div>
      <h1 style={{ fontSize: '1.9rem', margin: '0.3rem 0 0.8rem' }}>Fundamentals of Construction</h1>
      <p style={{ color: 'var(--muted)', margin: '0 0 0.6rem' }}>
        A course taught for two decades, rebuilt as a set of interactive modules. Each module is
        designed backward from the course&apos;s own <strong>Takeaways</strong> — the twenty things a
        student should be able to do at the end of the semester — and is photographed and filmed on a
        real highway reconstruction and computed by the same engine that runs the{' '}
        <Link href="/">situational simulation</Link>.
      </p>
      <p style={{ color: 'var(--muted)', margin: 0 }}>
        Every module gives you a problem, a model you can change, and the plots that answer. When a
        module has a &ldquo;play&rdquo; part you can pause the simulation, make a decision, and see what it
        was worth. The chips under each module are the Takeaways it teaches; hover one to read it.
      </p>

      <section className="card" style={{ marginTop: '1.4rem', borderColor: 'var(--accent)' }}>
        <div className="label" style={{ color: 'var(--accent)' }}>Start here · Module 1</div>
        <h2 style={{ fontSize: '1.3rem', margin: '0.2rem 0 0.3rem' }}>{first.title}</h2>
        <p style={{ margin: '0 0 0.6rem', fontSize: '0.95rem' }}>
          Design an earthwork operation: a CAT 235C excavator, a fleet of trucks, and 20,000 bank cubic
          yards to move. Draw the activity cycle diagram, size the fleet for continuous operation, watch
          it run, pause and change your mind, then build an operation of your own with a tutor who can
          see your drawing.
        </p>
        <div style={{ display: 'flex', gap: '0.3rem', flexWrap: 'wrap', marginBottom: '0.7rem' }}>
          {first.takeaways.map((n) => (
            <span key={n} title={TAKEAWAYS[n - 1].text} className="num"
              style={{ fontSize: '0.72rem', border: '1px solid var(--line)', borderRadius: 99, padding: '0.05rem 0.45rem', color: 'var(--muted)' }}>
              T{n}
            </span>
          ))}
        </div>
        <Link href={`/learn/${first.slug}`} className="primary" style={{ display: 'inline-block', textDecoration: 'none' }}>
          Open Module 1 →
        </Link>
      </section>

      <h2 style={{ fontSize: '1.15rem', margin: '1.8rem 0 0.6rem' }}>The seven modules</h2>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.8rem' }}>
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

      <h2 style={{ fontSize: '1.15rem', margin: '1.8rem 0 0.5rem' }}>The twenty Takeaways</h2>
      <p style={{ fontSize: '0.88rem', color: 'var(--muted)', margin: '0 0 0.6rem' }}>
        What a student of CE3332 should be able to do. The modules above are built from this list, not the other way round.
      </p>
      <ol style={{ margin: 0, paddingLeft: '1.4rem', fontSize: '0.92rem', lineHeight: 1.55, columns: '2 20rem', columnGap: '2rem' }}>
        {TAKEAWAYS.map((t) => {
          const mod = MODULES.find((m) => m.takeaways.includes(t.n));
          return (
            <li key={t.n} style={{ breakInside: 'avoid', marginBottom: '0.35rem', color: mod?.status === 'live' ? 'var(--ink)' : 'var(--muted)' }}>
              {t.text}
              {mod && <span className="num" style={{ fontSize: '0.72rem', color: 'var(--muted)' }}> · {mod.title}</span>}
            </li>
          );
        })}
      </ol>

      <p style={{ fontSize: '0.8rem', color: 'var(--muted)', marginTop: '1.8rem' }}>
        Course material and media by Amlan Mukherjee. Photographs and video from the I-69 reconstruction,
        Michigan DOT. The simulation engine and its history are described on the{' '}
        <Link href="/about">About page</Link>.
      </p>
    </main>
  );
}
