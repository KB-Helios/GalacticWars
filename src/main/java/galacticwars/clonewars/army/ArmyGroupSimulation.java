package galacticwars.clonewars.army;

import java.util.Objects;

public record ArmyGroupSimulation(
        ArmyGroupLifecycleState lifecycleState,
        ArmyLocation anchor,
        long lastSimulationGameTime,
        long revision,
        long snapshotGeneration,
        String blockedReason,
        ArmyMarchState marchState
) {
    public ArmyGroupSimulation {
        Objects.requireNonNull(lifecycleState, "lifecycleState");
        Objects.requireNonNull(anchor, "anchor");
        if (lastSimulationGameTime < 0L || revision < 0L || snapshotGeneration < 0L) {
            throw new IllegalArgumentException("Army simulation counters cannot be negative");
        }
        blockedReason = blockedReason == null ? "" : blockedReason.trim();
        marchState = marchState == null ? ArmyMarchState.halted(ArmyFormation.LINE) : marchState;
    }

    /** Legacy constructor for saves and call sites created before adaptive marching. */
    public ArmyGroupSimulation(
            ArmyGroupLifecycleState lifecycleState,
            ArmyLocation anchor,
            long lastSimulationGameTime,
            long revision,
            long snapshotGeneration,
            String blockedReason
    ) {
        this(lifecycleState, anchor, lastSimulationGameTime, revision, snapshotGeneration,
                blockedReason, ArmyMarchState.halted(ArmyFormation.LINE));
    }

    public ArmyGroupSimulation advance(ArmyLocation anchor, long gameTime, String blockedReason) {
        return advance(anchor, gameTime, blockedReason, marchState);
    }

    public ArmyGroupSimulation advance(
            ArmyLocation anchor,
            long gameTime,
            String blockedReason,
            ArmyMarchState nextMarchState
    ) {
        return new ArmyGroupSimulation(
                lifecycleState, anchor, Math.max(lastSimulationGameTime, gameTime), revision + 1,
                snapshotGeneration, blockedReason, nextMarchState);
    }

    public ArmyGroupSimulation withLifecycle(ArmyGroupLifecycleState state, long gameTime, long generation) {
        return new ArmyGroupSimulation(state, anchor, Math.max(lastSimulationGameTime, gameTime), revision + 1,
                Math.max(snapshotGeneration, generation), "", marchState);
    }

    public ArmyGroupSimulation withMarch(ArmyLocation nextAnchor, ArmyMarchState nextMarch, long gameTime) {
        Objects.requireNonNull(nextAnchor, "nextAnchor");
        Objects.requireNonNull(nextMarch, "nextMarch");
        if (anchor.equals(nextAnchor) && marchState.equals(nextMarch) && blockedReason.isEmpty()) {
            return this;
        }
        return new ArmyGroupSimulation(
                lifecycleState, nextAnchor, Math.max(lastSimulationGameTime, gameTime), revision + 1,
                snapshotGeneration, "", nextMarch);
    }

    public ArmyGroupSimulation touch(String nextBlockedReason) {
        return new ArmyGroupSimulation(lifecycleState, anchor, lastSimulationGameTime,
                revision + 1, snapshotGeneration, nextBlockedReason, marchState);
    }

    public ArmyGroupSimulation resetMarch(ArmyFormation formation) {
        ArmyMarchState reset = ArmyMarchState.forming(
                Objects.requireNonNull(formation, "formation"), marchState.yawDegrees(), lastSimulationGameTime);
        return new ArmyGroupSimulation(lifecycleState, anchor, lastSimulationGameTime,
                revision + 1, snapshotGeneration, "", reset);
    }
}
