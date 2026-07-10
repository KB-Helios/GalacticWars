package middleearth.lotr.warmod.kingdom;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record WorkOrder(
        UUID id,
        String type,
        Optional<UUID> assignedRecruitId,
        String state,
        String dimensionId,
        int targetX,
        int targetY,
        int targetZ,
        String resourceId,
        int quantity
) {
    public WorkOrder {
        Objects.requireNonNull(id, "id");
        type = KingdomNormalizers.normalize(type, "type");
        assignedRecruitId = assignedRecruitId == null ? Optional.empty() : assignedRecruitId;
        state = KingdomNormalizers.normalize(state, "state");
        dimensionId = KingdomNormalizers.normalize(dimensionId, "dimensionId");
        resourceId = resourceId == null ? "" : resourceId.trim().toLowerCase(Locale.ROOT);
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
    }

}
