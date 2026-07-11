# Army Formation Planner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add clean-room formation planning for army groups so future recruit AI goals can turn follow/hold/move commands into deterministic unit target positions.

**Architecture:** Keep the planner pure Java and independent of Minecraft classes. Represent formations as enum values, calculate immutable formation slots as side/forward offsets, and derive `ArmyPosition` targets from an anchor position and spacing.

**Tech Stack:** Java 25, plain Java records/classes, lightweight `javac`/`java` test harness.

---

## Files

- Create: `src/test/java/middleearth/lotr/warmod/army/FormationPlannerTest.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyFormation.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/FormationSlot.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/FormationPlanner.java`

### Task 1: Red Tests

- [ ] **Step 1: Add formation behavior harness**

Create `FormationPlannerTest` with a `main` method that checks:

- `LINE` creates centered side-by-side offsets.
- `COLUMN` creates straight forward offsets.
- `WEDGE` creates alternating left/right ranks behind the lead slot.
- `SQUARE` creates centered row-major ranks.
- Zero units returns an empty plan.
- Negative unit counts and non-positive spacing are rejected.

- [ ] **Step 2: Run red compile**

Run:

```powershell
javac -d build\formation-test-classes src\main\java\middleearth\lotr\warmod\army\ArmyPosition.java src\test\java\middleearth\lotr\warmod\army\FormationPlannerTest.java
```

Expected: compile fails because `ArmyFormation`, `FormationSlot`, and `FormationPlanner` are missing.

### Task 2: Production Planner

- [ ] **Step 1: Add formation enum**

Create `ArmyFormation` with `LINE`, `COLUMN`, `WEDGE`, and `SQUARE`.

- [ ] **Step 2: Add formation slot record**

Create `FormationSlot(int index, int sideOffset, int forwardOffset)`.

- [ ] **Step 3: Add planner methods**

Create `FormationPlanner` with:

- `planSlots(ArmyFormation formation, int unitCount, int spacing)`
- `planPositions(ArmyPosition anchor, ArmyFormation formation, int unitCount, int spacing)`

The planner must return immutable lists, reject negative unit counts, reject spacing less than one, and use `anchor.x + sideOffset`, `anchor.y`, `anchor.z + forwardOffset` for world positions.

### Task 3: Verification

- [ ] **Step 1: Compile the pure model and tests**

Run:

```powershell
javac -d build\formation-test-classes src\main\java\middleearth\lotr\warmod\army\ArmyPosition.java src\main\java\middleearth\lotr\warmod\army\ArmyFormation.java src\main\java\middleearth\lotr\warmod\army\FormationSlot.java src\main\java\middleearth\lotr\warmod\army\FormationPlanner.java src\test\java\middleearth\lotr\warmod\army\FormationPlannerTest.java
```

Expected: compile succeeds.

- [ ] **Step 2: Run the behavior harness**

Run:

```powershell
java -cp build\formation-test-classes galacticwars.clonewars.army.FormationPlannerTest
```

Expected: prints `FormationPlannerTest passed`.

- [ ] **Step 3: Re-run existing pure army/faction harnesses**

Run:

```powershell
javac -d build\army-core-test-classes src\main\java\middleearth\lotr\warmod\army\*.java src\main\java\middleearth\lotr\warmod\faction\*.java src\test\java\middleearth\lotr\warmod\army\ArmyCoreTest.java
java -cp build\army-core-test-classes galacticwars.clonewars.army.ArmyCoreTest
javac -d build\faction-policy-test-classes src\main\java\middleearth\lotr\warmod\army\HiringDecision.java src\main\java\middleearth\lotr\warmod\army\HiringPolicy.java src\main\java\middleearth\lotr\warmod\faction\*.java src\test\java\middleearth\lotr\warmod\faction\FactionHiringPolicyTest.java
java -cp build\faction-policy-test-classes galacticwars.clonewars.faction.FactionHiringPolicyTest
```

Expected: both harnesses print their `passed` messages.

- [ ] **Step 4: Run clean-room and identifier checks**

Run:

```powershell
rtk rg -n "LINE|COLUMN|WEDGE|SQUARE|FormationPlanner|FormationSlot|ArmyFormation" src docs\superpowers\plans\2026-06-28-army-formation-planner.md
rtk rg -n "talhanation|AbstractRecruit|RecruitEntity|PatrolLeader|package com\.talhanation|net\.talhanation" src\main\java src\test\java
```

Expected: first command finds the new formation code and tests; second command finds no matches.
