package galacticwars.clonewars.data;

import galacticwars.clonewars.army.ArmyUnitDefinition;
import galacticwars.clonewars.faction.FactionDefinition;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.workforce.WorkerProfession;
import galacticwars.clonewars.world.CivilianArchetypeDefinition;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Code-bound launch roster. Datapacks may tune these definitions, but cannot
 * silently add, remove, or remap runtime types that require registered code.
 */
public final class CoreContentBindings {
    private static final Map<String, String> FACTION_CHIPS = orderedMap(
            "galacticwars:republic", "galacticwars:republic_identity_chip",
            "galacticwars:separatist", "galacticwars:separatist_identity_chip",
            "galacticwars:mandalorian", "galacticwars:mandalorian_identity_chip",
            "galacticwars:hutt_cartel", "galacticwars:hutt_cartel_identity_chip",
            "galacticwars:nightsister", "galacticwars:nightsister_identity_chip");

    private static final Set<String> UNIT_IDS = orderedSet(
            "galacticwars:arc_trooper",
            "galacticwars:b1_battle_droid",
            "galacticwars:b1_security_droid",
            "galacticwars:b2_super_battle_droid",
            "galacticwars:bounty_hunter",
            "galacticwars:clone_trooper",
            "galacticwars:commando_droid",
            "galacticwars:hutt_enforcer",
            "galacticwars:jedi_knight",
            "galacticwars:mandalorian_heavy",
            "galacticwars:mandalorian_marksman",
            "galacticwars:mandalorian_warrior",
            "galacticwars:nightbrother_brute",
            "galacticwars:nightsister_acolyte",
            "galacticwars:nightsister_archer",
            "galacticwars:phase_i_arc_trooper",
            "galacticwars:phase_i_clone_trooper",
            "galacticwars:republic_honor_guard",
            "galacticwars:senate_commando",
            "galacticwars:sith_acolyte",
            "galacticwars:smuggler");

    private static final Map<String, VehicleBinding> VEHICLES = Map.of(
            "barc_speeder", new VehicleBinding(
                    "galacticwars:barc_speeder", "galacticwars:barc_speeder_deployment_kit"),
            "at_rt", new VehicleBinding(
                    "galacticwars:at_rt", "galacticwars:at_rt_deployment_kit"),
            "stap", new VehicleBinding(
                    "galacticwars:stap", "galacticwars:stap_deployment_kit"),
            "aat", new VehicleBinding(
                    "galacticwars:aat", "galacticwars:aat_deployment_kit"),
            "laat_gunship", new VehicleBinding(
                    "galacticwars:laat_gunship", "galacticwars:laat_gunship_deployment_kit"));

    private static final Map<String, List<String>> FORCE_SLOTS = Map.of(
            "jedi", List.of("light_push", "light_leap"),
            "sith", List.of("dark_push", "dark_dash"),
            "nightsister", List.of("magick_push", "shadow_step"));

    private static final Map<String, PlanetBinding> PLANETS = Map.of(
            "tatooine", new PlanetBinding("galacticwars:tatooine", "spaceport"),
            "geonosis", new PlanetBinding("galacticwars:geonosis", "foundry_outpost"),
            "kamino", new PlanetBinding("galacticwars:kamino", "platform_city"),
            "coruscant", new PlanetBinding("galacticwars:coruscant", "senate_district"));

    private CoreContentBindings() {
    }

    public static Map<String, String> factionChips() {
        return FACTION_CHIPS;
    }

    public static Map<String, VehicleBinding> vehicles() {
        return VEHICLES;
    }

    public static Map<String, PlanetBinding> planets() {
        return PLANETS;
    }

    public static List<String> forceSlots(String path) {
        String resolved = path == null ? "" : path;
        if (resolved.equals("light")) resolved = "jedi";
        if (resolved.equals("dark")) resolved = "nightsister";
        return FORCE_SLOTS.getOrDefault(resolved, List.of());
    }

