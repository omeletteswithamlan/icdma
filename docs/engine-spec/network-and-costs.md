# iCDMA Engine Spec — Project Construction, TONAE Network, Scheduling, Cost Model

Clean-room port target: TypeScript. Source of truth: `legacy/src/mtu/construction/`.
All money is IEEE-754 `double` (= JS `number`). All times/days/quantities are 32-bit
signed ints unless noted. Divisions marked **(int)** truncate toward zero; everything
else is float division.

Verified oracle: project 523 → per-activity totals 13901.60 / 69717.36 / 66275.20 /
38667.36 / 19789.12, as-planned cumulative total TreeMap `lastKey = 13`,
`get(13) = 208350.64`.

---

## 0. Glossary / units

| Symbol | Meaning | Unit |
|---|---|---|
| `duration` | as-planned activity duration | integer time steps ("days") |
| `t_now` | current time step, **1-indexed** | day |
| `MAXIMUM_DURATION` | `10000` | days |
| `total_work` / `total_work_left` | PNode work counters | **dollars of material** |
| `total_percent_ordered` | PNode order fraction | **percent, 0..100** |
| `overhead` | `project.overhead` | fraction (0.15 = 15%) |
| `overstockPenalty` | `project.overstock_penalty` | fraction |

Time step size (`TimeFrame.interval` ∈ {1, 7, 28}) does **not** enter any as-planned
cost formula. It affects only (a) overtime normalization in the as-built path and
(b) calendar advance (`currentDate += 24*interval hours` per step).

---

## 1. Project construction from data

`VCDBInterpreter.buildProject()` — strict order (later steps depend on earlier ones):

```
1.  skills      = SELECT * FROM skill                     -- no ORDER BY
    skill       = skills[0]                               -- media lookups keyed on this
2.  project     = getProject()
3.  laborTypes  = getLaborTypes()
4.  materialTypes = getMaterialTypes()
5.  csiDivisions  = getCSIDivision()
6.  responsibilities = getResponsibility()
7.  activities  = getActivities(csiDivisions, responsibilities)
8.  variables   = getVariables(projectid, materialTypes)
9.  rules       = getRules(variables)
10. laborCrews  = getLaborCrews(laborTypes)
11. constraints = getConstraints(activities)      -- side effect: wires links/backlinks
                                                  --   and propagates start times
12. for a in activities: addResourceUsage(a)      -- materialuse + laborcrewuse
13. i = 1; for a in activities: a.id = i++        -- RENUMBER 1..n; a.realid keeps DB pk
```

### 1.1 Project

```sql
SELECT * FROM project p WHERE p.projectid = :pid
```
```
space              = getInt("space")                       -- int
overstockPenalty   = (double) getFloat("overstock_penalty")-- NOTE: float32 narrowing
overhead           = (double) getFloat("overhead")         -- NOTE: float32 narrowing
startDate          = getDate("startdate")
interval           = getInt("interval")
timeFrame = interval==1 ? ONE_DAY : interval==7 ? ONE_WEEK
          : interval==28 ? FOUR_WEEK : throw PlanException
date = GregorianCalendar(date.getYear()+1900, date.getMonth(), date.getDate())
```

> **Float-narrowing fidelity note.** Java reads `overhead`/`overstock_penalty` with
> `ResultSet.getFloat` then widens to double, so `0.15` becomes
> `0.15000000596046448`. With that value the project-523 total is
> `208350.64091791154`; with a true `double` `0.15` it is **exactly `208350.64`**,
> which matches the oracle. **Port using `double` (plain JS number).** Optionally
> expose a `legacyFloatOverhead` compat flag that applies `Math.fround`.

### 1.2 LaborType

```sql
SELECT DISTINCT l.laborid, l.description, l.unitcost
FROM activity a, project p, labor l, laboruse lu
WHERE p.projectid=a.projectid AND a.activityid=lu.activityid
  AND lu.laborid=l.laborid AND p.projectid=:pid
ORDER BY l.laborid
```
`LaborType{ id:int, description:string, cost:double }`.

> **Trap:** the `laboruse` table is used **only** to enumerate which labor types belong
> to the project. Its `amount` columns are **never** used in any cost formula.
> All labor cost flows through `laborcrewuse → laborcrewentry → labor.unitcost`.
> A `laborcrewentry.laborid` not present in this set resolves to `null` and, in Java,
> NPEs later; in TS, treat as a data-integrity error.

### 1.3 MaterialType

```sql
SELECT DISTINCT m.* FROM activity a, project p, material m, materialuse mu
WHERE p.projectid=a.projectid AND a.activityid=mu.activityid
  AND mu.materialid=m.materialid AND p.projectid=:pid
ORDER BY m.materialid
```
`MaterialType{ id:int, description, cost:double /*unitcost*/, size:double /*area*/,
perishable:boolean }`.

