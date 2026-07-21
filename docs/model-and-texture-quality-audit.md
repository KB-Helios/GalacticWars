# Model and Texture Quality Audit

Date: 2026-07-20

## Findings

| Asset family | Evidence in the checkout | Decision |
| --- | --- | --- |
| Lightsabers | The former model was a vanilla `elements` item with 17 cuboids. Its blade was 18.2 model units against an 8.3-unit hilt, and every hilt face sampled a generic 32x32 strip. | Replace the renderer, model, UVs, materials, and glow path with a GeckoLib item. |
| Vehicles | The five models already used GeckoLib and 256x256 authored atlases, but only contained 6-11 cuboids each. Large areas such as the BARC chassis, AAT hull, and LAAT fuselage were single boxes. | Preserve the material atlases and animation bones; rebuild silhouettes with additional UV-safe detail cubes. |
| Recruits and civilians | The former twenty GeckoLib models used 128x128 box-UV atlases, only 12-28 cubes, the same eight base bones, and byte-identical animations. The first 256x256 generated overhaul improved the contract but its clone, B1, and Mandalorian art failed visual review. | Keep family-specific animation rigs. Replace the rejected families with permissioned authored art from the two pinned upstream repositories, preserving each source atlas and explicit UV layout. |
| Equipped armor | Five GeckoLib armor models used generic layered cuboids despite explicit six-face UVs on 1024x1024 atlases. The first generated Republic replacement still did not read as high-quality clone armor. | Use the Galaxies selectable Phase I/Phase II Blockbench clone kit for both NPC and wearable geometry, with the permissioned Forge 501st texture supplying Phase II blue. Preserve the authored 128x128 clone atlas rather than upscaling it without detail. |
| Recruit spawn eggs | Twenty items used a shared capsule presentation. | Extend the established capsule contract and unit-specific materials to both Phase I units. |
| Inventory sprites | Combat icons are intentionally 16x16 for Minecraft readability. The old saber icons emphasized the hilt and visually shortened the blade. | Redraw only the six saber icons with a longer blade silhouette. |

## Implemented quality gate

- Twenty-six NPCs use explicit six-face UV mappings. Nine project-authored sets retain the
  deterministic 256x256/two-texel contract. Seventeen permissioned sets preserve source-appropriate
  contracts: 128x128 clones, Mandalorians, guards, and Trandoshan; 128x64 B1 variants; and 256x256
  non-overlapping Togruta, Duros, and Rodian composites.
- Clone, ARC, droid, robed, brute, Mandalorian, and civilian families own child rigs and distinct
  idle, walk/run, and attack data. Regular and ARC clones intentionally share their phase texture
  because their silhouettes differ through authored equipment geometry.
- Existing `clone_trooper` and `arc_trooper` resources are Phase II/501st variants. New Phase I clone
  and ARC units use the source model's separate helmet, equipment, spawn-capsule, and 128x128 wearable
  armor resources. ARC variants enable rangefinder, pauldron, and kama geometry.
- Canonical-role dimensions are enforced for the 1.93-block B1 and 1.91-block BX; the Hutt enforcer and Separatist technician hitboxes were realigned to their rebuilt silhouettes.
- The Python character pipeline runs the permissioned-source importer last and remains deterministic.
  Both obsolete Java texture generators were removed so they cannot restore 64x64 armor or NPC textures.

- Lightsabers now use one 24-cuboid GeckoLib geometry with separate `hilt` and `blade` bones.
- The energy blade is 36 model units long and the visible blade-to-hilt ratio is approximately 3.4:1.
- The hilt now straddles the GeckoLib item origin and uses sword-like first- and third-person transforms,
  so the hand meets the grip instead of the pommel floating above it.
