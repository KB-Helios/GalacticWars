package galacticwars.clonewars.progression;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class GalacticProgressionCoordinator {
    private GalacticProgressionCoordinator() {
    }

    public static ProgressionDecision apply(ProgressionState state, ProgressionEvent event) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(event, "event");
        if (!state.playerId().equals(event.playerId())) {
            return ProgressionDecision.rejected("wrong_player", state);
        }
        if (LaunchContentCatalog.factions().isEmpty()) {
            return ProgressionDecision.rejected("content_unavailable", state);
        }
        if (state.processed(event.id())) {
            return ProgressionDecision.accepted(state, state);
        }
        if (event.type() == ProgressionEventType.CREDIT_TRANSACTION) {
            return ProgressionDecision.rejected("physical_currency_required", state);
        }
        ProgressionIntegrityPolicy.Validation integrity = ProgressionIntegrityPolicy.validate(event);
        if (!integrity.accepted()) {
            return ProgressionDecision.rejected(integrity.reason(), state);
        }
        String faction = state.factionId();
        if (event.type() == ProgressionEventType.FACTION_PLEDGED) {
            if (!LaunchContentCatalog.factions().contains(event.subjectId())) {
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
        if (event.type() == ProgressionEventType.PLANET_VISITED
                && !LaunchContentCatalog.planets().contains(event.subjectId())) {
            return ProgressionDecision.rejected("unknown_planet", state);
        }
        if (event.type() == ProgressionEventType.QUEST_ADVANCED) {
            ProgressionDecision questValidation = validateQuest(state, event, faction);
            if (questValidation != null) {
                return questValidation;
            }
        }
        if (event.type() == ProgressionEventType.CAMPAIGN_COMPLETED) {
            ProgressionDecision campaignValidation = validateCampaign(state, event, faction);
            if (campaignValidation != null) {
                return campaignValidation;
            }
        }
        try {
            return ProgressionDecision.accepted(state, state.apply(event, faction, unlocks(state, event)));
        } catch (ArithmeticException exception) {
            return ProgressionDecision.rejected("progression_limit_reached", state);
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
        if (!LaunchContentCatalog.quests().contains(questId)) {
            return ProgressionDecision.rejected("unknown_quest", state);
        }
        String factionPath = faction.substring(faction.indexOf(':') + 1);
        boolean campaignChapter = questId.matches(
                java.util.regex.Pattern.quote(factionPath) + "_chapter_[1-9][0-9]*");
        boolean forceTraining = questId.matches(
                java.util.regex.Pattern.quote(factionPath) + "_force_training_[1-3]");
        if (!campaignChapter && !forceTraining) {
            return ProgressionDecision.rejected("wrong_faction_quest", state);
        }
        if (state.hasSubject(ProgressionEventType.QUEST_ADVANCED, questId)) {
            return ProgressionDecision.accepted(state, state);
        }
        int chapter = Integer.parseInt(questId.substring(questId.lastIndexOf('_') + 1));
        if (chapter > 1) {
            String prerequisite = questId.substring(0, questId.lastIndexOf('_') + 1) + (chapter - 1);
            if (!state.hasSubject(ProgressionEventType.QUEST_ADVANCED, prerequisite)) {
                return ProgressionDecision.rejected("quest_prerequisite_missing", state);
            }
        }
        for (LaunchContentDefinitions.QuestObjectiveDefinition objective
                : LaunchContentCatalog.questObjectives(questId)) {
            if (!objectiveComplete(state, objective)) {
                return ProgressionDecision.rejected(
                        "quest_objective_missing:" + objective.id(), state);
            }
        }
        return null;
    }

    private static ProgressionDecision validateCampaign(
            ProgressionState state,
            ProgressionEvent event,
            String faction
    ) {
        String factionPath = faction.substring(faction.indexOf(':') + 1);
        String campaignId = factionPath + "_campaign";
        if (!event.subjectId().equals(campaignId)) {
            return ProgressionDecision.rejected("wrong_faction_campaign", state);
        }
        Set<String> chapters = LaunchContentCatalog.quests().stream()
                .filter(quest -> quest.startsWith(factionPath + "_chapter_"))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (chapters.isEmpty()) {
            return ProgressionDecision.rejected("unknown_campaign", state);
        }
        if (chapters.stream().anyMatch(
                chapter -> !state.hasSubject(ProgressionEventType.QUEST_ADVANCED, chapter))) {
            return ProgressionDecision.rejected("campaign_chapter_missing", state);
        }
        return state.hasSubject(ProgressionEventType.CAMPAIGN_COMPLETED, campaignId)
                ? ProgressionDecision.accepted(state, state) : null;
    }

    /** Shared authoritative predicate used by campaign commits and player-facing progress views. */
    public static boolean objectiveComplete(
            ProgressionState state,
            LaunchContentDefinitions.QuestObjectiveDefinition objective
    ) {
        return objective != null && objectiveProgress(state, objective) >= objective.requiredCount();
    }

    /** Returns the exact server-side contribution toward a typed quest objective. */
    public static int objectiveProgress(
            ProgressionState state,
            LaunchContentDefinitions.QuestObjectiveDefinition objective
    ) {
        if (state == null || objective == null) {
            return 0;
        }
        try {
            ProgressionEventType eventType = ProgressionEventType.valueOf(
                    objective.eventType().toUpperCase(Locale.ROOT));
            return state.subjectTotal(eventType, objective.subjectIds());
        } catch (IllegalArgumentException unknownEventType) {
            return 0;
        }
    }

    private static Set<String> unlocks(ProgressionState state, ProgressionEvent event) {
        HashSet<String> unlocks = new HashSet<>();
        switch (event.type()) {
            case FACTION_PLEDGED -> unlocks.add("faction_intro");
            case BUILDING_COMPLETED -> {
                String buildingId = subjectPath(event.subjectId());
                if (buildingId.equals("command_center")) {
                    unlocks.addAll(Set.of("treasury", "recruitment"));
                }
                if (buildingId.equals("starter_camp")) {
                    unlocks.addAll(Set.of("workforce", "army_command", "formation_basic"));
                }
                if (buildingId.equals("barracks")) {
                    unlocks.addAll(Set.of("formation_advanced", "larger_squads"));
                }
                if (buildingId.equals("forward_base")) {
                    unlocks.addAll(Set.of("commander", "planet_travel", "simultaneous_orders"));
                }
                if (buildingId.equals("supply_depot")) {
                    unlocks.addAll(Set.of("vehicle_crafting", "sustained_march", "field_resupply"));
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
            case CAMPAIGN_COMPLETED -> unlocks.addAll(Set.of(
                    "campaign_victory", "veteran_operations"));
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
