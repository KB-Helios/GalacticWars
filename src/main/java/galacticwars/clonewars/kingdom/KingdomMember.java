package galacticwars.clonewars.kingdom;

import java.util.Objects;
import java.util.UUID;

public record KingdomMember(UUID playerId, KingdomMemberRole role) {
    public KingdomMember {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(role, "role");
    }
}
