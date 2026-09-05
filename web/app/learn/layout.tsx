import { createClient } from '../../lib/supabase/server';

/**
 * Wraps every gated module page with a quiet line saying who is signed in and
 * offering a way out. Lives in the layout so new modules pick it up for free.
 */
export default async function LearnLayout({ children }: { children: React.ReactNode }) {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();

  return (
    <>
      {children}
      {user ? (
        <div
          style={{
            maxWidth: '50rem',
            margin: '0 auto',
            padding: '0 1.2rem 2.5rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.8rem',
            flexWrap: 'wrap',
            fontSize: '0.85rem',
            color: 'var(--muted)',
          }}
        >
          <span>Signed in as {user.email ?? 'your Google account'}.</span>
          <form action="/auth/signout" method="post">
            <button type="submit" className="ghost" style={{ fontSize: '0.82rem', padding: '0.25rem 0.7rem' }}>
              Sign out
            </button>
          </form>
        </div>
      ) : null}
    </>
  );
}
