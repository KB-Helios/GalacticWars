package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.Optional;

/**
 * Optional squad doctrine. The absent yaw means legacy world-axis placement;
 * the default doctrine deliberately preserves the existing defensive/free-fire
 * behavior until a player changes it.
 */
public record ArmyGroupTactics(
        Optional<Float> formationYawDegrees,
        boolean holdFormation,
        boolean tightFormation,
        ArmyEngagementStance engagementStance,
        ArmyTargetPriority targetPriority,
        ArmyRangedFirePolicy rangedFirePolicy
) {
    public static final ArmyGroupTactics DEFAULT = new ArmyGroupTactics(
            Optional.empty(), false, false, ArmyEngagementStance.DEFENSIVE,
            ArmyTargetPriority.COMMAND_TARGET, ArmyRangedFirePolicy.FREE_FIRE);

    public ArmyGroupTactics {
        formationYawDegrees = normalizeYaw(formationYawDegrees);
        Objects.requireNonNull(engagementStance, "engagementStance");
        Objects.requireNonNull(targetPriority, "targetPriority");
        Objects.requireNonNull(rangedFirePolicy, "rangedFirePolicy");
    }

    public float effectiveFormationYawDegrees() {
        return formationYawDegrees.orElse(0.0F);
    }

    public ArmyGroupTactics withFormationYaw(float formationYawDegrees) {
        return new ArmyGroupTactics(Optional.of(formationYawDegrees), holdFormation, tightFormation,
                engagementStance, targetPriority, rangedFirePolicy);
    }

    public ArmyGroupTactics withFormationControls(boolean holdFormation, boolean tightFormation) {
        return new ArmyGroupTactics(formationYawDegrees, holdFormation, tightFormation,
                engagementStance, targetPriority, rangedFirePolicy);
    }

    public ArmyGroupTactics withEngagement(ArmyEngagementStance engagementStance) {
        return new ArmyGroupTactics(formationYawDegrees, holdFormation, tightFormation,
                engagementStance, targetPriority, rangedFirePolicy);
    }

    public ArmyGroupTactics withTargetPriority(ArmyTargetPriority targetPriority) {
        return new ArmyGroupTactics(formationYawDegrees, holdFormation, tightFormation,
                engagementStance, targetPriority, rangedFirePolicy);
    }

    public ArmyGroupTactics withRangedFirePolicy(ArmyRangedFirePolicy rangedFirePolicy) {
        return new ArmyGroupTactics(formationYawDegrees, holdFormation, tightFormation,
                engagementStance, targetPriority, rangedFirePolicy);
    }

    private static Optional<Float> normalizeYaw(Optional<Float> yawDegrees) {
        Optional<Float> normalized = yawDegrees == null ? Optional.empty() : yawDegrees;
        return normalized.map(value -> {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("formationYawDegrees must be finite");
            }
            float yaw = value % 360.0F;
            return yaw < 0.0F ? yaw + 360.0F : yaw;
        });
    }
}
