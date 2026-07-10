package middleearth.lotr.warmod.settlement;

import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

@EventBusSubscriber(modid = KingdomWarsMiddleEarth.MODID)
public final class KingdomHallEvents {
    private KingdomHallEvents() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(level.getBlockEntity(event.getPos()) instanceof KingdomHallBlockEntity hall)) {
            return;
        }
        if (hall.ownerId() != null && !hall.isOwner(event.getPlayer())) {
            event.setCanceled(true);
            event.setNotifyClient(true);
            event.getPlayer().sendSystemMessage(
                    Component.translatable("message.kingdomwarsmiddleearth.kingdom_hall.not_owner"));
        }
    }
}
