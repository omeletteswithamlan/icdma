# Run 1745 — first clean interactive run of the restored iCDMA

2026-08-26, project 523 "RoadConstruction" (5-activity bridge), played
interactively in the restored Swing GUI by Amlan — the first complete session
since ~2013.

## Outcome

| | As-planned | This run | Headless oracle (queryFutures, 20 samples) |
|---|---|---|---|
| Duration | 14 turns | 19 turns (Load Test ending t=20) | 18 turns |
| Total cost | $208,350.64 | **$227,828.88** | $227,689.76 |

Costs at final recorded turn: material $154,000.00 + labor $36,328.88 +
indirect $37,500.00. No stochastic events fired during the run.

Schedule slip is visible in the record: Cutting Members' forecast end drifted
7→10 as material ordering paced the work, and Assemble Trusses drifted 14→16 —
the live-network behavior (float consumption, cascading end dates) working as
designed. The interactive run landing within $140 of the Monte-Carlo oracle's
default-play cost is a strong consistency signal between the TONAE and Updater
code paths — exactly what Phase 1's port will lean on.

## Files

- `record.json` — full database record: per-turn costs/space, active-activity
  windows per turn, events (none).
- `gui-log.txt` — application console log for the session (1,442 lines, zero
  exceptions).
