package galacticwars.clonewars.kingdom;

import java.util.Objects;
import java.util.UUID;

public record DiplomacyProposal(
        UUID id, UUID proposerKingdomId, UUID targetKingdomId,
        KingdomRelation relation, long treatyDurationTicks,
        long createdGameTime, long expiresGameTime
) {
    public DiplomacyProposal {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(proposerKingdomId, "proposerKingdomId");
        Objects.requireNonNull(targetKingdomId, "targetKingdomId");
        Objects.requireNonNull(relation, "relation");
        if (proposerKingdomId.equals(targetKingdomId) || relation != KingdomRelation.ALLY
                || treatyDurationTicks <= 0L || createdGameTime < 0L
                || expiresGameTime <= createdGameTime) {
            throw new IllegalArgumentException("Invalid diplomacy proposal");
        }
    }
}