### 1.4 Activity

```sql
SELECT * FROM activity a, project p, csidivision c, responsibility r
WHERE a.projectid=p.projectid AND a.csidivisionid=c.csiid
  AND a.responsibilityid=r.responsibilityid AND p.projectid=:pid
ORDER BY a.activityid
```
Constructed as:
```
Activity{
  id = realid = activityid            -- id later overwritten to 1..n (step 13)
  description, code
  duration:int
  div = findCSIDiv(csidivisionid)     -- throws PlanException if absent
  respons = findResponsibility(responsibilityid)  -- throws if absent
  drivingMaterials = SELECT materialid FROM driving_material WHERE activityid=:aid
                     (Set<int>; on SQLException the field becomes null)
  backlinks = {}   links = {}
  materialuse = {} materialinfo = {} laboruse = {}
  start = 1                           -- initial as-planned start
}
```
`Activity.equals`/`hashCode` are **id-based only** (`hashCode() === id`). Any Map/Set
keyed by Activity must key by `id`.
`Activity.compareTo` = `this.id - other.id` (drives `TreeMap<Activity,…>` order in
`CostSchedule`).

### 1.5 Constraints — length + soft flag

```sql
SELECT DISTINCT c.constraintid, c.fromactivityid AS "from", c.toactivityid AS "to",
       c.length AS duration, c.soft AS soft
FROM constraints c, activity a, project p
WHERE p.projectid=a.projectid
  AND (a.activityid=c.fromactivityid OR a.activityid=c.toactivityid)
  AND p.projectid=:pid
-- no ORDER BY
```
```
Constraint{ id, from:Activity, to:Activity, length:int, soft:boolean }
isHardConstraint() === !soft
getDuration()      === length          -- lag, in days
```

`Constraint` construction calls `setActivities(from,to)`, whose side effects are:
```
from.links.add(this)
to.backlinks.add(this)
from.setStart(from.start)              -- kicks a monotone forward relaxation
```

`Activity.setStart(s)` (recursive, monotone — order-independent fixed point):
```
this.start = s
for c in this.links:
    if c.to.start < s + this.duration + c.length:
        c.to.setStart(s + this.duration + c.length)
```
Because it only ever *increases* successor starts, the resulting as-planned start
vector is independent of constraint insertion order. Iterate to fixed point.

`Activity.getEnd() = start + duration` (exclusive end / first free day).

For project 523: durations 2,3,4,3,1, all chained with `length = 0` ⇒
starts 1,3,6,10,13; ends 3,6,10,13,14.

### 1.6 Material use

```sql
SELECT m.materialid, m.quantity FROM materialuse m WHERE m.activityid = :aid
```
```
Activity.setMaterialUse(amt, mat):
    materialuse[mat]  = amt                                  -- per-DAY quantity
    materialinfo[mat] = MaterialInfo{ material: mat,
                                      total_need: amt * this.duration,  -- int mul
                                      total_used: 0 }
```
Note `materialinfo` is captured at load time using the **then-current** `duration`;
later `setDuration` does not refresh it.

### 1.7 Labor use (crews)

```sql
-- crews used anywhere in the project
SELECT DISTINCT l.laborcrewid, l.description
FROM laborcrew l, laborcrewuse u, activity a, project p
WHERE p.projectid=:pid AND a.projectid=p.projectid
  AND a.activityid=u.activityid AND u.laborcrewid=l.laborcrewid
-- no ORDER BY

-- crew composition
SELECT laborid, amount FROM laborcrewentry WHERE laborcrewid = :cid

-- crews attached to an activity
SELECT laborcrewid FROM laborcrewuse WHERE activityid = :aid
```
```
LaborCrew{ id, name, laboramt: TreeMap<LaborType,int> }   -- sorted ASC by laborid
LaborCrew.getDailyCost() = Σ over laboramt entries, IN ASCENDING laborid ORDER:
                              laborType.cost * amount
```
Summation order matters for exact float reproduction — iterate labor types in
ascending `laborid`.

`Activity.laboruse` is a `Set<LaborCrew>` (crew membership, no multiplicity).
`LaborCrew.equals` is id-based; `compareTo` is `other.id - this.id` (**descending**).

### 1.8 Variables

```sql
SELECT DISTINCT v.* FROM projectrule projr, rule r, ruleprecondition prer,
                          precondition pre, variable v
WHERE projr.ruleid=r.ruleid AND prer.ruleid=r.ruleid
  AND prer.preconditionid=pre.preconditionid AND pre.variableid=v.variableid
  AND projr.projectid=:pid
UNION
SELECT DISTINCT v.* FROM projectrule projr, rule r, rulepostcondition postr,
                          postcondition post, variable v
WHERE projr.ruleid=r.ruleid AND postr.ruleid=r.ruleid
  AND postr.postconditionid=post.postconditionid AND post.variableid=v.variableid
  AND projr.projectid=:pid
```
Per row:
```
discreet ? DiscreteV(variableid, label, global, initialstate /*string*/)
         : ContinV  (variableid, label, global, Number(initialstate))
then: variable.matassoc = { MaterialType : materialid IN
        (SELECT materialid FROM materialvariable
         WHERE projectid=:pid AND variableid=:vid) }
```

