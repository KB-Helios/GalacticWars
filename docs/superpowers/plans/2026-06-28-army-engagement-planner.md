# Army Engagement Planner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add clean-room engagement stance planning so army units can convert selected hostile targets into attack or idle behavior decisions.

**Architecture:** Keep target scoring in `ArmyTargetSelector`; the engagement planner only filters candidates by stance and translates a selected target into `ArmyBehaviorDecision.attack(...)`. This keeps passive, defensive, and aggressive modes testable without Minecraft entity classes.

**Tech Stack:** Java 25, plain Java records/classes, lightweight `javac`/`java` test harness.

---

## Files

- Create: `src/test/java/middleearth/lotr/warmod/army/ArmyEngagementPlannerTest.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyEngagementStance.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyEngagementDecision.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyEngagementPlanner.java`

### Task 1: Red Tests

- [ ] **Step 1: Add engagement planner harness**

Create `ArmyEngagementPlannerTest` with a `main` method that checks:

- Passive stance idles even when an enemy is attacking the owner.
- Defensive stance engages owner/recruit attackers but ignores ordinary enemies.
- Defensive stance idles when no active defensive threat exists.
- Aggressive stance engages ordinary hostile enemies.
- Planner and decision inputs reject invalid values.

- [ ] **Step 2: Run red compile**

Run:

```powershell
javac -d build\engagement-test-classes src\main\java\middleearth\lotr\warmod\army\ArmyBehaviorDecision.java src\main\java\middleearth\lotr\warmod\army\ArmyBehaviorIntent.java src\main\java\middleearth\lotr\warmod\army\ArmyPosition.java src\main\java\middleearth\lotr\warmod\army\ArmyTargetCandidate.java src\main\java\middleearth\lotr\warmod\army\ArmyTargetSelection.java src\main\java\middleearth\lotr\warmod\army\ArmyTargetSelector.java src\main\java\middleearth\lotr\warmod\faction\FactionId.java src\main\java\middleearth\lotr\warmod\faction\FactionRelation.java src\main\java\middleearth\lotr\warmod\faction\FactionDefinition.java src\main\java\middleearth\lotr\warmod\faction\FactionCatalog.java src\test\java\middleearth\lotr\warmod\army\ArmyEngagementPlannerTest.java
```

Expected: compile fails because engagement planner classes are missing.

### Task 2: Production Engagement Model

- [ ] **Step 1: Add engagement stance**

Create `ArmyEngagementStance` with `PASSIVE`, `DEFENSIVE`, and `AGGRESSIVE`.

- [ ] **Step 2: Add engagement decision**

Create `ArmyEngagementDecision(boolean engaging, ArmyBehaviorDecision behaviorDecision, ArmyTargetSelection targetSelection, String reasonCode)`:

- `behaviorDecision` and non-blank `reasonCode` are required.
- `targetSelection` is required when `engaging` is true.
- `targetSelection` must be null when `engaging` is false.
- Add `static engage(ArmyTargetSelection selection)` and `static idle(String reasonCode)` factories.

- [ ] **Step 3: Add engagement planner**

Create `ArmyEngagementPlanner.plan(ArmyEngagementStance stance, FactionId ownFaction, ArmyPosition origin, List<ArmyTargetCandidate> candidates, FactionCatalog factions, int maxRange)`:

- Reject null arguments and negative range through the selector.
- `PASSIVE`: return idle with reason `passive_stance`.
- `DEFENSIVE`: filter to candidates with `attackingOwner` or `attackingRecruit`; if selector finds a target, engage it, otherwise idle with `no_defensive_threat`.
- `AGGRESSIVE`: run selector on all candidates; if selector finds a target, engage it, otherwise idle with `no_target`.

### Task 3: Verification

- [ ] **Step 1: Compile and run engagement planner harness**

Run:

```powershell
javac -d build\engagement-test-classes src\main\java\middleearth\lotr\warmod\army\ArmyBehaviorDecision.java src\main\java\middleearth\lotr\warmod\army\ArmyBehaviorIntent.java src\main\java\middleearth\lotr\warmod\army\ArmyPosition.java src\main\java\middleearth\lotr\warmod\army\ArmyTargetCandidate.java src\main\java\middleearth\lotr\warmod\army\ArmyTargetSelection.java src\main\java\middleearth\lotr\warmod\army\ArmyTargetSelector.java src\main\java\middleearth\lotr\warmod\army\ArmyEngagementStance.java src\main\java\middleearth\lotr\warmod\army\ArmyEngagementDecision.java src\main\java\middleearth\lotr\warmod\army\ArmyEngagementPlanner.java src\main\java\middleearth\lotr\warmod\faction\FactionId.java src\main\java\middleearth\lotr\warmod\faction\FactionRelation.java src\main\java\middleearth\lotr\warmod\faction\FactionDefinition.java src\main\java\middleearth\lotr\warmod\faction\FactionCatalog.java src\test\java\middleearth\lotr\warmod\army\ArmyEngagementPlannerTest.java
java -cp build\engagement-test-classes galacticwars.clonewars.army.ArmyEngagementPlannerTest
```

Expected: prints `ArmyEngagementPlannerTest passed`.

- [ ] **Step 2: Re-run army/faction pure harnesses**

Run engagement planner, patrol planner, target selector, tactical planner, group order, behavior, army core, formation, unit catalog, and faction hiring harnesses.

Expected: all print their `passed` messages.

- [ ] **Step 3: Static checks**

Run:

```powershell
rtk rg -n "ArmyEngagementStance|ArmyEngagementDecision|ArmyEngagementPlanner|passive_stance|no_defensive_threat|no_target" src docs\superpowers\plans\2026-06-28-army-engagement-planner.md
rtk rg -n "talhanation|AbstractRecruit|RecruitEntity|PatrolLeader|package com\.talhanation|net\.talhanation" src\main\java src\test\java
```

Expected: first command finds the new engagement code/tests; second command finds no upstream class/package names.
