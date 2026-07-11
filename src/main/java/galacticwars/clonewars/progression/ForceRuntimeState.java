package galacticwars.clonewars.progression;

import java.util.Map;

public record ForceRuntimeState(int energy, Map<String, Long> cooldownEnds) {
    public static final int MAX_ENERGY = 100;

    public ForceRuntimeState {
        if (energy < 0 || energy > MAX_ENERGY) {
            throw new IllegalArgumentException("Force energy must be between 0 and " + MAX_ENERGY);
        }
        cooldownEnds = Map.copyOf(cooldownEnds);
    }

    public static ForceRuntimeState full() {
        return new ForceRuntimeState(MAX_ENERGY, Map.of());
    }

    public ForceRuntimeState regenerate(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("regeneration amount cannot be negative");
        }
        return new ForceRuntimeState(Math.min(MAX_ENERGY, Math.addExact(energy, amount)), cooldownEnds);
    }
}
