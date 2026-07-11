# Faction Hiring Policy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first faction catalog and hiring policy layer that connects LOTR-style factions/alignment to clean-room army ownership limits.

**Architecture:** Keep the runtime policy pure Java and independent of Minecraft classes. Represent factions as immutable definitions, calculate relations through a catalog, and make hiring decisions through an explicit result type that future commands, packets, GUIs, and recruit entities can consume.

**Tech Stack:** Java 25, plain Java records/classes, JSON resource seed files, lightweight `javac`/`java` test harness.

---

## Files

- Create: `src/test/java/middleearth/lotr/warmod/faction/FactionHiringPolicyTest.java`
- Create: `src/main/java/middleearth/lotr/warmod/faction/FactionRelation.java`
- Create: `src/main/java/middleearth/lotr/warmod/faction/FactionDefinition.java`
- Create: `src/main/java/middleearth/lotr/warmod/faction/FactionCatalog.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/HiringDecision.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/HiringPolicy.java`
- Create: `src/main/resources/data/galacticwars/kingdomwars/factions/republic.json`
- Create: `src/main/resources/data/galacticwars/kingdomwars/factions/mandalorian.json`
- Create: `src/main/resources/data/galacticwars/kingdomwars/factions/separatist.json`

### Task 1: Red Tests

- [ ] **Step 1: Add behavior harness**

Create `FactionHiringPolicyTest` with a `main` method that checks:

- `FactionDefinition` stores hire cost, minimum alignment, max recruits, allies, and enemies.
- `FactionCatalog.relation(a, b)` returns `SAME`, `ALLY`, `ENEMY`, or `NEUTRAL`.
- `HiringPolicy.canHire` accepts a player with enough alignment, enough coins, and room under the recruit cap.
- `HiringPolicy.canHire` rejects low alignment, low coins, and max-recruit cases with stable reason codes.

- [ ] **Step 2: Run red compile**

Run:

```powershell
javac -d build\faction-policy-test-classes src\main\java\middleearth\lotr\warmod\faction\FactionId.java src\main\java\middleearth\lotr\warmod\faction\FactionAlignment.java src\test\java\middleearth\lotr\warmod\faction\FactionHiringPolicyTest.java
```

Expected: compile fails because `FactionDefinition`, `FactionCatalog`, `FactionRelation`, `HiringDecision`, and `HiringPolicy` are missing.

### Task 2: Production Policy

- [ ] **Step 1: Add relation enum**

Create `FactionRelation` with `SAME`, `ALLY`, `NEUTRAL`, and `ENEMY`.

- [ ] **Step 2: Add immutable faction definitions**

Create `FactionDefinition` with:

- `FactionId id`
- `String displayName`
- `int hireCost`
- `int minimumHiringAlignment`
- `int maxOwnedRecruits`
- `Set<FactionId> allies`
- `Set<FactionId> enemies`

The constructor must defensively copy relation sets.

- [ ] **Step 3: Add faction catalog**

Create `FactionCatalog` with immutable definitions by ID, `definition(FactionId)`, `contains(FactionId)`, and `relation(FactionId, FactionId)`.

- [ ] **Step 4: Add hiring decision type**

Create `HiringDecision` with `accepted()`, `reasonCode()`, `cost()`, and static factories:

- `accepted(int cost)`
- `rejected(String reasonCode)`

- [ ] **Step 5: Add hiring policy**

Create `HiringPolicy.canHire(FactionAlignment alignment, FactionDefinition faction, int availableCoins, int ownedRecruitCount)` with reason codes:

- `accepted`
- `unknown_player`
- `alignment_too_low`
- `coins_too_low`
- `recruit_limit_reached`

### Task 3: Faction Resources

- [ ] **Step 1: Add Republic JSON**

Create `republic.json` with display name, hire cost `25`, minimum alignment `10`, max recruits `12`, ally `galacticwars:mandalorian`, and enemy `galacticwars:separatist`.

- [ ] **Step 2: Add Mandalorian JSON**

Create `mandalorian.json` with display name, hire cost `20`, minimum alignment `8`, max recruits `10`, ally `galacticwars:republic`, and enemy `galacticwars:separatist`.

- [ ] **Step 3: Add Separatist JSON**

Create `separatist.json` with display name, hire cost `30`, minimum alignment `15`, max recruits `16`, no allies, and enemies `galacticwars:republic` and `galacticwars:mandalorian`.

### Task 4: Verification

- [ ] **Step 1: Compile the pure model and tests**

Run:

```powershell
javac -d build\faction-policy-test-classes src\main\java\middleearth\lotr\warmod\army\HiringDecision.java src\main\java\middleearth\lotr\warmod\army\HiringPolicy.java src\main\java\middleearth\lotr\warmod\faction\*.java src\test\java\middleearth\lotr\warmod\faction\FactionHiringPolicyTest.java
```

Expected: compile succeeds.

- [ ] **Step 2: Run the behavior harness**

Run:

```powershell
java -cp build\faction-policy-test-classes galacticwars.clonewars.faction.FactionHiringPolicyTest
```

Expected: prints `FactionHiringPolicyTest passed`.

- [ ] **Step 3: Validate resource JSON**

Run:

```powershell
Get-ChildItem -LiteralPath 'src\main\resources\data\galacticwars\kingdomwars\factions' -File -Filter '*.json' | ForEach-Object { $null = Get-Content -Raw -LiteralPath $_.FullName | ConvertFrom-Json -AsHashtable; $_.Name }
```

Expected: prints `republic.json`, `separatist.json`, and `mandalorian.json` with no parse errors.

- [ ] **Step 4: Run static scope checks**

Run:

```powershell
rtk rg -n "FactionDefinition|FactionCatalog|HiringPolicy|alignment_too_low|recruit_limit_reached|galacticwars:republic|galacticwars:mandalorian|galacticwars:separatist" src docs\superpowers\plans\2026-06-28-faction-hiring-policy.md
```

Expected: matches exist in tests, production classes, resources, and plan.
