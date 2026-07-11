package galacticwars.clonewars.progression;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class GalacticSystemsService {
    private static final Set<String> REGIONS = Set.of(
            "tatooine_spaceport", "geonosis_foundry", "kamino_platform", "coruscant_district");
    private static final Map<String, String> VEHICLE_REQUIREMENTS = Map.of(
            "barc_speeder", "republic_chapter_2",
            "at_rt", "republic_chapter_3",
            "stap", "separatist_chapter_2",
            "aat", "separatist_chapter_3",
            "laat_gunship", "vehicle_mastery");
    private static final Map<String, String> FORCE_REQUIREMENTS = Map.of(
            "light_push", "republic_chapter_2",
            "light_pull", "republic_chapter_2",
            "light_leap", "republic_chapter_3",
            "dark_push", "nightsister_chapter_2",
            "dark_choke", "nightsister_chapter_3",
            "dark_dash", "nightsister_chapter_2");

    private GalacticSystemsService() {
    }

    public static SystemDecision acquireVehicle(
            ProgressionState state,
            UUID eventId,
            String vehicleId
    ) {
        if (!LaunchContentCatalog.VEHICLES.contains(vehicleId)) {
            return SystemDecision.rejected("unknown_vehicle", state);
        }
        String requirement = VEHICLE_REQUIREMENTS.get(vehicleId);
        if (!requirementSatisfied(state, requirement)) {
            return SystemDecision.rejected("vehicle_quest_locked", state);
        }
        return apply(state, new ProgressionEvent(eventId, state.playerId(),
                ProgressionEventType.VEHICLE_ACQUIRED, vehicleId, 1));
    }

    public static SystemDecision unlockForceAbility(
            ProgressionState state,
            UUID eventId,
            String abilityId
    ) {
        if (!LaunchContentCatalog.FORCE_ABILITIES.contains(abilityId)) {
            return SystemDecision.rejected("unknown_force_ability", state);
        }
        if (!state.unlocks().contains("force_path")) {
            return SystemDecision.rejected("force_path_locked", state);
        }
        if (!requirementSatisfied(state, FORCE_REQUIREMENTS.get(abilityId))) {
            return SystemDecision.rejected("force_quest_locked", state);
        }
        if (state.hasSubject(ProgressionEventType.FORCE_ABILITY_UNLOCKED, abilityId)) {
            return SystemDecision.duplicate(state);
        }
        String chosenPath = abilityId.substring(0, abilityId.indexOf('_'));
        Set<String> unlocked = state.eventSubjects().getOrDefault(
                ProgressionEventType.FORCE_ABILITY_UNLOCKED, Set.of());
        boolean otherPath = unlocked.stream().anyMatch(existing -> !existing.startsWith(chosenPath + "_"));
        if (otherPath) {
            return SystemDecision.rejected("force_path_already_chosen", state);
        }
        return apply(state, new ProgressionEvent(eventId, state.playerId(),
                ProgressionEventType.FORCE_ABILITY_UNLOCKED, abilityId, 1));
    }

    public static SystemDecision purchase(
            ProgressionState state,
            UUID eventId,
            String tradeId
    ) {
        LaunchContentCatalog.TradeDefinition trade = LaunchContentCatalog.TRADES.get(tradeId);
        if (trade == null) {
            return SystemDecision.rejected("unknown_trade", state);
        }
        if (!state.factionId().equals("galacticwars:" + trade.factionId())) {
            return SystemDecision.rejected("hostile_merchant", state);
        }
        if (!state.unlocks().contains(trade.requiredUnlock())) {
            return SystemDecision.rejected("trade_locked", state);
        }
        ProgressionDecision payment = GalacticProgressionCoordinator.apply(state,
                new ProgressionEvent(eventId, state.playerId(), ProgressionEventType.CREDIT_TRANSACTION,
                        "trade:" + tradeId, -trade.price()));
        if (!payment.accepted()) {
            return SystemDecision.rejected(payment.reason(), state);
        }
        if (!payment.changed()) {
            return SystemDecision.duplicate(payment.state());
        }
        UUID completionId = derived(eventId, "trade");
        ProgressionDecision completion = GalacticProgressionCoordinator.apply(payment.state(),
                new ProgressionEvent(completionId, state.playerId(), ProgressionEventType.TRADE_COMPLETED,
                        tradeId, 1));
        if (!completion.accepted()) {
            return SystemDecision.rejected(completion.reason(), state);
        }
        return new SystemDecision(true, completion.changed(), completion.reason(), completion.state(),
                completion.changed() ? trade.itemId() : "", completion.changed() ? trade.itemCount() : 0);
    }

    public static SystemDecision captureRegion(
            ProgressionState state,
            UUID eventId,
            String regionId
    ) {
        if (!REGIONS.contains(regionId)) {
            return SystemDecision.rejected("unknown_region", state);
        }
        String factionPath = state.factionId().contains(":")
                ? state.factionId().substring(state.factionId().indexOf(':') + 1)
                : state.factionId();
        boolean chapterTwoComplete = state.hasSubject(
                ProgressionEventType.QUEST_ADVANCED, factionPath + "_chapter_2");
        if (!state.unlocks().contains("conquest") && !chapterTwoComplete) {
            return SystemDecision.rejected("conquest_locked", state);
        }
        return apply(state, new ProgressionEvent(eventId, state.playerId(),
                ProgressionEventType.REGION_CAPTURED, regionId, 1));
    }

    private static SystemDecision apply(ProgressionState state, ProgressionEvent event) {
        ProgressionDecision decision = GalacticProgressionCoordinator.apply(state, event);
        return new SystemDecision(decision.accepted(), decision.changed(), decision.reason(), decision.state(),
                decision.changed() ? event.subjectId() : "", decision.changed() ? 1 : 0);
    }

    private static UUID derived(UUID id, String suffix) {
        return UUID.nameUUIDFromBytes((id + ":" + suffix).getBytes(StandardCharsets.UTF_8));
    }

    private static boolean requirementSatisfied(ProgressionState state, String requirement) {
        return requirement != null && (state.unlocks().contains(requirement)
                || state.hasSubject(ProgressionEventType.QUEST_ADVANCED, requirement));
    }

    public record SystemDecision(
            boolean accepted,
            boolean changed,
            String reason,
            ProgressionState state,
            String resultId,
            int resultCount
    ) {
        public SystemDecision {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(resultId, "resultId");
            if (resultCount < 0 || (resultId.isEmpty() != (resultCount == 0))) {
                throw new IllegalArgumentException("System result id and count must describe the same reward");
            }
        }

        static SystemDecision rejected(String reason, ProgressionState state) {
            return new SystemDecision(false, false, reason, state, "", 0);
        }

        static SystemDecision duplicate(ProgressionState state) {
            return new SystemDecision(true, false, "duplicate_event", state, "", 0);
        }
    }
}
