'use client';

import { useState } from 'react';
import type { CostView } from 'icdma-engine';

const W = 720;
const H = 220;
const M = { top: 14, right: 90, bottom: 24, left: 64 };

export default function CostChart({ view }: { view: CostView }) {
  const [hoverDay, setHoverDay] = useState<number | null>(null);
  const allDays = [...view.planned.map(([d]) => d), ...view.built.map(([d]) => d)];
  const allVals = [...view.planned.map(([, v]) => v), ...view.built.map(([, v]) => v)];
  const maxDay = Math.max(1, ...allDays);
  const maxVal = Math.max(1, ...allVals) * 1.05;

  const x = (d: number) => M.left + (d / maxDay) * (W - M.left - M.right);
  const y = (v: number) => H - M.bottom - (v / maxVal) * (H - M.top - M.bottom);

  const path = (pts: [number, number][]) =>
    pts.map(([d, v], i) => `${i === 0 ? 'M' : 'L'}${x(d).toFixed(1)},${y(v).toFixed(1)}`).join(' ');

  const actual = view.built.filter(([d]) => d <= view.today);
  const projected = view.built.filter(([d]) => d >= Math.max(1, view.today));

  const yTicks = [0.25, 0.5, 0.75, 1].map((f) => maxVal * f);
  const fmtK = (v: number) => (v >= 1e6 ? `$${(v / 1e6).toFixed(1)}M` : `$${Math.round(v / 1e3)}k`);

  const near = (day: number) => {
    const p = view.planned.find(([d]) => d === day)?.[1];
    const b = view.built.find(([d]) => d === day)?.[1];
    return { p, b };
  };

  return (
    <div style={{ overflowX: 'auto' }}>
      <svg
        viewBox={`0 0 ${W} ${H}`}
        style={{ width: '100%', minWidth: '540px', display: 'block' }}
        role="img"
        aria-label="Cumulative cost: as-planned baseline versus as-built actuals and projection"
        onMouseMove={(e) => {
          const rect = (e.currentTarget as SVGSVGElement).getBoundingClientRect();
          const px = ((e.clientX - rect.left) / rect.width) * W;
          const d = Math.round(((px - M.left) / (W - M.left - M.right)) * maxDay);
          setHoverDay(d >= 1 && d <= maxDay ? d : null);
        }}
        onMouseLeave={() => setHoverDay(null)}
      >
        {yTicks.map((v) => (
          <g key={v}>
            <line x1={M.left} y1={y(v)} x2={W - M.right} y2={y(v)} stroke="var(--vd-grid)" strokeWidth="1" />
            <text x={M.left - 8} y={y(v) + 3.5} fontSize="10" fill="var(--muted)" textAnchor="end" className="num">
              {fmtK(v)}
            </text>
          </g>
        ))}
        <line x1={M.left} y1={H - M.bottom} x2={W - M.right} y2={H - M.bottom} stroke="var(--line)" strokeWidth="1" />
        <text x={W - M.right} y={H - 8} fontSize="10" fill="var(--muted)" textAnchor="end" className="num">
          day {maxDay}
        </text>

        {/* planned baseline */}
        <path d={path(view.planned)} fill="none" stroke="var(--muted)" strokeWidth="1.6" strokeDasharray="5 4" opacity="0.8" />
        {/* as-built actuals */}
        {actual.length > 1 && <path d={path(actual)} fill="none" stroke="var(--vd-blue)" strokeWidth="2.2" />}
        {/* projection */}
        {projected.length > 1 && (
          <path d={path(projected)} fill="none" stroke="var(--vd-orange)" strokeWidth="1.8" strokeDasharray="2 4" />
        )}
        {/* endpoint markers + direct labels */}
        {view.planned.length > 0 && (
          <text
            x={x(view.planned[view.planned.length - 1][0]) + 6}
            y={y(view.planned[view.planned.length - 1][1]) - 6}
            fontSize="10.5" fill="var(--muted)" fontWeight="600"
          >
            planned
          </text>
        )}
        {actual.length > 0 && (
          <>
            <circle
              cx={x(actual[actual.length - 1][0])} cy={y(actual[actual.length - 1][1])}
              r="4" fill="var(--vd-blue)" stroke="var(--surface)" strokeWidth="2"
            />
            <text
              x={x(actual[actual.length - 1][0]) - 6} y={y(actual[actual.length - 1][1]) - 8}
              fontSize="10.5" fill="var(--vd-blue)" fontWeight="600" textAnchor="end"
            >
              actual
            </text>
          </>
        )}
        {projected.length > 1 && (
          <text
            x={x(projected[projected.length - 1][0]) + 6}
            y={y(projected[projected.length - 1][1]) + 13}
            fontSize="10.5" fill="var(--vd-orange)" fontWeight="600"
          >
            projected
          </text>
        )}
        {/* crosshair */}
        {hoverDay !== null && (() => {
          const { p, b } = near(hoverDay);
          return (
            <g>
              <line x1={x(hoverDay)} y1={M.top} x2={x(hoverDay)} y2={H - M.bottom} stroke="var(--muted)" strokeWidth="1" opacity="0.5" />
              <g transform={`translate(${Math.min(x(hoverDay) + 8, W - 170)}, ${M.top + 4})`}>
                <rect width="160" height={p !== undefined && b !== undefined ? 46 : 32} rx="5"
                  fill="var(--surface)" stroke="var(--line)" />
                <text x="8" y="14" fontSize="10" fill="var(--muted)" className="num">day {hoverDay}</text>
                {b !== undefined && (
                  <text x="8" y="27" fontSize="10.5" fill="var(--ink)" className="num">
                    {hoverDay <= view.today ? 'actual' : 'projected'} {fmtK(b)}
                  </text>
                )}
                {p !== undefined && (
                  <text x="8" y={b !== undefined ? 40 : 27} fontSize="10.5" fill="var(--muted)" className="num">
                    planned {fmtK(p)}
                  </text>
                )}
              </g>
            </g>
          );
        })()}
      </svg>
    </div>
  );
}
