package galacticwars.clonewars.classes;

import galacticwars.clonewars.ability.AbilityId;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record ClassProgressState(
        int schemaVersion,
        String classId,
        int rank,
        long experience,
        int resource,
        Map<String, Long> cooldownEnds
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int MAX_RANK = 10;
    public static final int MAX_RESOURCE = 100;

    public ClassProgressState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported class progress schema " + schemaVersion);
        }
        classId = classId == null ? "" : classId.trim().toLowerCase();
        if (rank < 0 || rank > MAX_RANK || classId.isEmpty() != (rank == 0)) {
            throw new IllegalArgumentException("rank does not match class assignment");
        }
        if (experience < 0L) {
            throw new IllegalArgumentException("experience cannot be negative");
        }
        if (resource < 0 || resource > MAX_RESOURCE) {
            throw new IllegalArgumentException("resource must be between 0 and " + MAX_RESOURCE);
        }
        HashMap<String, Long> normalizedCooldowns = new HashMap<>();
        for (Map.Entry<String, Long> entry : Objects.requireNonNull(cooldownEnds, "cooldownEnds").entrySet()) {
            String abilityId = AbilityId.of(entry.getKey()).toString();
            long end = Objects.requireNonNull(entry.getValue(), "cooldown end");
            if (end < 0L) {
                throw new IllegalArgumentException("cooldown end cannot be negative");
            }
            normalizedCooldowns.put(abilityId, end);
        }
        cooldownEnds = Map.copyOf(normalizedCooldowns);
    }

    public static ClassProgressState unassigned() {
        return new ClassProgressState(CURRENT_SCHEMA_VERSION, "", 0, 0L, MAX_RESOURCE, Map.of());
    }

    public ClassProgressState assign(UnitClassId classId) {
        String normalized = Objects.requireNonNull(classId, "classId").toString();
        if (this.classId.equals(normalized)) {
            return this;
        }
        return new ClassProgressState(CURRENT_SCHEMA_VERSION, normalized, 1, 0L, MAX_RESOURCE, Map.of());
    }

    public ClassProgressState gainExperience(long amount) {
        if (amount <= 0L || classId.isEmpty() || rank >= MAX_RANK) {
            return this;
        }
        long total = experience > Long.MAX_VALUE - amount ? Long.MAX_VALUE : experience + amount;
        int nextRank = rank;
        long remaining = total;
        while (nextRank < MAX_RANK) {
            long threshold = 100L * nextRank;
            if (remaining < threshold) {
                break;
            }
            remaining -= threshold;
            nextRank++;
        }
        if (nextRank == MAX_RANK) {
            remaining = 0L;
        }
        return new ClassProgressState(schemaVersion, classId, nextRank, remaining,
                resource, cooldownEnds);
    }

    public ClassProgressState regenerate(int amount) {
        if (amount <= 0) {
            return this;
        }
        return new ClassProgressState(schemaVersion, classId, rank, experience,
                (int) Math.min(MAX_RESOURCE, (long) resource + amount), cooldownEnds);
    }

    ClassProgressState activate(AbilityId abilityId, int resourceCost, long cooldownEnd, long gameTime) {
        HashMap<String, Long> updated = new HashMap<>();
        cooldownEnds.forEach((id, end) -> {
            if (end > gameTime) {
                updated.put(id, end);
            }
        });
        updated.put(abilityId.toString(), cooldownEnd);
        return new ClassProgressState(schemaVersion, classId, rank, experience,
                resource - resourceCost, updated);
    }
}
