package galacticwars.clonewars.workforce;

import java.util.Objects;

public record WorkerTaskDecision(
        WorkerTaskType taskType,
        WorkAreaType requiredAreaType,
        String reasonCode
) {
    public WorkerTaskDecision {
        Objects.requireNonNull(taskType, "taskType");
        Objects.requireNonNull(requiredAreaType, "requiredAreaType");
        reasonCode = requireNonBlank(reasonCode, "reasonCode");
    }

    private static String requireNonBlank(String value, String label) {
        Objects.requireNonNull(value, label);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return trimmed;
    }
}
