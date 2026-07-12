# Optional Library Evaluation

Evaluated 2026-07-12 against the mod's current Minecraft 26.2 / NeoForge 26.2 target.

## Decision Summary

| Library | Current fit | Decision |
| --- | --- | --- |
| Yet Another Config Lib (YACL) | The mod currently exposes five booleans and already uses NeoForge's generated configuration screen. YACL would add richer controls and organization, but the upstream 26.2 target is still based on a prerelease Minecraft/NeoForge build and has no corresponding tagged release. | Defer. Re-evaluate when a supported 26.2 release exists and the settings need categories, sliders, or rich help. |
| Curios API | A strong domain fit for future utility belts, holocrons, scanners, jetpacks, and other wearable equipment. The official API supplies compatible, data-driven equipment slots without adding slots by default. Its current 26.x release is a 26.1.2 beta rather than a verified 26.2 build. | Design future wearable items behind a small internal equipment abstraction, then integrate Curios when the first wearable mechanic is implemented and a 26.2 build is available. Do not make it a dependency yet. |
| Resourceful Lib | A 26.2 / 5.0.1 artifact is available, but the current code does not need its resource-pack/highlight helpers or another general utility layer. It would add a runtime dependency without replacing an existing subsystem. | Do not add. Reconsider only when a concrete Team Resourceful API feature replaces code we would otherwise maintain. |

## Integration Guardrails

- Keep NeoForge `ModConfigSpec` as the source of truth until YACL has a production-ready 26.2 artifact; a future custom screen should wrap the same values rather than create a second config format.
- Keep Curios optional at the design boundary. Core combat, Force, army, and settlement systems must not require an accessory mod to load.
- Add third-party libraries for a named player-facing feature, not as speculative infrastructure.

## Sources

- YACL repository and releases: <https://github.com/isXander/YetAnotherConfigLib>, <https://github.com/isXander/YetAnotherConfigLib/releases>
- Curios overview and current 26.x metadata: <https://github.com/TheIllusiveC4/Curios/blob/26.x/README.md>, <https://github.com/TheIllusiveC4/Curios/blob/26.x/gradle.properties>
- Resourceful Lib repository, documentation, and releases: <https://github.com/Team-Resourceful/ResourcefulLib>, <https://lib.wiki.teamresourceful.com/>, <https://github.com/Team-Resourceful/ResourcefulLib/releases/tag/v5.0.1>
