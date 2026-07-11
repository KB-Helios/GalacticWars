# Galactic Wars: Clone Wars Port Design

Date: 2026-06-28

## Purpose

Port Galactic Wars: Clone Wars from the current NeoForge 26.2 template toward a full Middle-earth mod that uses LOTR-derived content and clean-room army behavior inspired by `talhanation/recruits`.

The target is not a one-shot source dump. The target is a phased port where each phase leaves the mod buildable and playable in a limited form while moving toward the full requested feature set.

## Source Inputs

### Local Template

- Project path: `C:\Users\kevin\Desktop\Programmering\Projekt\MIddle-earth-renewed`
- Current state: small NeoForge 26.2 MDK-style template.
- Target stack: Minecraft `26.2`, NeoForge `26.2.0.7-beta`, Java 25.
- Mod ID: `galacticwars`.
- Current metadata still says `mod_license=All Rights Reserved`; this must change before LOTR-derived code or assets are ported.

### LOTR Upstream

- Repository: `quentin452/The-Lord-of-the-Rings`
- Source stack: Minecraft 1.7.10, Forge 10.13.4.1614, Java 8 / RetroFuturaGradle.
- License: GPLv3.
- Scope found during research: thousands of source and resource files, including blocks, items, equipment, dimensions, biomes, structures, factions, alignment, NPCs, quests, trading, networking, commands, GUI, rendering, sound, and old coremod behavior.

LOTR-derived code or assets require this project to be GPLv3-compatible and require preservation of notices/source availability.

### Recruits Upstream

- Repository: `talhanation/recruits`
- Source stack: Minecraft 1.20.1, Forge 47.4.10, Java 17.
- License: All Rights Reserved.
- Scope found during research: recruit entities, groups, follow/hold/move/protect behavior, formations, hiring, commands, inventory/equipment, ranged combat, morale/hunger/upkeep, patrol leaders, factions, claims, diplomacy, GUI, networking, compatibility layers, and mixins.

Because this source is All Rights Reserved, it will be used only as behavioral reference unless separate permission or a compatible license is provided. Do not copy or mechanically port its source files, assets, packet names, UI, or class structure.

## Approved Approach

Use a GPL LOTR foundation plus a clean-room army layer.

This means:

- Convert metadata and project notices to GPLv3-compatible before importing LOTR-derived material.
- Port LOTR-derived content/assets legally and incrementally.
- Rebuild `recruits`-style army behavior from observed behavior and target requirements, not by copying its implementation.
- Prefer modern NeoForge 26.2 APIs over compatibility shims for old Forge APIs.
- Keep every phase compiling and runnable before expanding scope.

## Architecture

The codebase should be split into focused modules:

- `core`: mod constants, logging, common setup, creative tabs, configuration, and event registration.
- `registry`: DeferredRegister classes for blocks, items, entities, sounds, menus, effects, tabs, and future custom registries.
- `content`: LOTR-derived blocks, items, materials, foods, equipment, and generated resource helpers.
- `world`: Middle-earth dimension, biomes, configured/placed features, structure hooks, and worldgen data.
- `faction`: faction definitions, faction relations, alignment rules, saved data, commands, and later conquest hooks.
- `army`: clean-room recruit entities, command state, group state, hiring, AI goals, combat roles, and formations.
- `network`: small explicit packets for commands and GUIs, with server-authoritative validation.
- `client`: client-only rendering, screens, model registration, particles, audio, overlays, and later map UI.
- `data`: datagen providers for models, blockstates, loot, recipes, tags, language, biome/dimension JSON, and generated migration outputs.

Avoid a single large mod class. The template sample blocks/items/config should be removed once real registries are introduced.

## Phases

### Phase 1: Foundation

Deliver a buildable NeoForge 26.2 base with:

- GPLv3-compatible metadata and notices.
- Clean package layout and registry classes.
- Removed MDK sample behavior.
- Real creative tabs and baseline config.
- Datagen wiring.
- A minimal build/run verification path.

### Phase 2: LOTR Content Seed

Port a small representative LOTR-derived content slice:

- Several stone, ore, and wood blocks.
- One material path for tools/weapons.
- One armor/material path.
- Basic generated models, blockstates, recipes, loot, tags, and language.
- Namespaced assets under `galacticwars`, with attribution and notices.

This phase proves the content pipeline before scaling to hundreds of blocks/items.

### Phase 3: Middle-earth World Seed

Add a minimal `galactic` dimension and initial biomes using modern data-driven worldgen.

Include:

- Dimension and dimension type data.
- A small biome set.
- Simple terrain/noise settings.
- A few placed/configured features from the content seed.

Do not attempt the full 1.7.10 genlayer/map/roads/structure system in this phase.

### Phase 4: Faction And Alignment Core

Implement the state needed to connect LOTR gameplay and army behavior:

- Faction definitions.
- Faction relations.
- Per-player alignment saved data.
- Basic commands or debug tools for inspecting/changing alignment.
- Server-side alignment update rules.

This becomes the base for NPC behavior, hiring requirements, trading, conquest, and army allegiance.

