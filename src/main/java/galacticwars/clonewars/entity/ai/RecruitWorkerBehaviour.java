package galacticwars.clonewars.entity.ai;

import galacticwars.clonewars.entity.GalacticRecruitEntity;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryCondition;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.tslat.smartbrainlib.api.core.behaviour.base.ExtendedBehaviour;
import net.tslat.smartbrainlib.util.BrainUtil;

public final class RecruitWorkerBehaviour extends ExtendedBehaviour<GalacticRecruitEntity> {
    public RecruitWorkerBehaviour() {
        this.noTimeout();
    }

    @Override
    public Set<MemoryCondition<?, ?>> getMemoryRequirements() {
        return Set.of();
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, GalacticRecruitEntity recruit) {
        return recruit.shouldRunWorkerCycle();
    }

    @Override
    protected boolean shouldKeepRunning(GalacticRecruitEntity recruit) {
        return recruit.shouldRunWorkerCycle();
    }

    @Override
    protected void start(GalacticRecruitEntity recruit) {
        BrainUtil.clearMemories(recruit,
                MemoryModuleType.WALK_TARGET,
                MemoryModuleType.LOOK_TARGET);
    }

    @Override
    protected void tick(GalacticRecruitEntity recruit) {
        recruit.tickWorkerController();
    }

    @Override
    protected void stop(GalacticRecruitEntity recruit) {
        recruit.pauseWorkerNavigation();
    }
}
