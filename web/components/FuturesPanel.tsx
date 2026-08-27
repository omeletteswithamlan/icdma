'use client';

import type { FuturesReport } from 'icdma-engine';

function quantile(sorted: number[], q: number): number {
  if (sorted.length === 0) return 0;
  const i = (sorted.length - 1) * q;
  const lo = Math.floor(i);
  return sorted[lo] + (sorted[Math.ceil(i)] - sorted[lo]) * (i - lo);
}

export default function FuturesPanel({
  report, plannedTotal, plannedDays,
}: { report: FuturesReport; plannedTotal: number; plannedDays: number }) {
  const days = [...report.days].sort((a, b) => a - b);
  const costs = [...report.costs].sort((a, b) => a - b);

  if (report.finished === 0) {
    return (
      <section className="card">
        <div className="label" style={{ marginBottom: '0.4rem' }}>The future, sampled</div>
        <div style={{ fontSize: '0.88rem', color: 'var(--muted)' }}>
          No sampled future finished within the forecast horizon — on this course the project
          does not complete. Order more aggressively or add working days, then sample again.
        </div>
      </section>
    );
  }

  const counts = new Map<number, number>();
  for (const d of report.days) counts.set(d, (counts.get(d) ?? 0) + 1);
  const dayKeys = [...counts.keys()].sort((a, b) => a - b);
  const maxCount = Math.max(1, ...counts.values());

  const W = 720; const H = 130; const M = { top: 8, right: 16, bottom: 26, left: 16 };
  const minD = Math.min(plannedDays, dayKeys[0] ?? plannedDays) - 1;
  const maxD = Math.max(plannedDays, dayKeys[dayKeys.length - 1] ?? plannedDays) + 1;
  const x = (d: number) => M.left + ((d - minD) / (maxD - minD)) * (W - M.left - M.right);
  const barW = Math.max(6, Math.min(26, (W - M.left - M.right) / (maxD - minD) - 2));

  const fmt = (v: number) => v.toLocaleString('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 });

  return (
    <section className="card">
      <div className="label" style={{ marginBottom: '0.4rem' }}>
        The future, sampled {report.samples} times
      </div>
      <div style={{ fontSize: '0.85rem', color: 'var(--muted)', marginBottom: '0.4rem' }}>
        Finishing your current course from here:{' '}
        <strong className="num" style={{ color: 'var(--ink)' }}>
          day {quantile(days, 0.5).toFixed(0)}
        </strong>{' '}
        and{' '}
        <strong className="num" style={{ color: 'var(--ink)' }}>{fmt(quantile(costs, 0.5))}</strong>{' '}
        in the median case; the worst sample ends day {days[days.length - 1]} at {fmt(costs[costs.length - 1])}.
        {report.finished < report.samples &&
          ` ${report.samples - report.finished} samples did not finish within the horizon.`}
      </div>
      <div style={{ overflowX: 'auto' }}>
        <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', minWidth: '540px', display: 'block' }}
          role="img" aria-label="Histogram of completion day across sampled futures">
          <line x1={M.left} y1={H - M.bottom} x2={W - M.right} y2={H - M.bottom} stroke="var(--line)" />
          {/* planned marker */}
          <line x1={x(plannedDays)} y1={M.top} x2={x(plannedDays)} y2={H - M.bottom} stroke="var(--vd-aqua)" strokeWidth="1.5" strokeDasharray="4 3" />
          <text x={x(plannedDays)} y={M.top + 8} fontSize="9.5" fill="var(--vd-aqua)" fontWeight="600" textAnchor="middle">plan</text>
          {dayKeys.map((d) => {
            const c = counts.get(d)!;
            const h = (c / maxCount) * (H - M.top - M.bottom - 14);
            return (
              <g key={d}>
                <rect x={x(d) - barW / 2} y={H - M.bottom - h} width={barW} height={h}
                  fill="var(--vd-blue)" rx="3" />
                <text x={x(d)} y={H - M.bottom - h - 4} fontSize="9.5" fill="var(--muted)" textAnchor="middle" className="num">{c}</text>
                <text x={x(d)} y={H - M.bottom + 13} fontSize="10" fill="var(--muted)" textAnchor="middle" className="num">{d}</text>
              </g>
            );
          })}
          <text x={W - M.right} y={H - 4} fontSize="9.5" fill="var(--muted)" textAnchor="end">completion day</text>
        </svg>
      </div>
    </section>
  );
}
