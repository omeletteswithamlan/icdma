# Restoration log

What it took to bring the 2009–2011 iCDMA system back to life on a 2026 machine,
and every deviation from the original source. Date: 2026-08-26.

## Environment

- Source: `Code Base/iCDMA` from the research archive (Eclipse project `NewCDMA`,
  CVS-era, Java 6 target, last touched Jan 2010).
- Database: full cluster dump of the MTU server (`construction-*.sql`, May 2024
  snapshot — all snapshots byte-identical since June 2023), restored into
  PostgreSQL 14 (Homebrew) on `localhost:5433`, data dir `.local/pgdata`.
  The restore produced exactly one error (pre-existing `postgres` role) — nothing lost.
- Compiler/runtime: OpenJDK 26 (Homebrew), compiled with `--release 8`.
  The 2009 source compiles clean apart from deprecation warnings.

## Deviations from the original source (all marked `restoration patch` in code)

1. **JDBC driver swapped.** The vendored `pgjdbc1.jar` (2007) speaks frontend
   protocol 2.0, which PostgreSQL 14 removed. Replaced on the classpath with
   `postgresql-42.7.8.jar` (Maven Central). No code change — same driver class name.
2. **Connection defaults repointed.** `Simulator.java` and `ConfigPanel.java`
   pointed at the retired MTU server with hardcoded credentials; now
   `localhost:5433 / vcdb / postgres`, empty password (local trust auth).
   The original credentials are scrubbed from this tree.
3. **`"Availible"` misspelling fixed in variable lookups** (`TONAE.checkMaterial`,
   `TONAE.checkLabor`, and their `Updater` twins). The database variables are
   spelled correctly (`Material Available`, `Labor Available`); the code's
   misspelled lookups returned null and crashed the turn loop. Evidently the data
   was fixed after the code freeze.
4. **Null guards for scenario-optional variables.** Activities without driving
   materials never get a `Driving Material Available` variable, and most
   scenarios define no per-activity labor-event variables; the code assumed both
   always exist. Guards skip the check when absent (semantics: nothing to check /
   labor available). Applied symmetrically in `TONAE` and `Updater`.

## Original vs. living copy

`legacy/original-2013/` holds the source exactly as developed (password masked
only) and is frozen. All edits go in `legacy/src`. After any edit, regenerate
the delta: `diff -ruN legacy/original-2013/src legacy/src > docs/modernization.patch`.

## Verification (SmokeTest.java, headless — no GUI needed)

Project 523 "RoadConstruction" (the 5-activity bridge example):

- Boot: connects, builds project from `vcdb`, constructs the TONAE, records a
  new history id — continuing the original research run sequence.
- As-planned network is correct: 5 activities, ES/EF chain 1→14, activity totals
  summing to **$208,350.64**, which the cost model reproduces exactly.
- `queryFutures(20)`: all 20 Monte-Carlo futures run the project to completion —
  day 18, final cost $227,689.76. The canonical engine loop (via `Updater`)
  performs work, purchases materials, and finishes activities.
- 60 hand-driven turns via `Simulator.update()` run exception-free with history
  recording, but perform no work: the Swing panels set per-node material-ordering
  state that the headless driver does not replicate. Understanding that
  choreography precisely is a Phase 1 task (read `TONAE.manageResources`).

## Known data notes

- The `activity` table contains orphaned activities for ~15 project ids that are
  no longer in `project` — deleted scenarios from the research years. Exports
  only cover the 12 live projects.
- All 20 identical futures on project 523 suggests its stochastic rules (10%
  bad-weather etc.) either did not fire or did not affect the critical path under
  default play — worth revisiting when the port's Monte-Carlo is compared.
- `vcdb2`, `vcdb3`, `vcdbbak`, `mdot`, `mdot2` restored alongside `vcdb` and not
  yet examined.

## Rebuild from scratch

```
brew install postgresql@14 openjdk
cd <repo>
/opt/homebrew/opt/postgresql@14/bin/initdb -D .local/pgdata -U postgres --auth=trust --encoding=UTF8 --locale=C
/opt/homebrew/opt/postgresql@14/bin/pg_ctl -D .local/pgdata -o "-p 5433 -c unix_socket_directories=/tmp" -l .local/pg.log start
/opt/homebrew/opt/postgresql@14/bin/psql -h /tmp -p 5433 -U postgres -f <path-to>/construction-daily_0-Sun.sql postgres
cd legacy
find src -name "*.java" > .sources.txt
/opt/homebrew/opt/openjdk/bin/javac --release 8 -nowarn -cp "ptolemy.jar:postgresql-42.7.8.jar" -d bin @.sources.txt
/opt/homebrew/opt/openjdk/bin/javac --release 8 -nowarn -cp "bin:ptolemy.jar:postgresql-42.7.8.jar" -d bin SmokeTest.java
/opt/homebrew/opt/openjdk/bin/java -cp "bin:ptolemy.jar:postgresql-42.7.8.jar:src" SmokeTest 523 60
```

The Swing GUI boots with `java -cp "bin:ptolemy.jar:postgresql-42.7.8.jar:src" mtu.construction.gui.old.MainWindow`
(defaults now point at the local database, project 523).
