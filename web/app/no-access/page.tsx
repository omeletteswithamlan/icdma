import Link from 'next/link';
import { redirect } from 'next/navigation';
import { createClient } from '../../lib/supabase/server';
import { isAllowed } from '../../lib/access';

export const metadata = {
  title: 'Not on the course list — iCDMA',
};

/**
 * Where a signed-in visitor lands when their address is not on the allowlist.
 * Checks the list again on load, so someone who has just been added gets in by
 * refreshing rather than by being told to sign out and back in.
 */
export default async function NoAccessPage() {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();

  if (!user) redirect('/sign-in');
  if (await isAllowed(supabase, user.email)) redirect('/learn');

  return (
    <main style={{ maxWidth: '32rem', margin: '0 auto', padding: '4rem 1.2rem' }}>
      <div className="label">
        <Link href="/" style={{ color: 'inherit', textDecoration: 'none' }}>iCDMA</Link> · Course access
      </div>
      <h1 style={{ fontSize: '1.5rem', margin: '0.3rem 0 0.8rem' }}>This address is not on the course list</h1>

      <p style={{ color: 'var(--muted)', margin: '0 0 1rem' }}>
        You are signed in as <strong style={{ color: 'var(--ink)' }}>{user.email}</strong>, but the
        Fundamentals of Construction modules are open only to addresses Amlan has added. Ask Amlan to
        add this one, then reload this page — you will not need to sign in again.
      </p>
      <p style={{ color: 'var(--muted)', margin: '0 0 1.4rem' }}>
        If you have a second Google account that might be on the list, sign out and try that one.
      </p>

      <div style={{ display: 'flex', gap: '0.6rem', flexWrap: 'wrap', alignItems: 'center' }}>
        <form action="/auth/signout" method="post">
          <button type="submit" className="ghost">Sign out</button>
        </form>
        <Link href="/" style={{ color: 'var(--accent)', fontSize: '0.9rem' }}>
          Go to the simulator, which is open to everyone →
        </Link>
      </div>
    </main>
  );
}
