/**
 * Sanitises a `next` redirect target taken from the query string.
 *
 * Only same-site absolute paths are allowed. Without this an attacker could
 * hand someone a /sign-in?next=https://evil.example link and use the site's
 * own login as an open redirect. `//evil.example` is protocol-relative and has
 * to be rejected too, which is why a leading-slash test alone is not enough.
 */
export function safeNext(value: string | undefined | null, fallback = '/learn') {
  if (!value) return fallback;
  if (!value.startsWith('/') || value.startsWith('//')) return fallback;
  return value;
}
