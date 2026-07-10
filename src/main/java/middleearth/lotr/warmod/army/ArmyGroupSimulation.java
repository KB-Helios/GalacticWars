package middleearth.lotr.warmod.army;

import java.util.Objects;

public record ArmyGroupSimulation(
        ArmyGroupLifecycleState lifecycleState,
        ArmyLocation anchor,
        long lastSimulationGameTime,
        long revision,
        long snapshotGeneration,
        String blockedReason
) {
    public ArmyGroupSimulation {
        Objects.requireNonNull(lifecycleState, "lifecycleState");
        Objects.requireNonNull(anchor, "anchor");
        if (lastSimulationGameTime < 0L || revision < 0L || snapshotGeneration < 0L) {
            throw new IllegalArgumentException("Army simulation counters cannot be negative");
        }
        blockedReason = blockedReason == null ? "" : blockedReason.trim();
    }

    public ArmyGroupSimulation advance(ArmyLocation anchor, long gameTime, String blockedReason) {
        return new ArmyGroupSimulation(
                lifecycleState, anchor, Math.max(lastSimulationGameTime, gameTime), revision + 1,
                snapshotGeneration, blockedReason);
    }

    public ArmyGroupSimulation withLifecycle(ArmyGroupLifecycleState state, long gameTime, long generation) {
        return new ArmyGroupSimulation(state, anchor, Math.max(lastSimulationGameTime, gameTime), revision + 1,
                Math.max(snapshotGeneration, generation), "");
    }
}
