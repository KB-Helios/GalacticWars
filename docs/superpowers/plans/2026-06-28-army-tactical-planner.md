# Army Tactical Planner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add clean-room tactical readiness rules so recruit behavior can retreat, regroup, hold, or execute orders based on health, morale, hunger, and upkeep pressure.

**Architecture:** Keep the planner pure Java and independent of Minecraft classes. Run it after `ArmyBehaviorPlanner`: behavior planning determines the requested command action, then tactical planning decides whether the recruit is fit to execute it.

**Tech Stack:** Java 25, plain Java records/classes, lightweight `javac`/`java` test harness.

---

## Files

- Create: `src/test/java/middleearth/lotr/warmod/army/ArmyTacticalPlannerTest.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/RecruitVitals.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyTacticalIntent.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyTacticalDecision.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyTacticalPlanner.java`

### Task 1: Red Tests

- [ ] **Step 1: Add tactical harness**

Create `ArmyTacticalPlannerTest` with a `main` method that checks:

- Healthy recruits execute the original behavior decision.
- Critical health retreats to the supplied fallback position.
- Broken morale retreats.
- Low morale regroups before executing an attack.
- Hunger exhaustion holds instead of moving.
- Overdue upkeep regroups.
- Vitals reject invalid health, morale, hunger, and upkeep values.

- [ ] **Step 2: Run red compile**

Run:

```powershell
javac -d build\tactical-test-classes src\main\java\middleearth\lotr\warmod\army\ArmyBehaviorDecision.java src\main\java\middleearth\lotr\warmod\army\ArmyBehaviorIntent.java src\main\java\middleearth\lotr\warmod\army\ArmyPosition.java src\test\java\middleearth\lotr\warmod\army\ArmyTacticalPlannerTest.java
```

Expected: compile fails because tactical planner classes are missing.

### Task 2: Production Tactical Model

- [ ] **Step 1: Add recruit vitals**

Create `RecruitVitals(currentHealth, maxHealth, morale, hunger, unpaidTicks)`:

- `maxHealth` must be at least 1.
- `currentHealth` must be between 0 and `maxHealth`.
- `morale` and `hunger` must be 0 through 100.
- `unpaidTicks` must be non-negative.
- Add helpers `healthPercent()`, `isCriticalHealth()`, `isBrokenMorale()`, `isLowMorale()`, `isExhausted()`, and `isUpkeepOverdue()`.

- [ ] **Step 2: Add tactical intent enum**

Create `ArmyTacticalIntent` with `EXECUTE_ORDER`, `RETREAT`, `REGROUP`, and `HOLD_POSITION`.

- [ ] **Step 3: Add tactical decision**

Create `ArmyTacticalDecision` with:

- `ArmyTacticalIntent intent`
- `ArmyBehaviorDecision behaviorDecision`
- `ArmyPosition tacticalTarget`
- `String reasonCode`

- [ ] **Step 4: Add planner**

Create `ArmyTacticalPlanner.plan(ArmyBehaviorDecision behaviorDecision, RecruitVitals vitals, ArmyPosition fallbackPosition)`:

- Critical health: `RETREAT`, fallback target, reason `health_critical`.
- Broken morale: `RETREAT`, fallback target, reason `morale_broken`.
- Low morale while behavior intent is `ATTACK_TARGET`: `REGROUP`, fallback target, reason `morale_low`.
- Hunger exhaustion: `HOLD_POSITION`, behavior move target when present otherwise fallback, reason `hunger_exhausted`.
- Upkeep overdue: `REGROUP`, fallback target, reason `upkeep_overdue`.
- Otherwise: `EXECUTE_ORDER`, no tactical target, reason `ready`.

### Task 3: Verification

- [ ] **Step 1: Compile and run tactical harness**

Run:

```powershell
javac -d build\tactical-test-classes src\main\java\middleearth\lotr\warmod\army\ArmyBehaviorDecision.java src\main\java\middleearth\lotr\warmod\army\ArmyBehaviorIntent.java src\main\java\middleearth\lotr\warmod\army\ArmyPosition.java src\main\java\middleearth\lotr\warmod\army\RecruitVitals.java src\main\java\middleearth\lotr\warmod\army\ArmyTacticalIntent.java src\main\java\middleearth\lotr\warmod\army\ArmyTacticalDecision.java src\main\java\middleearth\lotr\warmod\army\ArmyTacticalPlanner.java src\test\java\middleearth\lotr\warmod\army\ArmyTacticalPlannerTest.java
java -cp build\tactical-test-classes galacticwars.clonewars.army.ArmyTacticalPlannerTest
```

Expected: prints `ArmyTacticalPlannerTest passed`.

- [ ] **Step 2: Re-run existing pure harnesses**

Run existing group order, behavior, army core, formation, unit catalog, and faction hiring harnesses.

Expected: all print their `passed` messages.

- [ ] **Step 3: Static checks**

Run:

```powershell
rtk rg -n "RecruitVitals|ArmyTacticalPlanner|ArmyTacticalIntent|health_critical|morale_broken|morale_low|hunger_exhausted|upkeep_overdue" src docs\superpowers\plans\2026-06-28-army-tactical-planner.md
rtk rg -n "talhanation|AbstractRecruit|RecruitEntity|PatrolLeader|package com\.talhanation|net\.talhanation" src\main\java src\test\java
```

Expected: first command finds the new tactical code/tests; second command finds no upstream class/package names.
