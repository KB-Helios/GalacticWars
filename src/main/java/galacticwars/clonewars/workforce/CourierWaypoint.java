package galacticwars.clonewars.workforce;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record CourierWaypoint(
        String dimensionId,
        int x,
        int y,
        int z,
        List<CourierTransferAction> actions
) {
    public CourierWaypoint {
        Objects.requireNonNull(dimensionId, "dimensionId");
        dimensionId = dimensionId.trim().toLowerCase(Locale.ROOT);
        if (dimensionId.isBlank()) {
            throw new IllegalArgumentException("dimensionId cannot be blank");
        }
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
    }
}
