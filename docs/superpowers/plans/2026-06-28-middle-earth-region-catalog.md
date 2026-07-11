# Middle-earth Region Catalog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a small, validated Middle-earth region catalog that can feed later worldgen, faction control, conquest, and spawn weighting work.

**Architecture:** Keep this slice as pure Java data modeling plus JSON resources. The Java API mirrors the existing faction and army unit catalogs: normalized ID value object, enum for bounded categories, immutable definition record, immutable catalog with lookup/filter helpers and duplicate rejection.

**Tech Stack:** Java 25 records/enums, existing main-method test harness style, Minecraft data resources under `src/main/resources/data/galacticwars/kingdomwars/regions`.

---

### Task 1: Region Catalog Test Harness

**Files:**
- Create: `src/test/java/middleearth/lotr/warmod/world/GalacticRegionCatalogTest.java`

- [ ] **Step 1: Write the failing test**

Create a main-method harness that imports `galacticwars.clonewars.world.*` and verifies:
- `GalacticRegionId.of("Republic")` normalizes to `galacticwars:republic`.
- `GalacticRegionDefinition` stores id, display name, controlling faction, climate, temperature, downfall, spawn weight, and features.
- `GalacticRegionCatalog` looks up by id, filters by faction, filters by climate, and rejects duplicate ids.

- [ ] **Step 2: Run the focused compile to verify it fails**

Run: `javac -cp src/main/java -d build/test-classes src/test/java/middleearth/lotr/warmod/world/GalacticRegionCatalogTest.java`

Expected: compile failure because `galacticwars.clonewars.world` classes do not exist yet.

### Task 2: Region Catalog Model

**Files:**
- Create: `src/main/java/middleearth/lotr/warmod/world/GalacticRegionId.java`
- Create: `src/main/java/middleearth/lotr/warmod/world/GalacticRegionClimate.java`
- Create: `src/main/java/middleearth/lotr/warmod/world/GalacticRegionDefinition.java`
- Create: `src/main/java/middleearth/lotr/warmod/world/GalacticRegionCatalog.java`

- [ ] **Step 1: Implement `GalacticRegionId`**

Use the same normalization behavior as `FactionId` and `ArmyUnitId`: default namespace `galacticwars`, lowercase path/namespace, reject blank or non `[a-z0-9_.-]+` parts, and format as `namespace:path`.

- [ ] **Step 2: Implement `GalacticRegionClimate`**

Add enum values `TEMPERATE`, `PLAINS`, `SHADOW`, `WOODLAND`, and `MOUNTAIN`.

- [ ] **Step 3: Implement `GalacticRegionDefinition`**

Use a record with fields:
`GalacticRegionId id`, `String displayName`, `FactionId controllingFaction`, `GalacticRegionClimate climate`, `float baseTemperature`, `float downfall`, `int spawnWeight`, and `Set<String> features`.

Validation: non-null object fields, non-blank display name and feature names, `baseTemperature` and `downfall` must be non-negative, `spawnWeight` must be non-negative, and `features` must be copied with `Set.copyOf`.

- [ ] **Step 4: Implement `GalacticRegionCatalog`**

Support construction from a `List<GalacticRegionDefinition>` and a `Map<GalacticRegionId, GalacticRegionDefinition>`. Expose `definition(id)`, `regionsForFaction(factionId)`, and `regionsForClimate(climate)`. Reject duplicate ids when indexing the list.

- [ ] **Step 5: Run the focused harness**

Run: `javac -cp src/main/java -d build/test-classes src/test/java/middleearth/lotr/warmod/world/GalacticRegionCatalogTest.java`

Run: `java -cp build/test-classes;src/main/java galacticwars.clonewars.world.GalacticRegionCatalogTest`

Expected: `GalacticRegionCatalogTest passed`.

### Task 3: Region Data Resources

**Files:**
- Create: `src/main/resources/data/galacticwars/kingdomwars/regions/republic.json`
- Create: `src/main/resources/data/galacticwars/kingdomwars/regions/mandalorian.json`
- Create: `src/main/resources/data/galacticwars/kingdomwars/regions/separatist.json`

- [ ] **Step 1: Add resource JSON**

Each file must include `display_name`, `controlling_faction`, `climate`, `base_temperature`, `downfall`, `spawn_weight`, and `features`.

- [ ] **Step 2: Parse the resource JSON**

Run: `Get-ChildItem src/main/resources/data/galacticwars/kingdomwars/regions -Filter *.json | ForEach-Object { Get-Content -Raw -LiteralPath $_.FullName | ConvertFrom-Json > $null }`

Expected: exit code `0`.

### Task 4: Regression Verification And PR

**Files:**
- Verify changed files only, plus existing main-method harnesses.
- Modify: `build.gradle`

- [ ] **Step 1: Run the full pure Java harness set**

Run all `src/test/java/**/*Test.java` main harnesses using `javac` then `java`.

Expected: every harness prints its `passed` line.

- [ ] **Step 2: Run static checks**

Run: `git diff --check`

Run: `rg "talhanation|recruits\\.server|lotr\\.common" src/main/java src/test/java src/main/resources`

Expected: whitespace check exits `0`; upstream package leakage search finds no matches.

- [ ] **Step 3: Keep Gradle CI compatible with main-method harnesses**

Run: `./gradlew test --no-daemon --console=plain --warning-mode=all`

Expected before the build fix: fail with `did not discover any tests to execute`.

Add this to `build.gradle` near the compile task configuration:

```groovy
tasks.withType(Test).configureEach {
    // Current verification harnesses are executable main classes, not JUnit tests.
    failOnNoDiscoveredTests = false
}
```

Run: `./gradlew test --no-daemon --console=plain --warning-mode=all`

Expected after the build fix: exit code `0`.

- [ ] **Step 4: Commit, push, and open PR**

Commit message: `Add Middle-earth region catalog`.

Push branch: `codex/region-catalog`.

PR title: `[codex] Add Middle-earth region catalog`.
