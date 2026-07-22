package galacticwars.clonewars.army;

import java.util.Objects;

/** Persisted adaptive-formation state shared by live and virtual squad execution. */
public record ArmyMarchState(
        ArmyMarchPhase phase,
        ArmyFormation activeFormation,
        int cohesionPercent,
        float yawDegrees,
        long phaseSinceGameTime
) {
    public ArmyMarchState {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(activeFormation, "activeFormation");
        if (cohesionPercent < 0 || cohesionPercent > 100) {
            throw new IllegalArgumentException("cohesionPercent must be between 0 and 100");
        }
        if (!Float.isFinite(yawDegrees)) {
            throw new IllegalArgumentException("yawDegrees must be finite");
        }
        if (phaseSinceGameTime < 0L) {
            throw new IllegalArgumentException("phaseSinceGameTime cannot be negative");
        }
        yawDegrees = normalizeYaw(yawDegrees);
    }

    public static ArmyMarchState halted(ArmyFormation formation) {
        return new ArmyMarchState(ArmyMarchPhase.HALTED, formation, 100, 0.0F, 0L);
    }

    public static ArmyMarchState forming(ArmyFormation formation, float yawDegrees, long gameTime) {
        return new ArmyMarchState(ArmyMarchPhase.FORMING, formation, 0, yawDegrees, Math.max(0L, gameTime));
    }

    public ArmyMarchState transition(
            ArmyMarchPhase nextPhase,
            ArmyFormation nextFormation,
            int cohesion,
            float yaw,
            long gameTime
    ) {
        long since = nextPhase == phase ? phaseSinceGameTime : Math.max(0L, gameTime);
        return new ArmyMarchState(nextPhase, nextFormation, cohesion, yaw, since);
    }

    private static float normalizeYaw(float yaw) {
        float normalized = yaw % 360.0F;
        if (normalized >= 180.0F) {
            normalized -= 360.0F;
        } else if (normalized < -180.0F) {
            normalized += 360.0F;
        }
        return normalized;
    }
}