**Synthetic fallbacks**, appended after the scan, only if the label was absent:
| condition | appended variable |
|---|---|
| no `"ID"` | `DiscreteV(id=-1, "ID", global=false, state="0")` |
| no `"ActivityTime"` | `DiscreteV(id=-2, "ActivityTime", global=false, state="0")` |
| no `"Productivity"` | `ContinV(id=-3, "Productivity", global=false, state=1.0)` |

`"Labor Available"`, `"Low Labor"`, `"Driving Material Available"`, `"Material
Available"`, `"Month"`, `"Day"`, `"Weather"` are **never synthesized** — they exist
only if the project's rules reference them. Consumers must null-check.

Project 523 concretely yields:
| label | global | discrete | initial |
|---|---|---|---|
| Weather | true | true | `"Sunny"` |
| ActivityTime | **true** | true | `"0"` |
| Low Labor | false | true | `"false"` |
| Productivity | false | false | `1.0` |
| ID *(synthesized)* | false | true | `"0"` |

### 1.9 Environment instantiation (per-activity variable materialization)

`TONAEState` ctor builds `Environment(activities)` with one empty discrete set and one
empty continuous set per Activity, plus two global sets.

`TONAE.initVariables()` runs **after** `constructActivities` + `initPresentNode` +
`new AgentM(this)`:
```
for v in project.variables:
    if v.global:  globalSet.add(v.clone())
    else:         for a in activities: perActivitySet[a].add(v.clone())   -- N clones
rules = Vector(project.rules)            -- projectrule.ordering ASC
```
So each **non-global** variable is instantiated once per activity, each with its own
`state` and `defstate` initialised from `initialstate`.

`Variable` runtime semantics:
```
setState(v, time):  state = v; timespan = time
                    (DiscreteV only: if time == 0 then defstate = v)
update():           timespan--; if timespan == 0 then state = defstate
```
`Environment.getVariable(a, name)` searches the activity's discrete set, then its
continuous set, then falls back to globals; returns `null` if not found.
`getContinuousVariable(a, name)` searches activity-continuous then global-continuous
and **throws** if absent.

`Environment.update(project, currentTime, aNodeSet, matPurchase)`:
```
1. every ContinV associated with any project MaterialType  -> setState(0, 1)
2. every variable -> update()                              -- timespan decay/revert
3. for each (activity a, localDiscreteSet):
     if v.label == "ID":            v.setState(String(a.realid), 0)   -- DB pk, not 1..n
     if v.label == "ActivityTime":
        for node in aNodeSet with node.outPrimaryArc != null and node.parent.id == a.id:
            start = node.earlyStart
            end   = node.out.head.out.head.earlyStart
            v.setState( (currentTime < start || currentTime > end) ? "-1"
                                                    : String(currentTime - start), 0)
4. if matPurchase != null:
     for each ContinV v, for each (mat,qty) in matPurchase:
        if v.hasMaterial(mat): v.setState(qty, 0)
```
> **Trap:** step 3 iterates only the **local** discrete sets. If `ActivityTime` is
> declared `global` (as in project 523), it is never updated per activity.

`Environment.updateValues(matPurchase)`: for each ContinV `v` associated with a
material in the purchase map, `matPurchase[mat] = (int) v.state` **(truncation)**.

### 1.10 Rules

```sql
SELECT r.* FROM projectrule projr, rule r
WHERE r.ruleid=projr.ruleid AND projr.projectid=:pid
ORDER BY projr.ordering
```
```
Rule{ description, message, preconditions[], postconditions[], probability:double,
      global:boolean, ruleid }

preconditions  -> Condition(variable.label, state:string, action:string)
postconditions -> variable is ContinV ? Condition(label, Number(state), time, action)
                                      : Condition(label, state, time, action)
```
Postcondition `time` is the `timespan` handed to `setState` (1 ⇒ one-step effect that
reverts on the next `Variable.update()`).

### 1.11 Project copies

```
asPlanned = p                      -- the interpreter's object
asBuilt   = deepCopy(p)            -- java serialization round-trip
baseline  = deepCopy(p)
getProject()  === asBuilt
```
`Project.fixSerialize()` exists but is **never called**; each copy is internally
self-consistent, and cross-copy identity is resolved solely by `Activity.id`.

> The TONAE A-Node graph's `parentActivity` pointers reference **asPlanned**
> activities, while `CostSchedule`/`AgentM` maps are keyed by **asBuilt** activities.
> This works only because `Activity.equals`/`hashCode` are id-based, and because
> `HashMap.put` on an equal key keeps the *original* key object. A TS port keyed by
> numeric `activityId` reproduces this exactly.

