# Army Patrol Planner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add clean-room patrol routing so army units can loop or ping-pong through guard waypoints with optional wait time.

**Architecture:** Keep patrol planning pure Java and independent of Minecraft classes. A route defines waypoints and traversal rules, state tracks the next waypoint and wait countdown, and the planner advances deterministically from the current position.

**Tech Stack:** Java 25, plain Java records/classes, lightweight `javac`/`java` test harness.

---

## Files

- Create: `src/test/java/middleearth/lotr/warmod/army/ArmyPatrolPlannerTest.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyPatrolMode.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyPatrolRoute.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyPatrolState.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyPatrolDecision.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyPatrolPlanner.java`

### Task 1: Red Tests

- [ ] **Step 1: Add patrol planner harness**

Create `ArmyPatrolPlannerTest` with a `main` method that checks:

- A unit outside the active waypoint moves toward it.
- Loop routes advance from the last waypoint back to the first.
- Ping-pong routes reverse direction at the end waypoint.
- Wait ticks hold the unit at the current position before moving to the next waypoint.
- Routes, states, and decisions reject invalid values.

- [ ] **Step 2: Run red compile**

Run:

```powershell
javac -d build\patrol-test-classes src\main\java\middleearth\lotr\warmod\army\ArmyPosition.java src\test\java\middleearth\lotr\warmod\army\ArmyPatrolPlannerTest.java
```

Expected: compile fails because patrol planner classes are missing.

### Task 2: Production Patrol Model

- [ ] **Step 1: Add patrol mode enum**

Create `ArmyPatrolMode` with `LOOP` and `PING_PONG`.

- [ ] **Step 2: Add patrol route**

Create `ArmyPatrolRoute(List<ArmyPosition> waypoints, ArmyPatrolMode mode, int arrivalDistance, int waitTicksAtWaypoint)`:

- `waypoints` must contain at least two non-null positions and be copied as an immutable list.
- `mode` must be non-null.
- `arrivalDistance` and `waitTicksAtWaypoint` must be non-negative.

- [ ] **Step 3: Add patrol state**

Create `ArmyPatrolState(int waypointIndex, int direction, int waitTicksRemaining)`:

- `waypointIndex` must be non-negative.
- `direction` must be `1` or `-1`.
- `waitTicksRemaining` must be non-negative.
- Add `static ArmyPatrolState start()` returning index `0`, direction `1`, wait `0`.

- [ ] **Step 4: Add patrol decision**

Create `ArmyPatrolDecision(ArmyPosition moveTarget, ArmyPatrolState nextState, boolean waiting, String reasonCode)`:

- `moveTarget`, `nextState`, and non-blank `reasonCode` are required.
- Stable reason codes are `moving_to_waypoint`, `advanced_waypoint`, `arrived_waiting`, and `waiting_at_waypoint`.

- [ ] **Step 5: Add planner**

Create `ArmyPatrolPlanner.advance(ArmyPatrolRoute route, ArmyPatrolState state, ArmyPosition currentPosition)`:

- Reject null inputs.
- Reject a state whose `waypointIndex` is outside the route.
- If `state.waitTicksRemaining() > 0`, return a waiting decision at `currentPosition` with wait decremented.
- If outside arrival distance of the active waypoint, return a move decision toward that waypoint with unchanged state.
- If arrived, compute the next route state using loop or ping-pong mode.
- If `route.waitTicksAtWaypoint() > 0`, return a waiting decision at `currentPosition` with the advanced state and full wait count.
- If no wait is configured, return a move decision toward the newly active waypoint with the advanced state.

### Task 3: Verification

- [ ] **Step 1: Compile and run patrol planner harness**

Run:

```powershell
javac -d build\patrol-test-classes src\main\java\middleearth\lotr\warmod\army\ArmyPosition.java src\main\java\middleearth\lotr\warmod\army\ArmyPatrolMode.java src\main\java\middleearth\lotr\warmod\army\ArmyPatrolRoute.java src\main\java\middleearth\lotr\warmod\army\ArmyPatrolState.java src\main\java\middleearth\lotr\warmod\army\ArmyPatrolDecision.java src\main\java\middleearth\lotr\warmod\army\ArmyPatrolPlanner.java src\test\java\middleearth\lotr\warmod\army\ArmyPatrolPlannerTest.java
java -cp build\patrol-test-classes galacticwars.clonewars.army.ArmyPatrolPlannerTest
```

Expected: prints `ArmyPatrolPlannerTest passed`.

- [ ] **Step 2: Re-run army/faction pure harnesses**

Run patrol planner, target selector, tactical planner, group order, behavior, army core, formation, unit catalog, and faction hiring harnesses.

Expected: all print their `passed` messages.

- [ ] **Step 3: Static checks**

Run:

```powershell
rtk rg -n "ArmyPatrolMode|ArmyPatrolRoute|ArmyPatrolState|ArmyPatrolDecision|ArmyPatrolPlanner|moving_to_waypoint|advanced_waypoint|arrived_waiting|waiting_at_waypoint" src docs\superpowers\plans\2026-06-28-army-patrol-planner.md
rtk rg -n "talhanation|AbstractRecruit|RecruitEntity|PatrolLeader|package com\.talhanation|net\.talhanation" src\main\java src\test\java
```

Expected: first command finds the new patrol code/tests; second command finds no upstream class/package names.
