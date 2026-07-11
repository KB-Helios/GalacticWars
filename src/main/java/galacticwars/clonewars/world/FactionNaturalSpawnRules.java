package galacticwars.clonewars.world;

import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public final class FactionNaturalSpawnRules {
    private FactionNaturalSpawnRules() {
    }

    public static boolean check(
            EntityType<GalacticRecruitEntity> entityType,
            ServerLevelAccessor level,
            EntitySpawnReason reason,
            BlockPos position,
            RandomSource random
    ) {
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
            return true;
        }
        String entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString();
        return level.getLevel().dimension().equals(Level.OVERWORLD)
                && GameplayDataManager.snapshot().overworldSpawnProfileForEntity(entityTypeId).isPresent()
                && level.getBlockState(position).isAir()
                && level.getBlockState(position.above()).isAir()
                && level.getFluidState(position).isEmpty()
                && level.getBlockState(position.below()).isFaceSturdy(level, position.below(), Direction.UP)
                && level.getRawBrightness(position, 0) > 8;
    }
}
