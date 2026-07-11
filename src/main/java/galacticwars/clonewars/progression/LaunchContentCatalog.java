package galacticwars.clonewars.progression;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LaunchContentCatalog {
    public static final List<String> FACTIONS = List.of(
            "galacticwars:republic", "galacticwars:separatist", "galacticwars:mandalorian",
            "galacticwars:hutt_cartel", "galacticwars:nightsister");
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
    public static final Map<String, TradeDefinition> TRADES = Map.of(
            "republic_quartermaster", new TradeDefinition(
                    "republic", 12, "galacticwars:energy_cell", 8, "faction_intro"),
            "separatist_foundry", new TradeDefinition(
                    "separatist", 12, "galacticwars:energy_cell", 8, "faction_intro"),
            "mandalorian_armorer", new TradeDefinition(
                    "mandalorian", 40, "galacticwars:beskar_ingot", 1, "advanced_trading"),
            "hutt_broker", new TradeDefinition(
                    "hutt_cartel", 28, "galacticwars:scatter_blaster", 1, "advanced_trading"),
            "nightsister_matron", new TradeDefinition(
                    "nightsister", 35, "galacticwars:nightsister_bow", 1, "force_path"));
    public static final Map<String, Set<String>> QUEST_UNLOCKS = Map.ofEntries(
            Map.entry("republic_chapter_1", Set.of("workforce")),
            Map.entry("republic_chapter_2", Set.of("barc_speeder", "force_path")),
            Map.entry("republic_chapter_3", Set.of("conquest", "vehicle_mastery")),
            Map.entry("separatist_chapter_1", Set.of("workforce")),
            Map.entry("separatist_chapter_2", Set.of("stap")),
            Map.entry("separatist_chapter_3", Set.of("conquest", "vehicle_mastery")),
            Map.entry("mandalorian_chapter_1", Set.of("workforce")),
            Map.entry("mandalorian_chapter_2", Set.of("vehicle_crafting")),
            Map.entry("mandalorian_chapter_3", Set.of("conquest", "vehicle_mastery")),
            Map.entry("hutt_cartel_chapter_1", Set.of("trading")),
            Map.entry("hutt_cartel_chapter_2", Set.of("vehicle_crafting")),
            Map.entry("hutt_cartel_chapter_3", Set.of("conquest", "vehicle_mastery")),
            Map.entry("nightsister_chapter_1", Set.of("workforce")),
            Map.entry("nightsister_chapter_2", Set.of("force_path")),
            Map.entry("nightsister_chapter_3", Set.of("conquest", "vehicle_mastery")));
    public static final Map<String, List<String>> QUEST_OBJECTIVES = Map.ofEntries(
            Map.entry("republic_chapter_1", List.of("faction_pledged", "command_center", "clone_trooper")),
            Map.entry("republic_chapter_2", List.of("delivery_completed", "forward_base", "kamino")),
            Map.entry("republic_chapter_3", List.of("vehicle_acquired", "trade_completed", "region_captured")),
            Map.entry("separatist_chapter_1", List.of("faction_pledged", "command_center", "b1_battle_droid")),
            Map.entry("separatist_chapter_2", List.of("delivery_completed", "forward_base", "geonosis")),
            Map.entry("separatist_chapter_3", List.of("vehicle_acquired", "trade_completed", "region_captured")),
            Map.entry("mandalorian_chapter_1", List.of("faction_pledged", "command_center", "mandalorian_warrior")),
            Map.entry("mandalorian_chapter_2", List.of("delivery_completed", "beskar_ingot", "tatooine")),
            Map.entry("mandalorian_chapter_3", List.of("vehicle_acquired", "bounty_hunter", "region_captured")),
            Map.entry("hutt_cartel_chapter_1", List.of("faction_pledged", "command_center", "hutt_enforcer")),
            Map.entry("hutt_cartel_chapter_2", List.of("trade_completed", "supply_depot", "tatooine")),
            Map.entry("hutt_cartel_chapter_3", List.of("vehicle_acquired", "smuggler", "region_captured")),
            Map.entry("nightsister_chapter_1", List.of("faction_pledged", "command_center", "nightsister_acolyte")),
            Map.entry("nightsister_chapter_2", List.of("delivery_completed", "forward_base", "coruscant")),
            Map.entry("nightsister_chapter_3", List.of("force_ability_unlocked", "trade_completed", "region_captured")));
    public static final Map<String, Integer> QUEST_REWARD_CREDITS = Map.ofEntries(
            Map.entry("republic_chapter_1", 40), Map.entry("republic_chapter_2", 70), Map.entry("republic_chapter_3", 120),
            Map.entry("separatist_chapter_1", 40), Map.entry("separatist_chapter_2", 70), Map.entry("separatist_chapter_3", 120),
            Map.entry("mandalorian_chapter_1", 40), Map.entry("mandalorian_chapter_2", 75), Map.entry("mandalorian_chapter_3", 125),
            Map.entry("hutt_cartel_chapter_1", 50), Map.entry("hutt_cartel_chapter_2", 80), Map.entry("hutt_cartel_chapter_3", 130),
            Map.entry("nightsister_chapter_1", 40), Map.entry("nightsister_chapter_2", 70), Map.entry("nightsister_chapter_3", 120));
    public static final Map<String, Integer> REGION_REWARD_CREDITS = Map.of(
            "tatooine_spaceport", 90,
            "geonosis_foundry", 100,
            "kamino_platform", 100,
            "coruscant_district", 120);
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

    public static List<String> questObjectives(String questId) {
        return QUEST_OBJECTIVES.getOrDefault(questId, List.of());
    }

    public static int questRewardCredits(String questId) {
        return QUEST_REWARD_CREDITS.getOrDefault(questId, 0);
    }

    public static int regionRewardCredits(String regionId) {
        return REGION_REWARD_CREDITS.getOrDefault(regionId, 0);
    }

    public record TradeDefinition(
            String factionId,
            int price,
            String itemId,
            int itemCount,
            String requiredUnlock
    ) {
        public TradeDefinition {
            if (factionId.isBlank() || itemId.isBlank() || requiredUnlock.isBlank()) {
                throw new IllegalArgumentException("Trade identifiers cannot be blank");
            }
            if (price <= 0 || itemCount <= 0) {
                throw new IllegalArgumentException("Trade price and item count must be positive");
            }
        }
    }
}
