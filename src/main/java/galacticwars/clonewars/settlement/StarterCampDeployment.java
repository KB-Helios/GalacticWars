package galacticwars.clonewars.settlement;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Durable, exact-once state for a kingdom's guided first-camp deployment. */
public record StarterCampDeployment(
        UUID kingdomId,
        String dimensionId,
        int originX,
        int originY,
        int originZ,
        int rotationSteps,
        StarterCampDeploymentPhase phase,
        boolean contractGranted,
        boolean suppliesGranted,
        Optional<UUID> builderId,
        Optional<UUID> projectId,
        String blocker,
        int revision
) {
    public StarterCampDeployment {
        Objects.requireNonNull(kingdomId, "kingdomId");
        dimensionId = requireNonBlank(dimensionId, "dimensionId").toLowerCase(Locale.ROOT);
        if (rotationSteps < 0 || rotationSteps > 3) {
            throw new IllegalArgumentException("rotationSteps must be between 0 and 3");
        }
        Objects.requireNonNull(phase, "phase");
        builderId = builderId == null ? Optional.empty() : builderId;
        projectId = projectId == null ? Optional.empty() : projectId;
        blocker = blocker == null ? "" : blocker.trim().toLowerCase(Locale.ROOT);
        if (revision < 0) {
            throw new IllegalArgumentException("revision cannot be negative");
        }
        if (phase == StarterCampDeploymentPhase.COMPLETE
                && (!contractGranted || !suppliesGranted || builderId.isEmpty() || projectId.isEmpty())) {
            throw new IllegalArgumentException("completed starter camp is missing its durable grants");
        }
    }

    public static StarterCampDeployment awaiting(
            UUID kingdomId,
            String dimensionId,
            int originX,
            int originY,
            int originZ,
            int rotationSteps
    ) {
        return new StarterCampDeployment(
                kingdomId, dimensionId, originX, originY, originZ, rotationSteps,
                StarterCampDeploymentPhase.AWAITING_CONFIRMATION, false, false,
                Optional.empty(), Optional.empty(), "", 0);
    }

    public StarterCampDeployment withSuppliesGranted() {
        return next(phase, contractGranted, true, builderId, projectId, "");
    }

    public StarterCampDeployment withBuilder(UUID builderId) {
        return next(phase, true, suppliesGranted, Optional.of(builderId), projectId, "");
    }

    public StarterCampDeployment building(UUID projectId) {
        return next(StarterCampDeploymentPhase.BUILDING, contractGranted, suppliesGranted,
                builderId, Optional.of(projectId), "");
    }

    public StarterCampDeployment blocked(String reason) {
        return next(StarterCampDeploymentPhase.BLOCKED, contractGranted, suppliesGranted,
                builderId, projectId, requireNonBlank(reason, "reason"));
    }

    /** Records that an unfinished project is recoverable but currently has no embodied builder. */
    public StarterCampDeployment blockedWithoutBuilder(String reason) {
        return next(StarterCampDeploymentPhase.BLOCKED, contractGranted, suppliesGranted,
                Optional.empty(), projectId, requireNonBlank(reason, "reason"));
    }

    public StarterCampDeployment complete() {
        return next(StarterCampDeploymentPhase.COMPLETE, contractGranted, suppliesGranted,
                builderId, projectId, "");
    }

    public StarterCampDeployment packedUp() {
        return next(StarterCampDeploymentPhase.PACKED_UP, contractGranted, suppliesGranted,
                builderId, projectId, "");
    }

    public StarterCampDeployment relocate(
            String dimensionId,
            int originX,
            int originY,
            int originZ,
            int rotationSteps
    ) {
        return new StarterCampDeployment(
                kingdomId, dimensionId, originX, originY, originZ, rotationSteps,
                StarterCampDeploymentPhase.AWAITING_CONFIRMATION,
                contractGranted, suppliesGranted, builderId, Optional.empty(), "", revision + 1);
    }

    public StarterCampDeployment reorient(int rotationSteps) {
        if (projectId.isPresent()) {
            return this;
        }
        return new StarterCampDeployment(
                kingdomId, dimensionId, originX, originY, originZ, rotationSteps,
                StarterCampDeploymentPhase.AWAITING_CONFIRMATION,
                contractGranted, suppliesGranted, builderId, Optional.empty(), "", revision + 1);
    }

    public boolean terminal() {
        return phase == StarterCampDeploymentPhase.COMPLETE;
    }

    private StarterCampDeployment next(
            StarterCampDeploymentPhase nextPhase,
            boolean nextContractGranted,
            boolean nextSuppliesGranted,
            Optional<UUID> nextBuilderId,
            Optional<UUID> nextProjectId,
            String nextBlocker
    ) {
        return new StarterCampDeployment(
                kingdomId, dimensionId, originX, originY, originZ, rotationSteps,
                nextPhase, nextContractGranted, nextSuppliesGranted,
                nextBuilderId, nextProjectId, nextBlocker, revision + 1);
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
