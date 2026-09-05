import { createServerClient } from '@supabase/ssr';
import { NextResponse, type NextRequest } from 'next/server';
import { isAllowed } from './lib/access';

/**
 * Gates the /learn section behind a Google sign-in.
 *
 * The simulator (/play, /about, the landing page) stays public — only the
 * course modules and the tutor endpoint they call require an account.
 *
 * Two gates, not one. Google establishes WHO someone is; the `course_access`
 * allowlist decides WHETHER they get in. Someone with a valid Google account
 * who is not on the list is signed in and still refused — they land on
 * /no-access rather than being bounced back to a sign-in they have already
 * completed, which would loop forever.
 *
 * The @supabase/ssr and @supabase/supabase-js versions in web/package.json are
 * PINNED EXACTLY on purpose. Middleware runs in the Edge runtime, where
 * generating code from strings is forbidden, and @supabase/ssr 0.12.6 with
 * supabase-js 2.115.0 bundles something that does exactly that at import time.
 * The failure is invisible in `next dev` and takes down EVERY route under
 * `next start` — the public landing page and the simulator included — with
 * "EvalError: Code generation from strings disallowed for this context".
 * If you bump these, run `next build && next start` and load `/` before
 * deploying; a passing dev server proves nothing here.
 */

/** Paths that require a signed-in user. Everything else is public. */
function isGated(pathname: string) {
  return pathname === '/learn' || pathname.startsWith('/learn/') || pathname === '/api/tutor';
}

export async function middleware(request: NextRequest) {
  let supabaseResponse = NextResponse.next({ request });

  const supabase = createServerClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
    {
      cookies: {
        getAll() {
          return request.cookies.getAll();
        },
        setAll(cookiesToSet) {
          cookiesToSet.forEach(({ name, value }) => request.cookies.set(name, value));
          supabaseResponse = NextResponse.next({ request });
          cookiesToSet.forEach(({ name, value, options }) =>
            supabaseResponse.cookies.set(name, value, options),
          );
        },
      },
    },
  );

  // IMPORTANT: Do not run code between createServerClient and getUser().
  // getUser() revalidates the token; skipping it causes hard-to-debug session bugs.
  const {
    data: { user },
  } = await supabase.auth.getUser();

  const { pathname, search } = request.nextUrl;
  if (!isGated(pathname)) return supabaseResponse;

  // Carries any refreshed auth cookies onto a redirect; without them the next
  // request arrives with a stale session and bounces again.
  const bounce = (to: string) => {
    const url = request.nextUrl.clone();
    url.pathname = to;
    url.search = to === '/sign-in' ? `?next=${encodeURIComponent(pathname + search)}` : '';
    const redirect = NextResponse.redirect(url);
    supabaseResponse.cookies.getAll().forEach((c) => redirect.cookies.set(c));
    return redirect;
  };

  // The tutor is called with fetch() from the module page, so it needs a status
  // code the client can react to rather than a redirect to an HTML page.
  const isTutor = pathname === '/api/tutor';

  if (!user) {
    return isTutor
      ? NextResponse.json({ error: 'Sign in to use the tutor.' }, { status: 401 })
      : bounce('/sign-in');
  }

  if (!(await isAllowed(supabase, user.email))) {
    return isTutor
      ? NextResponse.json({ error: 'This account is not on the course list.' }, { status: 403 })
      : bounce('/no-access');
  }

  return supabaseResponse;
}

export const config = {
  matcher: ['/learn', '/learn/:path*', '/api/tutor'],
};
