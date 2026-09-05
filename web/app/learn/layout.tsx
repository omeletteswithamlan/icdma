import { createClient } from '../../lib/supabase/server';

/**
 * Wraps every gated module page with a badge in the top-right corner saying
 * who is signed in and offering a way out. Lives in the layout so new modules
 * pick it up for free, and is fixed to the corner so the sign-out control is
 * still reachable from the bottom of a long module.
 */
export default async function LearnLayout({ children }: { children: React.ReactNode }) {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();

  return (
    <>
      {user ? (
        <div className="session-badge">
          <span className="who" title={user.email ?? undefined}>
            {user.email ?? 'Signed in'}
          </span>
          <form action="/auth/signout" method="post">
            <button type="submit" className="ghost">Sign out</button>
          </form>
        </div>
      ) : null}
      {children}
    </>
  );
}
