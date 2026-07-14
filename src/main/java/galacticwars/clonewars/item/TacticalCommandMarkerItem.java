package galacticwars.clonewars.item;

import galacticwars.clonewars.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Selects an explicit block or entity before the player opens a command GUI. */
public final class TacticalCommandMarkerItem extends Item {
    public TacticalCommandMarkerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)
                || !(context.getPlayer() instanceof ServerPlayer player)) {
            return context.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        var target = CommandTargetSelection.block(level, context.getClickedPos());
        context.getItemInHand().set(ModDataComponents.COMMAND_TARGET.get(), target);
        player.sendSystemMessage(Component.translatable(
                "message.galacticwars.command_marker.block",
                target.x(), target.y(), target.z()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(player.level() instanceof ServerLevel level)) {
            return player.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        stack.set(ModDataComponents.COMMAND_TARGET.get(), CommandTargetSelection.entity(level, target));
        serverPlayer.sendSystemMessage(Component.translatable(
                "message.galacticwars.command_marker.entity", target.getDisplayName()));
        return InteractionResult.SUCCESS;
    }
}
