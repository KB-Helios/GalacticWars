package galacticwars.clonewars.force;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Physical, faction-specific initiation and training site. */
public final class ForceShrineBlock extends Block {
    private final String traditionId;

    public ForceShrineBlock(String traditionId, BlockBehaviour.Properties properties) {
        super(properties);
        this.traditionId = traditionId;
    }

    public String traditionId() {
        return traditionId;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (level instanceof ServerLevel && player instanceof ServerPlayer serverPlayer) {
            ForceShrineService.open(serverPlayer, pos, traditionId);
        }
        return InteractionResult.SUCCESS;
    }
}
