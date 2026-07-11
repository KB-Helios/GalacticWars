# Clean-Room Army Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first clean-room army command/state core that later recruit entities, AI goals, groups, and packets can use.

**Architecture:** Keep this slice pure Java so it can be tested without Minecraft or NeoForge runtime classes. Model command intent separately from recruit/group state, enforce owner/group validation in one place, and add a minimal faction alignment primitive that future hiring and allegiance systems can depend on.

**Tech Stack:** Java 25, plain Java records/classes, lightweight `javac`/`java` test harness.

---

## Files

- Create: `src/test/java/middleearth/lotr/warmod/army/ArmyCoreTest.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyCommandType.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyPosition.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyCommand.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/RecruitState.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyGroupState.java`
- Create: `src/main/java/middleearth/lotr/warmod/faction/FactionId.java`
- Create: `src/main/java/middleearth/lotr/warmod/faction/FactionAlignment.java`

### Task 1: Red Tests

- [ ] **Step 1: Add plain Java behavior tests**

Create `src/test/java/middleearth/lotr/warmod/army/ArmyCoreTest.java` with a `main` method that checks:

- Owner-issued follow commands apply to a recruit.
- Non-owner commands are rejected.
- Group membership is tracked and move commands apply to the group.
- Faction IDs normalize to the project namespace and alignment scores accumulate.

- [ ] **Step 2: Run the red test compile**

Run:

```powershell
javac -d build\army-core-test-classes src\test\java\middleearth\lotr\warmod\army\ArmyCoreTest.java
```

Expected: compile fails because the new army and faction classes do not exist yet.

### Task 2: Production Model

- [ ] **Step 1: Add command primitives**

Create:

- `ArmyCommandType`: `FOLLOW_OWNER`, `HOLD_POSITION`, `MOVE_TO_POSITION`, `PROTECT_OWNER`, `ATTACK_TARGET`, `CLEAR_TARGET`.
- `ArmyPosition`: immutable `x`, `y`, `z` record.
- `ArmyCommand`: immutable command record with static factories for each command kind.

- [ ] **Step 2: Add recruit state**

Create `RecruitState` with immutable owner, recruit, group, and current command fields. `applyCommand` accepts commands from the owner only and returns a new state with the command applied.

- [ ] **Step 3: Add group state**

Create `ArmyGroupState` with immutable owner, group, recruit membership, and current command fields. It should add/remove members immutably and apply commands only when issuer and group ID match.

- [ ] **Step 4: Add faction alignment primitives**

Create `FactionId` and `FactionAlignment`. `FactionId.of("republic")` returns `galacticwars:republic`; `FactionAlignment` tracks per-faction score additions.

### Task 3: Green Verification

- [ ] **Step 1: Compile the pure model and tests**

Run:

```powershell
javac -d build\army-core-test-classes src\main\java\middleearth\lotr\warmod\army\*.java src\main\java\middleearth\lotr\warmod\faction\*.java src\test\java\middleearth\lotr\warmod\army\ArmyCoreTest.java
```

Expected: compile succeeds.

- [ ] **Step 2: Run the behavior harness**

Run:

```powershell
java -cp build\army-core-test-classes galacticwars.clonewars.army.ArmyCoreTest
```

Expected: prints `ArmyCoreTest passed`.

- [ ] **Step 3: Run static scope checks**

Run:

```powershell
rtk rg -n "FOLLOW_OWNER|HOLD_POSITION|MOVE_TO_POSITION|PROTECT_OWNER|ATTACK_TARGET|CLEAR_TARGET|FactionAlignment|FactionId" src docs\superpowers\plans\2026-06-28-clean-room-army-core.md
```

Expected: matches exist in the new tests, production classes, and plan.
