# Foundation Content Seed Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the NeoForge MDK sample with a GPL-compatible Galactic Wars: Clone Wars foundation and a small Middle-earth content seed.

**Architecture:** Keep the main mod class thin and move registrations into focused `registry` classes. Use current NeoForge template APIs already present in the checkout, add manual resource JSON for the first content seed, and avoid copying ARR `recruits` code.

**Tech Stack:** Java 25, Minecraft 26.2, NeoForge 26.2.0.7-beta, Gradle, JSON resources.

---

## Files

- Modify: `gradle.properties`
- Modify: `README.md`
- Modify: `src/main/templates/META-INF/neoforge.mods.toml`
- Modify: `src/main/java/middleearth/lotr/warmod/GalacticWars.java`
- Modify: `src/main/java/middleearth/lotr/warmod/Config.java`
- Modify: `src/main/java/middleearth/lotr/warmod/GalacticWarsClient.java`
- Create: `src/main/java/middleearth/lotr/warmod/registry/ModBlocks.java`
- Create: `src/main/java/middleearth/lotr/warmod/registry/ModItems.java`
- Create: `src/main/java/middleearth/lotr/warmod/registry/ModCreativeTabs.java`
- Modify: `src/main/resources/assets/galacticwars/lang/en_us.json`
- Create: `src/main/resources/assets/galacticwars/blockstates/galactic_stone.json`
- Create: `src/main/resources/assets/galacticwars/blockstates/beskar_ore.json`
- Create: `src/main/resources/assets/galacticwars/blockstates/nightsister_weave_log.json`
- Create: `src/main/resources/assets/galacticwars/models/block/galactic_stone.json`
- Create: `src/main/resources/assets/galacticwars/models/block/beskar_ore.json`
- Create: `src/main/resources/assets/galacticwars/models/block/nightsister_weave_log.json`
- Create: `src/main/resources/assets/galacticwars/models/item/galactic_stone.json`
- Create: `src/main/resources/assets/galacticwars/models/item/beskar_ore.json`
- Create: `src/main/resources/assets/galacticwars/models/item/nightsister_weave_log.json`
- Create: `src/main/resources/assets/galacticwars/models/item/beskar_ingot.json`
- Create: `src/main/resources/data/galacticwars/loot_tables/blocks/galactic_stone.json`
- Create: `src/main/resources/data/galacticwars/loot_tables/blocks/beskar_ore.json`
- Create: `src/main/resources/data/galacticwars/loot_tables/blocks/nightsister_weave_log.json`
- Create: `src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json`
- Create: `src/main/resources/data/minecraft/tags/blocks/mineable/axe.json`
- Create: `src/main/resources/data/minecraft/tags/blocks/needs_iron_tool.json`
- Create: `NOTICE.md`

### Task 1: Red Static Checks

- [ ] **Step 1: Verify MDK sample behavior is currently present**

Run: `rtk rg -n "example_block|example_item|Example Mod Tab|HELLO FROM COMMON SETUP|MAGIC_NUMBER|LOG_DIRT_BLOCK" src gradle.properties`

Expected: Matches exist, proving the checkout still contains MDK sample behavior that the implementation must remove.

- [ ] **Step 2: Verify current license metadata is not GPL-compatible**

Run: `rtk rg -n "mod_license=All Rights Reserved|All rights reserved" gradle.properties src/main/templates/META-INF/neoforge.mods.toml`

Expected: Matches exist, proving the metadata still needs the approved GPL-compatible change.

### Task 2: License And Metadata Foundation

- [ ] **Step 1: Change mod metadata to GPL**

Edit `gradle.properties`:

```properties
mod_license=GPL-3.0-only
```

- [ ] **Step 2: Replace template description and credits**

Edit `src/main/templates/META-INF/neoforge.mods.toml` so the metadata describes the port and credits the upstream LOTR source:

```toml
credits="The Lord of the Rings Mod by Mevans and quentin452/The-Lord-of-the-Rings GPLv3 source fork; recruits by talhanation used as behavior reference only"
authors="KB"

description='''
Galactic Wars: Clone Wars is a NeoForge 26.2 Middle-earth port foundation.
LOTR-derived material is handled under GPLv3-compatible terms.
Army behavior is a clean-room implementation inspired by observed recruit command behavior.
'''
```

- [ ] **Step 3: Add notices**

Create `NOTICE.md` with:

```markdown
# Galactic Wars: Clone Wars Notices

This project is being developed as a GPL-3.0-only compatible NeoForge 26.2 port.

## LOTR-Derived Material

Code or assets ported from `quentin452/The-Lord-of-the-Rings` are derived from GPLv3 source and must preserve GPL-compatible terms and attribution. The upstream fork states it is decompiled, corrected, and refactored code of the Minecraft 1.7.10 "The Lord of the Rings" mod originally developed by Mevans.

## Recruits Reference

`talhanation/recruits` is All Rights Reserved. It is used only as behavioral reference unless explicit permission or a compatible license is provided. Do not copy its source files, assets, UI, packet names, or class structure into this project.
```

### Task 3: Registry Foundation And Content Seed

- [ ] **Step 1: Replace the main mod class**

`GalacticWars.java` registers config and the focused registry classes only. It must not define example blocks or items.

- [ ] **Step 2: Create block registry**

`ModBlocks.java` registers:

- `galactic_stone`
- `beskar_ore`
- `nightsister_weave_log`

- [ ] **Step 3: Create item registry**

`ModItems.java` registers block items for all seed blocks and `beskar_ingot`.

- [ ] **Step 4: Create creative tab registry**

`ModCreativeTabs.java` registers `galactic` with `beskar_ingot` as the icon and displays the seed content.

- [ ] **Step 5: Simplify config**

`Config.java` exposes only startup logging and content seed toggles:

- `logStartup`
- `enableContentSeed`

### Task 4: Resources

- [ ] **Step 1: Replace language entries**

`en_us.json` contains only real mod entries:

- Creative tab name.
- Seed block names.
- Seed item names.
- Config labels.

- [ ] **Step 2: Add blockstates and models**

Create blockstates and models for the three seed blocks. Use vanilla textures for now so the resources load without importing binary assets in this task.

- [ ] **Step 3: Add item models**

Create item models for the three block items and `beskar_ingot`.

- [ ] **Step 4: Add loot tables**

Create snightsister-drop loot tables for the three seed blocks.

- [ ] **Step 5: Add mining tags**

Tag stone/ore for pickaxe mining, nightsister_weave log for axe mining, and beskar ore as needing iron tool level.

### Task 5: Verification

- [ ] **Step 1: Confirm MDK sample identifiers are gone**

Run: `rtk rg -n "example_block|example_item|Example Mod Tab|HELLO FROM COMMON SETUP|MAGIC_NUMBER|LOG_DIRT_BLOCK" src gradle.properties`

Expected: No matches.

- [ ] **Step 2: Confirm seed content identifiers exist**

Run: `rtk rg -n "galactic_stone|beskar_ore|nightsister_weave_log|beskar_ingot|GPL-3.0-only|recruits.*behavior reference" .`

Expected: Matches across Java, resources, metadata, and notices.

- [ ] **Step 3: Compile when Gradle access is available**

Run: `rtk .\gradlew.bat build`

Expected: Build succeeds. If this cannot run due sandbox approval or dependency cache/network restrictions, report it as unverified instead of claiming compile success.
