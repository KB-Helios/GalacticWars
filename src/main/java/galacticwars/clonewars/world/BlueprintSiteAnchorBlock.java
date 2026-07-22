package galacticwars.clonewars.world;

import com.mojang.serialization.MapCodec;
import galacticwars.clonewars.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class BlueprintSiteAnchorBlock extends BaseEntityBlock {
    public static final MapCodec<BlueprintSiteAnchorBlock> CODEC = simpleCodec(BlueprintSiteAnchorBlock::new);

    public BlueprintSiteAnchorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlueprintSiteAnchorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntityTypes.BLUEPRINT_SITE_ANCHOR.get(),
                BlueprintSiteAnchorBlockEntity::serverTick);
    }
}
