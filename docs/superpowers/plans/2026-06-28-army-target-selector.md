# Army Target Selector Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add clean-room hostile target selection so army behavior can choose valid enemy targets from nearby candidates.

**Architecture:** Keep the selector pure Java and independent of Minecraft classes. It consumes faction relations from `FactionCatalog`, candidate metadata from a small record, and returns an optional deterministic target selection for later behavior planners.

**Tech Stack:** Java 25, plain Java records/classes, lightweight `javac`/`java` test harness.

---

## Files

- Create: `src/test/java/middleearth/lotr/warmod/army/ArmyTargetSelectorTest.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyTargetCandidate.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyTargetSelection.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyTargetSelector.java`

### Task 1: Red Tests

- [ ] **Step 1: Add target selector harness**

Create `ArmyTargetSelectorTest` with a `main` method that checks:

- Allies, the same faction, neutral/unknown factions, and out-of-range enemies are ignored.
- An enemy attacking the owner is selected over a closer ordinary enemy.
- An enemy attacking the recruit is selected over a higher-threat ordinary enemy when no owner attacker exists.
- Ordinary enemy selection uses threat first and distance as a deterministic tie-breaker.
- Candidate and selector inputs reject invalid values.

- [ ] **Step 2: Run red compile**

Run:

```powershell
javac -d build\target-selector-test-classes src\main\java\middleearth\lotr\warmod\army\ArmyPosition.java src\main\java\middleearth\lotr\warmod\faction\FactionId.java src\main\java\middleearth\lotr\warmod\faction\FactionRelation.java src\main\java\middleearth\lotr\warmod\faction\FactionDefinition.java src\main\java\middleearth\lotr\warmod\faction\FactionCatalog.java src\test\java\middleearth\lotr\warmod\army\ArmyTargetSelectorTest.java
```

Expected: compile fails because `ArmyTargetCandidate`, `ArmyTargetSelection`, and `ArmyTargetSelector` are missing.

### Task 2: Production Target Model

- [ ] **Step 1: Add target candidate**

Create `ArmyTargetCandidate(UUID entityId, FactionId factionId, ArmyPosition position, boolean attackingOwner, boolean attackingRecruit, int threat)`:

- `entityId`, `factionId`, and `position` must be non-null.
- `threat` must be 0 through 100.

- [ ] **Step 2: Add target selection**

Create `ArmyTargetSelection(UUID targetId, ArmyPosition targetPosition, String reasonCode, int score)`:

- `targetId`, `targetPosition`, and non-blank `reasonCode` are required.
- `score` must be non-negative.

- [ ] **Step 3: Add target selector**

Create `ArmyTargetSelector.selectTarget(FactionId ownFaction, ArmyPosition origin, List<ArmyTargetCandidate> candidates, FactionCatalog factions, int maxRange)`:

- Reject null arguments and negative `maxRange`.
- Ignore candidates whose relation to `ownFaction` is not `FactionRelation.ENEMY`.
- Ignore candidates with squared distance greater than `maxRange * maxRange`.
- Score enemies as `1000 + ownerAttackerBonus + recruitAttackerBonus + threatBonus - distancePenalty`.
- `ownerAttackerBonus = 10000`, `recruitAttackerBonus = 5000`, `threatBonus = threat * 20`, and `distancePenalty = manhattanDistance`.
- Select the highest score; tie-break by lower manhattan distance, then by lexicographically smaller UUID string.
- Reason codes are `protect_owner`, `self_defense`, or `hostile_threat`.
- Return `Optional.empty()` when no valid enemy exists.

### Task 3: Verification

- [ ] **Step 1: Compile and run target selector harness**

Run:

```powershell
javac -d build\target-selector-test-classes src\main\java\middleearth\lotr\warmod\army\ArmyPosition.java src\main\java\middleearth\lotr\warmod\army\ArmyTargetCandidate.java src\main\java\middleearth\lotr\warmod\army\ArmyTargetSelection.java src\main\java\middleearth\lotr\warmod\army\ArmyTargetSelector.java src\main\java\middleearth\lotr\warmod\faction\FactionId.java src\main\java\middleearth\lotr\warmod\faction\FactionRelation.java src\main\java\middleearth\lotr\warmod\faction\FactionDefinition.java src\main\java\middleearth\lotr\warmod\faction\FactionCatalog.java src\test\java\middleearth\lotr\warmod\army\ArmyTargetSelectorTest.java
java -cp build\target-selector-test-classes galacticwars.clonewars.army.ArmyTargetSelectorTest
```

Expected: prints `ArmyTargetSelectorTest passed`.

- [ ] **Step 2: Re-run army/faction pure harnesses**

Run target selector, tactical planner, group order, behavior, army core, formation, unit catalog, and faction hiring harnesses.

Expected: all print their `passed` messages.

- [ ] **Step 3: Static checks**

Run:

```powershell
rtk rg -n "ArmyTargetCandidate|ArmyTargetSelection|ArmyTargetSelector|protect_owner|self_defense|hostile_threat" src docs\superpowers\plans\2026-06-28-army-target-selector.md
rtk rg -n "talhanation|AbstractRecruit|RecruitEntity|PatrolLeader|package com\.talhanation|net\.talhanation" src\main\java src\test\java
```

Expected: first command finds the new target selector code/tests; second command finds no upstream class/package names.
