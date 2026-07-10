package middleearth.lotr.warmod.settlement;

import java.util.UUID;
import middleearth.lotr.warmod.kingdom.KingdomRecord;
import middleearth.lotr.warmod.kingdom.KingdomSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public final class KingdomHallLifecycleService {
    private KingdomHallLifecycleService() {
    }

    public static boolean prepareOwnerRemoval(ServerLevel level, KingdomHallBlockEntity hall) {
        UUID ownerId = hall.ownerId();
        if (ownerId == null) {
            return false;
        }
        KingdomSavedData data = KingdomSavedData.get(level);
        boolean authoritative = data.isHallActive(ownerId)
                && data.kingdomForOwner(ownerId)
                        .map(KingdomRecord::settlement)
                        .filter(settlement -> settlement.dimensionId()
                                .equals(level.dimension().identifier().toString()))
                        .filter(settlement -> settlement.hallX() == hall.getBlockPos().getX()
                                && settlement.hallY() == hall.getBlockPos().getY()
                                && settlement.hallZ() == hall.getBlockPos().getZ())
                        .isPresent();
        if (!authoritative) {
            return false;
        }
        data.cancelActiveCampaigns(ownerId, "hall_removed");
        data.applyPendingCampaignRefunds(ownerId, amount -> {
            int inserted = hall.refundEmeralds(amount);
            int overflow = amount - inserted;
            if (overflow > 0) {
                Block.popResource(level, hall.getBlockPos(), new ItemStack(Items.EMERALD, overflow));
            }
            return amount;
        });
        return data.deactivateHall(ownerId, level.dimension().identifier().toString(), hall.getBlockPos());
    }
}
