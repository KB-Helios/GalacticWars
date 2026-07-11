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
    public static final List<String> QUESTS = UNITS.keySet().stream()
            .sorted()
            .flatMap(faction -> java.util.stream.IntStream.rangeClosed(1, 3)
                    .mapToObj(chapter -> faction + "_chapter_" + chapter))
            .toList();

    private LaunchContentCatalog() {
    }
}
