package galacticwars.clonewars.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

/** NightsisterWeave does not grow from random ticks; a player must use bonemeal. */
public final class PlayerTriggeredSaplingBlock extends SaplingBlock {
    public PlayerTriggeredSaplingBlock(TreeGrower grower, BlockBehaviour.Properties properties) {
        super(grower, properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Player-triggered growth only.
    }
}
