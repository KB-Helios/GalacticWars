package galacticwars.clonewars.item;

import galacticwars.clonewars.network.ClientPacketBridge;
import galacticwars.clonewars.registry.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Selects an explicit block or entity before the player opens a command GUI. */
public final class TacticalCommandMarkerItem extends Item {
    public TacticalCommandMarkerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * Right-clicking air opens Field Command. Sneak-right-clicking air clears the stored marker.
     * Block and entity interactions remain explicit target-selection actions.
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                stack.remove(ModDataComponents.COMMAND_TARGET.get());
                player.sendSystemMessage(Component.translatable(
                        "message.galacticwars.command_marker.cleared"));
            }
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) {
            ClientPacketBridge.openFieldCommandScreen();
        }
        return InteractionResult.SUCCESS;
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

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.accept(Component.translatable("tooltip.galacticwars.command_marker.open")
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.galacticwars.command_marker.block")
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.galacticwars.command_marker.entity")
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.galacticwars.command_marker.clear")
                .withStyle(ChatFormatting.DARK_GRAY));
        CommandTargetSelection target = stack.get(ModDataComponents.COMMAND_TARGET.get());
        if (target == null) {
            tooltip.accept(Component.translatable("tooltip.galacticwars.command_marker.target.none")
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            String key = target.entityId().isPresent()
                    ? "tooltip.galacticwars.command_marker.target.entity"
                    : "tooltip.galacticwars.command_marker.target.block";
            tooltip.accept(Component.translatable(key, target.x(), target.y(), target.z())
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
