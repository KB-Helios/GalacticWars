package middleearth.lotr.warmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.gameevent.GameEvent;

/** Spawn egg with an explicit entity-type fallback instead of relying only on the item component. */
public final class RecruitSpawnEggItem extends SpawnEggItem {
    private final EntityType<MiddleEarthRecruitEntity> recruitType;

    public RecruitSpawnEggItem(
            EntityType<MiddleEarthRecruitEntity> recruitType,
            Properties properties
    ) {
        super(properties.spawnEgg(recruitType));
        this.recruitType = recruitType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        EntityType<?> componentType = SpawnEggItem.getType(context.getItemInHand());
        if (componentType != null) {
            return componentType == recruitType ? super.useOn(context) : InteractionResult.FAIL;
        }
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity clickedBlockEntity = serverLevel.getBlockEntity(context.getClickedPos());
        if (clickedBlockEntity instanceof Spawner) {
            return InteractionResult.FAIL;
        }
        if (!recruitType.canSpawn(serverLevel)) {
            return InteractionResult.FAIL;
        }
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = serverLevel.getBlockState(clickedPos);
        BlockPos spawnPos = clickedState.getCollisionShape(serverLevel, clickedPos).isEmpty()
                ? clickedPos
                : clickedPos.relative(context.getClickedFace());
        MiddleEarthRecruitEntity recruit = recruitType.spawn(
                serverLevel, spawnPos, EntitySpawnReason.SPAWN_ITEM_USE);
        if (recruit == null) {
            return InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (stack.get(DataComponents.CUSTOM_NAME) != null) {
            recruit.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
        }
        if (player == null || !player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, spawnPos);
        return InteractionResult.SUCCESS;
    }
}
