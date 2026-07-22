package galacticwars.clonewars.progression;

import galacticwars.clonewars.data.LaunchContentDefinitions;

import java.util.Objects;
import java.util.UUID;

public final class GalacticSystemsService {
    private GalacticSystemsService() {
    }

    public static SystemDecision acquireVehicle(
            ProgressionState state,
            UUID eventId,
            String vehicleId
    ) {
        return acquireVehicle(state, eventId, vehicleId, LaunchContentCatalog.data());
    }

    static SystemDecision acquireVehicle(
            ProgressionState state,
            UUID eventId,
            String vehicleId,
            LaunchContentDefinitions content
    ) {
        LaunchContentDefinitions.VehicleDefinition vehicle = content.vehicles().get(vehicleId);
        if (vehicle == null) {
            return SystemDecision.rejected("unknown_vehicle", state);
        }
        if (!requirementSatisfied(state, vehicle.requiredUnlock())) {
            return SystemDecision.rejected("vehicle_quest_locked", state);
        }
        return apply(state, new ProgressionEvent(eventId, state.playerId(),
                ProgressionEventType.VEHICLE_ACQUIRED, vehicleId, 1));
    }

    public static SystemDecision useForceAbility(
            ProgressionState state,
            UUID eventId,
            String abilityId
    ) {
        return useForceAbility(state, eventId, abilityId, LaunchContentCatalog.data());
    }

    public static SystemDecision useForceAbility(
            ProgressionState state,
            UUID eventId,
            String abilityId,
            LaunchContentDefinitions content
    ) {
        LaunchContentDefinitions.ForceAbilityDefinition ability =
                content.forceAbilities().get(abilityId);
        if (ability == null) {
            return SystemDecision.rejected("unknown_force_ability", state);
        }
        if (!ability.enabled()) {
            return SystemDecision.rejected("force_ability_disabled", state);
        }
        if (!state.hasSubject(ProgressionEventType.QUEST_ADVANCED, ability.requiredQuest())) {
            return SystemDecision.rejected("force_quest_locked", state);
        }
        String faction = state.factionId().contains(":")
                ? state.factionId().substring(state.factionId().indexOf(':') + 1) : state.factionId();
        String tradition = switch (faction) {
            case "republic" -> "jedi";
            case "separatist" -> "sith";
            case "nightsister" -> "nightsister";
            default -> "";
        };
        String abilityTradition = ability.path().equals("light") ? "jedi"
                : ability.path().equals("dark")
                ? (faction.equals("separatist") ? "sith" : "nightsister") : ability.path();
        if (!tradition.equals(abilityTradition)) {
            return SystemDecision.rejected("force_path_unavailable", state);
        }
        ProgressionDecision decision = CampaignRuntimeService.record(state,
                new ProgressionEvent(eventId, state.playerId(),
                        ProgressionEventType.FORCE_ABILITY_USED, abilityId, 1));
        return new SystemDecision(decision.accepted(), decision.changed(), decision.reason(),
                decision.state(), decision.changed() ? abilityId : "", decision.changed() ? 1 : 0);
    }

    public static SystemDecision captureRegion(
            ProgressionState state,
            UUID eventId,
            String regionId
    ) {
        return captureRegion(state, eventId, regionId, LaunchContentCatalog.data());
    }

    static SystemDecision captureRegion(
            ProgressionState state,
            UUID eventId,
            String regionId,
            LaunchContentDefinitions content
    ) {
        if (!content.conquestRegions().containsKey(regionId)) {
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
