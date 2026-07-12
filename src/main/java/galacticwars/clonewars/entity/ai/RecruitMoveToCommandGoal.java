package galacticwars.clonewars.entity.ai;

import java.util.EnumSet;

import galacticwars.clonewars.entity.GalacticRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

public class RecruitMoveToCommandGoal extends Goal {
    private static final double ARRIVAL_DISTANCE_SQUARED = 4.0;
    private static final double MIN_PROGRESS_SQUARED = 0.25;
    private static final int REPATH_INTERVAL = 20;
    private static final int STALL_TIMEOUT = 200;
    private static final int RETRY_BACKOFF = 40;

    private final GalacticRecruitEntity recruit;
    private final double speedModifier;
    private BlockPos trackedTarget;
    private double bestDistanceSqr;
    private int stalledTicks;
    private int retryTicks;
    private boolean waitingToRetry;

    public RecruitMoveToCommandGoal(GalacticRecruitEntity recruit, double speedModifier) {
        this.recruit = recruit;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.retryTicks > 0) {
            this.retryTicks--;
            return false;
        }
        return this.recruit.shouldMoveToCommandTarget();
    }

    @Override
    public boolean canContinueToUse() {
        return this.recruit.shouldMoveToCommandTarget()
                && !this.waitingToRetry
                && this.distanceToTargetSqr() > ARRIVAL_DISTANCE_SQUARED;
    }

    @Override
    public void start() {
        this.waitingToRetry = false;
        this.resetProgress(this.recruit.getMoveTarget());
        this.moveToTarget();
    }

    @Override
    public void tick() {
        BlockPos target = this.recruit.getMoveTarget();
        if (target == null) {
            this.waitingToRetry = true;
            return;
        }
        if (!target.equals(this.trackedTarget)) {
            this.resetProgress(target);
        }
        double distanceSqr = this.distanceToTargetSqr();
        if (distanceSqr < this.bestDistanceSqr - MIN_PROGRESS_SQUARED) {
            this.bestDistanceSqr = distanceSqr;
            this.stalledTicks = 0;
        } else {
            this.stalledTicks++;
        }
        if (this.stalledTicks >= STALL_TIMEOUT) {
            this.recruit.getNavigation().stop();
            this.retryTicks = RETRY_BACKOFF;
            this.waitingToRetry = true;
            return;
        }
        if (this.recruit.getNavigation().isDone()
                || this.recruit.tickCount % REPATH_INTERVAL == 0) {
            this.moveToTarget();
        }
    }

    @Override
    public void stop() {
        this.recruit.getNavigation().stop();
        this.trackedTarget = null;
        this.waitingToRetry = false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
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

    private void resetProgress(BlockPos target) {
        this.trackedTarget = target == null ? null : target.immutable();
        this.bestDistanceSqr = this.distanceToTargetSqr();
        this.stalledTicks = 0;
    }
}
