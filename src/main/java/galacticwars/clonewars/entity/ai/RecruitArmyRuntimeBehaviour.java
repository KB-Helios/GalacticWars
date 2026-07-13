package galacticwars.clonewars.entity.ai;

import galacticwars.clonewars.entity.GalacticRecruitEntity;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryCondition;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.tslat.smartbrainlib.api.core.behaviour.base.ExtendedBehaviour;
import net.tslat.smartbrainlib.util.BrainUtil;

/** Keeps grouped navigation/combat under the existing saved-data-backed controller. */
public final class RecruitArmyRuntimeBehaviour extends ExtendedBehaviour<GalacticRecruitEntity> {
    private boolean hadAuthoritativeGroup;

    public RecruitArmyRuntimeBehaviour() {
        this.noTimeout();
    }

    @Override
    public Set<MemoryCondition<?, ?>> getMemoryRequirements() {
        return Set.of();
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, GalacticRecruitEntity recruit) {
        return true;
    }

    @Override
    protected boolean shouldKeepRunning(GalacticRecruitEntity recruit) {
        return true;
    }

    @Override
    protected void tick(GalacticRecruitEntity recruit) {
        if (!(recruit.level() instanceof ServerLevel level)) {
            return;
        }
        if (recruit.hasAuthoritativeArmyGroup()) {
            BrainUtil.clearMemories(recruit,
                    MemoryModuleType.ATTACK_TARGET,
                    MemoryModuleType.WALK_TARGET,
                    MemoryModuleType.LOOK_TARGET,
                    MemoryModuleType.PATH);
            if (!hadAuthoritativeGroup) {
                recruit.getNavigation().stop();
            }
            hadAuthoritativeGroup = true;
        } else {
            hadAuthoritativeGroup = false;
        }
        recruit.tickArmyRuntimeController(level);
    }
}
