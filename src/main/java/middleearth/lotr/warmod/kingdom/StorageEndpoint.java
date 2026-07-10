package middleearth.lotr.warmod.kingdom;

import java.util.Objects;

public record StorageEndpoint(
        String dimensionId,
        int x,
        int y,
        int z,
        int slots
) {
    public StorageEndpoint {
        dimensionId = KingdomNormalizers.normalize(dimensionId, "dimensionId");
        if (slots < 1) {
            throw new IllegalArgumentException("slots must be positive");
        }
    }
}
