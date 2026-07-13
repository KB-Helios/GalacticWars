package galacticwars.clonewars.ability;

import java.util.Objects;

public record AbilityDefinition(
        AbilityId id,
        String displayName,
        AbilityKind kind,
        AbilityActivation activation,
        int cooldownTicks,
        int resourceCost,
        double range,
        int aiEvaluationIntervalTicks,
        boolean enabled
) {
    public AbilityDefinition {
        Objects.requireNonNull(id, "id");
        displayName = requireNonBlank(displayName, "displayName");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(activation, "activation");
        requireNonNegative(cooldownTicks, "cooldownTicks");
        requireNonNegative(resourceCost, "resourceCost");
        if (!Double.isFinite(range) || range < 0.0D) {
            throw new IllegalArgumentException("range cannot be negative or non-finite");
        }
        if (aiEvaluationIntervalTicks < 1) {
            throw new IllegalArgumentException("aiEvaluationIntervalTicks must be positive");
        }
        if (kind == AbilityKind.FORCE && enabled) {
            throw new IllegalArgumentException("Force abilities are reserved but cannot be enabled yet");
        }
        if (activation == AbilityActivation.PASSIVE
                && (cooldownTicks != 0 || resourceCost != 0)) {
            throw new IllegalArgumentException("Passive abilities cannot consume resources or use cooldowns");
        }
    }

    public boolean active() {
        return activation != AbilityActivation.PASSIVE;
    }

    private static void requireNonNegative(int value, String label) {
        if (value < 0) {
            throw new IllegalArgumentException(label + " cannot be negative");
        }
    }

    private static String requireNonBlank(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return normalized;
    }
}
