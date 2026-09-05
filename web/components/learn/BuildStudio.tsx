'use client';

import { useCallback, useState } from 'react';
import AcdBuilder, { type Graph } from './AcdBuilder';
import TutorChat, { PROBLEMS } from './TutorChat';

/** Part B: a problem, a blank canvas, and a tutor who can see the drawing. */
export default function BuildStudio() {
  const [problemId, setProblemId] = useState(PROBLEMS[0].id);
  const problem = PROBLEMS.find((p) => p.id === problemId) ?? PROBLEMS[0];
  const [snapshot, setSnapshot] = useState<{ graph: Graph; errors: string[] }>({ graph: { nodes: [], arcs: [] }, errors: [] });
  const onSnapshot = useCallback((graph: Graph, errors: string[]) => setSnapshot({ graph, errors }), []);

  // the tutor sees a compact description, not pixel positions
  const graphForTutor = {
    nodes: snapshot.graph.nodes.map((n) => ({
      id: n.id, kind: n.kind, label: n.label,
      ...(n.kind === 'queue' ? { units: n.tokens, fleet: !!n.fleet, resource: !!n.resource } : {}),
      ...(n.kind === 'combi' || n.kind === 'normal' ? { minutes: n.duration } : {}),
      ...(n.kind === 'counter' ? { perCompletion: n.units, unit: n.unitLabel } : {}),
    })),
    arrows: snapshot.graph.arcs.map((a) => `${a.from} -> ${a.to}`),
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem' }}>
      <section className="card">
        <div className="label" style={{ marginBottom: '0.4rem' }}>Pick a problem</div>
        <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap', marginBottom: '0.6rem' }}>
          {PROBLEMS.map((p) => (
            <button key={p.id} className="ghost" onClick={() => setProblemId(p.id)}
              style={p.id === problemId ? { borderColor: 'var(--accent)', color: 'var(--accent)', background: 'var(--wash-accent)' } : undefined}>
              {p.title}
            </button>
          ))}
        </div>
        <p style={{ fontSize: '0.95rem', margin: 0 }}>{problem.statement}</p>
      </section>
      <div style={{ display: 'grid', gap: '0.9rem', gridTemplateColumns: 'minmax(0, 3fr) minmax(18rem, 1fr)', alignItems: 'start' }} className="studio-grid">
        <AcdBuilder variant="build" onSnapshot={onSnapshot} />
        <section className="card" style={{ position: 'sticky', top: '0.8rem' }}>
          <TutorChat problem={problem} graph={graphForTutor} errors={snapshot.errors} />
        </section>
      </div>
    </div>
  );
}
