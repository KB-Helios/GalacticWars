# Galactic Wars Asset Provenance

## Polished clone, droid, and lightsaber pass

The detailed Clone Trooper, B1 Battle Droid, and lightsaber direction was generated with the built-in image generation workflow as project-owned concept material. Because general image generation does not preserve Minecraft cube UV coordinates, `tools/generate_polished_textures.py` translates that direction into deterministic UV-safe 64x64 GeckoLib skins and native 16x16 item sprites. The existing Gondorian-derived clone/droid geometry, bone names, pivots, animations, and UV origins remain unchanged; only the texture pixels are replaced.

All assets listed here are original, project-bound artwork. No official Star Wars artwork, film stills, logos, third-party mod textures, or other third-party assets were used.

## Method

- Generated source images with the built-in OpenAI image-generation tool on 2026-07-11.
- Used one focused prompt per deliverable. Opaque blocks were generated edge-to-edge; inventory and UI art used a flat `#ff00ff` chroma background.
- Fitted sources to the manifest dimensions with nearest-neighbor resampling. Chroma pixels were removed locally using a conservative magenta color threshold because the bundled Pillow helper was unavailable in the active runtime.
- Validated exact dimensions, opaque/transparent pixel contracts, non-empty visible coverage, distinct SHA-256 hashes, and visual readability at native resolution.
- Beskar ore, raw Beskar, and the Beskar ingot were transformations of the project's own Mithril-derived artwork, then substantially recolored and redrawn by the generator.

## Shared prompt constraints

Every prompt required crisp Minecraft-readable pixel art with hard pixel clusters, no antialiasing, text, logos, faction insignia, watermarks, or official Star Wars imagery. Transparent assets additionally required a uniform `#ff00ff` background, no shadows, and no magenta in the subject.

## Core batch

| Final asset | Generated source | Asset-specific prompt |
| --- | --- | --- |
| `textures/block/command_center_side.png` | `exec-cf5ea313-ba97-411e-bd6e-d3ba22beef63.png` | Armored charcoal duracrete side panel with inset gunmetal plating, amber status light, and restrained cyan circuit accents. |
| `textures/block/command_center_top.png` | `exec-e13fc952-e395-4adb-9619-c9e9985c36e3.png` | Top-down segmented command-center panel with a circular landing-console motif and small amber/cyan indicators. |
| `textures/block/command_center_bottom.png` | `exec-cb352f0c-c57b-4e30-8605-fe7d93baa280.png` | Heavy duracrete underside with cross-braced gunmetal seams, recessed bolts, and industrial wear. |
| `textures/block/duracrete.png` | `exec-07a97d60-42aa-408d-a585-79637ca8247e.png` | Cool gray poured composite with aggregate speckles and restrained panel seams for large base surfaces. |
| `textures/block/beskar_ore.png` | `exec-0d27b0e1-7903-464c-9617-3cffdbfa99bd.png` | Rework project-owned Mithril ore into dark duracrete with sparse cold silver-blue Beskar veins. |
| `textures/item/raw_beskar.png` | `exec-be930583-9f7f-4037-af74-dbf60bcfbab1.png` | Angular unprocessed Beskar chunk with dark gunmetal facets and cold silver-blue highlights. |
| `textures/item/beskar_ingot.png` | `exec-a250625c-7a88-41a2-84a4-a5ec7060fe0d.png` | Compact beveled Beskar ingot with brushed gunmetal, cold edge highlights, and forge grooves. |
| `textures/item/credit_chip.png` | `exec-21a2c6bf-e264-40a5-9f22-94c27e9e2843.png` | Generic hexagonal sci-fi credit chip with an amber inset and cyan edge lights. |
| `textures/item/energy_cell.png` | `exec-e0cd83b5-c09a-47d3-aac8-069fa4e03963.png` | Compact gunmetal blaster cartridge with a cyan core slit and amber charge cap. |
| `textures/gui/galactic_wars_tab.png` | `exec-a6d8fe53-fc73-4c10-9b48-de5c50d5afd7.png` | Abstract four-point hyperspace compass around an amber star, designed for tiny UI scale. |

Generated sources remain in the Codex image-generation store for auditability. The distributable sources of truth are the final PNGs under `src/main/resources/assets/galacticwars/`.

## Remaining manifest batches

- `factions`: identity chips, five armor layer sets, and armor inventory icons.
- `combat_and_tools`: complete; all seventeen icons and their item/model references are installed and validated.

