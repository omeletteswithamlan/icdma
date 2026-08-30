'use client';

import type { CrewStaffView, PlayerDecision } from 'icdma-engine';

/**
 * Hire/fire panel for one activity's crews. Counts reset to the full
 * complement each morning (legacy behavior) — changes apply to today's turn.
 */
export default function CrewEditor({
  views, decision, onChange, disabled,
}: {
  views: CrewStaffView[];
  decision: PlayerDecision;
  onChange: (staffing: NonNullable<PlayerDecision['staffing']>) => void;
  disabled: boolean;
}) {
  const setCount = (crewId: number, laborId: number, count: number) => {
    const staffing = (decision.staffing ?? []).map((s) =>
      s.crewId === crewId
        ? { ...s, members: s.members.map((m) => (m.laborId === laborId ? { ...m, count } : m)) }
        : s,
    );
    onChange(staffing);
  };

  const fmt = (x: number) => x.toLocaleString('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 });
  const totalCost = views.reduce((s, v) => s + v.dailyCost, 0);
  const pace = views.length ? Math.min(...views.map((v) => v.pace)) : 0;

  return (
    <details style={{ marginTop: '0.35rem' }}>
      <summary style={{ fontSize: '0.78rem', color: 'var(--muted)', cursor: 'pointer' }}>
        Crews: <strong className="num" style={{ color: pace === 0 ? 'var(--caution)' : 'var(--ink)' }}>
          {Math.round(pace * 100)}% pace
        </strong>{' '}
        · <span className="num">{fmt(totalCost)}</span>/day — hire or fire
      </summary>
      {views.map((v) => (
        <div key={v.crewId} style={{ margin: '0.4rem 0 0.2rem', paddingLeft: '0.4rem', borderLeft: '2px solid var(--line)' }}>
          <div style={{ fontSize: '0.75rem', fontFamily: 'var(--font-display)', fontWeight: 600 }}>
            {v.name} <span className="num" style={{ color: 'var(--muted)', fontWeight: 400 }}>{fmt(v.dailyCost)}/day</span>
          </div>
          {v.members.map((m) => (
            <div key={m.laborId} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.76rem', marginTop: '0.15rem' }}>
              <span style={{ flex: 1, color: m.count === 0 ? 'var(--muted)' : 'var(--ink)' }}>
                {m.isEquipment ? '⚙ ' : ''}{m.name}
                <span className="num" style={{ color: 'var(--muted)' }}> {fmt(m.unitCost)}/d</span>
              </span>
              <button
                className="ghost" disabled={disabled || m.count <= 0}
                style={{ padding: '0 0.45rem', lineHeight: '1.2rem' }}
                onClick={() => setCount(v.crewId, m.laborId, m.count - 1)}
                aria-label={`Remove one ${m.name}`}
              >−</button>
              <span className="num" style={{
                minWidth: '2.4rem', textAlign: 'center',
                color: m.count < m.baseCount ? 'var(--caution)' : m.count > m.baseCount ? 'var(--accent)' : 'var(--ink)',
              }}>
                {m.count}<span style={{ color: 'var(--muted)' }}>/{m.baseCount}</span>
              </span>
              <button
                className="ghost" disabled={disabled || m.count >= m.baseCount * 3}
                style={{ padding: '0 0.45rem', lineHeight: '1.2rem' }}
                onClick={() => setCount(v.crewId, m.laborId, m.count + 1)}
                aria-label={`Hire one ${m.name}`}
              >+</button>
            </div>
          ))}
          {v.warnings.map((w) => (
            <div key={w} style={{ fontSize: '0.72rem', color: 'var(--caution)', marginTop: '0.2rem' }}>{w}</div>
          ))}
        </div>
      ))}
      <div style={{ fontSize: '0.7rem', color: 'var(--muted)', marginTop: '0.3rem' }}>
        Staffing resets to the full complement each morning. Fewer hands slow the pace;
        extra hands cost full wages at 80% effectiveness.
      </div>
    </details>
  );
}
