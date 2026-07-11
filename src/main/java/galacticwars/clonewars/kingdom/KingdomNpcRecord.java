package galacticwars.clonewars.kingdom;

import galacticwars.clonewars.recruitment.NpcServiceBranch;
import java.util.Objects;
import java.util.UUID;

public record KingdomNpcRecord(UUID recruitId, UUID settlementId, NpcServiceBranch serviceBranch) {
    public KingdomNpcRecord {
        Objects.requireNonNull(recruitId, "recruitId");
        Objects.requireNonNull(settlementId, "settlementId");
        Objects.requireNonNull(serviceBranch, "serviceBranch");
    }

    public KingdomNpcRecord withServiceBranch(NpcServiceBranch branch) {
        return branch == serviceBranch ? this : new KingdomNpcRecord(recruitId, settlementId, branch);
    }
}
