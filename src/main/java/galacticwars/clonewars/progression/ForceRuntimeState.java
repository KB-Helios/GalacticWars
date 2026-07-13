package galacticwars.clonewars.progression;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record ForceRuntimeState(
        String path,
        int energy,
        Map<String, Long> cooldownEnds,
        Set<UUID> processedActivationIds
) {
    public static final int MAX_ENERGY = 100;

    public ForceRuntimeState {
        path = path == null ? "" : path;
        if (!path.isEmpty() && !path.equals("light") && !path.equals("dark")) {
            throw new IllegalArgumentException("Unknown Force path " + path);
        }
        if (energy < 0 || energy > MAX_ENERGY) {
            throw new IllegalArgumentException("Force energy must be between 0 and " + MAX_ENERGY);
        }
        cooldownEnds = Map.copyOf(cooldownEnds);
        processedActivationIds = Set.copyOf(processedActivationIds);
    }

    public ForceRuntimeState(int energy, Map<String, Long> cooldownEnds) {
        this("", energy, cooldownEnds, Set.of());
    }

    public static ForceRuntimeState full() {
        return new ForceRuntimeState("", MAX_ENERGY, Map.of(), Set.of());
    }

    public ForceRuntimeState withPath(String selectedPath) {
        return new ForceRuntimeState(selectedPath, energy, cooldownEnds, processedActivationIds);
    }

    public ForceRuntimeState regenerate(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("regeneration amount cannot be negative");
        }
        int regeneratedEnergy = (int) Math.min(MAX_ENERGY, (long) energy + amount);
        return new ForceRuntimeState(path, regeneratedEnergy, cooldownEnds, processedActivationIds);
    }
}
