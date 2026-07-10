package middleearth.lotr.warmod.kingdom;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record WorkOrder(
        UUID id,
        WorkOrderType type,
        Optional<UUID> assignedRecruitId,
        WorkOrderState state,
        Optional<UUID> worksiteId,
        Optional<UUID> projectId,
        String dimensionId,
        int targetX,
        int targetY,
        int targetZ,
        String resourceId,
        int quantity,
        int completedQuantity,
        String blockedReason,
        int revision
) {
    public WorkOrder {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        assignedRecruitId = assignedRecruitId == null ? Optional.empty() : assignedRecruitId;
        Objects.requireNonNull(state, "state");
        worksiteId = worksiteId == null ? Optional.empty() : worksiteId;
        projectId = projectId == null ? Optional.empty() : projectId;
        dimensionId = KingdomNormalizers.normalize(dimensionId, "dimensionId");
        resourceId = resourceId == null ? "" : resourceId.trim().toLowerCase(Locale.ROOT);
        if (quantity < 1 || completedQuantity < 0 || completedQuantity > quantity || revision < 0) {
            throw new IllegalArgumentException("invalid work order progress or revision");
        }
        blockedReason = blockedReason == null ? "" : blockedReason.trim();
        if ((state == WorkOrderState.CLAIMED || state == WorkOrderState.IN_PROGRESS)
                && assignedRecruitId.isEmpty()) {
            throw new IllegalArgumentException("active work order requires an assigned recruit");
        }
        if ((state == WorkOrderState.QUEUED || state == WorkOrderState.CANCELLED)
                && assignedRecruitId.isPresent()) {
            throw new IllegalArgumentException("unassigned work order state cannot retain a recruit");
        }
        if (state == WorkOrderState.COMPLETED && completedQuantity != quantity) {
            throw new IllegalArgumentException("completed work order must have full progress");
        }
        if (state == WorkOrderState.BLOCKED && blockedReason.isBlank()) {
            throw new IllegalArgumentException("blocked work order requires a reason");
        }
    }

    public WorkOrder claim(UUID recruitId) {
        if (state != WorkOrderState.QUEUED || assignedRecruitId.isPresent()) {
            return this;
        }
        return copy(Optional.of(recruitId), WorkOrderState.CLAIMED, completedQuantity, "");
    }

    public WorkOrder progress(int amount) {
        if ((state != WorkOrderState.CLAIMED && state != WorkOrderState.IN_PROGRESS)
                || assignedRecruitId.isEmpty() || amount < 1) {
            return this;
        }
        int next = completedQuantity + Math.min(amount, quantity - completedQuantity);
        return copy(assignedRecruitId, next == quantity ? WorkOrderState.COMPLETED : WorkOrderState.IN_PROGRESS,
                next, "");
    }

    public WorkOrder block(String reason) {
        String normalizedReason = reason == null ? "" : reason.trim();
        if (state.terminal() || normalizedReason.isBlank()) {
            return this;
        }
        return copy(assignedRecruitId, WorkOrderState.BLOCKED, completedQuantity, normalizedReason);
    }

    public WorkOrder resume(UUID recruitId) {
        if (state != WorkOrderState.BLOCKED
                || assignedRecruitId.filter(recruitId::equals).isEmpty()) {
            return this;
        }
        return copy(assignedRecruitId,
                completedQuantity == 0 ? WorkOrderState.CLAIMED : WorkOrderState.IN_PROGRESS,
                completedQuantity, "");
    }

    public WorkOrder cancel() {
        return state.terminal() ? this : copy(Optional.empty(), WorkOrderState.CANCELLED, completedQuantity, "cancelled");
    }

    public WorkOrder release() {
        return state.terminal() ? this : copy(Optional.empty(), WorkOrderState.QUEUED, completedQuantity, "");
    }

    private WorkOrder copy(Optional<UUID> assigned, WorkOrderState nextState, int progress, String reason) {
        return new WorkOrder(id, type, assigned, nextState, worksiteId, projectId, dimensionId,
                targetX, targetY, targetZ, resourceId, quantity, progress, reason, revision + 1);
    }
}
