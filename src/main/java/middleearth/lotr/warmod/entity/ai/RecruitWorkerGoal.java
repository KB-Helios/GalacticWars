package middleearth.lotr.warmod.entity.ai;

import java.util.EnumSet;

import middleearth.lotr.warmod.entity.MiddleEarthRecruitEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class RecruitWorkerGoal extends Goal {
    private final MiddleEarthRecruitEntity recruit;

    public RecruitWorkerGoal(MiddleEarthRecruitEntity recruit) {
        this.recruit = recruit;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.recruit.shouldRunWorkerCycle();
    }

    @Override
    public boolean canContinueToUse() {
        return this.recruit.shouldRunWorkerCycle();
    }

    @Override
    public void tick() {
        this.recruit.tickWorkerController();
    }

    @Override
    public void stop() {
        this.recruit.pauseWorkerNavigation();
    }
}
