# iCDMA Engine Spec — Turn Loop (clean-room port reference)

Source: `legacy/src/mtu/construction/`. Every formula below is transcribed from code; line refs are `file:line`.
Two near-duplicate implementations exist: **TONAE** (interactive path) and **Updater** (query/Monte-Carlo path). They are *not* identical; §7 lists every divergence.

## 0. Key objects and invariants

| Concept | Java | Notes |
|---|---|---|
| Simulation clock | `TONAEState.t_now` | 1-indexed. `setTime(t)` also sets `global.earlyStart = t`. |
| Calendar | `TONAEState.currentDate` | starts at `project.getDate()`; advanced `+24h × interval` at the **end** of each turn. On turn *t*, date = startdate + (t−1) days. |
| Ready list | `TreeSet<PNode>` ordered by label | Active activities this turn. |
| Work remaining | `PNode.total_work_left` (dollars) | Initialized `= activity.getTotalMaterial()`. Work is measured in **material dollars**, not effort. |
| Cost book | `CostSchedule` (as-planned + as-built) | Per-activity, per-day, **cumulative** values. |
| Global mode flag | `static TONAE.querymode` | Read by `CostSchedule` and `Rule.trigger`. True for the whole of `queryFutures`. |

Constants: `OVER_TIME_RATE = 2.0`, `OVER_TIME_DAYS = 5`, `OVER_TIME_HOURS = 8`; `overhead`/`overstockLoss` static, loaded from project.

## 1. The turn sequence

### 1.1 `TONAE.update(...)` (`TONAE:1862-2032`) — exact order

```
 1. buildPurchaceList(resourcerequest)                  // §3.6
 2. environment.update(project, t, aNodeSet, purchasedmaterial)   // §2.1
 3. crewlist = shallow copy of crews[]                  // crews[] destroyed by buildRequestList
 4. checkDrivingMaterials(resourcerequest)              // §2.5
 5. checkLaborCompliment(resourcerequest, crews)        // §2.6
 6. updateMonthVar()
 7. updateDayVar()
 8. rulemap = {}; for rule in rules: acts = rule.apply(env, readyList); if acts != null: rulemap[rule] = acts
 9. environment.updateValues(purchasedmaterial)         // §2.3
10. tRules.clear()
11. checkLabor(resourcerequest, crews, unmapped, hired, rulemap)
12. buildRequestList(resourcerequest, soldmaterial, crews, rulemap)
13. checkMaterial(rulemap, resourcerequest)
14. for (rule, acts) in rulemap: rule.trigger(...)      // GUI events
15. manageResources(resourcerequest, querymode)         // §3
16. db.recordturn if (!querymode && dbrecord)
17-18. prints; usedMaterials.clear()
19. endActivities(state)
20. setTime(t+1)
21. startActivities(state)
22. stock.clearPerishable(); rejected.clear(); purchased.clear()
23. currentDate += 24h * interval
24. if (!querymode && dbrecord) { fsched = getLateSchedule(); db.endrecord }
25. queryFutures(Simulator.numFutures)
```

### 1.2 `Updater.update(...)` (`Updater:71-162`)

Same skeleton, steps 1–13, 15, 19–23, with **absent**: 10, 14 (rule triggering), 16–18 (DB/print), 24, 25. Step 13 is the reduced `checkMaterial(rulemap)`. Step 15 is `manageResources(resourcerequest, true)`.

### 1.3 Node lifecycle — identical in both; see network-and-costs.md §2.5.

### 1.4 `delayActivity(PNode, delay)` — TONAE `1119-1188`, Updater `881-947`.
TONAE clamps end to `t_now + 1` and starts the cascade `latest` at `t_now + 1`; **Updater uses `t_now` for both** (divergence D). Otherwise identical; see network-and-costs.md §2.5 for the cascade.

`isFinished() ⇔ t_now >= getLastTimeStep()` (`= max earlyStart over ANodes`).

## 2. Environment and rules