---

## 2. TONAE network construction

### 2.1 Node model

```
Node {
  outPrimaryArc: Arc|null      // null ⇒ this node ENDS an activity
  inPrimaryArc:  Arc|null      // null ⇒ this node BEGINS an activity
  outConstraintArcs: Set<Arc>
  inConstraintArcs:  Set<Arc>
  earlyStart: int   = -1
  timeOfResolution: int = -1   // -1 = unresolved
  parentActivity: Activity|null
  label: string
}
ANode extends Node                       // "activity" node (start or end)
PNode extends Node {                     // "present" node — the moving now-marker
  isActive: boolean = false
  isGlobal: boolean = false
  events: Set<Arc>
  total_percent_ordered: double = 0      // PERCENT 0..100
  total_work:      double                // $ of material
  total_work_left: double                // $ of material
}
Arc {
  tailNode, headNode
  lower: int, upper: int                 // lower <= head.earlyStart - tail.earlyStart <= upper
  threshold: int
  label: string
  penIndex: int, penBase: int, penRate: double
}
```

### 2.2 `constructActivities(plan)` — exact

```
aNodeSet = ∅
for x in 0..activities.length-1:            // array order = activityid ASC
    a = activities[x]
    start = new ANode();  end = new ANode()
    start.label      = a.getLabel() + ": Start"
    start.earlyStart = a.start
    start.parent     = a
    end.label        = a.getLabel() + ": End"
    end.earlyStart   = a.start + a.duration
    end.parent       = a

    arc = new Arc()
    arc.tail = start;  arc.head = end
    arc.label = a.getLabel() + ": Arc"
    arc.lower = a.duration
    arc.upper = a.duration + MAXIMUM_DURATION      // 10000
    arc.penaltyRate = 10.0
    arc.penaltyBase = 0

    start.outPrimaryArc = arc
    end.inPrimaryArc    = arc
    aNodeSet.add(start); aNodeSet.add(end)

for x in 0..constraints.length-1:
    c = constraints[x]
    // pick the LATEST A-Node of `from` and the EARLIEST A-Node of `to`
    start = argmax_{n in aNodeSet, n.parent === c.from} n.earlyStart   // strict <, first wins on tie
    end   = argmin_{n in aNodeSet, n.parent === c.to  } n.earlyStart   // strict >, first wins on tie
    if start == null or end == null: FATAL

    carc = new Arc()
    carc.tail = start; carc.head = end
    carc.label = end.label + ": Constraint"
    carc.lower = c.length
    carc.upper = c.length + MAXIMUM_DURATION
    carc.threshold = c.isHardConstraint()
                     ? c.length + MAXIMUM_DURATION       // == upper  ⇒ HARD
                     : MAXIMUM_DURATION                  // != upper  ⇒ SOFT
    carc.penaltyRate = 20.0
    carc.penaltyBase = 1
    start.addOutConstraint(carc)
    end.addInConstraint(carc)
```

**Soft-flag → threshold semantics.** Softness is *encoded* in the arc and *decoded*
at the use site (`delayActivity`) as:
```
soft := (arc.upper != arc.threshold)
```
Hard: `upper = threshold = length + 10000`. Soft: `upper = length + 10000`,
`threshold = 10000`.
> **Known defect to preserve or flag:** when `length == 0`, a *soft* constraint yields
> `upper = threshold = 10000` and is therefore decoded as **hard**. Project 523's
> constraints all have `length = 0, soft = false`, so behaviour is unaffected there.

### 2.3 `initPresentNode(startANode)` — splices a PNode into every primary arc

Run once, in the TONAE constructor, for every A-Node with `outPrimaryArc != null`.

```
pNode = new PNode()
pNode.setParentAct(a)                 // see 2.4 — initialises total_work*
currentArc = start.outPrimaryArc      // the arc built in 2.2
newBefore = new Arc(); newAfter = new Arc()
newBefore.penaltyRate = newAfter.penaltyRate = currentArc.penaltyRate
newBefore.penaltyBase = newAfter.penaltyBase = currentArc.penaltyBase

newBefore.tail = start;  newBefore.head = pNode
newAfter.tail  = pNode;  newAfter.head  = currentArc.head   // the end A-Node

start.outPrimaryArc            = newBefore
pNode.inPrimaryArc             = newBefore
pNode.outPrimaryArc            = newAfter
currentArc.head.inPrimaryArc   = newAfter

newBefore.lower = 0;  newBefore.upper = 0                    // the "past"
newAfter.lower  = currentArc.lower                           // = duration
newAfter.upper  = currentArc.upper                           // = duration + 10000

pNode.earlyStart = start.earlyStart
pNode.label      = "Y-" + start.label
```
Post-condition topology per activity: `startANode --newBefore--> PNode --newAfter--> endANode`.
PNodes are **not** stored in any collection; they are reachable only via arcs (and via
the ready list once active). `pNode.timeOfResolution` stays `-1` until `startActivities`.

