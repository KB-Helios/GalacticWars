package middleearth.lotr.warmod.entity;

import java.util.List;
import java.util.UUID;
import middleearth.lotr.warmod.kingdom.KingdomSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public final class RecruitLifecycleService {
    private RecruitLifecycleService() {
    }

    public static void releaseSettlementState(
            ServerLevel level,
            UUID ownerId,
            UUID recruitId,
            boolean commander
    ) {
        KingdomSavedData data = KingdomSavedData.get(level);
        if (commander) {
            data.cancelActiveCampaigns(ownerId, "commander_lost");
        }
        boolean removed = data.unregisterRecruit(ownerId, recruitId);
        if (commander && !removed) {
            data.clearCommander(ownerId, recruitId);
        }
    }

    public static void dropCarriedItems(ServerLevel level, Entity source, List<ItemStack> carriedItems) {
        for (ItemStack stack : carriedItems) {
            if (!stack.isEmpty()) {
                source.spawnAtLocation(level, stack.copy());
            }
        }
    }
}
