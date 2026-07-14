package galacticwars.clonewars.settlement;

import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Places the blueprint origin in-world and starts the selected recruit's builder work. */
public final class BlueprintProjectorItem extends Item {
    public BlueprintProjectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)
                || !(context.getPlayer() instanceof ServerPlayer player)) {
            return context.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        ItemStack stack = context.getItemInHand();
        ConstructionPlan plan = stack.get(ModDataComponents.CONSTRUCTION_PLAN.get());
        if (plan == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.galacticwars.blueprint_projector.unconfigured"));
            return InteractionResult.FAIL;
        }
        var result = ConstructionProjectService.start(
                level, player, plan, context.getClickedPos().relative(context.getClickedFace()));
        if (!result.accepted()) {
            player.sendSystemMessage(Component.translatable(
                    "message.galacticwars.blueprint_projector.rejected",
                    Component.translatable("reason.galacticwars.construction." + result.reason())));
            return InteractionResult.FAIL;
        }
        stack.remove(ModDataComponents.CONSTRUCTION_PLAN.get());
        String blueprintName = GameplayDataManager.snapshot().blueprint(plan.blueprintId())
                .map(KingdomBaseBlueprint::displayName).orElse(plan.blueprintId());
        player.sendSystemMessage(Component.translatable(
                "message.galacticwars.blueprint_projector.started",
                Component.literal(blueprintName), plan.rotationSteps() * 90));
        return InteractionResult.SUCCESS;
    }
}