Canonical traversals used everywhere:
```
startANode(a)                = the ANode with outPrimaryArc != null and parent.id == a.id
pnode(a)                     = startANode.outPrimaryArc.headNode
endANode(a)                  = startANode.outPrimaryArc.headNode.outPrimaryArc.headNode
PNode.getStart()             = inPrimaryArc.tailNode.earlyStart
PNode.getEnd()               = outPrimaryArc.headNode.earlyStart
PNode.getDuration()          = getEnd() - getStart()
PNode.isFirstDay()           = earlyStart == getStart()
PNode.isLastDay()            = earlyStart == getEnd()
```

There is additionally one **global PNode** (`TONAEState.global`), created with
`isGlobal = true`, `timeOfResolution = 0`, `earlyStart = 0`, label `"Global"`, no
parent activity and no primary arcs. `setTime(t)` sets `global.earlyStart = t`.

### 2.4 `total_work`, `total_work_left`, `total_percent_ordered`

```
PNode.setParentAct(a):
    super.setParentAct(a)
    if a != null:
        total_work_left = a.getTotalMaterial()     // = computeDailyMaterialCost() * duration
        total_work      = total_work_left
```
**Units: dollars of material** (not days, not quantity). Consumed as material is used:
```
total_work_left -= materialType.cost * usedQuantity       // per material, per day
```
Derived quantities:
```
PNode.getPercentCompletion() : int
    = 100 - (int)(total_work_left / total_work * 100)     // (int) truncates toward zero

PNode.setOrdered(d)          : total_percent_ordered = d  // percent, monotonicity NOT enforced
PNode.getPercentOrdered()    : total_percent_ordered

PNode.getOrderedAmount(t)    : int
    amount  = parentAct.materialUse[t]        // int; throws if t not used
    amount *= parentAct.duration              // int mul
    return (int)(amount * total_percent_ordered / 100)   // int→double→truncate
```
Remaining-duration inference (as-built path):
```
base_work        = parentAct.computeDailyMaterialCost()
unadjusted       = total_work_left / base_work - 0.01      // -1% quantization nudge
new_duration     = ceil(unadjusted)                        // int
current_duration = endANode.earlyStart - pnode.earlyStart
delay            = new_duration - current_duration + 1
if delay != 0: delayActivity(pnode, delay)
```

### 2.5 PNode advancement

`startActivities(state)` — called once in the constructor (with `t_now = 1`) and once
per `update()` immediately after `setTime(t+1)`:
```
for node in aNodeSet:                               // HashSet order; result order-independent
    if node.earlyStart == state.t_now
       and node.timeOfResolution == -1
       and node.outPrimaryArc != null:              // starting A-Nodes only
        node.timeOfResolution = state.t_now
        p = node.outPrimaryArc.headNode             // the PNode
        p.earlyStart       = state.t_now
        p.timeOfResolution = state.t_now
        p.isActive         = true
        state.readyList.add(p)                      // TreeSet ordered by PNode.label ASC
```
`readyList` is a `TreeSet<PNode>` with comparator `p1.label.compareTo(p2.label)`
(lexicographic on `"Y-<activity label>: Start"`). **Two PNodes with equal labels
collide and one is silently dropped** — activity labels must be unique.

`endActivities(state)` — called at the *top* of the end-of-turn block, **before**
the time increment:
```
victims = ∅
for p in state.readyList:
    if p.outPrimaryArc.headNode.earlyStart <= t_now + 1:     // "+1 because we remove before update"
        p.timeOfResolution = t_now
        victims.add(p)
        p.isActive = false
for p in victims: state.readyList.remove(p)
```

`incrementPresentNodes(state)` — called from `manageResources()`, i.e. *before*
`endActivities` and *before* the time increment:
```
for p in state.readyList: incrementPresentNode(p)
incrementPresentNode(state.global)

incrementPresentNode(p):
    p.earlyStart += 1
    if p.inPrimaryArc  != null: p.inPrimaryArc.lower  += 1; p.inPrimaryArc.upper  += 1
    if p.outPrimaryArc != null: p.outPrimaryArc.lower -= 1; p.outPrimaryArc.upper -= 1
    for arc in p.events:        arc.lower += 1;             arc.upper += 1
```
Invariant maintained: `newBefore` spans `[0, elapsed]` and `newAfter.lower` counts
days of nominal work remaining.

