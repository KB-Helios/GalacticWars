# Repository Guidelines

## Product Direction

This repository is now **Galactic Wars: Clone Wars**, an unofficial Star Wars fan mod and a first-hand battle simulator. The player should remain physically present in the world: fighting beside recruited troops, issuing orders on the battlefield, building and supplying bases, assigning workers, and travelling with squads. Use Villager Recruits and Ancient Warfare as high-level behavioral references for embodied NPC command, logistics, and settlement play—not as code or asset sources. Favor playable in-world interactions over detached grand-strategy screens or menu-only automation. Army, economy, progression, planet, vehicle, and Force systems should reinforce this recruit-command-build-fight loop.

## Project Structure & Module Organization

This is a Java 25 NeoForge mod. Production code lives in `src/main/java/galacticwars/clonewars`, grouped by gameplay domain such as `army`, `combat`, `faction`, `kingdom`, `progression`, `settlement`, `world`, and `workforce`. Mod metadata is generated from `src/main/templates`; assets, datapacks, mixins, models, textures, and localization belong under `src/main/resources`. Keep new resource and registry paths namespaced with `galacticwars`. Test harnesses mirror production packages in `src/test/java`; runtime GameTests live in `src/main/java/galacticwars/clonewars/gametest`. Design history is stored in `docs`; older Middle-earth plans are historical context, not current product direction. Treat `build/`, `.gradle/`, and `run/` as generated local state.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper (Windows examples shown):

- `.\gradlew.bat build` compiles sources, processes resources, and runs the standard verification lifecycle used by CI.
- `.\gradlew.bat runHarnesses` runs all dependency-light executable test classes and is included in `check`/`build`.
- `.\gradlew.bat runClient` launches a development Minecraft client.
- `.\gradlew.bat runServer` starts a local server with `--nogui`.
- `.\gradlew.bat runData` regenerates data into `src/generated/resources`.
- `.\gradlew.bat runGameTestServer` launches NeoForge GameTests when registered.

Unix-like environments should use `./gradlew` instead.

## Coding Style & Naming Conventions

Follow the existing Java style: four-space indentation, UTF-8, braces on the declaration line, and one public top-level type per file. Use `UpperCamelCase` for classes and records, `lowerCamelCase` for methods and variables, and lowercase package names. Prefer small domain types and immutable value objects. Name resource files and registry identifiers in lowercase `snake_case`; keep Java packages aligned with their directories. No formatter or linter is configured, so match nearby code and keep imports organized.

## Testing Guidelines

Most tests are dependency-light executable classes named `*Test.java`, with a `public static void main` and explicit assertions; they are not JUnit tests. Add them beside the matching domain package and print `<ClassName> passed` on success. Use GameTests for Minecraft runtime behavior that pure harnesses cannot prove. Run focused harnesses while iterating, then `.\gradlew.bat build`; run `.\gradlew.bat runGameTestServer` for entity AI, menus, combat, travel, persistence, or world interaction changes. Do not describe a catalog, policy, or source-presence test as completed gameplay until its player-facing runtime path is wired and tested.

## Commit & Pull Request Guidelines

Recent commits use short, imperative subjects such as `Add atomic squad planet travel` and `Add playable blaster heat and ranged AI`. Keep each commit focused. Pull requests should explain player-facing behavior and scope, list verification commands, and include screenshots for GUI, model, or texture changes. Clearly distinguish playable runtime features from data/model foundations and deferred milestones. Ensure CI’s Java 25 Gradle build passes.

## Licensing & Source Use

Follow `NOTICE.md` and keep GPL-derived historical code GPL-3.0-only compatible. This is an unofficial fan project: do not import official Star Wars assets or unlicensed third-party mod code/assets. New art and writing must be original and provenance-tracked. Treat Villager Recruits, Ancient Warfare, and `talhanation/recruits` only as behavioral references unless explicit compatible permission exists.
