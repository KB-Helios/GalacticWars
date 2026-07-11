package middleearth.lotr.warmod.kingdom;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import middleearth.lotr.warmod.settlement.KingdomBaseBlueprint;

public record BuildProject(
        UUID id,
        String blueprintId,
        String dimensionId,
        int originX,
        int originY,
        int originZ,
        int rotationSteps,
        String definitionHash,
        List<Integer> completedPlacements,
        BuildProjectState state,
        String blockedReason,
        int revision
) {
    public BuildProject {
        Objects.requireNonNull(id, "id");
        blueprintId = KingdomBaseBlueprint.canonicalId(blueprintId);
        dimensionId = normalize(dimensionId, "dimensionId");
        if (rotationSteps < 0 || rotationSteps > 3) {
            throw new IllegalArgumentException("rotationSteps must be between 0 and 3");
        }
        definitionHash = normalize(definitionHash, "definitionHash");
        Objects.requireNonNull(completedPlacements, "completedPlacements");
        TreeSet<Integer> normalized = new TreeSet<>();
        for (Integer placement : completedPlacements) {
            if (placement == null || placement < 0) {
                throw new IllegalArgumentException("completed placement indices cannot be negative");
            }
            normalized.add(placement);
        }
        completedPlacements = List.copyOf(normalized);
        Objects.requireNonNull(state, "state");
        blockedReason = blockedReason == null ? "" : blockedReason.trim();
        if (revision < 0) {
            throw new IllegalArgumentException("revision cannot be negative");
        }
    }

    public BuildProject(UUID id, String blueprintId, String dimensionId, int originX, int originY, int originZ,
            int rotationSteps, List<Integer> completedPlacements, String blockedReason) {
        this(id, blueprintId, dimensionId, originX, originY, originZ, rotationSteps, "legacy",
                completedPlacements, blockedReason.isBlank() ? BuildProjectState.ACTIVE : BuildProjectState.BLOCKED,
                blockedReason, 0);
    }

    static BuildProject fromPersistence(
            UUID id,
            String blueprintId,
            String dimensionId,
            int originX,
            int originY,
            int originZ,
            int rotationSteps,
            Optional<String> definitionHash,
            List<Integer> completedPlacements,
            Optional<BuildProjectState> state,
            String blockedReason,
            int revision
    ) {
        BuildProjectState migratedState = state.orElseGet(() -> blockedReason.isBlank()
                ? BuildProjectState.ACTIVE
                : BuildProjectState.BLOCKED);
        return new BuildProject(
                id, blueprintId, dimensionId, originX, originY, originZ, rotationSteps,
                definitionHash.orElse("legacy"), completedPlacements, migratedState, blockedReason, revision);
    }

    public BuildProject markCompleted(int placement) {
        if (state == BuildProjectState.COMPLETED || state == BuildProjectState.CANCELLED) {
            throw new IllegalStateException("terminal build projects cannot advance");
        }
        if (completedPlacements.contains(placement)) {
            return this;
        }
        LinkedHashSet<Integer> updated = new LinkedHashSet<>(completedPlacements);
        updated.add(placement);
        return new BuildProject(id, blueprintId, dimensionId, originX, originY, originZ,
                rotationSteps, definitionHash, List.copyOf(updated), BuildProjectState.ACTIVE, "", revision + 1);
    }

    public BuildProject block(String reason) {
        if (state == BuildProjectState.COMPLETED || state == BuildProjectState.CANCELLED) {
            return this;
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("blocked build projects require a reason");
        }
        String normalizedReason = reason.trim();
        if (state == BuildProjectState.BLOCKED && blockedReason.equals(normalizedReason)) {
            return this;
        }
        return new BuildProject(id, blueprintId, dimensionId, originX, originY, originZ,
                rotationSteps, definitionHash, completedPlacements, BuildProjectState.BLOCKED,
                normalizedReason, revision + 1);
    }

    BuildProject complete() {
        if (state == BuildProjectState.COMPLETED || state == BuildProjectState.CANCELLED) {
            return this;
        }
        return new BuildProject(id, blueprintId, dimensionId, originX, originY, originZ,
                rotationSteps, definitionHash, completedPlacements, BuildProjectState.COMPLETED, "", revision + 1);
    }

    public BuildProject cancel() {
        if (state == BuildProjectState.COMPLETED || state == BuildProjectState.CANCELLED) {
            return this;
        }
        return new BuildProject(id, blueprintId, dimensionId, originX, originY, originZ,
                rotationSteps, definitionHash, completedPlacements, BuildProjectState.CANCELLED,
                "cancelled", revision + 1);
    }

    public boolean sameAuthority(BuildProject other) {
        return other != null
                && id.equals(other.id)
                && blueprintId.equals(other.blueprintId)
                && dimensionId.equals(other.dimensionId)
                && originX == other.originX
                && originY == other.originY
                && originZ == other.originZ
                && rotationSteps == other.rotationSteps
                && definitionHash.equals(other.definitionHash);
    }

    public boolean hasAllPlacements(int placementCount) {
        if (placementCount < 1 || completedPlacements.size() != placementCount) {
            return false;
        }
        for (int index = 0; index < placementCount; index++) {
            if (completedPlacements.get(index) != index) {
                return false;
            }
        }
        return true;
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
