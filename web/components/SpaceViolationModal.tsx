'use client';

import { useState } from 'react';
import type { SpaceViolationInfo } from 'icdma-engine';

export default function SpaceViolationModal({
  violation, overstockPenalty, onApply, onCancel,
}: {
  violation: SpaceViolationInfo;
  overstockPenalty: number;
  onApply: (cut: number) => void;
  onCancel: () => void;
}) {
  const v = violation;
  const [cut, setCut] = useState(Math.ceil(v.minimumCut));
  const r2 = (x: number) => Math.round(x * 100) / 100;

  return (
    <div
      role="dialog" aria-modal="true" aria-label="Site space exceeded"
      style={{
        position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 40, padding: '1rem',
      }}
    >
      <div className="card" style={{ maxWidth: '30rem', width: '100%', borderTop: '3px solid var(--caution)' }}>
        <h2 style={{ fontSize: '1.05rem', margin: '0 0 0.4rem' }}>Not enough room on site</h2>
        <p style={{ fontSize: '0.88rem', color: 'var(--muted)', margin: '0 0 0.6rem' }}>
          Today&apos;s orders need more storage space than the site has. Cut the orders to fit —
          returned materials are refunded at {Math.round(overstockPenalty * 100)}% of value.
        </p>
        <div className="num" style={{ fontSize: '0.9rem', display: 'grid', gridTemplateColumns: 'auto auto', gap: '0.15rem 1rem', width: 'fit-content' }}>
          <span style={{ color: 'var(--muted)' }}>Space available on site</span><strong>{r2(v.spaceAvailable)}</strong>
          <span style={{ color: 'var(--muted)' }}>Space required by orders</span><strong>{r2(v.spaceRequired)}</strong>
          <span style={{ color: 'var(--muted)' }}>Minimum cut to fit</span><strong style={{ color: 'var(--caution)' }}>{r2(v.minimumCut)}</strong>
        </div>
        <div style={{ marginTop: '0.8rem', display: 'flex', gap: '0.6rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <label style={{ fontSize: '0.85rem', color: 'var(--muted)' }}>
            Space to cut{' '}
            <input
              type="number" className="num"
              min={Math.ceil(v.minimumCut)} max={Math.ceil(v.spaceRequired)} step={1}
              value={cut}
              onChange={(e) => setCut(Number(e.target.value))}
            />
          </label>
          <button
            className="primary"
            onClick={() => onApply(Math.max(cut, v.minimumCut))}
          >
            Cut proportionally &amp; simulate
          </button>
          <button className="ghost" onClick={onCancel}>Back to decisions</button>
        </div>
        <div style={{ fontSize: '0.75rem', color: 'var(--muted)', marginTop: '0.6rem' }}>
          The cut is spread across today&apos;s ordering activities in proportion to the space each needs:
          {' '}{v.perActivity.map((p) => `${p.name} (${r2(p.space)})`).join(', ')}.
        </div>
      </div>
    </div>
  );
}
