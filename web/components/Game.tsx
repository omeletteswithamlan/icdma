'use client';

import { useMemo, useRef, useState } from 'react';
import Link from 'next/link';
import {
  Session, scheduleRows, costView, statusView,
  type PlayerDecision, type SpaceViolationInfo, type FuturesReport,
} from 'icdma-engine';
import { findScenario } from '../lib/scenarios';
import GanttChart from './GanttChart';
import CostChart from './CostChart';
import DecisionPanel from './DecisionPanel';
import EventFeed, { type FeedItem } from './EventFeed';
import FuturesPanel from './FuturesPanel';
import SpaceViolationModal from './SpaceViolationModal';

export default function Game({ slug, title }: { slug: string; title: string }) {
  const entry = useMemo(() => findScenario(slug)!, [slug]);
  const sessionRef = useRef<Session | null>(null);
  if (!sessionRef.current) sessionRef.current = new Session(entry.scenario, Date.now() % 100000);
  const s = sessionRef.current;

  const [, setVersion] = useState(0);
  const bump = () => setVersion((v) => v + 1);
  const [decisions, setDecisions] = useState<PlayerDecision[]>(() => s.defaultDecisions());
  const [violation, setViolation] = useState<SpaceViolationInfo | null>(null);
  const [feed, setFeed] = useState<FeedItem[]>([]);
  const [futures, setFutures] = useState<FuturesReport | null>(null);
  const [busy, setBusy] = useState(false);

  const status = statusView(s.engine);
  const weekday = new Date(status.dateISO + 'T00:00:00Z').toUTCString().slice(0, 3);
  const isWeekend = weekday === 'Sat' || weekday === 'Sun';
  const rows = scheduleRows(s.engine);
  const costs = costView(s.engine);

  const commit = (d: PlayerDecision[]) => {
    const report = s.playTurn(d);
    if (report.events.length > 0) {
      setFeed((f) => [
        ...report.events.map((e) => ({
          turn: report.turn,
          title: e.name,
          message: e.message || undefined,
        })),
        ...f,
      ]);
    }
    setDecisions(s.defaultDecisions());
    setFutures(null);
    bump();
  };

  const simulate = () => {
    const v = s.previewSpaceViolation(decisions);
    if (v) setViolation(v);
    else commit(decisions);
  };

  const runFutures = () => {
    setBusy(true);
    // let the button render its busy state before the synchronous crunch
    setTimeout(() => {
      try {
        setFutures(s.queryFutures(20));
      } catch (err) {
        console.error('queryFutures failed', err);
        setFutures({ samples: 20, finished: 0, days: [], costs: [] });
      }
      setBusy(false);
    }, 30);
  };

  const fmt = (x: number) =>
    x.toLocaleString('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 });

  return (
    <main style={{ maxWidth: '72rem', margin: '0 auto', padding: '1.2rem 1.2rem 3rem' }}>
      <header style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '0.9rem' }}>
        <div style={{ marginRight: 'auto' }}>
          <div className="label">
            <Link href="/" style={{ color: 'inherit', textDecoration: 'none' }}>iCDMA</Link> · {title}
          </div>
          <div style={{ display: 'flex', gap: '1.1rem', alignItems: 'baseline', flexWrap: 'wrap' }}>
            <h1 style={{ fontSize: '1.35rem', margin: '0.1rem 0 0' }}>
              Day {status.turn}
              <span style={{ color: 'var(--muted)', fontWeight: 500 }}> · {weekday} {status.dateISO}</span>
            </h1>
            {isWeekend && (
              <span className="num" style={{ fontSize: '0.8rem', padding: '0.1rem 0.55rem', borderRadius: '99px', background: 'var(--wash-accent)', color: 'var(--accent)', fontFamily: 'var(--font-display)', fontWeight: 600 }}>
                weekend — crews off unless scheduled
              </span>
            )}
            {status.weather && (
              <span
                className="num"
                style={{
                  fontSize: '0.8rem', padding: '0.1rem 0.55rem', borderRadius: '99px',
                  background: status.weather === 'Sunny' ? 'var(--wash-good)' : 'var(--wash-caution)',
                  color: status.weather === 'Sunny' ? 'var(--good)' : 'var(--caution)',
                  fontFamily: 'var(--font-display)', fontWeight: 600,
                }}
              >
                {status.weather}
              </span>
            )}
          </div>
        </div>
        <div className="num" style={{ textAlign: 'right', fontSize: '0.85rem', color: 'var(--muted)' }}>
          <div>Planned: <strong style={{ color: 'var(--ink)' }}>{fmt(costs.plannedTotal)}</strong> by day {costs.planned.length}</div>
          <div>Spent: <strong style={{ color: costs.builtToDate > costs.plannedTotal ? 'var(--caution)' : 'var(--ink)' }}>{fmt(costs.builtToDate)}</strong>
            {' '}· site space {Math.round(status.spaceUsed)}/{status.spaceTotal}</div>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button className="ghost" onClick={runFutures} disabled={busy || status.finished}>
            {busy ? 'Sampling…' : 'Query futures'}
          </button>
          <button className="primary" onClick={simulate} disabled={status.finished}>
            Simulate day {status.turn}
          </button>
        </div>
      </header>

      {status.finished && (
        <div
          className="card"
          style={{ marginTop: '1rem', borderLeft: '3px solid var(--good)', fontSize: '0.95rem' }}
        >
          <strong style={{ fontFamily: 'var(--font-display)' }}>Project complete</strong> — finished on
          day {status.turn - 1} against a plan of {costs.planned.length} days,
          for {fmt(costs.builtToDate)} against a budget of {fmt(costs.plannedTotal)}.
          {costs.builtToDate > costs.plannedTotal
            ? ` The overrun is ${fmt(costs.builtToDate - costs.plannedTotal)}.`
            : ' Under budget.'}
        </div>
      )}

      <div
        style={{
          display: 'grid', gap: '0.9rem', marginTop: '1rem',
          gridTemplateColumns: 'minmax(0, 7fr) minmax(17rem, 3fr)',
        }}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem', minWidth: 0 }}>
          <section className="card">
            <div className="label" style={{ marginBottom: '0.5rem' }}>Schedule</div>
            <GanttChart rows={rows} today={status.turn} lastTimeStep={status.lastTimeStep} />
          </section>
          <section className="card">
            <div className="label" style={{ marginBottom: '0.5rem' }}>Cumulative cost</div>
            <CostChart view={costs} />
          </section>
          {futures && <FuturesPanel report={futures} plannedTotal={costs.plannedTotal} plannedDays={costs.planned.length} />}
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem', minWidth: 0 }}>
          <DecisionPanel
            rows={rows}
            decisions={decisions}
            onChange={setDecisions}
            disabled={status.finished}
          />
          <EventFeed items={feed} />
        </div>
      </div>

      {violation && (
        <SpaceViolationModal
          violation={violation}
          overstockPenalty={entry.scenario.site.overstockPenalty}
          onApply={(cut) => {
            const d = s.applyProportionalCut(decisions, violation, cut);
            setViolation(null);
            commit(d);
          }}
          onCancel={() => setViolation(null)}
        />
      )}
    </main>
  );
}