- Six four-frame 256x1024 UV atlases and matching emissive glowmasks replace the generic split hilt/blade strips.
- Minecraft item definitions use `minecraft:special` plus `geckolib:geckolib`; the Java item implements `GeoItem` and renders through `GeoItemRenderer`.
- Equipped armor uses 24/27 cuboids for permissioned Phase I/Phase II clone sets and 56/54/33/54
  cuboids for the project-authored Separatist, Mandalorian, Nightsister, and Beskar sets. The clone
  geometry uses visibly different authored helmet groups and the ARC NPCs add specialist equipment.
- Clone wearable armor preserves the source 128x128 atlas and source equipment sprites. The other
  four armor families retain their 1024x1024 exact face maps.
- All twenty-six recruit eggs now use one eighteen-cuboid GeckoLib capsule with separate `shell` and
  animated `core` bones, individual 512x512 material/glowmask pairs, and 16x16 fallback capsule icons.
- `SpawnCapsuleAssetIntegrationTest` protects the full set of item definitions, shared geometry,
  animation, renderer/glow layer, per-unit material atlases, and preserved vanilla spawn binding.
- Vehicle cube counts increased from 6/10/6/6/11 to 20/28/20/23/40 for BARC, AT-RT, STAP, AAT, and LAAT respectively. Existing root, leg, turret, wing, engine, steering, and weapon bones remain unchanged, so their GeckoLib animations continue to target the same contract.
- `ModelAssetQualityTest` prevents the long blade, GeckoLib bones, 256x256 vehicle atlas contract, and minimum vehicle silhouette detail from regressing.

The 2026-07-13 development-client continuation equipped all four pieces of every armor family. Republic
plastoid rendered as a white-and-blue clone-infantry silhouette with a horizontal visor, vertical nose
slit, grille, cheek plates, fin, and ear modules; Separatist, Mandalorian, Nightsister, and Beskar sets
rendered with distinct bronze mechanical, teal plate, black/crimson hooded, and silver heavy identities.
The blue saber was inspected in first and third person: the hand meets the segmented hilt and the long
blade projects from the emitter without the former floating-pommel alignment. The Republic recruit capsule
also rendered as a stepped 3D object in first and third person. GeckoLib loaded 32 models and 32 animations,
and the client log contained no Galactic Wars model, texture, animation, or renderer-loading error.

Local evidence screenshots are retained under `run/screenshots`: `2026-07-13_23.46.12.png` through
`2026-07-13_23.46.58.png` cover the five equipped armor families, while
`2026-07-13_23.47.17.png` and `2026-07-13_23.47.18.png` show the first-person saber and capsule.

## 2026-07-21 Clone Wars 3D visual overhaul

- The deterministic turnaround covers all 26 NPCs, three commanders, six armor families, five
  vehicles, six lightsabers, four blasters, and representative capsules in front/side/back views,
  applying cube rotations and the bone hierarchy.
- Species imports now add Togruta head-tails/montrals, Duros cranium/brow, Rodian muzzle/antennae, and
  Trandoshan muzzle/brow landmarks. Weak project-authored characters now use role-specific robes,
  cloth panels, horns/tattoos, droid profiles, cannon, cape, bandolier, and civilian details.
- Mandalorians use extended 256x256 atlases with T-visors, rangefinders, jetpacks, and role-specific
  cape/pauldron/pack pieces. Clone commander atlases preserve the exact soldier alpha mask.
- Nightsister armor is cloth/wrap/hood/skirt construction rather than plate; Separatist armor gains
  visible servos, reactor, mechanical spine, and knee pistons.
- Vehicle cube counts are 23/32/22/26/44 for BARC/AT-RT/STAP/AAT/LAAT, preserving bones, seats,
  hitboxes, gameplay IDs, and animations.
- Four blasters are distinct 256x256 emissive GeckoLib items. Six sabers preserve the shared `blade`
  bone and 36-unit blade while owning different hilt geometry.
- Display labels are `Rodian Civilian`, `Duros Smuggler`, and `Trandoshan Enforcer`; registry/save IDs
  remain stable.