## Unit batch

The fifteen 64x64 entity textures use generated character concepts only as palette, silhouette, and material references. Final distributable atlases are deterministically authored into the exact cube-face regions declared by the locked GeckoLib models under `assets/galacticwars/geckolib/models/entity/`; model-specific and shared humanoid base regions are opaque, while remaining atlas space is transparent.

| Final atlas | Generated concept source |
| --- | --- |
| `textures/entity/clone_trooper.png` | `exec-4e523ad4-febb-4172-ae0b-61c6c215abfe.png` |
| `textures/entity/arc_trooper.png` | `exec-20fe8cda-3d58-486d-9fe9-92d2ca35e1f7.png` |
| `textures/entity/jedi_knight.png` | `exec-9d6656e8-fb76-41c1-9c34-1d5f958bc62a.png` |
| `textures/entity/b1_battle_droid.png` | `exec-3b346fd3-4bd8-4210-b985-fb35d758007c.png` |
| `textures/entity/b2_super_battle_droid.png` | `exec-6ef7139a-42fe-4b11-8090-b16a08a702f7.png` |
| `textures/entity/commando_droid.png` | `exec-3efdb814-4e46-4a4a-a828-909989e517a0.png` |
| `textures/entity/mandalorian_warrior.png` | `exec-0a04ce0a-657f-48bb-ba65-d4daf23dd241.png` |
| `textures/entity/mandalorian_marksman.png` | `exec-ef79dde2-8ac3-4cb1-86cb-b24c3b5c5acb.png` |
| `textures/entity/mandalorian_heavy.png` | `exec-a90d7134-bcb3-4947-b8e1-0976020e843f.png` |
| `textures/entity/hutt_enforcer.png` | `exec-deb6a950-d46f-4029-bfdc-a1755df3d7aa.png` |
| `textures/entity/bounty_hunter.png` | `exec-76c207a7-7298-4f3c-bbe1-59ae3fc00387.png` |
| `textures/entity/smuggler.png` | `exec-2a9a37dc-6c9d-40b3-806f-c8deb5c5ef65.png` |
| `textures/entity/nightsister_acolyte.png` | `exec-51af9e24-e176-41c3-9d13-3703eb0cfa96.png` |
| `textures/entity/nightsister_archer.png` | `exec-3f844138-1ccf-470c-bc7c-c384447deef3.png` |
| `textures/entity/nightbrother_brute.png` | `exec-acb59286-5603-4519-909d-e4e809863517.png` |

### UV validation correction

The initial geometry-only transparency mask cleared `hutt_enforcer.png` pixel `16,31`, which belongs to the canonical body-west face. The atlas was regenerated with the shared head, body, arm, and leg UV boxes always preserved in addition to geometry-specific boxes. The corrected pixel now uses the surrounding authored body material and is fully opaque. `RecruitTextureAtlasTest` subsequently validated required face opacity, bounds, dimensions, animation references, alpha-safe atlases, and distinct unit texture hashes for all fifteen units.

## Combat and tools partial batch

Each completed source was fitted to 16x16 with nearest-neighbor resampling, cleaned to a transparent inventory background, and linked through matching item-definition and handheld-model JSON files.

| Final icon | Generated source |
| --- | --- |
| `textures/item/vibroblade.png` | `exec-26e2e6d2-9af2-4b76-87d7-fb7502bc9621.png` |
| `textures/item/beskar_vibroblade.png` | `exec-e73ef179-48a3-4e47-a970-ec451b8afbd8.png` |
| `textures/item/plasma_cutter.png` | `exec-723720af-b9eb-4752-8b0d-be7998b1a283.png` |
| `textures/item/power_drill.png` | `exec-3f6324bf-78a0-4404-a8f9-6719e42de260.png` |
| `textures/item/sonic_excavator.png` | `exec-7999e613-9f6c-4ac4-b616-81d66ab099ee.png` |
| `textures/item/hydrospanner.png` | `exec-84f3ede6-0f56-463a-974b-b553fa61c8e7.png` |
| `textures/item/dc15_blaster.png` | `exec-2d527b16-359c-4535-8e46-224711344039.png` |
| `textures/item/e5_blaster.png` | `exec-24cfabe0-11a5-457e-9475-cc4b4bafcec0.png` |

