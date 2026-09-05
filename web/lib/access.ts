import type { SupabaseClient } from '@supabase/supabase-js';

/**
 * Is this address on the course allowlist?
 *
 * Signing in with Google only establishes who someone is; `course_access`
 * (migration 0123 in the omeletteswithamlan repo) decides whether they get in.
 * RLS lets a signed-in caller read only their own row, so a hit means "you are
 * on the list" and a miss means "you are not" — the query cannot be used to
 * enumerate anyone else.
 *
 * A database error is treated as "not allowed" rather than "allowed": if the
 * roster cannot be read, the safe answer is the closed door.
 */
export async function isAllowed(supabase: SupabaseClient, email: string | undefined) {
  if (!email) return false;
  const { data, error } = await supabase
    .from('course_access')
    .select('email')
    .eq('email', email.toLowerCase())
    .maybeSingle();
  if (error) {
    console.error('course_access lookup failed', error.message);
    return false;
  }
  return data !== null;
}
