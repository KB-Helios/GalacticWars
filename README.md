# Galactic Wars: Clone Wars

Galactic Wars: Clone Wars is an unofficial, GPL-3.0-only Architectury mod for Minecraft 26.2 on Fabric and NeoForge. It focuses on faction armies, working settlements, planetary travel, vehicles, Force progression, quests, trading, and conquest.

This is a clean-break conversion. Existing KingdomWars-Middle-Earth worlds and registry IDs are not compatible; start a fresh world.

## Installation

1. Install Java 25 and Minecraft 26.2 with either Fabric Loader 0.19.3+ or NeoForge 26.2.0.25-beta+.
2. Install the matching `galacticwars-fabric` or `galacticwars-neoforge` JAR.
3. Install the loader-matched Architectury API 21.0.4+, GeckoLib 5.5.3+, SmartBrainLib 2.0.x, and YACL 3.9.5+ builds. Fabric also requires Fabric API and Fabric Language Kotlin; NeoForge requires Kotlin for Forge.
4. Remove earlier KingdomWars-Middle-Earth builds and start a fresh world.
5. Keep the same loader, Galactic Wars build, and dependency versions on every multiplayer client and server.

## Requirements

- Minecraft 26.2
- Java 25
- Architectury API 21.0.4 or newer compatible release
- Fabric Loader 0.19.3, Fabric API 0.155.2+26.2, and Fabric Language Kotlin 1.13.13+kotlin.2.4.10; or NeoForge 26.2.0.25-beta and Kotlin for Forge 6.3.0
- GeckoLib 5.5.3 or newer compatible release
- SmartBrainLib 2.0.x for the selected loader
- YACL 3.9.5 or newer compatible 26.2 release for the selected loader

## Controls

- Place a Command Center to open the faction picker. Choosing a faction atomically creates and pledges your kingdom; the Command Center, recruitment rules, alignment, and progression all use that saved choice.
- Interact with a recruit to open the command screen. Hire first, then choose combat orders, formations, a worksite, storage, a profession, or construction controls.
- Use a blaster to fire; each accepted shot consumes one Energy Cell, applies weapon durability and a short server-authoritative cooldown.
- Interact with an active Command Center for the keyboard-focusable Overview, Campaign, Construction, Squads, Workforce, Kingdom, Diplomacy, and Storage dashboard. Sneak-interact for planetary navigation.
- Craft a Tactical Command Marker and use it on an entity or block before issuing explicit attack, move, rally, patrol, worksite, or storage orders. Prepare a Blueprint Projector in the Construction tab, then place the projection in-world; builders withdraw real blocks from Command Center or linked storage.
- Press `G` to open the rebindable Field Command screen when you are near your squads. Select up to eight commandable squads and issue battlefield movement, combat, formation, and patrol orders through the same server-authoritative army state used by the Command Center.
- After pledging and completing a Forward Base, sneak-use your active Command Center to open planetary navigation. Craft and carry a Hyperspace Navigator to reopen the console away from home. Every jump still requires your active Command Center and paid upkeep, and prepares a reusable non-destructive arrival platform.
- Deploy a fabricated vehicle kit in-world. Use movement keys to drive, Jump/Crouch to climb or descend in flight vehicles, and `R` to fire the mounted weapon. Use an Energy Cell to refuel and Duracrete to repair an authorized vehicle.
- Republic and Nightsister campaign paths activate Force abilities with `Z`, `X`, and `C`. The HUD reports energy and per-slot cooldowns; targeting, costs, replay protection, PvP policy, and effects are server authoritative.

## Core gameplay loop

1. Build a Command Center and pledge to the Republic, Separatists, Mandalorians, Hutt Cartel, or Nightsisters.
2. Earn physical Credit Chips through faction objectives, work, exploration, and trading.
3. Recruit soldiers, droids, specialists, and workers. Give squads follow, hold, move, protect, attack, patrol, and formation orders.
4. Assign workers to real farms, salvage zones, mines, storage, deliveries, and construction projects.
5. Complete a Forward Base, Barracks, Supply Depot, Moisture Farm, Salvage Yard, and Mine to expand housing, logistics, travel, vehicles, and command capacity.
6. Travel between Tatooine, Geonosis, Kamino, and Coruscant; complete faction chapters and unlock vehicles or a light/dark Force path.
7. Trade with aligned merchants and capture regional objectives without overwriting protected player builds.