### Phase 5: Army Core

Implement clean-room army behavior inspired by `recruits`:

- Basic recruit entity.
- Owner UUID and owned flag.
- Group UUID.
- Aggro state.
- Follow state.
- Hold, move, protect, follow, attack, and clear-target commands.
- Hiring flow with cost and max recruit limits.
- Melee combat and wander behavior.
- Synced entity data and NBT persistence.

Keep this first army slice server-authoritative and command-driven. Do not copy source from `recruits`.

### Phase 6: Combat Expansion

Expand army behavior after the core is stable:

- Inventory and equipment.
- Group-scoped commands.
- Basic formations.
- Ranged units.
- Ranged-fire toggle.
- Strategic fire.
- Morale, hunger, upkeep, and payment.
- Patrol leaders.
- Simple army tactical controller and retreat logic.

### Phase 7: Feature Backlog

Port or rebuild larger LOTR systems in dependency order:

- NPC families and faction unit sets.
- Trading.
- Quests.
- Structures and villages.
- Waypoints and map UI.
- Fast travel.
- Conquest.
- Custom rendering, audio, sky, and weather.
- Utumno.
- Any needed replacement for old coremod behavior.
- Mounts, ships, siege weapons, and optional compatibility layers.

These are intentionally deferred because the upstream implementations depend heavily on legacy Forge/1.7.10 APIs or ARR `recruits` code.

## Data Flow

Static content should flow through NeoForge registries and generated resources:

- Blocks and items.
- Entities and attributes.
- Creative tabs.
- Sounds.
- Menus.
- Recipes, tags, loot, models, blockstates, and language.
- Biome, dimension, feature, and structure data.

Runtime state should be split by ownership:

- Per-world saved data: faction definitions, faction relations, generated Middle-earth progression flags, and army group records.
- Per-player saved data: alignment, pledges, unlocked fast travel/waypoints later, and recruit limits.
- Per-entity synced data: recruit owner UUID, group UUID, aggro state, follow state, hold/move/protect targets, morale, hunger, and later equipment state.
- Networking: small explicit packets for commands and GUIs only.

Army command flow:

1. Client screen or server command sends a command intent.
2. Server validates player ownership, group membership, target entity IDs, and target positions.
3. Server updates recruit or group state.
4. Entity AI goals react to state on tick.
5. Minimal visual state syncs back to tracking clients.

The server remains authoritative for hiring, movement commands, combat state, alignment changes, and faction effects.

## Error Handling

During development, fail loudly where broken data would make later debugging expensive. In game runtime, reject invalid inputs safely.

Required handling:

- Missing registry entries should include namespace/path and the registry type in logs.
- Malformed faction definitions should log the resource ID and skip only the invalid definition when safe.
- Invalid biome/dimension data should fail datagen/build validation before runtime where possible.
- Bad packet payloads should be rejected without changing server state.
- Invalid recruit ownership, stale group IDs, or stale entity IDs should return a clear server-side message and leave existing state untouched.
- Imported LOTR resources should be validated in batches so bad assets do not hide among thousands of files.
- Client-only classes must stay out of dedicated-server paths.

## Testing And Verification

Every phase should end with verification proportional to its scope:

- `gradlew build`
- Datagen once providers exist.
- Client run smoke test for registry/resource loading.
- Server run smoke test for dedicated-server safety.
- Focused GameTests where practical.

High-value GameTests:

- Saved data load/save for factions, alignment, and army groups.
- Faction relation and alignment update rules.
- Recruit hiring state transition.
- Recruit follow/hold/move/protect command state transitions.
- Ownership validation for recruit commands.

The first implementation plan should define done as:

- The mod builds on NeoForge 26.2.
- MDK sample behavior is removed.
- GPL metadata and attribution are in place.
- A small playable vertical slice exists.

It must not claim full LOTR parity until the backlog systems are actually implemented and verified.

## Non-Goals For The First Implementation Plan

- Full 694-block and 822-item LOTR parity.
- Full 444-NPC LOTR entity set.
- Full 1.7.10 map/genlayer recreation.
- Full structure/village generation.
- Full quest/trading/fellowship/conquest system.
- Full custom sky/weather/music/rendering system.
- Copying `recruits` source code.
- Recreating old coremods before proving no modern API or scoped mixin can replace them.

## Risks

- The LOTR upstream is GPLv3 and derived from decompiled/refactored 1.7.10 code. The project must stay GPL-compatible if importing it.
- The `recruits` upstream is All Rights Reserved. It is reference-only unless permission changes.
- NeoForge 26.2 targets Java 25 and modern Minecraft APIs. Both upstreams require substantial rewrite rather than direct compile fixes.
- Large resource migration can create silent missing-model or missing-lang issues unless datagen and validation are introduced early.
- Army AI can become unstable if group state, entity state, and network commands are not kept server-authoritative.

## Open Constraint

The current workspace is not a Git repository. The brainstorming process asks for the design document to be committed, but `git status` currently fails with "not a git repository". The design can be written and reviewed now; committing requires initializing this folder as a repository or moving the work into an existing repository.
