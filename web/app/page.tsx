import Link from 'next/link';
import { SCENARIOS } from '../lib/scenarios';

export default function Home() {
  return (
    <main style={{ maxWidth: '52rem', margin: '0 auto', padding: '3rem 1.2rem 4rem' }}>
      <div className="label">iCDMA · interactive construction decision-making</div>
      <h1 style={{ fontSize: '2rem', margin: '0.4rem 0 0.6rem', letterSpacing: '-0.01em' }}>
        Run the job. Own the consequences.
      </h1>
      <p style={{ color: 'var(--muted)', maxWidth: '38rem', marginTop: 0 }}>
        Pick a project. Each day you decide crews, hours, and material orders; the simulation
        decides what the weather, the deliveries, and your earlier choices do to the schedule
        and the budget. Query the future when you want to see the risk you are carrying.
      </p>
      <div
        style={{
          display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(15rem, 1fr))',
          gap: '0.8rem', marginTop: '1.6rem',
        }}
      >
        {SCENARIOS.map((s) => (
          <Link key={s.slug} href={`/play/${s.slug}`} style={{ textDecoration: 'none', color: 'inherit' }}>
            <div className="card" style={{ height: '100%' }}>
              <div style={{ fontFamily: 'var(--font-display)', fontWeight: 600 }}>{s.title}</div>
              <div style={{ fontSize: '0.85rem', color: 'var(--muted)', marginTop: '0.3rem' }}>{s.blurb}</div>
              <div className="num" style={{ fontSize: '0.78rem', color: 'var(--accent)', marginTop: '0.5rem' }}>
                {s.scenario.activities.length} activities · start {s.scenario.time.startDate.slice(0, 10)}
              </div>
            </div>
          </Link>
        ))}
      </div>
      <p style={{ fontSize: '0.8rem', color: 'var(--muted)', marginTop: '2.2rem' }}>
        Engine ported from the Virtual Coach / iCDMA research system (2003–2013) and verified
        against it to the cent. Scenarios recovered from the original research database.{' '}
        <Link href="/about" style={{ color: 'var(--accent)' }}>About this project — the people, the papers, the funding</Link>.
        {' '}New: <Link href="/learn" style={{ color: 'var(--accent)' }}>interactive course modules</Link> built from twenty years of CE3332.
      </p>
    </main>
  );
}