The project-owned Mandalorian ingot texture and both resource references were renamed from `mandalorian_alloy_iron_ingot` to the registered `mandalorian_alloy_ingot` identifier without altering the artwork.

### Consolidated source-sheet mapping

The remaining nine icons came from the generated 3x3 source sheet `exec-aeab48fc-acaa-441f-ac1f-4329e9681096.png`. Each equal cell was cropped independently, nearest-neighbor fitted to 16x16, and chroma-cleaned.

| Cell | Final icon |
| --- | --- |
| Row 1, column 1 | `textures/item/westar_blaster.png` |
| Row 1, column 2 | `textures/item/scatter_blaster.png` |
| Row 1, column 3 | `textures/item/nightsister_bow.png` |
| Row 2, column 1 | `textures/item/blue_lightsaber.png` |
| Row 2, column 2 | `textures/item/green_lightsaber.png` |
| Row 2, column 3 | `textures/item/red_lightsaber.png` |
| Row 3, column 1 | `textures/item/blaster_bolt.png` |
| Row 3, column 2 | `textures/item/force_light.png` |
| Row 3, column 3 | `textures/item/force_dark.png` |

All seventeen combat/tool textures passed the final contract audit: exact 16x16 dimensions, transparent corners and backgrounds, non-empty visible subjects, zero visible chroma pixels, seventeen distinct SHA-256 hashes, and resolving `assets/galacticwars/items` plus `models/item` references.

## Faction batch

Faction inventory art came from `exec-f7a0bd99-9610-42b1-ae4e-8c0a7d07113b.png`. The 5x5 sheet maps columns left-to-right to `republic`, `separatist`, `mandalorian`, `hutt_cartel`, and `nightsister`; rows top-to-bottom map to `identity_chip`, `helmet`, `chestplate`, `leggings`, and `boots`. Every cell was independently center-cropped, nearest-neighbor fitted to 16x16, and chroma-cleaned to its corresponding `textures/item/{faction}_{row}.png` path.

The adult, baby, and leggings layers were recolored from project-owned UV-safe armor templates using the same generated faction palettes, preserving transparency and exact vanilla armor UV positions. Their paths are `textures/entity/equipment/{humanoid|humanoid_baby|humanoid_leggings}/{faction}.png` and are connected by `equipment/{faction}.json`.

Validation passed for all forty faction PNGs: twenty-five unique 16x16 inventory hashes, five distinct textures within each armor-layer class, exact 64x32 adult/leggings and 64x64 baby dimensions, non-empty transparency and visible coverage, zero residual chroma, resolving item/model references, and five complete equipment descriptors.

## Planets batch

The four planet-selection icons and navigation console were generated on a uniform magenta background, nearest-neighbor fitted to their final GUI dimensions, and chroma-cleaned to transparent PNGs. The four environment materials were generated as opaque edge-to-edge square sources and nearest-neighbor fitted to 16x16.

| Final asset | Generated source |
| --- | --- |
| `textures/gui/planet/tatooine.png` | `exec-7a284c37-bea1-400a-a566-21897a7869ad.png` |
| `textures/gui/planet/geonosis.png` | `exec-c15aa39c-90cf-4a2b-a873-5fa96849fc68.png` |
| `textures/gui/planet/kamino.png` | `exec-1c60ac36-588f-4873-8e4c-280dcbb8fe94.png` |
| `textures/gui/planet/coruscant.png` | `exec-090d0da3-28ef-4775-a447-f7baa974d97b.png` |
| `textures/block/tatooine_sand.png` | `exec-816bfdcc-02d5-4cd9-b055-c6f9f75b31ab.png` |
| `textures/block/geonosis_rock.png` | `exec-22c00903-b5eb-4149-aaf2-fdbf62e194fb.png` |
| `textures/block/kamino_panel.png` | `exec-b198b307-3f5c-4cbb-93d0-951a9add76bd.png` |
| `textures/block/coruscant_panel.png` | `exec-c18810f8-3243-4f27-8c78-eb4bf14882fb.png` |
| `textures/gui/navigation_console.png` | `exec-a25927d5-7c63-42b7-b4ea-5ee435fe099b.png` |

The completed planet gate passed exact-dimension and alpha checks: every planet icon is 128x128 with transparent corners, the navigation console is 256x256 with a transparent exterior and center, every block is fully opaque at 16x16, all transparent assets have non-empty visible coverage and zero residual magenta, and all nine PNGs have distinct SHA-256 hashes. Each environment block has resolving blockstate, block-model, item-definition, and item-model JSON references.

