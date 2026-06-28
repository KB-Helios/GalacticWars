
# KingdomWars-Middle-Earth

KingdomWars-Middle-Earth is a NeoForge 26.2 port foundation for Middle-earth
content and clean-room army behavior.

## Scope

- Port compatible features from `quentin452/The-Lord-of-the-Rings` in phased,
  NeoForge-native slices.
- Build army command behavior from clean-room observations of
  `talhanation/recruits`.
- Keep registrations, gameplay systems, and resources modular enough for later
  biomes, factions, NPCs, structures, and command UI work.

## Licensing

The project metadata is GPL-3.0-only compatible because LOTR-derived material is
based on GPLv3 source. `talhanation/recruits` is All Rights Reserved and is used
only as behavior reference unless explicit compatible permission is provided.

See `NOTICE.md` for attribution and source-use rules.

## Development

Build with the included Gradle wrapper:

```bash
./gradlew build
```

Animated entity work uses GeckoLib 5.5.3 for NeoForge 26.2. The dependency is
declared in Gradle and in the generated NeoForge mod metadata so clients and
servers load it before animated Middle-earth entities are introduced.
