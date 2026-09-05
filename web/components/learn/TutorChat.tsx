'use client';

import { useEffect, useRef, useState } from 'react';

interface Msg { role: 'user' | 'assistant'; content: string }

export interface TutorProblem {
  id: string;
  title: string;
  statement: string;
}

export const PROBLEMS: TutorProblem[] = [
  {
    id: 'cut-to-fill',
    title: 'Cut to fill with one excavator',
    statement: 'Move 20,000 BCY of common earth (25% swell) from a cut to a fill 2 miles away. One hydraulic excavator with a 2.0 LCY bucket cycles in about 25 seconds per pass; 16-LCY dump trucks are available for rent. Round-trip haul is about 8 minutes out and 6 minutes back, and dumping takes 1.5 minutes. Draw the operation, find the fleet size that keeps the excavator continuously busy, and estimate production and the number of 8-hour shifts.',
  },
  {
    id: 'two-loaders',
    title: 'Two loaders, one fleet',
    statement: 'Two identical wheel loaders (each fills a 12-LCY truck in 2.5 minutes) share one fleet of trucks hauling gravel to a stockpile 5 minutes away (4 minutes back, 1 minute to dump). Draw the operation so both loaders can work in parallel, then size the fleet so neither loader waits.',
  },
  {
    id: 'concrete-pump',
    title: 'Placing concrete with a pump',
    statement: 'A pump places concrete for a bridge deck at 10 CY per 8-minute truckload. Ready-mix trucks (10 CY) take 20 minutes to arrive from the plant and 15 minutes to return; the plant loads a truck in 6 minutes and only one truck can load at a time. Draw the placement operation (plant, trucks, pump, deck) and find the number of trucks that keeps the pump continuously placing.',
  },
];

export default function TutorChat({ problem, graph, errors }: {
  problem: TutorProblem;
  graph: unknown;
  errors: string[];
}) {
  const [messages, setMessages] = useState<Msg[]>([]);
  const [draft, setDraft] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const scroller = useRef<HTMLDivElement>(null);

  useEffect(() => { setMessages([]); setError(null); }, [problem.id]);
  useEffect(() => { scroller.current?.scrollTo({ top: scroller.current.scrollHeight }); }, [messages, busy]);

  const send = async (text: string) => {
    const content = text.trim();
    if (!content || busy) return;
    const next: Msg[] = [...messages, { role: 'user', content }];
    setMessages(next); setDraft(''); setBusy(true); setError(null);
    try {
      const res = await fetch('/api/tutor', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ problem: problem.statement, graph, errors, messages: next }),
      });
      const data = (await res.json()) as { reply?: string; error?: string };
      if (!res.ok || !data.reply) { setError(data.error ?? 'The tutor did not answer.'); }
      else setMessages([...next, { role: 'assistant', content: data.reply }]);
    } catch {
      setError('The tutor could not be reached.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: '26rem' }}>
      <div className="label" style={{ marginBottom: '0.4rem' }}>Tutor</div>
      <div ref={scroller} style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '0.5rem', padding: '0.2rem 0.1rem', fontSize: '0.9rem' }}>
        {messages.length === 0 && (
          <div style={{ color: 'var(--muted)' }}>
            Draw as much as you can, then ask. I can see your diagram as you build it — ask things
            like &ldquo;what should LOAD connect to?&rdquo;, &ldquo;is my fleet right?&rdquo;, or &ldquo;why won&apos;t it simulate?&rdquo;
          </div>
        )}
        {messages.map((m, i) => (
          <div key={i} style={{
            alignSelf: m.role === 'user' ? 'flex-end' : 'flex-start', maxWidth: '92%',
            padding: '0.45rem 0.7rem', borderRadius: 10,
            background: m.role === 'user' ? 'var(--wash-accent)' : 'var(--bg)',
            border: '1px solid var(--line)', whiteSpace: 'pre-wrap',
          }}>{m.content}</div>
        ))}
        {busy && <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Thinking…</div>}
        {error && <div style={{ color: 'var(--caution)', fontSize: '0.85rem' }}>{error}</div>}
      </div>
      <div style={{ display: 'flex', gap: '0.4rem', marginTop: '0.5rem' }}>
        <input
          value={draft} onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') send(draft); }}
          placeholder="Ask the tutor…" disabled={busy}
          style={{ flex: 1, padding: '0.45rem 0.6rem', fontSize: '0.92rem', border: '1px solid var(--line)', borderRadius: 6, background: 'var(--surface)', color: 'var(--ink)' }}
        />
        <button className="primary" onClick={() => send(draft)} disabled={busy || !draft.trim()}>Send</button>
      </div>
      <div style={{ display: 'flex', gap: '0.3rem', flexWrap: 'wrap', marginTop: '0.4rem' }}>
        {['Where do I start?', 'Check my diagram', 'How many trucks?'].map((q) => (
          <button key={q} className="ghost" style={{ fontSize: '0.75rem' }} onClick={() => send(q)} disabled={busy}>{q}</button>
        ))}
      </div>
    </div>
  );
}