Per-turn order inside `TONAE.update(...)`:
```
buildPurchaceList → environment.update → checkDrivingMaterials → checkLaborCompliment
→ updateMonthVar → updateDayVar → rule application → environment.updateValues
→ checkLabor → buildRequestList → checkMaterial
→ [player turn: rule notifications]
→ manageResources(...)            // consumes material, books cost, incrementPresentNodes
→ dbRecorder.recordturn
→ endActivities(state)
→ setTime(t_now + 1)              // also sets global.earlyStart
→ startActivities(state)
→ stock.clearPerishable(); rejected.clear(); purchased.clear()
→ currentDate += 24 * interval hours
→ if !querymode && dbrecord: fsched = getLateSchedule(); dbRecorder.endrecord()
→ queryFutures(numFutures)
```

`delayActivity(pnode, delay)` — pushes `endANode` and then relaxes the whole network:
```
start = pnode.inPrimaryArc.tail;  end = pnode.outPrimaryArc.head
end.earlyStart += delay
end.earlyStart = max(end.earlyStart, t_now + 1, start.earlyStart)

repeat until no change:
  for endNode in aNodeSet where endNode.outPrimaryArc == null:      // end A-Nodes
      startNode = endNode.inPrimaryArc.tail.inPrimaryArc.tail
      if startNode.earlyStart > t_now:                              // not yet started
          latest = t_now + 1
          for carc in startNode.inConstraintArcs:
              lastEnd   = carc.tailNode
              soft      = (carc.upper != carc.threshold)
              suggested = lastEnd.earlyStart + carc.lower
              if soft and endNode.earlyStart <  lastEnd.earlyStart:
                  suggested = lastEnd.earlyStart
              else if soft and endNode.earlyStart >= lastEnd.earlyStart
                                and endNode.earlyStart < suggested:
                  suggested = endNode.earlyStart
              latest = max(latest, suggested)
          if startNode.earlyStart != latest:                        // note: != (can pull EARLIER)
              dur = endNode.earlyStart - startNode.earlyStart
              startNode.earlyStart = latest
              endNode.earlyStart   = latest + dur
              changed = true
```

---

## 3. Scheduling queries

```
getLastTimeStep(): int
    if aNodeSet empty: return 0
    return max over aNodeSet of n.earlyStart          // = latest end A-Node
```
For project 523 at load: `14`.

```
getEarlyStart(a): int   = start A-Node's earlyStart (unique node with
                          outPrimaryArc != null and parent.id == a.id)
getEarlyFinish(a): int  = end A-Node's earlyStart (outPrimaryArc == null)
```
Early finish is **exclusive** (`= earlyStart + currentDuration`); no `-1` adjustment.

```
getLateStart(a)  = fsched[a].key
getLateFinish(a) = fsched[a].value
```
where `fsched` is produced by `getLateSchedule()` — computed once in the constructor
and refreshed at the end of each `update()` **only when `!querymode && dbrecord`**.

`getLateSchedule()` — reverse relaxation, repeat-until-complete:
```
lastday = getLastTimeStep()
sched   = {}                                   // Activity -> (LS, LF)
while sched.size != activities.length:
    changed = false
    for anode in aNodeSet:
        if anode.outPrimaryArc == null: continue
        if sched.has(anode.parent):     continue
        start = anode
        end   = anode.outPrimaryArc.head.outPrimaryArc.head   // end A-Node

        min   = lastday
        noadd = false
        for c in start.parent.getConstraints():               // OUTGOING (successor) links
            if sched.has(c.to):  min = Math.min(min, sched[c.to].LS - c.length)
            else:                noadd = true                 // successor not resolved yet
        if noadd: continue

        stime   = min - (end.earlyStart - start.earlyStart)   // LF - currentDuration
        if start.earlyStart <= t_now: stime = start.earlyStart   // freeze started activities
        if end.earlyStart   <= t_now: min   = end.earlyStart
        sched[start.parent] = (LS: stime, LF: min)
        changed = true
    if !changed: throw Error("FATAL INTERNAL ERROR WHILE COMPUTING LATE SCHEDULE")
```
Notes:
- Terminal activities (no successors) get `LF = getLastTimeStep()`.
- `Activity.getConstraints()` returns `links` = **outgoing** constraints only.
- `stime` uses the *current network* duration (`end.earlyStart - start.earlyStart`),
  not `Activity.duration`.

```
isCritical(a) = (getEarlyStart(a) == getLateStart(a)) && (getEarlyFinish(a) == getLateFinish(a))
```

Worked example (project 523 at load, `t_now = 1`, `lastday = 14`):

| act | dur | ES | EF | LF | LS | critical |
|---|---|---|---|---|---|---|
| 1 (234) | 2 | 1 | 3 | 3 | 1 | yes |
| 2 (235) | 3 | 3 | 6 | 6 | 3 | yes |
| 3 (236) | 4 | 6 | 10 | 10 | 6 | yes |
| 4 (237) | 3 | 10 | 13 | 13 | 10 | yes |
| 5 (238) | 1 | 13 | 14 | 14 | 13 | yes |

