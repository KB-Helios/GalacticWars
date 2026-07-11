# Army Behavior Planner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a clean-room army behavior planner that maps recruit command state and local context into movement or attack intents.

**Architecture:** Keep the planner pure Java and independent of Minecraft classes. Store runtime facts in a context record, return immutable decisions, and keep command validation in `RecruitState` while behavior selection stays in `ArmyBehaviorPlanner`.

**Tech Stack:** Java 25, plain Java records/classes, lightweight `javac`/`java` test harness.

---

## Files

- Create: `src/test/java/middleearth/lotr/warmod/army/ArmyBehaviorPlannerTest.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyBehaviorIntent.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyBehaviorContext.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyBehaviorDecision.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyBehaviorPlanner.java`

### Task 1: Red Tests

- [ ] **Step 1: Add behavior harness**

Create `ArmyBehaviorPlannerTest` with a `main` method that checks:

- `FOLLOW_OWNER` moves to owner when outside follow range and idles when close.
- `MOVE_TO_POSITION` moves to the command target position.
- `HOLD_POSITION` holds at the command target position.
- `ATTACK_TARGET` attacks when the command target is alive, otherwise idles with a stable reason code.
- `PROTECT_OWNER` attacks the owner threat when present and otherwise follows the owner.
- `CLEAR_TARGET` returns idle.

- [ ] **Step 2: Run red compile**

Run:

```powershell
javac -d build\behavior-test-classes src\main\java\middleearth\lotr\warmod\army\ArmyCommand.java src\main\java\middleearth\lotr\warmod\army\ArmyCommandType.java src\main\java\middleearth\lotr\warmod\army\ArmyPosition.java src\main\java\middleearth\lotr\warmod\army\RecruitState.java src\test\java\middleearth\lotr\warmod\army\ArmyBehaviorPlannerTest.java
```

Expected: compile fails because behavior planner classes are missing.

### Task 2: Production Planner

- [ ] **Step 1: Add behavior intent enum**

Create `ArmyBehaviorIntent` with `IDLE`, `FOLLOW_OWNER`, `MOVE_TO_POSITION`, `HOLD_POSITION`, `PROTECT_OWNER`, and `ATTACK_TARGET`.

- [ ] **Step 2: Add behavior context**

Create `ArmyBehaviorContext` with:

- `ArmyPosition selfPosition`
- `ArmyPosition ownerPosition`
- `UUID visibleThreatToOwner`
- `boolean commandTargetAlive`
- `int followRange`

Use a static factory `of(...)` and reject `followRange < 1`.

- [ ] **Step 3: Add behavior decision**

Create `ArmyBehaviorDecision` with:

- `ArmyBehaviorIntent intent`
- `ArmyPosition moveTarget`
- `UUID attackTargetId`
- `String reasonCode`

Add static factories for `idle`, `move`, `hold`, `follow`, `protect`, and `attack`.

- [ ] **Step 4: Add planner**

Create `ArmyBehaviorPlanner.plan(RecruitState recruit, ArmyBehaviorContext context)`:

- `FOLLOW_OWNER`: follow if squared horizontal distance to owner is greater than `followRange * followRange`, otherwise idle `within_follow_range`.
- `MOVE_TO_POSITION`: move to command target position.
- `HOLD_POSITION`: hold command target position.
- `ATTACK_TARGET`: attack command target when `commandTargetAlive`, otherwise idle `target_unavailable`.
- `PROTECT_OWNER`: attack visible owner threat when present, otherwise follow owner.
- `CLEAR_TARGET`: idle `target_cleared`.

### Task 3: Verification

- [ ] **Step 1: Compile and run behavior harness**

Run:

```powershell
javac -d build\behavior-test-classes src\main\java\middleearth\lotr\warmod\army\ArmyCommand.java src\main\java\middleearth\lotr\warmod\army\ArmyCommandType.java src\main\java\middleearth\lotr\warmod\army\ArmyPosition.java src\main\java\middleearth\lotr\warmod\army\RecruitState.java src\main\java\middleearth\lotr\warmod\army\ArmyBehaviorIntent.java src\main\java\middleearth\lotr\warmod\army\ArmyBehaviorContext.java src\main\java\middleearth\lotr\warmod\army\ArmyBehaviorDecision.java src\main\java\middleearth\lotr\warmod\army\ArmyBehaviorPlanner.java src\test\java\middleearth\lotr\warmod\army\ArmyBehaviorPlannerTest.java
java -cp build\behavior-test-classes galacticwars.clonewars.army.ArmyBehaviorPlannerTest
```

Expected: prints `ArmyBehaviorPlannerTest passed`.

- [ ] **Step 2: Re-run all pure harnesses**

Run existing army core, formation, unit catalog, and faction hiring harnesses.

Expected: all print their `passed` messages.

- [ ] **Step 3: Static checks**

Run:

```powershell
rtk rg -n "ArmyBehaviorPlanner|ArmyBehaviorIntent|within_follow_range|target_unavailable|target_cleared|PROTECT_OWNER" src docs\superpowers\plans\2026-06-28-army-behavior-planner.md
rtk rg -n "talhanation|AbstractRecruit|RecruitEntity|PatrolLeader|package com\.talhanation|net\.talhanation" src\main\java src\test\java
```

Expected: first command finds the new planner code/tests; second command finds no upstream class/package names.
