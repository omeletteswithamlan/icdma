import { notFound } from 'next/navigation';
import { findScenario, SCENARIOS } from '../../../lib/scenarios';
import Game from '../../../components/Game';

export function generateStaticParams() {
  return SCENARIOS.map((s) => ({ slug: s.slug }));
}

export default async function PlayPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const entry = findScenario(slug);
  if (!entry) notFound();
  return <Game slug={entry.slug} title={entry.title} />;
}
