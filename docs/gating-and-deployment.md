# Gating, middleware, and deployment — state as of 2026-09-05

What stands between a visitor and the course modules, and how the site gets
built. Two sessions changed this on the same day; this is the merged picture.

## Who can see what

| Path | Public? | Gate |
|---|---|---|
| `/`, `/play/*`, `/about` | yes | none — the simulator and its history are open |
| `/learn` (syllabus), `/learn/*` (modules) | **no** | Google sign-in **and** the `course_access` allowlist |
| `/api/tutor` | **no** | same two checks, repeated inside the route |
| `/sign-in`, `/no-access`, `/auth/*` | yes | the gate's own pages |

## Layer 1 — build-time switch (`web/next.config.ts`)

- Until 2026-09-05 the `/learn` tree was **hidden in production** unless
  `SHOW_LEARN=1` was set: `next.config.ts` emitted redirects from `/learn/:path*`
  to `/` and the landing-page link was suppressed via `NEXT_PUBLIC_SHOW_LEARN`.
- On 2026-09-05 Amlan asked for a public "Fundamentals of Construction" card
  on omeletteswithamlan linking to the syllabus. That was the go-live decision,
  so the switch was **inverted to opt-out**: the section is visible unless
  `HIDE_LEARN=1` is set on the deployment. Local dev always shows it.
- Modules still under construction are marked `status: 'soon'` in
  `web/lib/takeaways.ts`; the syllabus lists them but does not link them. New
  modules are built on localhost and flipped to `'live'` only when Amlan says so.

## Layer 2 — request-time middleware (`web/middleware.ts`, commit bfa1695)

Added the same afternoon in a separate session. It gates `/learn`, `/learn/*`
and `/api/tutor` behind two checks:

1. **Identity — Google sign-in through Supabase Auth.** The middleware builds a
   server client from `NEXT_PUBLIC_SUPABASE_URL` / `NEXT_PUBLIC_SUPABASE_ANON_KEY`
   (set on the Vercel project for Production) and calls `getUser()` first,
   before any other work, so the token is revalidated on every gated request.
2. **Authorization — the `course_access` allowlist** (`web/lib/access.ts`).
   The table lives in the shared Supabase project and is created by
   **migration 0123 in the omeletteswithamlan repo**. Row-level security lets a
   signed-in caller read only their own row, so the lookup answers "are you on
   the list" and cannot enumerate other addresses. Emails are compared
   lower-cased. A database error counts as *not allowed*: if the roster cannot
   be read, the door stays closed.

Behaviour on refusal:

- Not signed in → page requests bounce to `/sign-in?next=<original path>`;
  the tutor endpoint returns **401** JSON instead, because it is called with
  `fetch()` and cannot follow an HTML redirect.
- Signed in but not on the list → `/no-access` (which names the refused address
  and re-checks the list on load); the tutor returns **403** JSON. Refused
  users are *not* sent back to sign-in, which would loop forever.
- Redirects carry any refreshed auth cookies with them; without that the next
  request arrives with a stale session and bounces again.

Supporting files: `web/app/sign-in/page.tsx`, `web/components/GoogleSignInButton.tsx`,
`web/app/auth/callback/route.ts`, `web/app/auth/signout/route.ts`,
`web/app/no-access/page.tsx`, `web/app/learn/layout.tsx` (signed-in badge,
top-right), `web/lib/safeNext.ts` (only same-site `next=` targets are honoured),
`web/lib/supabase/{client,server}.ts`.

**Pinned dependencies.** `@supabase/ssr` and `@supabase/supabase-js` are pinned
to exact versions in `web/package.json`. Middleware runs on the Edge runtime,
which forbids generating code from strings; `@supabase/ssr 0.12.6` with
`supabase-js 2.115.0` does exactly that at import time. The failure is invisible
under `next dev` and takes down **every** route under `next start`, the public
landing page included. Before bumping either package: `next build && next start`
and load `/`.

**Local development bypass.** With `LEARN_AUTH_BYPASS=1` in `web/.env.local`
and `NODE_ENV=development`, the middleware returns early and the course pages
open without a Google session, so modules can be built and checked headlessly.
Vercel builds always run with `NODE_ENV=production`, so the flag has no effect
there. The tutor route does not honour it: testing the tutor locally still
needs a signed-in, allowlisted account.

**Modules under construction.** A module page whose entry in
`web/lib/takeaways.ts` is not `status: 'live'` calls `notFound()` in
production (see `web/app/learn/haul/page.tsx`), so pushing it to master keeps it
reachable on localhost only. Flipping the status publishes it and links it from
the syllabus in one change.

## The tutor route (`web/app/api/tutor/route.ts`)

- Reads `ANTHROPIC_API_KEY` from the environment (Vercel: Production, Preview
  and Development all set on 2026-09-05; locally `web/.env.local`, gitignored).
  Returns 503 with instructions when it is missing, and a plain "no credits"
  message when the API account's balance is empty.
- Explicit `baseURL` so an unrelated `ANTHROPIC_BASE_URL` in a shell cannot
  redirect it.
- Repeats the sign-in and allowlist checks itself rather than trusting the
  middleware matcher, since every call spends money.
- Rate limit keyed on the **account**, not the IP, so a shared campus address
  cannot put a whole class on one budget; message and payload sizes are capped.
- Model `claude-opus-5`, adaptive thinking, cached system prompt.

## Deployment

- Vercel project `icdma` is connected to `github.com/omeletteswithamlan/icdma`
  (default branch `master`, which Vercel uses as the production branch) as of
  2026-09-05. Every push builds and deploys; nothing is deployed from the CLI
  any more. The first Git builds failed because `engine/dist` is gitignored and
  the CLI deploys had been uploading a local build, so `web/package.json`'s
  build script now runs `pnpm --filter icdma-engine build && next build`.
- Changing an environment variable on Vercel does **not** rebuild. Push an
  empty commit or press Redeploy.
- Repeated scripted requests against `icdma.vercel.app` trigger Vercel's bot
  challenge ("Vercel Security Checkpoint", HTTP 403); a browser passes through.

## Open question

The public homepage card advertises the syllabus, but the syllabus itself is
behind the sign-in gate. A visitor who follows the card meets `/sign-in` before
seeing anything. If the syllabus should be readable by anyone and only the
modules and tutor gated, remove `'/learn'` from `isGated()` and the middleware
`matcher` in `web/middleware.ts`.
