package galacticwars.clonewars.workforce.logistics;

import java.util.Objects;
import net.minecraft.world.Container;

/** Physical inventory plus the identity, slot boundary, and policy that authorize its use. */
public record LogisticsEndpoint(
        LogisticsEndpointIdentity identity,
        LogisticsInventory inventory,
        int authorizedSlotCount,
        LogisticsAccessPolicy policy
) {
    public LogisticsEndpoint {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(inventory, "inventory");
        Objects.requireNonNull(policy, "policy");
        if (authorizedSlotCount <= 0 || authorizedSlotCount > inventory.size()) {
            throw new IllegalArgumentException("authorized slots must be within the physical inventory");
        }
    }

    public static LogisticsEndpoint container(
            LogisticsEndpointIdentity identity,
            Container container,
            int authorizedSlotCount,
            LogisticsAccessPolicy policy
    ) {
        return new LogisticsEndpoint(
                identity, new MinecraftContainerInventory(container), authorizedSlotCount, policy);
    }
}
