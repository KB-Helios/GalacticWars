package galacticwars.clonewars.progression;

import java.util.Set;
import java.util.UUID;
import java.util.Objects;

public record VehicleRuntimeState(
        String vehicleId,
        UUID ownerId,
        String factionId,
        String dimensionId,
        int fuel,
        int health,
        Set<UUID> passengers,
        boolean paused
) {
    public VehicleRuntimeState {
        Objects.requireNonNull(vehicleId, "vehicleId");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(factionId, "factionId");
        Objects.requireNonNull(dimensionId, "dimensionId");
        Objects.requireNonNull(passengers, "passengers");
        if (vehicleId.isBlank() || factionId.isBlank() || dimensionId.isBlank()) {
            throw new IllegalArgumentException("vehicle identity fields cannot be blank");
        }
        if (fuel < 0 || health < 0) {
            throw new IllegalArgumentException("vehicle fuel and health cannot be negative");
        }
        passengers = Set.copyOf(passengers);
    }
}
