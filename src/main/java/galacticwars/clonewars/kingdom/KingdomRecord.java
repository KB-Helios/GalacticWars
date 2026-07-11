package galacticwars.clonewars.kingdom;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record KingdomRecord(
        UUID id,
        UUID ownerId,
        String factionId,
        SettlementRecord settlement
) {
    public KingdomRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(factionId, "factionId");
        factionId = factionId.trim().toLowerCase(Locale.ROOT);
        if (factionId.isEmpty()) {
            throw new IllegalArgumentException("factionId cannot be blank");
        }
        Objects.requireNonNull(settlement, "settlement");
    }

    public KingdomRecord withSettlement(SettlementRecord settlement) {
        return new KingdomRecord(id, ownerId, factionId, settlement);
    }

    public KingdomRecord withFaction(String factionId) {
        return new KingdomRecord(id, ownerId, factionId, settlement);
    }
}
