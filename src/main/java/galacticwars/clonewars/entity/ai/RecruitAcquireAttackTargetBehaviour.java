package galacticwars.clonewars.entity.ai;

import galacticwars.clonewars.entity.GalacticRecruitEntity;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryCondition;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.tslat.smartbrainlib.api.core.behaviour.base.ExtendedBehaviour;
import net.tslat.smartbrainlib.util.BrainUtil;

/** Promotes an authorized local target from sensors or an explicit command into fight memory. */
public final class RecruitAcquireAttackTargetBehaviour
        extends ExtendedBehaviour<GalacticRecruitEntity> {
    @Override
    public Set<MemoryCondition<?, ?>> getMemoryRequirements() {
        return Set.of();
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, GalacticRecruitEntity recruit) {
        return !BrainUtil.hasMemory(recruit, MemoryModuleType.ATTACK_TARGET)
                && candidate(recruit) != null;
    }

    @Override
    protected void start(GalacticRecruitEntity recruit) {
        LivingEntity target = candidate(recruit);
        if (target == null) {
            return;
        }
        BrainUtil.clearMemory(recruit, MemoryModuleType.WALK_TARGET);
        BrainUtil.setTargetOfEntity(recruit, target);
        recruit.setTarget(target);
        recruit.setAggressive(true);
    }

    private static LivingEntity candidate(GalacticRecruitEntity recruit) {
        return recruit.selectLocalAttackTarget(
                BrainUtil.getMemory(recruit, MemoryModuleType.NEAREST_ATTACKABLE));
    }
}
