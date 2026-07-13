# Runtime and Visual QA

This checklist is the repeatable creative-world acceptance pass for player-facing
Galactic Wars content. Run it with the built JAR in the GDLauncher NeoForge 26.2
instance and YACL plus GeckoLib enabled.

## Automated gates

- `clean build --no-daemon`: passed on 2026-07-13.
- `runGameTestServer --no-daemon`: 29/29 required GameTests passed.
- Spawn eggs: all 20 registry entries passed player-game-mode interaction,
  entity-type component, persistence, loadout/archetype, creative/survival,
  replaceable-block, custom-name, stack-consumption, and spawner checks.
- Assets: every authored item model has a modern item definition and every
  inherited model texture resolves to a PNG.
- Client log: no `galacticwars` missing-model or missing-texture warning.

## Lightsaber acceptance

The six colors inherit one seventeen-part hilt/blade geometry and override only the
hilt and animated blade textures. The first-person extension preserves the
vanilla hand transform and adds only the ready/slash offset.

The rebuilt visual language was developed from the original project-owned
reference in `lightsaber-design-reference.png`, generated for this repository
on 2026-07-13. The shipped 32-pixel material atlases are deterministic original
pixel art produced by `tools/generate-lightsaber-textures.ps1`; no third-party
mod textures or geometry are included.

| Context | Result | Evidence |
| --- | --- | --- |
| Creative inventory/GUI | Pass | `lightsaber-gui.png` |
| First person | Pass | `lightsaber-first-person.png` |
| Third person | Recheck after pose/transform changes | Capture front and back |
| Dropped item | Recheck on a clear stone floor | Capture after one animation cycle |
| Item frame | Recheck on a plain wall | Capture fixed transform |

## Content audit order

Use daylight, a plain stone floor, hitboxes off, and the same camera distance.
Capture idle, walk, attack/held-item, and seated views where applicable.

1. NPCs: all 20 egg variants; verify body scale, seams, surfaces, held items,
   armor, idle/walk/attack clips, and neutral ownership.
2. Vehicles: BARC speeder, AT-RT, STAP, AAT, and LAAT gunship; verify kit icon,
   deployed model, collision box, rider seat, and animation.
3. Weapons and armor: all lightsabers, blasters, tools, and faction armor.
4. Blocks and kits: command/logistics blocks, beacons, transponders, deployment
   kits, and environmental blocks in GUI, placed, dropped, and framed views.

Any missing `galacticwars` model/texture line in `latest.log` is a release
failure, even if Minecraft substitutes a fallback model.
