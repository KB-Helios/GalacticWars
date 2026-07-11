package galacticwars.clonewars.world;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Builds compact original faction shelters without replacing containers or player structures. */
public final class FactionOutpostMarkerService {
    private static final Map<String, Palette> PALETTES = Map.of(
            "galacticwars:republic", new Palette(
                    Blocks.SMOOTH_STONE, Blocks.WOOL.pick(DyeColor.BLUE), Blocks.IRON_BLOCK),
            "galacticwars:separatist", new Palette(
                    Blocks.POLISHED_DEEPSLATE, Blocks.WOOL.pick(DyeColor.GRAY), Blocks.DEEPSLATE_TILES),
            "galacticwars:mandalorian", new Palette(
                    Blocks.POLISHED_DEEPSLATE, Blocks.WOOL.pick(DyeColor.CYAN), Blocks.DEEPSLATE_TILES),
            "galacticwars:hutt_cartel", new Palette(
                    Blocks.SMOOTH_SANDSTONE, Blocks.WOOL.pick(DyeColor.YELLOW), Blocks.SANDSTONE),
            "galacticwars:nightsister", new Palette(
                    Blocks.COBBLED_DEEPSLATE, Blocks.WOOL.pick(DyeColor.PURPLE), Blocks.DARK_OAK_PLANKS));

    private FactionOutpostMarkerService() {
    }

    public static BlockPos shelterCenter(FactionOutpostRecord outpost) {
        return new BlockPos(outpost.x(), outpost.y(), outpost.z());
    }

    public static boolean generate(ServerLevel level, FactionOutpostRecord outpost) {
        BlockPos center = shelterCenter(outpost);
        if (!canBuild(level, center)) {
            return false;
        }
        Palette palette = PALETTES.getOrDefault(outpost.factionId(), new Palette(
                Blocks.SMOOTH_STONE, Blocks.WOOL.pick(DyeColor.WHITE), Blocks.STONE_BRICKS));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(center.offset(dx, -1, dz), palette.foundation().defaultBlockState(), 3);
            }
        }
        // A 5x4 one-room shelter. The north-center opening remains clear as its door.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 1; dz++) {
                boolean wall = dx == -2 || dx == 2 || dz == -2 || dz == 1;
                boolean doorway = dx == 0 && dz == -2;
                if (wall && !doorway) {
                    level.setBlock(center.offset(dx, 0, dz), palette.wall().defaultBlockState(), 3);
                    level.setBlock(center.offset(dx, 1, dz), palette.wall().defaultBlockState(), 3);
                }
                level.setBlock(center.offset(dx, 2, dz), palette.roof().defaultBlockState(), 3);
            }
        }
        // Physical shared-storage landmark; stock and permissions are handled by settlement systems later.
        level.setBlock(center.offset(1, 0, 0), Blocks.BARREL.defaultBlockState(), 3);
        level.setBlock(center.offset(-1, 0, 0), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        level.setBlock(center.offset(-2, 0, 2), Blocks.CAMPFIRE.defaultBlockState(), 3);
        return true;
    }

    private static boolean canBuild(ServerLevel level, BlockPos center) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos ground = center.offset(dx, -1, dz);
                if (!replaceableNaturalGround(level.getBlockState(ground))
                        || level.getBlockEntity(ground) != null) {
                    return false;
                }
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos volume = center.offset(dx, dy, dz);
                    if (!level.getBlockState(volume).canBeReplaced() || level.getBlockEntity(volume) != null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean replaceableNaturalGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)
                || state.is(Blocks.STONE) || state.is(Blocks.TERRACOTTA);
    }

    private record Palette(Block foundation, Block wall, Block roof) {
    }
}
