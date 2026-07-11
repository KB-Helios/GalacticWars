package galacticwars.clonewars.kingdom;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record KingdomClaim(
        UUID id,
        UUID kingdomId,
        String dimensionId,
        ClaimedChunk center,
        List<ClaimedChunk> chunks,
        boolean capital
) {
    public KingdomClaim {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kingdomId, "kingdomId");
        dimensionId = KingdomNormalizers.normalize(dimensionId, "dimensionId");
        Objects.requireNonNull(center, "center");
        chunks = List.copyOf(new LinkedHashSet<>(Objects.requireNonNull(chunks, "chunks")));
        if (chunks.isEmpty() || !chunks.contains(center)) {
            throw new IllegalArgumentException("claim must contain its center chunk");
        }
    }

    public static KingdomClaim capital(UUID kingdomId, SettlementRecord settlement) {
        ClaimedChunk center = new ClaimedChunk(settlement.hallX() >> 4, settlement.hallZ() >> 4);
        ArrayList<ClaimedChunk> chunks = new ArrayList<>(9);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                chunks.add(new ClaimedChunk(center.x() + x, center.z() + z));
            }
        }
        UUID id = UUID.nameUUIDFromBytes((kingdomId + ":capital").getBytes(StandardCharsets.UTF_8));
        return new KingdomClaim(id, kingdomId, settlement.dimensionId(), center, chunks, true);
    }

    public static KingdomClaim outpost(UUID kingdomId, SettlementRecord settlement) {
        ClaimedChunk center = new ClaimedChunk(settlement.hallX() >> 4, settlement.hallZ() >> 4);
        UUID id = UUID.nameUUIDFromBytes(
                (kingdomId + ":outpost:" + settlement.id()).getBytes(StandardCharsets.UTF_8));
        return new KingdomClaim(id, kingdomId, settlement.dimensionId(), center, List.of(center), false);
    }

    public boolean contains(String dimensionId, int chunkX, int chunkZ) {
        return this.dimensionId.equals(dimensionId) && chunks.contains(new ClaimedChunk(chunkX, chunkZ));
    }

    public boolean canExpandTo(ClaimedChunk chunk) {
        return !chunks.contains(chunk) && chunks.stream().anyMatch(existing -> existing.adjacentTo(chunk));
    }

    public KingdomClaim expandedTo(ClaimedChunk chunk) {
        if (!canExpandTo(chunk)) {
            throw new IllegalArgumentException("claim expansion must be contiguous and new");
        }
        ArrayList<ClaimedChunk> updated = new ArrayList<>(chunks);
        updated.add(chunk);
        return new KingdomClaim(id, kingdomId, dimensionId, center, updated, capital);
    }

    public KingdomClaim transferredTo(UUID newKingdomId) {
        return new KingdomClaim(id, newKingdomId, dimensionId, center, chunks, capital);
    }
}
