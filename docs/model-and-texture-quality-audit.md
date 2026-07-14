# Model and Texture Quality Audit

Date: 2026-07-13

## Findings

| Asset family | Evidence in the checkout | Decision |
| --- | --- | --- |
| Lightsabers | The former model was a vanilla `elements` item with 17 cuboids. Its blade was 18.2 model units against an 8.3-unit hilt, and every hilt face sampled a generic 32x32 strip. | Replace the renderer, model, UVs, materials, and glow path with a GeckoLib item. |
| Vehicles | The five models already used GeckoLib and 256x256 authored atlases, but only contained 6-11 cuboids each. Large areas such as the BARC chassis, AAT hull, and LAAT fuselage were single boxes. | Preserve the material atlases and animation bones; rebuild silhouettes with additional UV-safe detail cubes. |
| Recruits and civilians | Twenty GeckoLib models use 128x128 UV-safe atlases and 12-28 model cuboids, with held-item bones and authored animation files. | Keep this pass intact; it is not the current resolution bottleneck. |
| Equipped armor | Five GeckoLib armor models used 36-42 mostly generic layered cuboids despite explicit six-face UVs on 1024x1024 atlases. Helmet silhouettes did not clearly communicate their faction or role. | Rebuild all equipped geometry and UV atlases while preserving the liked 16x16 inventory/item-frame icons. |
| Recruit spawn eggs | Twenty items used flat 16x16 generated sprites only. | Retain vanilla spawn semantics but replace presentation with a shared animated GeckoLib recruitment capsule and unit-specific materials. |
| Inventory sprites | Combat icons are intentionally 16x16 for Minecraft readability. The old saber icons emphasized the hilt and visually shortened the blade. | Redraw only the six saber icons with a longer blade silhouette. |

## Implemented quality gate

- Lightsabers now use one 24-cuboid GeckoLib geometry with separate `hilt` and `blade` bones.
- The energy blade is 36 model units long and the visible blade-to-hilt ratio is approximately 3.4:1.
- The hilt now straddles the GeckoLib item origin and uses sword-like first- and third-person transforms,
  so the hand meets the grip instead of the pommel floating above it.
- Six four-frame 256x1024 UV atlases and matching emissive glowmasks replace the generic split hilt/blade strips.
- Minecraft item definitions use `minecraft:special` plus `geckolib:geckolib`; the Java item implements `GeoItem` and renders through `GeoItemRenderer`.
- Equipped armor now uses 60/51/54/55/54 cuboids across Republic, Separatist, Mandalorian,
  Nightsister, and Beskar families. Every set has a distinct modeled helmet, torso, arm, belt, leg,
  and boot language; the Republic helmet has eighteen layered clone-infantry-style components.
- All five 1024x1024 armor atlases were regenerated against the new exact face maps. The twenty
  existing armor item icons were not regenerated or modified.
- All twenty recruit eggs now use one eighteen-cuboid GeckoLib capsule with separate `shell` and
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
