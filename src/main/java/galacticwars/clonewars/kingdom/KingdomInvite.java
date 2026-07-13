package galacticwars.clonewars.kingdom;

import java.util.Objects;
import java.util.UUID;

public record KingdomInvite(
        UUID id, UUID kingdomId, UUID inviterId, UUID targetPlayerId,
        KingdomMemberRole offeredRole, long expiresGameTime
) {
    public KingdomInvite {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kingdomId, "kingdomId");
        Objects.requireNonNull(inviterId, "inviterId");
        Objects.requireNonNull(targetPlayerId, "targetPlayerId");
        Objects.requireNonNull(offeredRole, "offeredRole");
        if (offeredRole == KingdomMemberRole.OWNER || expiresGameTime < 0L) {
            throw new IllegalArgumentException("Invalid kingdom invitation");
        }
    }
}
