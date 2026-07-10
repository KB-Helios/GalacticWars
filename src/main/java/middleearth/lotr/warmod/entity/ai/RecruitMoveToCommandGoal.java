package middleearth.lotr.warmod.entity.ai;

import java.util.EnumSet;

import middleearth.lotr.warmod.entity.MiddleEarthRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

public class RecruitMoveToCommandGoal extends Goal {
    private static final double ARRIVAL_DISTANCE_SQUARED = 4.0;

    private final MiddleEarthRecruitEntity recruit;
    private final double speedModifier;

    public RecruitMoveToCommandGoal(MiddleEarthRecruitEntity recruit, double speedModifier) {
        this.recruit = recruit;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.recruit.shouldMoveToCommandTarget();
    }

    @Override
    public boolean canContinueToUse() {
        return this.recruit.shouldMoveToCommandTarget()
                && !this.recruit.getNavigation().isDone()
                && this.distanceToTargetSqr() > ARRIVAL_DISTANCE_SQUARED;
    }

    @Override
    public void start() {
        this.moveToTarget();
    }

    @Override
    public void tick() {
        if (this.recruit.tickCount % 20 == 0) {
            this.moveToTarget();
        }
    }

    @Override
    public void stop() {
        this.recruit.getNavigation().stop();
    }

    private void moveToTarget() {
        BlockPos target = this.recruit.getMoveTarget();
        if (target != null) {
            this.recruit.getNavigation().moveTo(
                    target.getX() + 0.5,
                    target.getY(),
                    target.getZ() + 0.5,
                    this.speedModifier);
        }
    }

    private double distanceToTargetSqr() {
        BlockPos target = this.recruit.getMoveTarget();
        if (target == null) {
            return 0.0;
        }
        return this.recruit.distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
    }
}
