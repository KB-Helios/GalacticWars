# GeckoLib Animation Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the GeckoLib dependency foundation for future animated Middle-earth entities on NeoForge 26.2.

**Architecture:** Keep this PR limited to dependency and metadata wiring. Future entity renderers and animated models will build on this after the dependency is available and declared consistently.

**Tech Stack:** NeoForge 26.2, Gradle ModDev, GeckoLib 5.5.3 for NeoForge 26.2, Java 25, lightweight `javac`/`java` metadata harness.

---

## Files

- Create: `src/test/java/middleearth/lotr/warmod/integration/GeckoLibDependencyMetadataTest.java`
- Modify: `gradle.properties`
- Modify: `build.gradle`
- Modify: `src/main/templates/META-INF/neoforge.mods.toml`
- Modify: `NOTICE.md`
- Modify: `README.md`

### Task 1: Red Metadata Test

- [ ] **Step 1: Add GeckoLib metadata harness**

Create `GeckoLibDependencyMetadataTest` with a `main` method that checks:

- `gradle.properties` declares `geckolib_version=5.5.3`.
- `build.gradle` declares the GeckoLib Cloudsmith Maven repository.
- `build.gradle` declares `implementation "software.bernie.geckolib:geckolib-neoforge-${minecraft_version}:${geckolib_version}"`.
- `neoforge.mods.toml` declares a required `geckolib` dependency with `${geckolib_version}` in the version range.
- `NOTICE.md` and `README.md` mention GeckoLib.

- [ ] **Step 2: Run red compile and test**

Run:

```powershell
javac -d build\geckolib-metadata-test-classes src\test\java\middleearth\lotr\warmod\integration\GeckoLibDependencyMetadataTest.java
java -cp build\geckolib-metadata-test-classes middleearth.lotr.warmod.integration.GeckoLibDependencyMetadataTest
```

Expected: compile succeeds and the test fails because GeckoLib metadata is not wired yet.

### Task 2: Dependency And Metadata Wiring

- [ ] **Step 1: Add version property**

Add `geckolib_version=5.5.3` to `gradle.properties`.

- [ ] **Step 2: Add GeckoLib Maven repository**

Add a `maven` block named `GeckoLib` to `repositories` with URL `https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/` and group filter `software.bernie.geckolib`.

- [ ] **Step 3: Add NeoForge GeckoLib dependency**

Add:

```groovy
implementation "com.geckolib:geckolib-neoforge-${minecraft_version}:${geckolib_version}"
```

- [ ] **Step 4: Add mod metadata dependency**

Add `geckolib_version` to the metadata replacement map and declare `geckolib` as a required dependency in `src/main/templates/META-INF/neoforge.mods.toml`.

- [ ] **Step 5: Document third-party dependency**

Update `NOTICE.md` and `README.md` to explain GeckoLib is MIT-licensed and used for animated entities.

### Task 3: Verification

- [ ] **Step 1: Run GeckoLib metadata harness**

Run:

```powershell
javac -d build\geckolib-metadata-test-classes src\test\java\middleearth\lotr\warmod\integration\GeckoLibDependencyMetadataTest.java
java -cp build\geckolib-metadata-test-classes middleearth.lotr.warmod.integration.GeckoLibDependencyMetadataTest
```

Expected: prints `GeckoLibDependencyMetadataTest passed`.

- [ ] **Step 2: Run pure Java regression harnesses**

Run the existing army/faction pure Java harnesses.

Expected: all harnesses print their `passed` messages.

- [ ] **Step 3: Static checks**

Run:

```powershell
rtk rg -n "geckolib|GeckoLib|geckolib_version|geckolib-neoforge" gradle.properties build.gradle src\main\templates\META-INF\neoforge.mods.toml NOTICE.md README.md src\test\java\middleearth\lotr\warmod\integration\GeckoLibDependencyMetadataTest.java
rtk rg -n "talhanation|AbstractRecruit|RecruitEntity|PatrolLeader|package com\.talhanation|net\.talhanation" src\main\java src\test\java
git diff --check
```

Expected: first command finds the new dependency metadata; second command finds no upstream class/package names; diff whitespace check exits 0.
