# icdma

Modernization of the Virtual Coach / iCDMA interactive construction simulation
(PhD research 2000–2005, extended to 2013) into a public, browser-based teaching
platform for construction engineering.

**Status: Phase 0 — salvage and archive.** The original system is restored and
running locally; all twelve original scenarios are recovered and exported.

## Layout

- `legacy/` — the original Java source (2009–2011), restored to working order.
  Credentials scrubbed; a handful of marked `restoration patch` fixes; compiles
  under `--release 8` on a current JDK. `SmokeTest.java` boots the engine
  headlessly. See [docs/restoration-log.md](docs/restoration-log.md).
- `db/schema/vcdb-schema.sql` — schema of the recovered scenario database.
- `scenarios/` — the twelve original scenarios exported as JSON
  (`icdma-scenario-raw/0.1`: a faithful per-table export; the Phase 1 engine
  defines the real format). Includes the I-69 highway family used in the 2013
  validation papers.
- `tools/export_scenarios.sh` — regenerates `scenarios/` from the local database.
- `docs/what-icdma-does.md` — the one-page explainer / future About page.
- `.local/` (untracked) — local PostgreSQL data dir and logs.

The database itself is restored from the archived MTU cluster dump (kept outside
this repo) into PostgreSQL 14 on `localhost:5433`; instructions in the
restoration log.

## Roadmap

Phase 0 salvage → Phase 1 TypeScript engine port (tested against this legacy
oracle) → Phase 2 playable web simulation → Phase 3 CE 3332 course modules →
Phase 4 scenario authoring, library, and research data collection.
Stack target: Next.js / Vercel / Supabase.