    public static void validate(GameplayDataSnapshot snapshot) {
        Map<String, String> actualFactions = snapshot.factions().definitions().values().stream()
                .collect(Collectors.toMap(
                        definition -> definition.id().toString(),
                        FactionDefinition::pledgeTokenItemId,
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        requireExactIds("factions", FACTION_CHIPS.keySet(), actualFactions.keySet());
        FACTION_CHIPS.forEach((factionId, chipId) -> {
            if (!chipId.equals(actualFactions.get(factionId))) {
                throw new IllegalArgumentException("Faction " + factionId
                        + " must use core identity chip " + chipId);
            }
        });

        Map<String, String> actualUnits = snapshot.units().definitions().values().stream()
                .collect(Collectors.toMap(
                        definition -> definition.id().toString(),
                        ArmyUnitDefinition::entityTypeId,
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        requireExactIds("units", UNIT_IDS, actualUnits.keySet());
        actualUnits.forEach((unitId, entityTypeId) -> {
            if (!unitId.equals(entityTypeId)) {
                throw new IllegalArgumentException("Core unit " + unitId
                        + " must bind to entity type " + unitId + " instead of " + entityTypeId);
            }
        });

        LaunchContentDefinitions launch = snapshot.launchContent();
        requireExactIds("vehicles", VEHICLES.keySet(), launch.vehicles().keySet());
        FORCE_SLOTS.forEach((tradition, abilityIds) -> abilityIds.forEach(abilityId -> {
            LaunchContentDefinitions.ForceAbilityDefinition ability = launch.forceAbilities().get(abilityId);
            if (ability == null || !tradition.equals(ability.path())) {
                throw new IllegalArgumentException("Force ability " + abilityId
                        + " must remain in the " + tradition + " tradition");
            }
        }));
        if (!launch.forceTraditions().keySet().equals(FORCE_SLOTS.keySet())) {
            throw new IllegalArgumentException("Core Force traditions do not match registered bindings");
        }

        requireExactIds("planets", PLANETS.keySet(), launch.planets().keySet());
        PLANETS.forEach((planetId, binding) -> {
            LaunchContentDefinitions.PlanetDefinition definition = launch.planets().get(planetId);
            if (!binding.dimensionId().equals(definition.dimensionId())
                    || !binding.arrivalProfile().equals(definition.arrival())) {
                throw new IllegalArgumentException("Planet " + planetId + " must bind to dimension "
                        + binding.dimensionId() + " and arrival profile " + binding.arrivalProfile());
            }
        });

        validateQuestObjectives(snapshot);
    }

    private static void validateQuestObjectives(GameplayDataSnapshot snapshot) {
        for (LaunchContentDefinitions.QuestDefinition quest
                : snapshot.launchContent().quests().values()) {
            for (LaunchContentDefinitions.QuestObjectiveDefinition objective : quest.objectives()) {
                ProgressionEventType eventType;
                try {
                    eventType = ProgressionEventType.valueOf(
                            objective.eventType().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException unknownType) {
                    throw new IllegalArgumentException("Quest " + quest.id() + " objective "
                            + objective.id() + " references unknown event type "
                            + objective.eventType(), unknownType);
                }
                if (objective.subjectIds().isEmpty()) {
                    continue;
                }
                Set<String> allowed = objectiveSubjects(snapshot, eventType);
                for (String subjectId : objective.subjectIds()) {
                    if (!matchesSubject(subjectId, allowed)) {
                        throw new IllegalArgumentException("Quest " + quest.id() + " objective "
                                + objective.id() + " references unknown "
                                + eventType.name().toLowerCase(Locale.ROOT)
                                + " subject " + subjectId);
                    }
                }
            }
        }
    }

    private static Set<String> objectiveSubjects(
            GameplayDataSnapshot snapshot,
            ProgressionEventType eventType
    ) {
        LaunchContentDefinitions launch = snapshot.launchContent();
        return switch (eventType) {
            case CAMPAIGN_RECHECK -> Set.of("eligible_quests");
            case FACTION_PLEDGED -> snapshot.factions().definitions().keySet().stream()
                    .map(Object::toString).collect(Collectors.toUnmodifiableSet());
            case RECRUIT_HIRED -> java.util.stream.Stream.concat(
                            snapshot.units().definitions().keySet().stream().map(Object::toString),
                            snapshot.civilianArchetypesByEntityType().values().stream()
                                    .map(CivilianArchetypeDefinition::id))
                    .collect(Collectors.toUnmodifiableSet());
            case PROFESSION_ASSIGNED -> java.util.Arrays.stream(WorkerProfession.values())
                    .map(WorkerProfession::id).collect(Collectors.toUnmodifiableSet());
            case BUILDING_COMPLETED -> {
                LinkedHashSet<String> buildings = new LinkedHashSet<>(snapshot.blueprints().keySet());
                buildings.add("command_center");
                yield Set.copyOf(buildings);
            }
            case PLANET_VISITED -> launch.planets().keySet();
            case VEHICLE_ACQUIRED -> launch.vehicles().keySet();
            case FORCE_ABILITY_USED -> launch.forceAbilities().keySet();
            case QUEST_ADVANCED -> launch.quests().keySet();
            case CAMPAIGN_COMPLETED -> launch.quests().keySet().stream()
                    .map(CoreContentBindings::campaignId)
                    .filter(id -> !id.isBlank())
                    .collect(Collectors.toUnmodifiableSet());
            case TRADE_COMPLETED -> launch.trades().keySet();
            case REGION_CAPTURED -> launch.conquestRegions().keySet();
            case CREDIT_TRANSACTION, DELIVERY_COMPLETED -> Set.of();
        };
    }

    private static String campaignId(String questId) {
        int marker = questId.lastIndexOf("_chapter_");
        return marker <= 0 ? "" : questId.substring(0, marker) + "_campaign";
    }

    private static boolean matchesSubject(String requested, Set<String> allowed) {
        String requestedPath = subjectPath(requested);
        if (requestedPath == null) {
            return false;
        }
        return allowed.stream().anyMatch(candidate ->
                candidate.equals(requested) || requestedPath.equals(subjectPath(candidate)));
    }

    private static String subjectPath(String subject) {
        int separator = subject.indexOf(':');
        if (separator < 0) {
            return subject;
        }
        if (separator != subject.lastIndexOf(':')
                || !subject.substring(0, separator).equals("galacticwars")) {
            return null;
        }
        return subject.substring(separator + 1);
    }

    static void requireExactIds(String label, Set<String> expected, Set<String> actual) {
        if (expected.equals(actual)) {
            return;
        }
        LinkedHashSet<String> missing = new LinkedHashSet<>(expected);
        missing.removeAll(actual);
        LinkedHashSet<String> unexpected = new LinkedHashSet<>(actual);
        unexpected.removeAll(expected);
        throw new IllegalArgumentException("Core " + label + " do not match registered bindings; missing="
                + missing + ", unexpected=" + unexpected);
    }

    private static Set<String> orderedSet(String... values) {
        return java.util.Collections.unmodifiableSet(new LinkedHashSet<>(List.of(values)));
    }

    private static Map<String, String> orderedMap(String... values) {
        if ((values.length & 1) != 0) {
            throw new IllegalArgumentException("key/value pairs required");
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(values[index], values[index + 1]);
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    public record VehicleBinding(String entityTypeId, String deploymentKitItemId) {
    }

    public record PlanetBinding(String dimensionId, String arrivalProfile) {
    }
}