Credits, unlocks, quest milestones, vehicles, Force choices, and conquest use an idempotent server-side progression coordinator: duplicate packets or reconnects must not duplicate charges or rewards.

## Launch content contract

- 5 factions and 15 unit definitions: three per faction.
- 4 separate planet dimensions.
- BARC Speeder, AT-RT, STAP, AAT, and LAAT Gunship definitions.
- Light Force path: Push, Pull, Leap. Dark path: Push, Choke, Dash.
- 15 quests arranged as one three-chapter campaign per faction.
- Credit-based recruitment, treasury, profession training, trading, vehicle acquisition, and upkeep.
- Original project-owned textures only. No official Star Wars or third-party mod assets are bundled.

## Roadmap to the completed release

- [x] Rename the mod, Java packages, mixins, resources, datapacks, tests, and artifact namespace to `galacticwars`.
- [x] Convert factions, Command Center gameplay, settlement blueprints, Beskar, Credit Chips, and atomic payment/refund behavior.
- [x] Complete the manifest-driven original art pass for five faction kits, fifteen units, planets, combat effects, GUI, and five vehicle UV atlases, with provenance and executable asset validation.
- [x] Register and data-drive all fifteen launch-unit IDs while retaining formation, command, hiring, worker, commander, and persistence foundations.
- [x] Finish player-facing blaster heat feedback and autonomous ranged AI goals: held blasters expose a synchronized segmented heat HUD and action-bar failures, player fire is server-authoritative, and grouped or local-order recruits use blasters and Nightsister bows without bypassing cooldown, PvP, faction, or owner protections.
- [x] Finish planetary respawn and cross-dimension squad transfer: successful jumps bind respawn to the reusable safe-arrival platform, and nearby follow/protect squads are preflight-snapshotted, transactionally virtualized, relocated, and rematerialized without leaving source duplicates or corrupting the group when teleport fails.
- [x] Implement drivable runtime entities, controls, persistence, combat, repair, and destruction for all five vehicles with authoritative ownership, allied boarding, seat limits, fuel, and dimension-transfer policy.
- [x] Implement runtime Force input, targeting, persistence/sync, effects, HUD feedback, unlock gates, energy, cooldown replay protection, regeneration, allied-NPC protection, and PvP policy.
- [x] Load the fifteen quest, trade, vehicle, planet, Force, and conquest definitions through the atomic datapack manager and persist player progress.
- [x] Connect physical conquest capture to controlling-faction patrols, regional merchant stock, arrival safety, rewards, and protected-build placement.
- [x] Complete automated dedicated-server, two-player authority, replay, persistence, codec, content-contract, and 47-GameTest runtime gates.
- [x] Record the fresh-client Command Center visual acceptance pass and verify every faction campaign through the five-path runtime matrix.

Post-release candidates: Dathomir, Naboo, Kashyyyk, additional unit families, more vehicles, free-flight space combat, capital ships, bosses, longer campaigns, and localization.

## Development

```powershell
.\gradlew.bat clean buildAll
.\gradlew.bat runHarnesses
.\gradlew.bat runGameTestServer
.\gradlew.bat :fabric:runClient
.\gradlew.bat :neoforge:runClient
```

`buildAll` produces loader-specific JARs under `fabric/build/libs` and `neoforge/build/libs`. Executable `*Test.java` harnesses are part of `check`; NeoForge hosts the shared Minecraft GameTest suite. Generated or edited textures must satisfy `docs/galacticwars-asset-manifest.json`, and generation provenance is recorded in `docs/galacticwars-asset-provenance.md`.

## Licensing and fan-project notice

This is an unofficial fan project and is not affiliated with or endorsed by Lucasfilm or Disney. Star Wars names identify the fan setting; newly introduced code, textures, models, and writing are project-owned originals. GPL-derived historical code remains GPL-3.0-only and its required attribution is retained in `NOTICE.md`.

Authorized derivative code from the local `talhanation/recruits` checkout is used only where recorded in `docs/authorized-source-intake.md`; each retained or adapted unit remains provenance-tracked. Its legacy networking, global managers, medieval assets, and unrelated gameplay systems are not included.
