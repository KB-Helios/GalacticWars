# Galactic Wars: Clone Wars

Galactic Wars: Clone Wars is an unofficial, GPL-3.0-only NeoForge 26.2 fan mod focused on faction armies, working settlements, planetary travel, vehicles, Force progression, quests, trading, and conquest.

This is a clean-break conversion. Existing KingdomWars-Middle-Earth worlds and registry IDs are not compatible; start a fresh world.

## Installation

1. Install the matching Minecraft 26.2 and NeoForge 26.2 client or dedicated server.
2. Place the Galactic Wars JAR, GeckoLib 5.5.3+, SmartBrainLib 2.0.x, and Framework 0.13.x in the instance's `mods` folder.
3. Install YACL 3.9.5 or newer compatible 26.2 build on clients for the in-game configuration screen. Dedicated servers do not require YACL.
4. Remove earlier KingdomWars-Middle-Earth builds from that folder and create a fresh world.
5. Keep the same Galactic Wars, GeckoLib, SmartBrainLib, and Framework versions on every multiplayer client and server.

## Requirements

- Minecraft 26.2
- NeoForge 26.2.0.7-beta or newer compatible 26.2 build
- Java 25
- GeckoLib 5.5.3 or newer compatible release
- SmartBrainLib 2.0.x for NeoForge 26.2
- MrCrayfish's Framework 0.13.x for NeoForge 26.2
- YACL 3.9.5 or newer compatible 26.2 release (client only)

## Controls

- Place a Command Center to open the faction picker. Choosing a faction atomically creates and pledges your kingdom; the Command Center, recruitment rules, alignment, and progression all use that saved choice.
- Interact with a recruit to open the command screen. Hire first, then choose combat orders, formations, a worksite, storage, a profession, or construction controls.
- Use a blaster to fire; each accepted shot consumes one Energy Cell, applies weapon durability and a short server-authoritative cooldown.
- Place blueprint structures through the recruit command screen. Builders withdraw real blocks from Command Center or linked storage before placing them.
- After pledging and completing a Forward Base, sneak-use your active Command Center to open planetary navigation. Craft and carry a Hyperspace Navigator to reopen the console away from home. Every jump still requires your active Command Center and paid upkeep, and prepares a reusable non-destructive arrival platform.
- Vehicle and Force bindings are tracked below and are not presented as playable controls until their runtime milestones are complete.

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
- [ ] Implement drivable runtime entities, controls, persistence, and combat for all five vehicles; authoritative ownership, allied boarding, seat limits, fuel, damage/destruction, and safe dimension-transfer policies are implemented and tested.
- [ ] Implement runtime Force input, targeting, persistence/sync, effects, and accessibility feedback; unlock validation, path/quest gates, energy, cooldown replay protection, regeneration, and PvP policy are implemented and tested.
- [ ] Load the fifteen quest, trade, vehicle, planet, Force, and conquest definitions through the atomic datapack manager and persist player progress.
- [ ] Connect conquest capture zones to faction spawns, merchant stock, travel safety, rewards, and protected-build rules.
- [ ] Complete multiplayer balance, accessibility, visual QA, dedicated-server testing, GameTests, and a fresh-world acceptance playthrough.

Post-release candidates: Dathomir, Naboo, Kashyyyk, additional unit families, more vehicles, free-flight space combat, capital ships, bosses, longer campaigns, and localization.

## Development

```powershell
.\gradlew.bat clean build
.\gradlew.bat runHarnesses
.\gradlew.bat runGameTestServer
```

Executable `*Test.java` harnesses are part of `check`. Generated or edited textures must satisfy `docs/galacticwars-asset-manifest.json`; generation provenance is recorded in `docs/galacticwars-asset-provenance.md`.

## Licensing and fan-project notice

This is an unofficial fan project and is not affiliated with or endorsed by Lucasfilm or Disney. Star Wars names identify the fan setting; newly introduced code, textures, models, and writing are project-owned originals. GPL-derived historical code remains GPL-3.0-only and its required attribution is retained in `NOTICE.md`.

`talhanation/recruits` remains a behavioral reference only. Its code and assets are not included.
