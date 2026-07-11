# Army Command Policy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a non-throwing server-side army command validation policy for packet/UI command intents before group state mutates.

**Architecture:** Keep existing `RecruitState` and `ArmyGroupState` exception guards as low-level invariants. Add `ArmyCommandValidation` as a stable result record and `ArmyCommandPolicy` as a pure Java policy that checks command issuer, group id, group membership, faction alignment, and command payload shape.

**Tech Stack:** Java 25 records/classes, existing main-method test harness style, current faction alignment model.

---

### Task 1: Command Policy Harness

**Files:**
- Create: `src/test/java/middleearth/lotr/warmod/army/ArmyCommandPolicyTest.java`

- [ ] **Step 1: Write the failing test**

Create a main-method harness that verifies:
- owner with matching group, non-empty group, sufficient alignment, and valid payload is accepted.
- non-owner is rejected with `not_owner`.
- wrong group id is rejected with `group_mismatch`.
- empty group is rejected with `empty_group`.
- low faction alignment is rejected with `alignment_too_low`.
- malformed payloads are rejected with stable reason codes.

- [ ] **Step 2: Run focused compile to verify it fails**

Run: `javac -d build/test-classes src/main/java/middleearth/lotr/warmod/faction/*.java src/main/java/middleearth/lotr/warmod/army/*.java src/test/java/middleearth/lotr/warmod/army/ArmyCommandPolicyTest.java`

Expected: compile failure because `ArmyCommandPolicy` and `ArmyCommandValidation` do not exist yet.

### Task 2: Command Policy Model

**Files:**
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyCommandValidation.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyCommandPolicy.java`

- [ ] **Step 1: Implement `ArmyCommandValidation`**

Use fields `boolean accepted` and `String reasonCode`. Provide static factories `accepted()` and `rejected(String reasonCode)`. Reject blank reason codes.

- [ ] **Step 2: Implement `ArmyCommandPolicy`**

Expose:

```java
public static ArmyCommandValidation canIssue(
        ArmyCommand command,
        ArmyGroupState group,
        FactionAlignment alignment,
        FactionId unitFaction,
        int minimumAlignment
)
```

Validation order:
1. null command -> `missing_command`
2. null group -> `missing_group`
3. null alignment -> `unknown_player`
4. null faction id -> `unknown_faction`
5. non-owner issuer -> `not_owner`
6. command group mismatch -> `group_mismatch`
7. empty group -> `empty_group`
8. low faction alignment -> `alignment_too_low`
9. malformed payload for command type -> `invalid_payload`
10. otherwise accepted

- [ ] **Step 3: Run focused harness**

Run: `javac -d build/test-classes src/main/java/middleearth/lotr/warmod/faction/*.java src/main/java/middleearth/lotr/warmod/army/*.java src/test/java/middleearth/lotr/warmod/army/ArmyCommandPolicyTest.java`

Run: `java -cp build/test-classes;src/main/java galacticwars.clonewars.army.ArmyCommandPolicyTest`

Expected: `ArmyCommandPolicyTest passed`.

### Task 3: Regression Verification And PR

**Files:**
- Verify changed files plus existing pure Java harnesses.

- [ ] **Step 1: Run all pure Java harnesses**

Compile and run all `src/test/java/**/*Test.java` main harnesses.

Expected: every harness prints its `passed` line.

- [ ] **Step 2: Run static and build checks**

Run: `git diff --check`

Run: `rg "talhanation|recruits\\.server|lotr\\.common" src/main/java src/test/java src/main/resources`

Run: `./gradlew build --no-daemon --console=plain --warning-mode=all`

Expected: whitespace check exits `0`; upstream package leakage search finds no matches; Gradle build exits `0`.

- [ ] **Step 3: Commit, push, and open stacked PR**

Commit message: `Add army command policy`.

Push branch: `codex/army-command-policy`.

PR title: `[codex] Add army command policy`.

PR base: `codex/alignment-core`.
