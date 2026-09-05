export default function PartHeader({ part, title, blurb }: { part: string; title: string; blurb: string }) {
  return (
    <div style={{ margin: '1.6rem 0 0.7rem', borderTop: '2px solid var(--ink)', paddingTop: '0.7rem' }}>
      <div className="label">{part}</div>
      <h2 style={{ fontSize: '1.25rem', margin: '0.15rem 0 0.25rem' }}>{title}</h2>
      <p style={{ fontSize: '0.92rem', color: 'var(--muted)', margin: 0, maxWidth: '52rem' }}>{blurb}</p>
    </div>
  );
}
