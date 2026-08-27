'use client';

import type { PlayerDecision, ScheduleRow } from 'icdma-engine';

export default function DecisionPanel({
  rows, decisions, onChange, disabled,
}: {
  rows: ScheduleRow[];
  decisions: PlayerDecision[];
  onChange: (d: PlayerDecision[]) => void;
  disabled: boolean;
}) {
  const byId = new Map(rows.map((r) => [r.id, r]));
  const set = (activityId: number, patch: Partial<PlayerDecision>) => {
    onChange(decisions.map((d) => (d.activityId === activityId ? { ...d, ...patch } : d)));
  };

  return (
    <section className="card">
      <div className="label" style={{ marginBottom: '0.5rem' }}>Today&apos;s decisions</div>
      {decisions.length === 0 && (
        <div style={{ fontSize: '0.85rem', color: 'var(--muted)' }}>No activities are ready today.</div>
      )}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.7rem' }}>
        {decisions.map((d) => {
          const r = byId.get(d.activityId);
          if (!r) return null;
          return (
            <div key={d.activityId} style={{ borderTop: '1px solid var(--line)', paddingTop: '0.6rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                <strong style={{ fontFamily: 'var(--font-display)', fontSize: '0.9rem' }}>{r.name}</strong>
                <span className="num" style={{ fontSize: '0.8rem', color: r.percentComplete >= 100 ? 'var(--good)' : 'var(--muted)' }}>
                  {r.percentComplete}%
                </span>
              </div>
              <label style={{ display: 'block', fontSize: '0.78rem', color: 'var(--muted)', marginTop: '0.35rem' }}>
                Order materials today: <strong className="num" style={{ color: 'var(--ink)' }}>{Math.round(d.order)}%</strong> of a day&apos;s need
                <input
                  type="range" min={0} max={100} step={1} value={Math.round(d.order)}
                  disabled={disabled}
                  onChange={(e) => set(d.activityId, { order: Number(e.target.value) })}
                  style={{ width: '100%', display: 'block', marginTop: '0.15rem' }}
                />
              </label>
              <div style={{ display: 'flex', gap: '0.6rem', flexWrap: 'wrap', marginTop: '0.35rem', fontSize: '0.78rem', color: 'var(--muted)' }}>
                <label>
                  Days/wk{' '}
                  <select
                    value={d.workDays} disabled={disabled}
                    onChange={(e) => set(d.activityId, { workDays: Number(e.target.value) })}
                  >
                    {[5, 6, 7].map((n) => <option key={n} value={n}>{n}</option>)}
                  </select>
                </label>
                <label>
                  Hrs/day{' '}
                  <select
                    value={d.workHours} disabled={disabled}
                    onChange={(e) => set(d.activityId, { workHours: Number(e.target.value) })}
                  >
                    {[8, 9, 10, 12].map((n) => <option key={n} value={n}>{n}</option>)}
                  </select>
                </label>
                <label>
                  Wage ×{' '}
                  <select
                    value={d.wageIncentive} disabled={disabled}
                    onChange={(e) => set(d.activityId, { wageIncentive: Number(e.target.value) })}
                  >
                    {[1, 1.25, 1.5, 2].map((n) => <option key={n} value={n}>{n.toFixed(2)}</option>)}
                  </select>
                </label>
              </div>
              {(d.workHours > 8 || d.workDays > 5) && (
                <div style={{ fontSize: '0.72rem', color: 'var(--caution)', marginTop: '0.25rem' }}>
                  Overtime hours are half as productive; weekend work costs double.
                </div>
              )}
            </div>
          );
        })}
      </div>
    </section>
  );
}
