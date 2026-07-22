package galacticwars.clonewars.force;

/** Dependency-light bounds and formulas shared by runtime physics and reducer tests. */
public final class ForcePhysicsRules {
    public static final double MAX_RANGE = 32.0D;
    public static final double MAX_VELOCITY = 2.5D;
    public static final double MAX_COLLISION_DAMAGE = 12.0D;
    public static final int COLLISION_IMMUNITY_TICKS = 10;
    public static final int MAX_CHANNEL_TICKS = 100;
    public static final int MAX_AOE_TARGETS = 16;
    public static final int MAX_ACTIVE_CHANNELS = 64;
    public static final int MAX_LIFTED_BLOCKS = 32;

    private ForcePhysicsRules() {
    }

    public static double boundedRange(double requested) {
        if (!Double.isFinite(requested)) return 0.0D;
        return Math.max(0.0D, Math.min(MAX_RANGE, requested));
    }

    public static int boundedChannelTicks(int requested) {
        return Math.max(0, Math.min(MAX_CHANNEL_TICKS, requested));
    }

    public static double impulseForMass(
            double requestedImpulse, double mass, double knockbackResistance,
            boolean bossAnchored
    ) {
        if (!Double.isFinite(requestedImpulse) || !Double.isFinite(mass)
                || !Double.isFinite(knockbackResistance) || requestedImpulse <= 0.0D) {
            return 0.0D;
        }
        double resistance = Math.max(0.0D, Math.min(1.0D, knockbackResistance));
        double scaled = requestedImpulse * (1.0D - resistance) / Math.max(0.25D, mass);
        if (bossAnchored) scaled *= 0.2D;
        return Math.max(0.0D, Math.min(MAX_VELOCITY, scaled));
    }

    public static double cappedCollisionDamage(double observedVelocity, double mass) {
        if (!Double.isFinite(observedVelocity) || !Double.isFinite(mass)
                || observedVelocity <= 0.45D || mass <= 0.0D) {
            return 0.0D;
        }
        return Math.min(MAX_COLLISION_DAMAGE,
                Math.max(0.0D, (observedVelocity - 0.45D) * Math.sqrt(mass) * 4.0D));
    }
}
