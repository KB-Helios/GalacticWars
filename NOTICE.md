# Galactic Wars: Clone Wars Notices

This project is developed as an unofficial GPL-3.0-only NeoForge 26.2 fan mod.

## LOTR-Derived Material

Code or assets ported from `quentin452/The-Lord-of-the-Rings` are derived from GPLv3 source and must preserve GPL-compatible terms and attribution. The upstream fork states it is decompiled, corrected, and refactored code of the Minecraft 1.7.10 "The Lord of the Rings" mod originally developed by Mevans.

## Authorized Workers and Recruits Reuse

On 2026-07-12, the project owner explicitly authorized derivative reuse and redistribution in
Galactic Wars of code and assets from the local `workers` and `recruits` project checkouts.
Those upstream checkouts identify themselves as All Rights Reserved, so every reused file must
remain traceable to that authorization. `docs/authorized-source-intake.md` records the source
path, destination, transformation, and derived status for each imported item.

Authorization does not make an old implementation automatically suitable for this project.
Forge 1.20.1 registry, packet, global-manager, and threading code must be adapted behind
Galactic Wars interfaces for NeoForge 26.2 rather than copied wholesale. Medieval or villager
art may be used as an authorized source only when it is deliberately transformed to match the
Galactic Wars visual language and recorded in the asset manifest.

## Project-Owned Galactic Wars Assets

New Galactic Wars textures, models, faction designs, GUI artwork, and writing are project assets. Generated texture prompts and transformations are recorded in `docs/galacticwars-asset-provenance.md`; the required dimensions and UV contracts are recorded in `docs/galacticwars-asset-manifest.json`. Official Star Wars artwork, film stills, logos, audio, and third-party mod assets are not bundled.

## User-Provided Resource Pack Assets

Legacy texture inputs from the prior conversion must not be distributed unless their provenance permits it. The Galactic Wars art pass replaces these files with generated project-bound artwork before release; Beskar generation may use the prior Mithril silhouette as a user-requested visual reference, but final PNGs contain newly generated pixels and are documented separately.

## Third-Party Runtime Libraries

GeckoLib is used as an MIT-licensed runtime dependency for animated Galactic Wars entities and models. Keep GeckoLib code and assets as an external dependency; do not vendor its source into this repository.

YetAnotherConfigLib (YACL) is used as an LGPL-3.0-or-later client dependency for the in-game configuration screen. It remains an external dependency and is not bundled into the Galactic Wars JAR.
