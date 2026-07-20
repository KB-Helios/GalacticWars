package galacticwars.clonewars.entity.ai;

import galacticwars.clonewars.army.ArmyPosition;
import java.util.Objects;

/** Ephemeral progress and bounded retry state for a squad movement target. */
public record ArmyPathStatus(
        ArmyPosition target,
        double bestDistanceSquared,
        int stalledTicks,
        int retryAfterTick
) {
    public ArmyPathStatus {
        Objects.requireNonNull(target, "target");
        if (!Double.isFinite(bestDistanceSquared) || bestDistanceSquared < 0.0D) {
            throw new IllegalArgumentException("bestDistanceSquared must be finite and non-negative");
        }
        if (stalledTicks < 0) {
            throw new IllegalArgumentException("stalledTicks cannot be negative");
        }
    }

    public static ArmyPathStatus tracking(ArmyPosition target, double distanceSquared) {
        return new ArmyPathStatus(target, Math.max(0.0D, distanceSquared), 0, 0);
    }

    public ArmyPathStatus progressed(double distanceSquared) {
        return new ArmyPathStatus(target, Math.max(0.0D, distanceSquared), 0, 0);
    }

    public ArmyPathStatus stalled(double distanceSquared) {
        return new ArmyPathStatus(target, Math.max(0.0D, bestDistanceSquared), stalledTicks + 1, 0);
    }

    public ArmyPathStatus retryAfter(int gameTick, double distanceSquared) {
        return new ArmyPathStatus(target, Math.max(0.0D, distanceSquared), 0, gameTick);
    }
}
