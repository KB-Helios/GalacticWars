package galacticwars.clonewars.workforce.logistics;

import java.util.Objects;
import java.util.UUID;

/** Expected endpoint identities and actor supplied by the caller for one transfer. */
public record LogisticsTransferAuthority(
        UUID actorId,
        LogisticsEndpointIdentity expectedSource,
        LogisticsEndpointIdentity expectedDestination
) {
    public LogisticsTransferAuthority {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(expectedSource, "expectedSource");
        Objects.requireNonNull(expectedDestination, "expectedDestination");
    }
}
