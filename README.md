
# KingdomWars-Middle-Earth

KingdomWars-Middle-Earth is a NeoForge 26.2 kingdom, workforce, recruitment,
and clean-room army gameplay mod for Middle-earth.

## Scope

- Port compatible features from `quentin452/The-Lord-of-the-Rings` in phased,
  NeoForge-native slices.
- Build army command behavior from clean-room observations of
  `talhanation/recruits`.
- Keep registrations, gameplay systems, and resources modular enough for later
  biomes, factions, NPCs, structures, and command UI work.

## Kingdom gameplay

1. Place a Kingdom Hall and sneak-use it to choose Gondor, Rohan, Mordor,
   dwarf, or elf before recruiting. Normal use opens its shared treasury and
   stockpile.
2. Hire an aligned recruit, then assign farmer, lumberjack, miner, courier, or
   builder duty from the recruit screen. Aim at a loaded block inside the Hall
   claim to assign worksites and real containers.
3. Fund the Hall and worker containers with real items. Workers walk to their
   targets, collect loot, consume tools and replanting materials, and deliver
   their inventory without virtual resource counters.
4. Select and queue the starter keep, house, storehouse, farm plot, lumber
   camp, or mine-site blueprint. Completing buildings adds housing, storage,
   worksite capacity, and the starter keep's commander slot.
5. Promote one owned soldier after completing the starter keep. The commander
   can issue group orders and run bounded recruitment campaigns that respect
   faction alignment, housing, treasury reserve, upkeep, and campaign limits.

## Licensing

The project metadata is GPL-3.0-only compatible because LOTR-derived material is
based on GPLv3 source. `talhanation/recruits` is All Rights Reserved and is used
only as a clean-room behavior reference; its code and assets are not included.

See `NOTICE.md` for attribution and source-use rules.

## Development

Build with the included Gradle wrapper:

```bash
./gradlew build
./gradlew runGameTestServer
```

Animated entity work uses GeckoLib 5.5.3 for NeoForge 26.2. The dependency is
declared in Gradle and in the generated NeoForge mod metadata so clients and
servers load it before animated Middle-earth recruit entities.

## Next milestones

- Persist army groups and orders across chunk unloads, then connect the existing
  formation, targeting, tactical, and command-policy planners to live recruits.
- Load faction/unit costs, attributes, alliances, and alignment from the current
  data resources instead of using shared runtime constants.
- Enforce worksite capacity and persisted work orders, and load building
  blueprints from their JSON resources.
- Complete survival acquisition for the Kingdom Hall, Mallorn wood, Mithril,
  and faction equipment before adding more decorative items.
- Enable the remaining professions only after the current worker loop has full
  runtime coverage; world generation, cavalry, ranged units, trading, quests,
  and conquest remain later expansion work.