Also present:
```
getBaselineStart() = min over asBuilt activities of a.start
getBaselineEnd()   = max over asBuilt activities of a.getEnd()
isFinished()       = getCurrentTimeStep() >= getLastTimeStep()
```

---

## 4. As-planned cost model

### 4.1 Per-activity primitives (`Activity`)

```
computeDailyMaterialCost() = Σ over materialuse of  quantity * material.cost
computeDailyLaborCost()    = Σ over laboruse crews of  crew.getDailyCost()
   where crew.getDailyCost() = Σ over laboramt in ASCENDING laborid order of
                                  laborType.cost * amount
computeDailyCost() = computeDailyMaterialCost() * (AgentM.overhead + 1)
                   + computeDailyLaborCost()
getTotal()         = computeDailyCost()         * duration      // BAC / PV
getTotalMaterial() = computeDailyMaterialCost() * duration      // PNode.total_work
getTotalLabor()    = computeDailyLaborCost()    * duration
```
`AgentM.overhead` is a **static** double, default `0.1`, assigned in the `AgentM`
constructor from `project.getOverhead()`. In a TS port, make it an explicit field of
the engine instance rather than a global.

### 4.2 `AgentM` construction (load time)

```
AgentM(tonae):
    AgentM.overstockLoss = tonae.project.overstockPenalty     // static
    AgentM.overhead      = tonae.project.overhead             // static
    asPlanned = new CostSchedule();  asBuilt = new CostSchedule()

    last = tonae.getLastTimeStep()                            // 14 for project 523
    for act in tonae.getProject().getActivities():            // asBuilt, id order 1..n
        map = new TreeMap<int,double>()
        for x = 1; x < last; x++:                             // 1..last-1  (EXCLUSIVE)
            if      act.getStart() > x:  map[x] = 0.0
            else if act.getEnd()   < x:  map[x] = 1.0
            else:                        map[x] = (x - act.getStart()) / act.duration
        asplannedprogress[act] = map
        asbuiltprogress[act]   = TreeMap{ 1 : 0.0 }

    computeCost(asPlanned, 1, last, tonae)
    computeCost(asBuilt,   1, last, tonae)
```

### 4.3 `computeCost(sched, firstDay, lastDay, tonae)` — the core loop

```
material  = { a : sched.getMaterial(a, firstDay-1) }   // cumulative baselines (0 at load)
labor     = { a : sched.getLabor   (a, firstDay-1) }
indirect  = { a : sched.getIndirect(a, firstDay-1) }

for x = firstDay .. lastDay:                       // INCLUSIVE both ends
    sched.setStockValue(x, 0)
    for anode in tonae.getANodeSet():
        if anode.outPrimaryArc == null: continue   // skip end A-Nodes
        a     = anode.parentActivity
        start = anode.earlyStart
        end   = anode.outPrimaryArc.head.outPrimaryArc.head.earlyStart
        if x >= start && x < end:                  // half-open [start, end)
            mat = a.computeDailyMaterialCost()
            labor   [a] += a.computeDailyLaborCost()
            material[a] += mat
            indirect[a] += mat * AgentM.overhead
    for (a,v) in material: sched.setMaterial(a, x, v)     // CUMULATIVE value stored
    for (a,v) in labor:    sched.setLabor   (a, x, v)
    for (a,v) in indirect: sched.setIndirect(a, x, v)
```
Key facts:
- The maps hold **running cumulative** totals; `get(a, day)` is cumulative-to-day.
- Indirect = `dailyMaterial × overhead`, accrued **only on active days**; labor
  carries no overhead.
- Half-open `[start, end)` ⇒ exactly `duration` day-charges per activity.
- Accumulate day-by-day (`+= daily`), not `daily * n` — faithful float order.

### 4.4 `CostSchedule` storage & aggregation

```
act_material, act_labor, act_indirect : TreeMap<Activity, TreeMap<int,double>>
stockvalue                            : TreeMap<int,double>
setX(a, day, cost):  query_futures_total += cost
                     if querymode: return          // query mode records nothing
                     act_X[a][day] = cost
addX(a, day, cost):  query_futures_total += cost
                     if querymode: return
                     act_X[a][day] = cost + getX(a, day)
getX(a, day):        act_X[a]?.[day] ?? 0
```
Daily roll-ups iterate activities in **id ASC order** (fixes float summation order):
```
getMaterialTotal(day) = Σ_a getMaterial(a, day) + getStockValue(day)
getTotal(tonae)[x]    = getMaterialTotal(x) + getLaborTotal(x) + getIndirectTotal(x)
```
Per-day series use `for x = 1; x < getLastTimeStep(); x++` — keys `1 .. last-1`.
**This exclusive bound is why 523's total lastKey is 13** (all activities fully
charged by then, so `get(13) = 208350.64` = grand total).

