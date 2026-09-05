import { NextResponse, type NextRequest } from 'next/server';
import { createClient } from '../../../lib/supabase/server';
import { safeNext } from '../../../lib/safeNext';

/**
 * Where Google sends the visitor back to. Trades the one-time code for a
 * session (the Supabase server client writes the auth cookies), then forwards
 * them to whatever they were trying to reach.
 */
export async function GET(request: NextRequest) {
  const { searchParams, origin } = request.nextUrl;
  const code = searchParams.get('code');
  const next = safeNext(searchParams.get('next'));

  if (code) {
    const supabase = await createClient();
    const { error } = await supabase.auth.exchangeCodeForSession(code);
    if (!error) return NextResponse.redirect(`${origin}${next}`);
  }

  const description = searchParams.get('error_description') ?? 'Could not complete the sign-in.';
  return NextResponse.redirect(
    `${origin}/sign-in?next=${encodeURIComponent(next)}&error=${encodeURIComponent(description)}`,
  );
}
