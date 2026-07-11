package galacticwars.clonewars.progression;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class GalacticProgressionCoordinator {
    private static final Set<String> FACTIONS = Set.of(
            "galacticwars:republic", "galacticwars:separatist", "galacticwars:mandalorian",
            "galacticwars:hutt_cartel", "galacticwars:nightsister");
    private static final Set<String> PLANETS = Set.of("tatooine", "geonosis", "kamino", "coruscant");

    private GalacticProgressionCoordinator() {
    }

    public static ProgressionDecision apply(ProgressionState state, ProgressionEvent event) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(event, "event");
        if (!state.playerId().equals(event.playerId())) {
            return ProgressionDecision.rejected("wrong_player", state);
        }
        if (state.processed(event.id())) {
            return ProgressionDecision.accepted(state, state);
        }
        String faction = state.factionId();
        if (event.type() == ProgressionEventType.FACTION_PLEDGED) {
            if (!FACTIONS.contains(event.subjectId())) {
                return ProgressionDecision.rejected("unknown_faction", state);
            }
            if (!faction.isEmpty()) {
                return ProgressionDecision.rejected("faction_already_pledged", state);
            }
            faction = event.subjectId();
        } else if (faction.isEmpty()
                && !(event.type() == ProgressionEventType.BUILDING_COMPLETED
                && subjectPath(event.subjectId()).equals("command_center"))) {
            return ProgressionDecision.rejected("faction_required", state);
        }
        if (event.type() == ProgressionEventType.PLANET_VISITED && !PLANETS.contains(event.subjectId())) {
            return ProgressionDecision.rejected("unknown_planet", state);
        }
        if (event.type() == ProgressionEventType.PLANET_VISITED
                && !state.unlocks().contains("planet_travel")
                && !event.subjectId().equals("coruscant")) {
            return ProgressionDecision.rejected("planet_travel_locked", state);
        }
        if (event.type() == ProgressionEventType.QUEST_ADVANCED) {
            ProgressionDecision questValidation = validateQuest(state, event, faction);
            if (questValidation != null) {
                return questValidation;
            }
        }
        try {
            return ProgressionDecision.accepted(state, state.apply(event, faction, unlocks(state, event)));
        } catch (IllegalStateException exception) {
            return ProgressionDecision.rejected(exception.getMessage(), state);
        }
    }

    private static ProgressionDecision validateQuest(
            ProgressionState state,
            ProgressionEvent event,
            String faction
    ) {
        String questId = event.subjectId();
        if (!LaunchContentCatalog.QUESTS.contains(questId)) {
            return ProgressionDecision.rejected("unknown_quest", state);
        }
        String factionPath = faction.substring(faction.indexOf(':') + 1);
        if (!questId.startsWith(factionPath + "_chapter_")) {
            return ProgressionDecision.rejected("wrong_faction_quest", state);
        }
        if (state.hasSubject(ProgressionEventType.QUEST_ADVANCED, questId)) {
            return ProgressionDecision.accepted(state, state);
        }
        int chapter = questId.charAt(questId.length() - 1) - '0';
        if (chapter > 1) {
            String prerequisite = questId.substring(0, questId.length() - 1) + (chapter - 1);
            if (!state.hasSubject(ProgressionEventType.QUEST_ADVANCED, prerequisite)) {
                return ProgressionDecision.rejected("quest_prerequisite_missing", state);
            }
        }
        return null;
    }

    private static Set<String> unlocks(ProgressionState state, ProgressionEvent event) {
        HashSet<String> unlocks = new HashSet<>();
        switch (event.type()) {
            case FACTION_PLEDGED -> unlocks.add("faction_intro");
            case BUILDING_COMPLETED -> {
                String buildingId = subjectPath(event.subjectId());
                if (buildingId.equals("command_center")) {
                    unlocks.addAll(Set.of("treasury", "recruitment", "workforce"));
                }
                if (buildingId.equals("forward_base")) {
                    unlocks.addAll(Set.of("commander", "planet_travel"));
                }
                if (buildingId.equals("supply_depot")) {
                    unlocks.add("vehicle_crafting");
                }
            }
            case DELIVERY_COMPLETED -> {
                if (state.total(ProgressionEventType.DELIVERY_COMPLETED) + Math.max(1, event.amount()) >= 3) {
                    unlocks.add("advanced_trading");
                }
            }
            case QUEST_ADVANCED -> unlocks.addAll(LaunchContentCatalog.questUnlocks(event.subjectId()));
            case VEHICLE_ACQUIRED -> unlocks.add("vehicle_control");
            case REGION_CAPTURED -> unlocks.add("veteran_trades");
            default -> {
            }
        }
        return Set.copyOf(unlocks);
    }

    private static String subjectPath(String subjectId) {
        int separator = subjectId.indexOf(':');
        return separator < 0 ? subjectId : subjectId.substring(separator + 1);
    }
}
