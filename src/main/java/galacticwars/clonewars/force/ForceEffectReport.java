package galacticwars.clonewars.force;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Bounded result of a server-side Force executor. */
public record ForceEffectReport(
        boolean succeeded,
        String reason,
        List<UUID> affectedEntities,
        double movement,
        double damage,
        boolean progressionEligible,
        List<String> visualCues
) {
    public ForceEffectReport {
        Objects.requireNonNull(reason, "reason");
        affectedEntities = List.copyOf(affectedEntities);
        visualCues = List.copyOf(visualCues);
        if (affectedEntities.size() > ForcePhysicsRules.MAX_AOE_TARGETS
                || visualCues.size() > 8 || movement < 0.0D || damage < 0.0D) {
            throw new IllegalArgumentException("Unbounded Force effect report");
        }
    }

    public static ForceEffectReport failed(String reason) {
        return new ForceEffectReport(false, reason, List.of(), 0.0D, 0.0D, false, List.of());
    }
}
