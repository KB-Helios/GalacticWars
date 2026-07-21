package galacticwars.clonewars.workforce.logistics;

import java.util.UUID;
import net.minecraft.world.item.ItemStack;

/** Explicit authority hook evaluated during simulation and immediately before commit. */
@FunctionalInterface
public interface LogisticsAccessPolicy {
    boolean allows(
            UUID actorId,
            LogisticsEndpointIdentity endpoint,
            LogisticsEndpointIdentity counterpart,
            Operation operation,
            int slot,
            ItemStack stack
    );

    static LogisticsAccessPolicy allowAll() {
        return (actorId, endpoint, counterpart, operation, slot, stack) -> true;
    }

    enum Operation {
        EXTRACT,
        INSERT
    }
}
