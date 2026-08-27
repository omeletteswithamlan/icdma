'use client';

import { useState } from 'react';
import type { ScheduleRow } from 'icdma-engine';

const ROW_H = 26;
const LABEL_W = 190;
const AXIS_H = 22;

export default function GanttChart({
  rows, today, lastTimeStep,
}: { rows: ScheduleRow[]; today: number; lastTimeStep: number }) {
  const [hover, setHover] = useState<number | null>(null);
  const span = Math.max(lastTimeStep, today + 1);
  const W = 720;
  const plotW = W - LABEL_W - 8;
  const x = (day: number) => LABEL_W + ((day - 1) / span) * plotW;
  const H = rows.length * ROW_H + AXIS_H;

  const ticks: number[] = [];
  const step = span > 200 ? 28 : span > 60 ? 14 : span > 28 ? 7 : 2;
  for (let d = 1; d <= span; d += step) ticks.push(d);

  return (
    <div style={{ overflowX: 'auto' }}>
      <svg
        viewBox={`0 0 ${W} ${H}`}
        style={{ width: '100%', minWidth: '540px', display: 'block' }}
        role="img"
        aria-label="Gantt chart of activities: planned window versus live schedule"
      >
        {ticks.map((d) => (
          <g key={d}>
            <line x1={x(d)} y1={AXIS_H - 6} x2={x(d)} y2={H} stroke="var(--vd-grid)" strokeWidth="1" />
            <text x={x(d)} y={AXIS_H - 10} fontSize="10" fill="var(--muted)" textAnchor="middle" className="num">
              {d}
            </text>
          </g>
        ))}
        {/* today line */}
        <line x1={x(today)} y1={AXIS_H - 4} x2={x(today)} y2={H} stroke="var(--vd-orange)" strokeWidth="1.5" strokeDasharray="4 3" />
        <text x={x(today) + 4} y={AXIS_H + 6} fontSize="9.5" fill="var(--vd-orange)" fontWeight="600">today</text>

        {rows.map((r, i) => {
          const y = AXIS_H + i * ROW_H;
          const mid = y + ROW_H / 2;
          const done = r.percentComplete >= 100;
          const liveFill = done ? 'var(--vd-aqua)' : r.active ? 'var(--vd-blue)' : 'var(--muted)';
          return (
            <g
              key={r.id}
              onMouseEnter={() => setHover(r.id)}
              onMouseLeave={() => setHover(null)}
              opacity={hover !== null && hover !== r.id ? 0.55 : 1}
            >
              <text
                x={LABEL_W - 8} y={mid + 3.5} fontSize="11" textAnchor="end"
                fill={r.critical ? 'var(--ink)' : 'var(--muted)'}
                fontWeight={r.critical ? 600 : 400}
              >
                {r.name.length > 26 ? `${r.name.slice(0, 25)}…` : r.name}
              </text>
              {/* planned window (outline) */}
              <rect
                x={x(r.plannedStart)} y={mid - 8}
                width={Math.max(2, x(r.plannedEnd) - x(r.plannedStart))} height={16}
                fill="none" stroke="var(--line)" strokeWidth="1" rx="3"
              />
              {/* live window */}
              <rect
                x={x(r.start)} y={mid - 5.5}
                width={Math.max(2, x(r.end) - x(r.start))} height={11}
                fill={liveFill} opacity={r.active || done ? 0.92 : 0.45} rx="3"
              />
              {/* completion overlay label */}
              {(r.active || done) && (
                <text
                  x={x(r.end) + 5} y={mid + 3.5} fontSize="10" className="num"
                  fill={done ? 'var(--vd-aqua)' : 'var(--muted)'} fontWeight="600"
                >
                  {r.percentComplete}%
                </text>
              )}
              {r.critical && (
                <rect x={LABEL_W + 1} y={mid - 5.5} width={2.5} height={11} fill="var(--vd-orange)" rx="1" />
              )}
              {hover === r.id && (
                <text x={LABEL_W + 6} y={y + ROW_H - 1} fontSize="9.5" fill="var(--muted)" className="num">
                  plan {r.plannedStart}–{r.plannedEnd - 1} · now {r.start}–{r.end - 1}
                  {r.start !== r.plannedStart || r.end !== r.plannedEnd
                    ? ` · slip +${r.end - r.plannedEnd}d` : ''}
                  {` · late ${r.lateStart}/${r.lateFinish}`}
                </text>
              )}
            </g>
          );
        })}
      </svg>
      <div style={{ display: 'flex', gap: '1rem', fontSize: '0.72rem', color: 'var(--muted)', marginTop: '0.3rem', flexWrap: 'wrap' }}>
        <span><span style={{ display: 'inline-block', width: 10, height: 10, borderRadius: 2, border: '1px solid var(--line)', verticalAlign: '-1px' }} /> planned</span>
        <span><span style={{ display: 'inline-block', width: 10, height: 10, borderRadius: 2, background: 'var(--vd-blue)', verticalAlign: '-1px' }} /> in progress</span>
        <span><span style={{ display: 'inline-block', width: 10, height: 10, borderRadius: 2, background: 'var(--vd-aqua)', verticalAlign: '-1px' }} /> complete</span>
        <span><span style={{ display: 'inline-block', width: 3, height: 10, borderRadius: 1, background: 'var(--vd-orange)', verticalAlign: '-1px' }} /> critical path</span>
      </div>
    </div>
  );
}