## Vehicle batch

Each vehicle began as a separately generated, edge-to-edge material kit containing original armor, engine, cockpit, joint, and weapon panels. The sources were nearest-neighbor fitted to 256x256 and copied only into the explicit cuboid face footprints declared by the matching GeckoLib geometry. All unused atlas space remains transparent.

| Final atlas | Generated material source | Geometry and animation contract |
| --- | --- | --- |
| `textures/entity/vehicle/barc_speeder.png` | `exec-367b9f7b-bcd4-4d68-9bd8-4d6fe21f7a9b.png` | `geckolib/{models,animations}/entity/vehicle/barc_speeder.*.json` |
| `textures/entity/vehicle/at_rt.png` | `exec-79a66d46-f2c3-4b78-8bf1-0a2648066303.png` | `geckolib/{models,animations}/entity/vehicle/at_rt.*.json` |
| `textures/entity/vehicle/stap.png` | `exec-456e75ae-c89e-4219-adbc-0aeb9ab753f7.png` | `geckolib/{models,animations}/entity/vehicle/stap.*.json` |
| `textures/entity/vehicle/aat.png` | `exec-b2870082-c235-4546-90d9-86eaf1b8aa2c.png` | `geckolib/{models,animations}/entity/vehicle/aat.*.json` |
| `textures/entity/vehicle/laat_gunship.png` | `exec-66e0e323-af49-443e-905e-fabcb722cb43.png` | `geckolib/{models,animations}/entity/vehicle/laat_gunship.*.json` |

Validation parsed every geometry and animation file, resolved parent and animated bone references, verified the `geometry.galacticwars.vehicle.*` identifiers and 256x256 declarations, and recalculated every box-UV footprint from its cube dimensions. All footprints are in bounds, non-overlapping, and fully opaque; every atlas also retains transparent unused space and a transparent corner. The five final PNGs have distinct SHA-256 hashes and passed native-resolution visual inspection.

The first LAAT audit identified a six-pixel-high overlap between the right-cannon and side-door footprints. The right-cannon origin was moved from `[112,128]` to `[112,168]`, the atlas was rebuilt from the same generated source, and the complete five-vehicle validation was rerun successfully after the correction.

## Effects and GUI batch

All six sources were generated on a uniform magenta background, visually reviewed together, cropped to their visible subject bounds, nearest-neighbor fitted with transparent padding to the exact manifest dimensions, and chroma-cleaned.

| Final asset | Generated source |
| --- | --- |
| `textures/particle/blaster_bolt.png` | `exec-b2c405e5-d86c-4d93-b888-80bbe178b103.png` |
| `textures/particle/force_wave.png` | `exec-5f00564a-979f-4c9c-9328-db2f39440d2a.png` |
| `textures/gui/quest_panel.png` | `exec-676222e1-84eb-4415-8d78-9ea53164ffc9.png` |
| `textures/gui/conquest_panel.png` | `exec-50d1b8b8-1f44-4178-a59f-45dd11189f74.png` |
| `textures/gui/vehicle_hud.png` | `exec-cf7dc42c-3058-4cd7-88d2-056246f19579.png` |
| `textures/gui/force_meter.png` | `exec-b0ddd5b5-edf8-46b4-a8da-87d793538ace.png` |

The particle sprites resolve through `particles/blaster_bolt.json` and `particles/force_wave.json`; GUI textures are direct named resources for their render layers. Validation passed exact sizes, transparent corners and interiors, non-empty visible coverage, zero residual chroma, six distinct SHA-256 hashes, JSON/reference resolution, and native-resolution readability.

## Armor icon refinement

The twenty armor inventory icons were regenerated as one cohesive project-owned pixel-art sheet (`exec-35cea704-fd2a-43e4-a596-d48046a7341d.png`). The retained source is `tools/source_art/generated_armor_icons.png`; `tools/generate_polished_textures.py` removes its flat green key, isolates the fixed five-by-four cells, fits each silhouette into a transparent 16x16 canvas, and writes the helmet, chestplate, leggings, and boots icons for all five armor families. This keeps the small in-game assets reproducible while preserving distinct Republic plastoid, Separatist alloy, Mandalorian alloy, Nightsister weave, and Beskar material language.
