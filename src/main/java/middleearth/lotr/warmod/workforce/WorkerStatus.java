package middleearth.lotr.warmod.workforce;

import java.util.Objects;
import java.util.Optional;

public record WorkerStatus(
        WorkerPhase phase,
        String reasonCode,
        Optional<Target> target
) {
    public WorkerStatus {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(reasonCode, "reasonCode");
        reasonCode = reasonCode.trim();
        if (reasonCode.isEmpty()) {
            throw new IllegalArgumentException("reasonCode cannot be blank");
        }
        target = target == null ? Optional.empty() : target;
    }

    public record Target(String dimensionId, int x, int y, int z) {
        public Target {
            Objects.requireNonNull(dimensionId, "dimensionId");
        }
    }
}
