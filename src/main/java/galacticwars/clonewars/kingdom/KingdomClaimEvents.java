package galacticwars.clonewars.kingdom;

import galacticwars.clonewars.GalacticWars;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

@EventBusSubscriber(modid = GalacticWars.MODID)
public final class KingdomClaimEvents {
    private KingdomClaimEvents() {
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && !allows(level, event.getPlayer(), event.getPos().getX(), event.getPos().getZ(),
                        KingdomPermission.BUILD)) {
            event.setCanceled(true);
            event.setNotifyClient(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel() instanceof ServerLevel level
                && entity instanceof Player player
                && !allows(level, player, event.getPos().getX(), event.getPos().getZ(), KingdomPermission.BUILD)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.getBlockEntity(event.getPos()) instanceof Container
                && !allows(level, event.getEntity(), event.getPos().getX(), event.getPos().getZ(),
                        KingdomPermission.USE_STORAGE)) {
            event.setCanceled(true);
        }
    }

    private static boolean allows(
            ServerLevel level,
            Player player,
            int blockX,
            int blockZ,
            KingdomPermission permission
    ) {
        if (player instanceof ServerPlayer serverPlayer
                && level.getServer().getPlayerList().isOp(serverPlayer.nameAndId())) {
            return true;
        }
        String dimensionId = level.dimension().identifier().toString();
        KingdomSavedData data = KingdomSavedData.get(level);
        var claim = data.claimAt(dimensionId, new ChunkPos(blockX >> 4, blockZ >> 4));
        if (claim.isEmpty()) {
            return true;
        }
        if (player == null) {
            return false;
        }
        return data.kingdom(claim.orElseThrow().kingdomId())
                .map(kingdom -> kingdom.allows(player.getUUID(), permission))
                .orElse(false);
    }
}
