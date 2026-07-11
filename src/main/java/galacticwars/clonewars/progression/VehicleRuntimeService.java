package galacticwars.clonewars.progression;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class VehicleRuntimeService {
    private static final Map<String, VehicleSpec> SPECS = Map.of(
            "barc_speeder", new VehicleSpec(1, 40, 1200),
            "at_rt", new VehicleSpec(1, 70, 1600),
            "stap", new VehicleSpec(1, 35, 1200),
            "aat", new VehicleSpec(2, 160, 3000),
            "laat_gunship", new VehicleSpec(8, 220, 5000));

    private VehicleRuntimeService() {
    }

    public static VehicleRuntimeState create(
            String vehicleId,
            UUID ownerId,
            String factionId,
            String dimensionId
    ) {
        VehicleSpec spec = spec(vehicleId);
        return new VehicleRuntimeState(vehicleId, Objects.requireNonNull(ownerId, "ownerId"),
                factionId, dimensionId, spec.fuelCapacity(), spec.maxHealth(), Set.of(), false);
    }

    public static VehicleDecision board(
            VehicleRuntimeState state,
            UUID playerId,
            boolean alliedWithOwner
    ) {
        VehicleSpec spec = spec(state.vehicleId());
        if (state.health() == 0) return VehicleDecision.rejected("vehicle_destroyed", state);
        if (state.paused()) return VehicleDecision.rejected("vehicle_paused", state);
        if (!state.ownerId().equals(playerId) && !alliedWithOwner) {
            return VehicleDecision.rejected("hostile_vehicle", state);
        }
        if (state.passengers().contains(playerId)) {
            return VehicleDecision.duplicate(state);
        }
        if (state.passengers().size() >= spec.seats()) {
            return VehicleDecision.rejected("vehicle_full", state);
        }
        HashSet<UUID> passengers = new HashSet<>(state.passengers());
        passengers.add(playerId);
        return VehicleDecision.accepted(copy(state, state.dimensionId(), state.fuel(), state.health(),
                passengers, false));
    }

    public static VehicleDecision disembark(VehicleRuntimeState state, UUID playerId) {
        if (!state.passengers().contains(playerId)) return VehicleDecision.duplicate(state);
        HashSet<UUID> passengers = new HashSet<>(state.passengers());
        passengers.remove(playerId);
        return VehicleDecision.accepted(copy(state, state.dimensionId(), state.fuel(), state.health(),
                passengers, state.paused()));
    }

    public static VehicleDecision consumeFuel(
            VehicleRuntimeState state,
            UUID driverId,
            int amount
    ) {
        if (amount <= 0) return VehicleDecision.rejected("invalid_fuel_request", state);
        if (!state.ownerId().equals(driverId) || !state.passengers().contains(driverId)) {
            return VehicleDecision.rejected("driver_not_authorized", state);
        }
        if (state.paused() || state.health() == 0) {
            return VehicleDecision.rejected("vehicle_unavailable", state);
        }
        if (state.fuel() < amount) return VehicleDecision.rejected("missing_fuel", state);
        return VehicleDecision.accepted(copy(state, state.dimensionId(), state.fuel() - amount,
                state.health(), state.passengers(), false));
    }

    public static VehicleDecision damage(VehicleRuntimeState state, int amount) {
        if (amount <= 0) return VehicleDecision.rejected("invalid_damage", state);
        int health = Math.max(0, state.health() - amount);
        boolean destroyed = health == 0;
        return VehicleDecision.accepted(copy(state, state.dimensionId(), state.fuel(), health,
                destroyed ? Set.of() : state.passengers(), destroyed || state.paused()));
    }

    public static VehicleDecision transfer(
            VehicleRuntimeState state,
            UUID ownerId,
            String destinationDimension
    ) {
        if (!state.ownerId().equals(ownerId)) return VehicleDecision.rejected("not_vehicle_owner", state);
        if (destinationDimension == null || destinationDimension.isBlank()) {
            return VehicleDecision.rejected("invalid_destination", state);
        }
        if (state.health() == 0) return VehicleDecision.rejected("vehicle_destroyed", state);
        return VehicleDecision.accepted(copy(state, destinationDimension, state.fuel(), state.health(),
                Set.of(), false));
    }

    public static VehicleDecision pauseForUnloadedDimension(VehicleRuntimeState state) {
        if (state.paused()) return VehicleDecision.duplicate(state);
        return VehicleDecision.accepted(copy(state, state.dimensionId(), state.fuel(), state.health(),
                Set.of(), true));
    }

    private static VehicleRuntimeState copy(
            VehicleRuntimeState state,
            String dimensionId,
            int fuel,
            int health,
            Set<UUID> passengers,
            boolean paused
    ) {
        return new VehicleRuntimeState(state.vehicleId(), state.ownerId(), state.factionId(), dimensionId,
                fuel, health, passengers, paused);
    }

    private static VehicleSpec spec(String vehicleId) {
        VehicleSpec spec = SPECS.get(vehicleId);
        if (spec == null) throw new IllegalArgumentException("unknown vehicle " + vehicleId);
        return spec;
    }

    public record VehicleDecision(boolean accepted, boolean changed, String reason, VehicleRuntimeState state) {
        public VehicleDecision {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(state, "state");
        }

        static VehicleDecision accepted(VehicleRuntimeState state) {
            return new VehicleDecision(true, true, "accepted", state);
        }

        static VehicleDecision duplicate(VehicleRuntimeState state) {
            return new VehicleDecision(true, false, "duplicate_request", state);
        }

        static VehicleDecision rejected(String reason, VehicleRuntimeState state) {
            return new VehicleDecision(false, false, reason, state);
        }
    }

    private record VehicleSpec(int seats, int maxHealth, int fuelCapacity) {
    }
}
