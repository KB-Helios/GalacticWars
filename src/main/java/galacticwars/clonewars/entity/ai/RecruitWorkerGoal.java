package galacticwars.clonewars.entity.ai;

import java.util.EnumSet;

import galacticwars.clonewars.entity.GalacticRecruitEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class RecruitWorkerGoal extends Goal {
    private final GalacticRecruitEntity recruit;

    public RecruitWorkerGoal(GalacticRecruitEntity recruit) {
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
