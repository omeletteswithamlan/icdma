# What iCDMA does

*The one-page explainer. Distilled from the research corpus (2003–2013); this
becomes the platform's About page.*

Construction management is usually taught as separate calculations — estimates,
schedules, cash flows — that students can perform but struggle to apply when a
real project starts pushing back. Aviation and medicine solved the analogous
problem with simulators. iCDMA (the Interactive Construction Decision-Making
Aid, descended from the Virtual Coach) is that simulator for construction: a
**situational simulation** in which a learner manages an unfolding project,
day by day, against weather, strikes, late deliveries, and their own earlier
decisions.

## How a session works

The learner receives an as-planned project: activities with durations and
precedence, crews, materials, budgets. Then time starts moving. Each turn
(a day or a week), they can act — assign or hire crews, set working hours and
wage incentives, order materials against limited site storage, accelerate or
delay work — or do nothing. The simulation computes the consequences: work
performed, money spent, the schedule cascading through the network. Events
interrupt with probability: rain halves productivity outdoors; overworked crews
get sick or strike; deliveries fail. A progress report compares as-planned
against as-built — schedule and cost performance indices, earned value — so the
learner watches the gap between plan and reality open or close in response to
their choices.

Two ideas make this more than a game. First, the schedule is a **live temporal
constraint network** (TONAE): activities are intervals, constraints are hard or
soft with penalty rates, and criticality is recomputed as events land — so the
learner experiences float being consumed, not just calculated. Second, the
learner can **query the future**: the engine clones the current project state
and plays the remainder hundreds of times with events firing at random,
returning a distribution of completion dates and costs. Risk stops being a
paragraph in a textbook and becomes a histogram that shifts when you act.

## Why it teaches

The design rests on situated-learning research: management judgment is not
recall of procedures but adaptation inside a system with feedback and delay.
The simulation's job is to make constraint violations and causal loops
perceivable — accelerating a non-critical activity wastes money; pushing crews
raises the odds of losing them. In the original classroom study, students'
weighting of constraint-related planning factors rose significantly after one
session, and think-aloud protocols caught the misconceptions the tool exists to
surface. The engine itself was later validated against a real Michigan DOT
highway reconstruction: with events off it reproduces the as-planned schedule
exactly; with event probabilities mined from field records, its do-nothing runs
land within a day of the as-built duration.

## What the platform adds

The original system was a desktop Java application with scenarios authored by
programmers against a lab database. The rebuild keeps the engine's ideas —
network, rules, futures — and changes the delivery: it runs in the browser,
scenarios are shareable files any instructor can author, course modules teach
the component skills (equipment production, fleet balancing, CPM, ownership
cost, cash flow) with the same engine underneath, and the full simulation is
the capstone. Runs can be recorded — with consent — so the platform is also an
instrument for research on how people learn to manage projects.
