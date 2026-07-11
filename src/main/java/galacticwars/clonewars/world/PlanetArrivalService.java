package galacticwars.clonewars.world;

import galacticwars.clonewars.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.Optional;

public final class PlanetArrivalService {
    private static final int PLATFORM_RADIUS = 2;
    private static final List<BlockPos> CANDIDATE_OFFSETS = List.of(
            BlockPos.ZERO,
            new BlockPos(32, 0, 0),
            new BlockPos(-32, 0, 0),
            new BlockPos(0, 0, 32),
            new BlockPos(0, 0, -32),
            new BlockPos(32, 0, 32),
            new BlockPos(-32, 0, -32),
            new BlockPos(32, 0, -32),
            new BlockPos(-32, 0, 32));

    private PlanetArrivalService() {
    }

    public static Optional<BlockPos> findOrCreate(ServerLevel level) {
        for (BlockPos offset : CANDIDATE_OFFSETS) {
            Optional<BlockPos> existing = findExistingPlatform(level, offset.getX(), offset.getZ());
            if (existing.isPresent()) {
                return existing;
            }
        }
        for (BlockPos offset : CANDIDATE_OFFSETS) {
            Optional<BlockPos> created = createPlatform(level, offset.getX(), offset.getZ());
            if (created.isPresent()) {
                return created;
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> findExistingPlatform(ServerLevel level, int centerX, int centerZ) {
        for (int feetY = level.getMinY() + 2; feetY < level.getMaxY() - 2; feetY++) {
            BlockPos feet = new BlockPos(centerX, feetY, centerZ);
            if (isPlatform(level, feet) && hasClearArrivalVolume(level, feet)) {
                return Optional.of(feet);
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> createPlatform(ServerLevel level, int centerX, int centerZ) {
        level.getChunk(centerX >> 4, centerZ >> 4);
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerX, centerZ);
        BlockPos feet = new BlockPos(centerX, Math.min(level.getMaxY() - 3, surfaceY + 3), centerZ);
        if (!canCreatePlatform(level, feet)) {
            return Optional.empty();
        }
        BlockState platform = ModBlocks.DURACRETE.get().defaultBlockState();
        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                level.setBlockAndUpdate(feet.offset(x, -1, z), platform);
            }
        }
        return Optional.of(feet);
    }

    private static boolean canCreatePlatform(ServerLevel level, BlockPos feet) {
        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                if (!replaceable(level.getBlockState(feet.offset(x, -1, z)))) {
                    return false;
                }
            }
        }
        return hasClearArrivalVolume(level, feet);
    }

    private static boolean isPlatform(ServerLevel level, BlockPos feet) {
        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                if (!level.getBlockState(feet.offset(x, -1, z)).is(ModBlocks.DURACRETE.get())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasClearArrivalVolume(ServerLevel level, BlockPos feet) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (!clearForPlayer(level.getBlockState(feet.offset(x, 0, z)))
                        || !clearForPlayer(level.getBlockState(feet.offset(x, 1, z)))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean replaceable(BlockState state) {
        return state.canBeReplaced() || !state.getFluidState().isEmpty();
    }

    private static boolean clearForPlayer(BlockState state) {
        return state.canBeReplaced() && state.getFluidState().isEmpty();
    }
}
