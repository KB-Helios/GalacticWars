package galacticwars.clonewars.world;

import galacticwars.clonewars.menu.CommandCenterNavigationMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class HyperspaceNavigatorItem extends Item {
    public HyperspaceNavigatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (!PlanetTravelService.hasActiveCommandCenter(serverPlayer)) {
            player.sendSystemMessage(Component.translatable("message.galacticwars.travel.not_owner"));
            return InteractionResult.FAIL;
        }
        player.openMenu(new CommandCenterNavigationMenuProvider());
        return InteractionResult.SUCCESS;
    }
}
