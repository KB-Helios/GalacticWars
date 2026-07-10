package middleearth.lotr.warmod.workforce;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record WorkerAssignment(
        WorkerProfession profession,
        String dimensionId,
        int workX,
        int workY,
        int workZ,
        int radius,
        Optional<UUID> workOrderId
) {
    public WorkerAssignment {
        Objects.requireNonNull(profession, "profession");
        Objects.requireNonNull(dimensionId, "dimensionId");
        dimensionId = dimensionId.trim().toLowerCase();
        if (dimensionId.isEmpty()) {
            throw new IllegalArgumentException("dimensionId cannot be blank");
        }
        if (radius < 1 || radius > 32) {
            throw new IllegalArgumentException("radius must be between 1 and 32");
        }
        workOrderId = workOrderId == null ? Optional.empty() : workOrderId;
    }
}
