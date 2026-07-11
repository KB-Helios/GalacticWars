package galacticwars.clonewars.settlement;

import galacticwars.clonewars.GalacticWars;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

@EventBusSubscriber(modid = GalacticWars.MODID)
public final class CommandCenterEvents {
    private CommandCenterEvents() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(level.getBlockEntity(event.getPos()) instanceof CommandCenterBlockEntity hall)) {
            return;
        }
        if (hall.ownerId() != null && !hall.isOwner(event.getPlayer())) {
            event.setCanceled(true);
            event.setNotifyClient(true);
            event.getPlayer().sendSystemMessage(
                    Component.translatable("message.galacticwars.command_center.not_owner"));
        }
    }
}
