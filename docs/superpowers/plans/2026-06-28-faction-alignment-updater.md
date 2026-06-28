# Faction Alignment Updater Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a pure Java faction alignment update layer that propagates reputation events through direct factions, allies, and enemies.

**Architecture:** Build on the existing immutable `FactionAlignment` and `FactionCatalog`. A `FactionAlignmentRule` describes the direct, ally, and enemy score deltas for one event; `FactionAlignmentUpdater` applies it through catalog relations and returns a `FactionAlignmentUpdateResult` containing the updated alignment plus audited `FactionAlignmentChange` entries.

**Tech Stack:** Java 25 records, existing main-method test harness style, no Minecraft runtime dependencies.

---

### Task 1: Alignment Propagation Harness

**Files:**
- Create: `src/test/java/middleearth/lotr/warmod/faction/FactionAlignmentUpdaterTest.java`

- [ ] **Step 1: Write the failing test**

Create a main-method harness that verifies:
- Helping Gondor applies a direct Gondor score gain, an allied Rohan score gain, and an enemy Mordor score loss.
- Harming Gondor applies a direct Gondor score loss, an allied Rohan score loss, and an enemy Mordor score gain.
- Neutral factions do not receive changes.
- Returned changes include faction id, before score, delta, after score, and a stable reason code.
- Blank reason codes and a zero-effect rule are rejected.

- [ ] **Step 2: Run focused compile to verify it fails**

Run: `javac -d build/test-classes src/main/java/middleearth/lotr/warmod/faction/*.java src/test/java/middleearth/lotr/warmod/faction/FactionAlignmentUpdaterTest.java`

Expected: compile failure because `FactionAlignmentRule`, `FactionAlignmentChange`, `FactionAlignmentUpdateResult`, and `FactionAlignmentUpdater` do not exist yet.

### Task 2: Alignment Update Model

**Files:**
- Create: `src/main/java/middleearth/lotr/warmod/faction/FactionAlignmentRule.java`
- Create: `src/main/java/middleearth/lotr/warmod/faction/FactionAlignmentChange.java`
- Create: `src/main/java/middleearth/lotr/warmod/faction/FactionAlignmentUpdateResult.java`
- Create: `src/main/java/middleearth/lotr/warmod/faction/FactionAlignmentUpdater.java`

- [ ] **Step 1: Implement `FactionAlignmentRule`**

Use fields `int directDelta`, `int allyDelta`, `int enemyDelta`, and `String reasonCode`. Reject blank reason codes and reject rules where all three deltas are zero.

- [ ] **Step 2: Implement `FactionAlignmentChange`**

Use fields `FactionId factionId`, `int beforeScore`, `int delta`, `int afterScore`, and `String reasonCode`. Reject null ids, zero deltas, inconsistent `beforeScore + delta != afterScore`, and blank reason codes.

- [ ] **Step 3: Implement `FactionAlignmentUpdateResult`**

Use fields `FactionAlignment alignment` and `List<FactionAlignmentChange> changes`. Copy the change list defensively with `List.copyOf`.

- [ ] **Step 4: Implement `FactionAlignmentUpdater`**

Expose `public static FactionAlignmentUpdateResult apply(FactionAlignment alignment, FactionCatalog catalog, FactionId sourceFaction, FactionAlignmentRule rule)`.

For each catalog definition:
- source faction gets `directDelta`.
- factions where `catalog.relation(sourceFaction, candidate)` is `ALLY` get `allyDelta`.
- factions where the relation is `ENEMY` get `enemyDelta`.
- `SELF` and `NEUTRAL` candidates other than the source produce no additional change.
- zero deltas are skipped.

- [ ] **Step 5: Run focused harness**

Run: `javac -d build/test-classes src/main/java/middleearth/lotr/warmod/faction/*.java src/test/java/middleearth/lotr/warmod/faction/FactionAlignmentUpdaterTest.java`

Run: `java -cp build/test-classes;src/main/java middleearth.lotr.warmod.faction.FactionAlignmentUpdaterTest`

Expected: `FactionAlignmentUpdaterTest passed`.

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

Commit message: `Add faction alignment updater`.

Push branch: `codex/alignment-core`.

PR title: `[codex] Add faction alignment updater`.

PR base: `codex/region-catalog`.
