package galacticwars.clonewars.progression;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LaunchContentCatalog {
    public static final Map<String, List<String>> UNITS = Map.of(
            "republic", List.of("clone_trooper", "arc_trooper", "jedi_knight"),
            "separatist", List.of("b1_battle_droid", "b2_super_battle_droid", "commando_droid"),
            "mandalorian", List.of("mandalorian_warrior", "mandalorian_marksman", "mandalorian_heavy"),
            "hutt_cartel", List.of("hutt_enforcer", "bounty_hunter", "smuggler"),
            "nightsister", List.of("nightsister_acolyte", "nightsister_archer", "nightbrother_brute"));
    public static final List<String> PLANETS = List.of("tatooine", "geonosis", "kamino", "coruscant");
    public static final List<String> VEHICLES = List.of("barc_speeder", "at_rt", "stap", "aat", "laat_gunship");
    public static final Set<String> FORCE_ABILITIES = Set.of(
            "light_push", "light_pull", "light_leap", "dark_push", "dark_choke", "dark_dash");
    public static final Map<String, Set<String>> QUEST_UNLOCKS = Map.ofEntries(
            Map.entry("republic_chapter_1", Set.of("workforce")),
            Map.entry("republic_chapter_2", Set.of("barc_speeder", "force_path")),
            Map.entry("republic_chapter_3", Set.of("conquest")),
            Map.entry("separatist_chapter_1", Set.of("workforce")),
            Map.entry("separatist_chapter_2", Set.of("stap")),
            Map.entry("separatist_chapter_3", Set.of("conquest")),
            Map.entry("mandalorian_chapter_1", Set.of("workforce")),
            Map.entry("mandalorian_chapter_2", Set.of("vehicle_crafting")),
            Map.entry("mandalorian_chapter_3", Set.of("conquest")),
            Map.entry("hutt_cartel_chapter_1", Set.of("trading")),
            Map.entry("hutt_cartel_chapter_2", Set.of("vehicle_crafting")),
            Map.entry("hutt_cartel_chapter_3", Set.of("conquest")),
            Map.entry("nightsister_chapter_1", Set.of("workforce")),
            Map.entry("nightsister_chapter_2", Set.of("force_path")),
            Map.entry("nightsister_chapter_3", Set.of("conquest")));
    public static final List<String> QUESTS = UNITS.keySet().stream()
            .sorted()
            .flatMap(faction -> java.util.stream.IntStream.rangeClosed(1, 3)
                    .mapToObj(chapter -> faction + "_chapter_" + chapter))
            .toList();

    private LaunchContentCatalog() {
    }

    public static Set<String> questUnlocks(String questId) {
        return QUEST_UNLOCKS.getOrDefault(questId, Set.of());
    }
}
