package galacticwars.clonewars.world;

import galacticwars.clonewars.recruitment.NpcServiceBranch;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record FactionOutpostRecord(
        UUID id,
        String factionId,
        String dimensionId,
        int x,
        int y,
        int z,
        int radius,
        List<UUID> militaryNpcIds,
        List<UUID> civilianNpcIds,
        long lastActivityGameTime
) {
    public FactionOutpostRecord {
        Objects.requireNonNull(id, "id");
        factionId = required(factionId, "factionId");
        dimensionId = required(dimensionId, "dimensionId");
        if (radius < 16 || lastActivityGameTime < 0) throw new IllegalArgumentException("invalid outpost bounds");
        militaryNpcIds = List.copyOf(new LinkedHashSet<>(Objects.requireNonNull(militaryNpcIds, "militaryNpcIds")));
        civilianNpcIds = List.copyOf(new LinkedHashSet<>(Objects.requireNonNull(civilianNpcIds, "civilianNpcIds")));
        if (militaryNpcIds.stream().anyMatch(civilianNpcIds::contains)) {
            throw new IllegalArgumentException("NPC cannot be both military and civilian");
        }
    }

    public static FactionOutpostRecord create(
            String factionId, String dimensionId, int x, int y, int z, int radius, long gameTime
    ) {
        return new FactionOutpostRecord(UUID.randomUUID(), factionId, dimensionId,
                x, y, z, radius, List.of(), List.of(), gameTime);
    }

    public boolean contains(UUID npcId) {
        return militaryNpcIds.contains(npcId) || civilianNpcIds.contains(npcId);
    }

    public long distanceSquared(int targetX, int targetZ) {
        long dx = (long) x - targetX;
        long dz = (long) z - targetZ;
        return dx * dx + dz * dz;
    }

    public FactionOutpostRecord withNpc(UUID npcId, NpcServiceBranch branch, long gameTime) {
        LinkedHashSet<UUID> military = new LinkedHashSet<>(militaryNpcIds);
        LinkedHashSet<UUID> civilians = new LinkedHashSet<>(civilianNpcIds);
        military.remove(npcId);
        civilians.remove(npcId);
        (branch == NpcServiceBranch.MILITARY ? military : civilians).add(npcId);
        return new FactionOutpostRecord(id, factionId, dimensionId, x, y, z, radius,
                List.copyOf(military), List.copyOf(civilians), gameTime);
    }

    public FactionOutpostRecord withoutNpc(UUID npcId, long gameTime) {
        if (!contains(npcId)) return this;
        return new FactionOutpostRecord(id, factionId, dimensionId, x, y, z, radius,
                militaryNpcIds.stream().filter(id -> !id.equals(npcId)).toList(),
                civilianNpcIds.stream().filter(id -> !id.equals(npcId)).toList(), gameTime);
    }

    private static String required(String value, String label) {
        value = Objects.requireNonNull(value, label).trim().toLowerCase(java.util.Locale.ROOT);
        if (value.isEmpty()) throw new IllegalArgumentException(label + " cannot be blank");
        return value;
    }
}
