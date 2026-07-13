package galacticwars.clonewars.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.gameevent.GameEvent;

/** Vanilla spawn egg whose entity type is bound through the standard item component. */
public final class RecruitSpawnEggItem extends SpawnEggItem {
    public RecruitSpawnEggItem(
            EntityType<GalacticRecruitEntity> recruitType,
            Properties properties
    ) {
        super(properties.spawnEgg(recruitType));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult vanillaResult = super.useOn(context);
        if (vanillaResult != InteractionResult.FAIL
                || !(context.getLevel() instanceof ServerLevel level)
                || context.getLevel().getBlockEntity(context.getClickedPos()) instanceof Spawner) {
            return vanillaResult;
        }

        ItemStack stack = context.getItemInHand();
        EntityType<?> type = SpawnEggItem.getType(stack);
        if (type == null || !type.canSpawn(level)) {
            return vanillaResult;
        }

        BlockPos clicked = context.getClickedPos();
        BlockPos[] fallbackPositions = {
                clicked.above(),
                clicked.north(),
                clicked.south(),
                clicked.east(),
                clicked.west()
        };
        for (BlockPos spawnPos : fallbackPositions) {
            if (type.spawn(
                    level,
                    stack,
                    context.getPlayer(),
                    spawnPos,
                    EntitySpawnReason.SPAWN_ITEM_USE,
                    true,
                    spawnPos.equals(clicked.above())) != null) {
                stack.consume(1, context.getPlayer());
                level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, spawnPos);
                return InteractionResult.SUCCESS;
            }
        }
        return vanillaResult;
    }
}
