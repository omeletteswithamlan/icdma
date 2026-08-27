'use client';

export interface FeedItem {
  turn: number;
  title: string;
  message?: string;
}

export default function EventFeed({ items }: { items: FeedItem[] }) {
  return (
    <section className="card">
      <div className="label" style={{ marginBottom: '0.5rem' }}>Events</div>
      {items.length === 0 ? (
        <div style={{ fontSize: '0.85rem', color: 'var(--muted)' }}>Nothing has gone wrong. Yet.</div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', maxHeight: '16rem', overflowY: 'auto' }}>
          {items.map((e, i) => (
            <div key={i} style={{ borderLeft: '3px solid var(--caution)', paddingLeft: '0.6rem' }}>
              <div style={{ fontSize: '0.78rem', color: 'var(--muted)' }} className="num">Day {e.turn}</div>
              <div style={{ fontFamily: 'var(--font-display)', fontWeight: 600, fontSize: '0.88rem' }}>{e.title}</div>
              {e.message && <div style={{ fontSize: '0.82rem', color: 'var(--muted)' }}>{e.message}</div>}
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
