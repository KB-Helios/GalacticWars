package middleearth.lotr.warmod.survival;

import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import middleearth.lotr.warmod.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = KingdomWarsMiddleEarth.MODID)
public final class HorseBrushingEvents {
    private static final String LAST_BRUSHED_DAY = KingdomWarsMiddleEarth.MODID + ":last_brushed_day";

    private HorseBrushingEvents() {
    }

    @SubscribeEvent
    public static void onHorseBrushed(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof AbstractHorse horse)
                || horse.isBaby()) {
            return;
        }
        ItemStack brush = player.getItemInHand(event.getHand());
        if (!brush.is(Items.BRUSH)) {
            return;
        }
        long day = horse.level().getGameTime() / 24000L;
        if (horse.getPersistentData().getLongOr(LAST_BRUSHED_DAY, Long.MIN_VALUE) == day) {
            player.sendSystemMessage(Component.translatable(
                    "message.kingdomwarsmiddleearth.horsehair.already_brushed"));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }
        horse.getPersistentData().putLong(LAST_BRUSHED_DAY, day);
        ItemStack hair = new ItemStack(ModItems.ROHAN_HORSEHAIR.get());
        if (!player.addItem(hair)) {
            player.drop(hair, false);
        }
        brush.hurtAndBreak(1, player, event.getHand());
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
