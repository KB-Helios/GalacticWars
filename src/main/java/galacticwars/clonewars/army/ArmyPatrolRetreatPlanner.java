package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.UUID;

/**
 * Pure, bounded escape placement for a loaded patrol that chooses to withdraw
 * from a hostile. The resulting target is transient: callers publish it to
 * the live brain only and leave the durable patrol route/order untouched.
 */
public final class ArmyPatrolRetreatPlanner {
    public static final int RETREAT_STEP_BLOCKS = 12;
    private static final double ZERO_VECTOR_EPSILON = 0.0001D;

    private ArmyPatrolRetreatPlanner() {
    }

    /**
     * Moves the shared squad anchor a small, bounded distance away from the
     * threat. When a threat overlaps the anchor, retreat uses the back of the
     * squad's formation-facing direction so the result stays deterministic.
     */
    public static ArmyPosition retreatAnchor(
            ArmyPosition groupAnchor,
            ArmyPosition threatPosition,
            float formationYawDegrees
    ) {
        Objects.requireNonNull(groupAnchor, "groupAnchor");
        Objects.requireNonNull(threatPosition, "threatPosition");
        if (!Float.isFinite(formationYawDegrees)) {
            throw new IllegalArgumentException("formationYawDegrees must be finite");
        }

        double awayX = (double) groupAnchor.x() - threatPosition.x();
        double awayZ = (double) groupAnchor.z() - threatPosition.z();
        double horizontalDistance = Math.hypot(awayX, awayZ);
        if (horizontalDistance < ZERO_VECTOR_EPSILON) {
            double radians = Math.toRadians(formationYawDegrees);
            // Minecraft yaw zero faces positive Z; retreating uses the back.
            awayX = Math.sin(radians);
            awayZ = -Math.cos(radians);
        } else {
            awayX /= horizontalDistance;
            awayZ /= horizontalDistance;
        }

        return new ArmyPosition(
                boundedCoordinate(groupAnchor.x() + (awayX * RETREAT_STEP_BLOCKS)),
                groupAnchor.y(),
                boundedCoordinate(groupAnchor.z() + (awayZ * RETREAT_STEP_BLOCKS)));
    }

    /**
     * Resolves a member's temporary retreat position around the shared escape
     * anchor, keeping the commander and persisted soldier slots cohesive.
     */
    public static ArmyPosition retreatPosition(
            ArmyGroupRecord group,
            UUID memberId,
            ArmyPosition groupAnchor,
            ArmyPosition threatPosition
    ) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(memberId, "memberId");
        ArmyPosition retreatAnchor = retreatAnchor(
                groupAnchor, threatPosition, group.effectiveTactics().effectiveFormationYawDegrees());
        return ArmyGroupOrderPlanner.formationPositionForMember(group, memberId, retreatAnchor);
    }

    private static int boundedCoordinate(double coordinate) {
        long rounded = Math.round(coordinate);
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, rounded));
    }
}