Activity-scoped "so far" helpers (`getMaterial(a, tonae)` etc.) sum entries with
`key < now` over a **cumulative** series ⇒ over-count. Known defect; preserve
verbatim only where AC replication requires it, and flag.
`getTotalByActivity` has a key-shift mismatch (writes key+1, tests unshifted). Defect.

### 4.5 Verified derivation of the project-523 numbers

```
overhead = 0.15
total(a) = (daily_material(a) × 1.15 + daily_labor(a)) × duration
```

| act | dur | daily_material | crew composition (laborid × amt × unitcost) | daily_labor | total |
|---|---|---|---|---|---|
| 234 | 2 | 50 @ 100 = 5000 | 103×1×416.8 + 120×2×392 | 1200.80 | **13901.60** |
| 235 | 3 | 6000 @ 3 = 18000 | 94×1×511.6 + 102×3×392 + 103×1×416.8 + 123×1×434.72 | 2539.12 | 69717.36 |
| 236 | 4 | 4000 @ 3 = 12000 | 102×6×392 + 103×1×416.8 | 2768.80 | 66275.20 |
| 237 | 3 | 3000 @ 3 = 9000 | 94×1×511.6 + 102×3×392 + 103×1×416.8 + 122×1×434.72 | 2539.12 | 38667.36 |
| 238 | 1 | 100 @ 150 = 15000 | 94×1×511.6 + 102×3×392 + 103×1×416.8 + 124×1×434.72 | 2539.12 | 19789.12 |

Component grand totals: material 154000.00, indirect 23100.00, labor 31250.64
⇒ **208350.64**. No calendar, overtime, wage incentive, productivity, or stock value
enters the as-planned figure.

### 4.6 Earned-value block (`TONAE` ~587-767)

```
BAC(a) = baseline(a).getTotal()
PV(a)  = asPlanned(a).getTotal()               // NOT time-phased
EV(a)  = asbuiltprogress[a][now-1] * BAC(a)    // 0 if absent
AC(a)  = asBuilt.getTotal(a, tonae)            // over-counting defect, see §4.4
CPI(a) = AC == 0 ? 0 : EV/AC ;  CV = EV - AC
EAC(a) = BAC/CPI  (Infinity when CPI == 0) ;  ETC = EAC - AC ;  VAC = BAC - EAC
SPI(a) = EV/PV  (unguarded) ;  SV = EV - PV
CVI(a) = perc==0 ? 0 : (mat+lab+ind at now-1)/(BAC*perc) - 1
SVI(a) = apperc==0 ? 0 : abperc/apperc - 1
```
`asbuiltprogress[a][now+1] = nowPos > end ? 1 : nowPos < start ? 0
                            : 1 - (end - nowPos)/activity.duration` (can go negative
under large delays — divisor is the static duration, window is the live network).

### 4.7 As-built booking summary (details in turn-loop spec)

Per non-query turn: carry cumulative forward for all activities; per ready PNode,
`newRate = min(materialAvailabilityRate, workRate × Productivity)`; book material
(`addMaterial`), labor (`time × crew.getDailyCost() × wageIncentive`), indirect
(`dailyMaterial × overhead`); `incrementPresentNodes`; stock value; then
`computeCost(sched, day+1, lastTimeStep)` re-projects the tail.
Overtime normalization (interval 1): weekend → ×2 if scheduled, else 0; hours > 8 →
`(h-8)*2 + 8`; divide by 8. `computeWorkQuantityMultiplier`: hourfactor
`h*d/40` (overtime half-productive above 1), wagefactor `2 - 1/wageIncentive`,
crew congestion `1 + (perc-1)*0.8` above 1, weekend gate, min across crews.
`LaborCrew.compareProductivity` special-cases "Foreman" (≤10 workers each),
"Crane"/"Oiler" (must be fully staffed or 0).

---

## 5. Port checklist / fidelity notes

1. Key every activity map by **numeric id**.
2. Iterate activities in **id ASC** for every summation (float order).
3. Iterate crew members in **ascending laborid**.
4. Cost schedule stores **cumulative** per-day values.
5. Per-day series keys `1 .. lastTimeStep - 1` (exclusive) — 523's lastKey = 13.
6. `computeCost` loop inclusive; window test half-open `[start, end)`.
7. Read overhead/overstock as **doubles** (Java's float narrowing is a defect).
8. Renumbering to 1..n happens after wiring; `realid` is what the "ID" variable reports.
9. Null-check never-synthesized variables (Labor Available, Low Labor, Driving
   Material Available, Material Available, Month, Day, Weather).
10. readyList ordered by PNode label; activity labels must be unique.
11. Known defects to reproduce-or-flag: soft+length==0 decodes hard; cumulative-sum
    AC; getTotalByActivity key shift; global ActivityTime never updated; SPI/EAC
    division by zero; getLateSchedule dead `minstart`.
