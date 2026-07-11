# Army Unit Catalog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add recruitable army unit definitions so factions can expose LOTR-style troop identities, roles, costs, and default formations.

**Architecture:** Keep the unit catalog pure Java and independent of Minecraft classes. Model unit IDs as normalized project IDs, associate each unit with a faction, role, hiring stats, and default formation, then add JSON resources that future loaders/datagen can consume.

**Tech Stack:** Java 25, plain Java records/classes, JSON resource seed files, lightweight `javac`/`java` test harness.

---

## Files

- Create: `src/test/java/middleearth/lotr/warmod/army/ArmyUnitCatalogTest.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyUnitId.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyUnitRole.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyUnitDefinition.java`
- Create: `src/main/java/middleearth/lotr/warmod/army/ArmyUnitCatalog.java`
- Create: `src/main/resources/data/galacticwars/kingdomwars/units/clone_trooper.json`
- Create: `src/main/resources/data/galacticwars/kingdomwars/units/mandalorian_rider.json`
- Create: `src/main/resources/data/galacticwars/kingdomwars/units/b1_battle_droid.json`

### Task 1: Red Tests

- [ ] **Step 1: Add unit catalog harness**

Create `ArmyUnitCatalogTest` with a `main` method that checks:

- Unit IDs normalize to `galacticwars:<path>`.
- A Republic soldier stores faction, role, hire cost, health, attack damage, and default formation.
- Catalog lookup returns units by ID.
- Catalog filtering returns units by faction and by role.
- Duplicate unit IDs are rejected.

- [ ] **Step 2: Run red compile**

Run:

```powershell
javac -d build\unit-catalog-test-classes src\main\java\middleearth\lotr\warmod\army\ArmyFormation.java src\main\java\middleearth\lotr\warmod\faction\FactionId.java src\test\java\middleearth\lotr\warmod\army\ArmyUnitCatalogTest.java
```

Expected: compile fails because unit catalog classes are missing.

### Task 2: Production Unit Model

- [ ] **Step 1: Add unit ID**

Create `ArmyUnitId` with the same namespace/path normalization style as `FactionId`.

- [ ] **Step 2: Add unit role enum**

Create `ArmyUnitRole` with `INFANTRY`, `CAVALRY`, `ARCHER`, and `BRUTE`.

- [ ] **Step 3: Add unit definition**

Create `ArmyUnitDefinition` with:

- `ArmyUnitId id`
- `String displayName`
- `FactionId factionId`
- `ArmyUnitRole role`
- `int hireCost`
- `int maxHealth`
- `int attackDamage`
- `ArmyFormation defaultFormation`

Reject blank names and negative numeric values.

- [ ] **Step 4: Add unit catalog**

Create `ArmyUnitCatalog` with immutable definitions by ID, `definition(ArmyUnitId)`, `unitsForFaction(FactionId)`, and `unitsForRole(ArmyUnitRole)`. Reject duplicate unit IDs.

### Task 3: Unit Resources

- [ ] **Step 1: Add Republic soldier JSON**

Create `clone_trooper.json` with faction `galacticwars:republic`, role `infantry`, hire cost `25`, max health `24`, attack damage `5`, and default formation `line`.

- [ ] **Step 2: Add Mandalorian rider JSON**

Create `mandalorian_rider.json` with faction `galacticwars:mandalorian`, role `cavalry`, hire cost `35`, max health `26`, attack damage `6`, and default formation `wedge`.

- [ ] **Step 3: Add Separatist orc JSON**

Create `b1_battle_droid.json` with faction `galacticwars:separatist`, role `brute`, hire cost `20`, max health `22`, attack damage `5`, and default formation `column`.

### Task 4: Verification

- [ ] **Step 1: Compile and run unit catalog harness**

Run:

```powershell
javac -d build\unit-catalog-test-classes src\main\java\middleearth\lotr\warmod\army\ArmyFormation.java src\main\java\middleearth\lotr\warmod\army\ArmyUnitId.java src\main\java\middleearth\lotr\warmod\army\ArmyUnitRole.java src\main\java\middleearth\lotr\warmod\army\ArmyUnitDefinition.java src\main\java\middleearth\lotr\warmod\army\ArmyUnitCatalog.java src\main\java\middleearth\lotr\warmod\faction\FactionId.java src\test\java\middleearth\lotr\warmod\army\ArmyUnitCatalogTest.java
java -cp build\unit-catalog-test-classes galacticwars.clonewars.army.ArmyUnitCatalogTest
```

Expected: prints `ArmyUnitCatalogTest passed`.

- [ ] **Step 2: Re-run existing pure harnesses**

Run the formation, army core, and faction policy harnesses.

Expected: all print their `passed` messages.

- [ ] **Step 3: Validate JSON resources**

Run:

```powershell
Get-ChildItem -LiteralPath 'src\main\resources' -Recurse -File -Filter '*.json' | ForEach-Object { $null = Get-Content -Raw -LiteralPath $_.FullName | ConvertFrom-Json -AsHashtable; $_.FullName.Substring($PWD.Path.Length + 1) }
```

Expected: all JSON files print with no parse errors.

- [ ] **Step 4: Run static checks**

Run:

```powershell
rtk rg -n "ArmyUnitId|ArmyUnitRole|ArmyUnitDefinition|ArmyUnitCatalog|clone_trooper|mandalorian_rider|b1_battle_droid" src docs\superpowers\plans\2026-06-28-army-unit-catalog.md
rtk rg -n "talhanation|AbstractRecruit|RecruitEntity|PatrolLeader|package com\.talhanation|net\.talhanation" src\main\java src\test\java src\main\resources\data\galacticwars\kingdomwars
```

Expected: first command finds the new unit code/resources/tests; second command finds no upstream class/package names.