### 2.1 `Environment.update(project, t, anodes, matpurchase)` (`Environment:343-407`)
```
1. for each MaterialType t, each ContinV v with v.hasMaterial(t): v.setState(0, 1)
2. every variable v: v.update()      // timespan--; if timespan == 0: revert to defstate
3. for each activity a, each LOCAL DiscreteV v of a:
     "ID"           → v.setState(a.realId, 0)
     "ActivityTime" → start/end from network; v.setState(t < start || t > end ? "-1" : t - start, 0)
4. for each (m, qty) in matpurchase, each ContinV v with v.hasMaterial(m): v.setState(qty, 0)
```
`setState(value, time)`: `state = value; timespan = time`. DiscreteV with `time == 0` **also overwrites defstate** (permanent). Step 3 touches only *local* discrete vars — a global `ActivityTime` is never updated (why 523's rule 50 can never fire).

### 2.2 `Rule.apply(env, readyList)` (`Rule:159-224`)
```
if rule.global:
    if rng.nextFloat() >= prob: return null        // ** probability BEFORE preconditions **
    for c in precond: if !c.isMet(env) return null // GLOBAL scope lookup only
    for c in postcond: c.apply(env)                // GLOBAL scope lookup only — local target silently no-ops
    return {} (fired globally)
else:
    hit = {}
    for pnode in readyList (label order):
        if rng.nextFloat() >= prob: continue       // one draw per (rule, activity)
        if any precond not met in scope a: continue
        for c in postcond: c.apply(env, a)         // local scope, global fallback
        hit.add(a)
    return hit.isEmpty() ? null : hit
```
Java uses a fresh unseeded `Random` per draw; a port threads one PRNG, drawing in ready-list order. Fires when `nextFloat() < prob`.

`Condition` semantics: discrete comparisons collapse — `gte|lte|eq` ⇒ string equality, `gt|lt|neq` ⇒ inequality; continuous ones are numeric. Mutators: discrete always `setState(state, time)` regardless of set/add/mul; continuous `set → v`, `add → state+v`, `mul → state*v`. Postcondition `time` = lifetime in turns (1 ⇒ this turn only; 0 ⇒ permanent + rewrites discrete default).

### 2.3 `updateValues(matpurchase)`: for every ContinV v, material m in purchase with v.hasMaterial(m): `matpurchase[m] = (int) v.state` (truncation) — rules rewrite today's delivery.

### 2.4 Month/Day: global DiscreteV "Month" ← long US month name; "Day" ← String(DAY_OF_WEEK) (1=Sunday..7=Saturday), both time=1, both no-ops when absent, both before rule application.

### 2.5 `checkDrivingMaterials`: per activity, min over driving materials of `(available/base)*100` (**integer division first**), 100 if none; write `String(min/10)` into local "Driving Material Available" (time=1), skip if absent.

### 2.6 Labor variable steps: `checkLaborCompliment` writes per-type counts into local DiscreteVs (only `!querymode`); `checkLabor` reads local "Labor Available"/"Low Labor": Available=="False" ⇒ clear all the activity's crews; else Low Labor=="True" ⇒ remove one random laborer from the first matching crew.

## 3. `manageResources` — TONAE `2747-3088`; Updater `551-723`

### 3.1 Prologue
```
interval; dayofweek = currentDate.DAY_OF_WEEK; cost = 0.0; sched = asBuilt; day = t_now
[TONAE, !querymode] stock_track.add(stock.clone())
spaceneeded = Σ purchased qty × size ; allowed = stock.getAvailableSpace()
[TONAE, !querymode] if allowed < spaceneeded: issueSpaceViolation(...)   // §4, synchronous
for (m, qty) in purchasedmaterial:
    accepted = stock.add(m, qty)      // clamps: qty = (int)((total - curamt)/m.size) if overflow
    rejectedmaterial[m] += qty - accepted
[both, !querymode] carry yesterday's cumulative setMaterial/setLabor/setIndirect forward
```

### 3.2 Work rate (identical)
```
for act in readyList (label order):
   alloc = allocation for act (TONAE: skip if null; Updater: NPE)
   baseworkrate = alloc.computeWorkQuantityMultiplier(interval, dayofweek)
   workrate     = baseworkrate * ContinV(act, "Productivity").state
```
`computeWorkQuantityMultiplier`:
```
hourfactor = workhours*workdays/40.0 ; if > 1: hourfactor = 1 + (hourfactor-1)*0.5
wagefactor = 2 - 1.0/wageincentive
work = -1
for c in activity base crews, o in granted crews with c.id == o.id:
    perc = c.compareProductivity(o) ; if perc > 1: perc = 1 + (perc-1)*0.8
    perc *= hourfactor ; work = min(work, perc)  (first assignment replaces -1)
if interval==1 and ((SAT && workdays<=5) || (SUN && workdays<=6)): return 0   // WEEKEND GATE
return work == -1 ? 0 : work * wagefactor
```
`compareProductivity(o)` (base = this): granted crew needs ≥1 "Foreman" (10 workers per foreman cap via dead counter — effectively just presence); missing "Crane"/"Oiler" counts ⇒ 0; else min over non-supervisory types of `o.amt/base.amt`; no non-supervisory types ⇒ 0.

### 3.3 Material availability
```
for (m, dailyUse) in materialUse:
    available = stock.remove(m, alloc.getRequested(m)) ; avail[m] = available
    perc = available / dailyUse ; materialrate = min(materialrate, perc)
    [TONAE] materialrate2 += available*m.cost ; dailyCost += dailyUse*m.cost
if materialrate == -1: materialrate = 0
```
**Updater**: `rate = min(workrate, materialrate)`.
**TONAE**: `materialrate2 /= dailyCost`; `newRate = min(materialrate2, workrate)`; `materialcost = newRate * dailyCost`.

### 3.4 Consumption — Updater (uncapped)
```
getAmt = (int)(dailyUse * rate) ; amtused = min(getAmt, available)
available -= amtused ; total_work_left -= m.cost*amtused
addMaterial(day, amtused*m.cost) ; cost += amtused*m.cost
stock.add(m, available)          // remainder returned
```
No cap vs remaining need or ordered ⇒ can over-bill and drive `total_work_left` negative (this happens on the oracle's Cutting Members day 4).

### 3.5 Consumption — TONAE (two passes, four caps)
Pass 1: `newAmt = ceil(dailyUse × newRate)`, capped by remaining need (`MaterialInfo.total_need - total_used`), by on-hand, and by `getOrderedAmount(m) - total_used`; surplus above the ordered cap is **destroyed**, not returned. Pass 2 (if `materialcost > 1`): tops up with `newMax = (int)(materialcost / dailyUse)` (dollars ÷ quantity — intended unit cost; defect), `newAmt = min(need, available, newMax+1)`.
`getOrderedAmount(m) = (int)(materialUse[m] × duration × total_percent_ordered / 100)`. With `total_percent_ordered == 0` pass 1 consumes nothing — the interactive path depends on the GUI advancing it (§3.7).

### 3.6 Stock, purchasing, ordering
```
Stock.add clamps to space; Stock.remove clamps to on-hand; getValue = Σ qty*cost;
clearPerishable at end of turn.
Updater.buildPurchaseList: cs = stock.clone; per alloc (INCLUDING null-activity), per material:
    amt = cs.remove(m, requested) ; purchase(requested - amt)      // top up to full daily use
TONAE.buildPurchaceList: per alloc with activity: purchase ceil(dailyUse × alloc.order / 100)
    // order-percent driven; ResourceAllocation.order defaults 1.0 (= 1%!)
buildRequestList: sell stock (rejected = removed, OVERWRITES); bind crews first-come-
    first-served in vector order, nulling the source array (shared-crew loser gets 0).
checkMaterial: global "Material Available" == "False" ⇒ clear delivery;
    TONAE additionally rolls back pnode.percentOrdered by alloc.order/duration.
Rejected material and overstockLoss have NO cost effect anywhere in the engine.
```

### 3.7 What advances `total_percent_ordered`
Nothing in the engine — only the GUI (ResourcePanel order spinner; default adds `100/duration` percentage points per turn = one day's worth), decremented by checkMaterial and the space-violation dialog. This is the interactive path's ordering-lag pacing; absent in the query path.

### 3.8 Duration recomputation (identical)
```
unadj = total_work_left / dailyMaterialCost - 0.01
new_duration = ceil(unadj) ; current = end.earlyStart - p.earlyStart
delay = new_duration - current + 1 ; if delay != 0: delayActivity(p, delay)
```
Fully productive day ⇒ delay 0; zero-work day ⇒ delay +1.

### 3.9 Labor and indirect (identical)
```
for c in granted crews:
   interval 1: time = workHours; if (workDays>5 && SAT)||(workDays>6 && SUN): time*=2
               else if SAT||SUN: time=0 ; else if time>8: time=(time-8)*2+8 ; time/=8
   interval 7: time = h*d; if >40: (t-40)*2+40; /=40      interval 28: ×4, 160 basis
   addLabor(day, time × c.getDailyCost() × wageIncentive) ; cost += same
addIndirect(day, dailyMaterialCost × overhead) ; cost += same
```
**Indirect accrues for every ready activity every turn at the full as-planned rate — including weekends and starved days.** Dominant overrun source.

### 3.10 Epilogue
```
incrementPresentNodes() ; setStockValue(day, stock.getValue())
cost += track[day-1] ?? 0 ; track[t_now] = cost            // cumulative per-turn track
[TONAE, !querymode] computeCost(sched, t_now+1, lastTimeStep)   // re-forecast tail
[TONAE, !querymode] asbuiltprogress[act][t_now+1] = now>end ? 1 : now<start ? 0
                                                   : 1 - (end-now)/act.duration
```
In query mode the per-activity books are untouched (recording off); the cumulative track is the run's only output.

## 4. Space violation

Interactive only (`!querymode`), before delivery: if `availableSpace < spaceneeded`, call listeners synchronously. Contract: mutate `purchasedmaterial` in place to fit; roll back each cut activity's order (`alloc.setOrder(new)`; `pnode.setOrdered(pnode.getPercentOrdered() - (old-new)/duration)`); block until done. **No penalty math exists** — overstockPenalty is label text only; what still doesn't fit is clamped by `Stock.add` into display-only `rejectedmaterial`. The query path has no check at all — the silent clamp is the throttle that drives the oracle's slip.

## 5. `queryFutures`

`TONAE.queryFutures(num)`: set global querymode; split across 7 threads (+ top-up loop, ≤100 tries); each thread runs an `Updater` over a deep clone of `TONAEState`; collect `QueryResult2`; unset querymode. A port should run N sequential futures.

`Updater.queryFuture()`:
```
clone state ; while (!isFinished() && time < 1000):
    update(getDefaultResourceAllocation(),   // rebuilt EVERY turn from live ready list
           {}, getDefaultLaborCrews(),       // fresh full-complement crew clones
           Unmapped, Hired)
return asBuilt.queryFuturesTotal             // TreeMap<day, cumulative cost>
```
Default allocations: one null-activity "stock" allocation + per ready activity: request 100% of daily material use, workDays 5, workHours 8, wageIncentive 1 (order unused by Updater).
`QueryResult2`: full curves, best/worst by final cost, day distribution over `lastKey` (last turn actually simulated; end node sits at lastKey+1), cost histograms.

## 6. Why 14 planned days become 18 — verified numerically

Project 523: starts Mon 2011-07-11, interval 1, space 2500, overhead 0.15; durations 2/3/4/3/1 chained hard FS.

**(a) Weekend gate**: rate 0 on Sat/Sun (default workDays 5) ⇒ no consumption ⇒ delay +1 per weekend day; indirect still charged; labor time = 0.
**(b) Site-space cap (query path)**: Cutting Members needs 6000 × 0.5 area = 3000 > 2500 space ⇒ delivery clamps to 5000 units ⇒ materialrate 5/6 ⇒ a 4th working day; on it the uncapped Updater consumption takes 5000 units (billing $15,000 for $9,000 of need; total_work_left → −6000).

Schedule: A1 turns 1–2; A2 turns 3–8 (works 3,4,5,8; weekend 6,7); A3 9–12; A4 13–17 (weekend 13,14); A5 18. Last simulated turn **18**.
```
material = 10000 + 4×15000 + 48000 + 27000 + 15000                 = 160000.00
labor    = 2×1200.80 + 4×2539.12 + 4×2768.80 + 3×2539.12 + 2539.12 =  33789.76
indirect = 0.15 × (2×5000 + 6×18000 + 4×12000 + 5×9000 + 15000)    =  33900.00
total    = 227689.76  ✓          as-planned = 208350.64  ✓
```
All 20 futures identical because neither rule can fire: rule 46 (global) mutates local `Productivity` through a global lookup ⇒ silent no-op (only Weather flips, unread); rule 50 preconditions on `ActivityTime eq "2"` but the global ActivityTime is never updated.

Interactive run 1745 (19 turns, $227,828.88): same weekend gate, different throttle — TONAE's need cap holds material to exactly $154,000, but ordering-lag pacing (§3.7) kept A2 on the ready list 7 turns. Verified: indirect $37,500 ✓, labor $36,328.88 ✓.

## 7. TONAE vs Updater — every genuine divergence

| # | Area | TONAE | Updater |
|---|---|---|---|
| A | buildPurchaseList | ceil(dailyUse × order/100), ignores stock, skips null alloc | requested − stock, ignores order, includes null alloc |
| B | Consumption | ceil + caps (need, on-hand, ordered); destroys surplus; 2nd pass | trunc, on-hand cap only; remainder returned |
| C | Rate | min(cost-weighted availability, workrate) | min(min-ratio availability, workrate) |
| D | delayActivity floor | t_now + 1 | t_now |
| E | Space violation | listener callback | none — silent clamp |
| F | checkMaterial | clears delivery + rolls back percentOrdered | clears delivery only |
| G | checkLabor | guards emptied crews; per-activity trigger; listener | no guard (nextInt(0) throws); no listener |
| H | checkLaborCompliment | writes variables when !querymode | dead code |
| I/L | Updater ctor arg swap | — | QueryThread passes (asPlanned, asBuilt) against signature (asBuilt, asPlanned) ⇒ Updater.getProject() = **as-planned** |
| J | Bookkeeping | carry-forward, stock track, tail re-forecast, progress curve | none (always querymode) |
| K | null alloc | guarded | NPE |
| M | End of turn | nested queryFutures every turn | none |
| N | TONAE.computeWorkQuantityMultiplier (member copy) | dead code — don't port | n/a |

All activity-keyed structures unify by **activity id** (as-planned vs as-built object split).

## 8. Load-bearing "bugs" checklist

1. Weekend gate returns 0 before wagefactor, regardless of crews.
2. Indirect accrues on every ready activity every turn, including zero-work turns.
3. `−0.01` then `ceil` in duration recomputation.
4. `delay = new − current + 1`.
5. `total_work_left` is material dollars; labor never reduces it.
6. Integer division in checkDrivingMaterials.
7. Global rules can't mutate local variables (silent no-op); local rules fall back to global.
8. Probability sampled before preconditions; one draw per (rule[, activity]) in ready-list order.
9. Discrete comparators collapse to equality/inequality; mul/add act as set for discrete.
10. Postcondition time 0 = permanent (+ rewrites discrete default); n = decays after n turns.
11. buildRequestList: first-come-first-served crew binding, nulls source array.
12. soldmaterial overwrites rejectedmaterial.
13. compareProductivity requires a Foreman in the granted crew (dead counter).
14. Congestion 0.8 before hourfactor; overtime 0.5 on hourfactor only.
15. wagefactor = 2 − 1/incentive — guard incentive == 0.
16. Over-ordering/rejected/returned material and overstockLoss: no cost effect.
17. getMaterialTotal adds stock value (reporting only).
18. Model querymode as an explicit context flag, not a global.
