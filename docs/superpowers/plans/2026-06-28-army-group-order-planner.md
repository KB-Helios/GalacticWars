# Army Group Order Planner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a clean-room group order planner that fans group commands out into deterministic per-recruit commands and formation targets.

**Architecture:** Keep the planner pure Java and independent of Minecraft classes. Use `ArmyGroupState` as the authoritative group command source, preserve group member insertion order, and delegate formation math to `FormationPlanner`.

**Tech Stack:** Java 25, plain Java records/classes, lightweight `javac`/`java` test harness.

---

## Files

- Create: `src/test/java/middleearth/lotr/warmod/army/ArmyGroupOrderPlannerTest.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyGroupOrderAssignment.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyGroupOrderPlanner.java`

### Task 1: Red Tests

- [ ] **Step 1: Add group order harness**

Create `ArmyGroupOrderPlannerTest` with a `main` method that checks:

- `MOVE_TO_POSITION` group commands become per-recruit move commands assigned to line formation positions.
- `HOLD_POSITION` group commands become per-recruit hold commands assigned to column formation positions.
- `ATTACK_TARGET`, `PROTECT_OWNER`, `FOLLOW_OWNER`, and `CLEAR_TARGET` propagate to every recruit without formation positions.
- Empty groups return an empty immutable assignment list.

- [ ] **Step 2: Run red compile**

Run:

```powershell
javac -d build\group-order-test-classes src\main\java\middleearth\lotr\warmod\army\ArmyCommand.java src\main\java\middleearth\lotr\warmod\army\ArmyCommandType.java src\main\java\middleearth\lotr\warmod\army\ArmyFormation.java src\main\java\middleearth\lotr\warmod\army\ArmyGroupState.java src\main\java\middleearth\lotr\warmod\army\ArmyPosition.java src\main\java\middleearth\lotr\warmod\army\FormationPlanner.java src\main\java\middleearth\lotr\warmod\army\FormationSlot.java src\test\java\middleearth\lotr\warmod\army\ArmyGroupOrderPlannerTest.java
```

Expected: compile fails because `ArmyGroupOrderAssignment` and `ArmyGroupOrderPlanner` are missing.

### Task 2: Production Planner

- [ ] **Step 1: Add assignment record**

Create `ArmyGroupOrderAssignment(UUID recruitId, ArmyCommand command, ArmyPosition assignedPosition, FormationSlot formationSlot)`.

- [ ] **Step 2: Add planner**

Create `ArmyGroupOrderPlanner.plan(ArmyGroupState group, ArmyFormation formation, int spacing)`:

- For `MOVE_TO_POSITION`, use group command target as the anchor and emit per-recruit `ArmyCommand.moveToPosition(owner, group, assignedPosition)`.
- For `HOLD_POSITION`, use group command target as the anchor and emit per-recruit `ArmyCommand.holdPosition(owner, group, assignedPosition)`.
- For `FOLLOW_OWNER`, `PROTECT_OWNER`, `ATTACK_TARGET`, and `CLEAR_TARGET`, emit equivalent per-recruit commands and set assignment position/slot to `null`.
- Return immutable assignments in group member insertion order.

### Task 3: Verification

- [ ] **Step 1: Compile and run group order harness**

Run:

```powershell
javac -d build\group-order-test-classes src\main\java\middleearth\lotr\warmod\army\ArmyCommand.java src\main\java\middleearth\lotr\warmod\army\ArmyCommandType.java src\main\java\middleearth\lotr\warmod\army\ArmyFormation.java src\main\java\middleearth\lotr\warmod\army\ArmyGroupOrderAssignment.java src\main\java\middleearth\lotr\warmod\army\ArmyGroupOrderPlanner.java src\main\java\middleearth\lotr\warmod\army\ArmyGroupState.java src\main\java\middleearth\lotr\warmod\army\ArmyPosition.java src\main\java\middleearth\lotr\warmod\army\FormationPlanner.java src\main\java\middleearth\lotr\warmod\army\FormationSlot.java src\test\java\middleearth\lotr\warmod\army\ArmyGroupOrderPlannerTest.java
java -cp build\group-order-test-classes galacticwars.clonewars.army.ArmyGroupOrderPlannerTest
```

Expected: prints `ArmyGroupOrderPlannerTest passed`.

- [ ] **Step 2: Re-run existing pure harnesses**

Run existing behavior, army core, formation, unit catalog, and faction hiring harnesses.

Expected: all print their `passed` messages.

- [ ] **Step 3: Static checks**

Run:

```powershell
rtk rg -n "ArmyGroupOrderPlanner|ArmyGroupOrderAssignment|move_group_order|hold_group_order" src docs\superpowers\plans\2026-06-28-army-group-order-planner.md
rtk rg -n "talhanation|AbstractRecruit|RecruitEntity|PatrolLeader|package com\.talhanation|net\.talhanation" src\main\java src\test\java
```

Expected: first command finds the new planner code/tests; second command finds no upstream class/package names.
