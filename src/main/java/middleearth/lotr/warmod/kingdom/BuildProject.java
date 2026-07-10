package middleearth.lotr.warmod.kingdom;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record BuildProject(
        UUID id,
        String blueprintId,
        String dimensionId,
        int originX,
        int originY,
        int originZ,
        int rotationSteps,
        List<Integer> completedPlacements,
        String blockedReason
) {
    public BuildProject {
        Objects.requireNonNull(id, "id");
        blueprintId = normalize(blueprintId, "blueprintId");
        dimensionId = normalize(dimensionId, "dimensionId");
        rotationSteps = Math.floorMod(rotationSteps, 4);
        Objects.requireNonNull(completedPlacements, "completedPlacements");
        LinkedHashSet<Integer> normalized = new LinkedHashSet<>();
        for (Integer placement : completedPlacements) {
            if (placement == null || placement < 0) {
                throw new IllegalArgumentException("completed placement indices cannot be negative");
            }
            normalized.add(placement);
        }
        completedPlacements = List.copyOf(normalized);
        blockedReason = blockedReason == null ? "" : blockedReason.trim();
    }

    public BuildProject markCompleted(int placement) {
        LinkedHashSet<Integer> updated = new LinkedHashSet<>(completedPlacements);
        updated.add(placement);
        return new BuildProject(id, blueprintId, dimensionId, originX, originY, originZ,
                rotationSteps, List.copyOf(updated), "");
    }

    private static String normalize(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return normalized;
    }
}
