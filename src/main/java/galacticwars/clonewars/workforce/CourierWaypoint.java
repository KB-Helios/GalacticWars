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
        List<CourierTransferAction> normalizedActions = List.copyOf(
                Objects.requireNonNull(actions, "actions"));
        actions = normalizedActions.size() <= WorkforceCodecs.MAX_ACTIONS_PER_WAYPOINT
                ? normalizedActions
                : List.copyOf(normalizedActions.subList(0, WorkforceCodecs.MAX_ACTIONS_PER_WAYPOINT));
    }
}
