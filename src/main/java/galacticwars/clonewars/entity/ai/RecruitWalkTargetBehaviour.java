package galacticwars.clonewars.entity.ai;

import galacticwars.clonewars.entity.GalacticRecruitEntity;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryCondition;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.pathfinder.Path;
import net.tslat.smartbrainlib.api.core.behaviour.base.ExtendedBehaviour;
import net.tslat.smartbrainlib.util.BrainUtil;

/**
 * Consumes SmartBrain walk memories while keeping non-brain navigation controllers independent.
 *
 * <p>SmartBrainLib's stock mover does not start its computed path on Minecraft 26.2, so this
 * behaviour supplies the same memory-to-navigation bridge without moving command-specific state
 * out of the behaviours that publish the target.</p>
 */
public final class RecruitWalkTargetBehaviour
        extends ExtendedBehaviour<GalacticRecruitEntity> {
    private static final int REPATH_INTERVAL = 20;
    private static final double MOVED_TARGET_DISTANCE_SQUARED = 4.0D;

    private BlockPos lastTargetPos;
    private int nextRepathTick;
    private boolean controlsNavigation;

    public RecruitWalkTargetBehaviour() {
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
        if (recruit.hasAuthoritativeArmyGroup()) {
            releaseWithoutStopping(recruit);
            return;
        }
        if (recruit.isTame() && recruit.isOrderedToSit()) {
            BrainUtil.clearMemory(recruit, MemoryModuleType.WALK_TARGET);
            stopOwnedNavigation(recruit);
            return;
        }

        WalkTarget walkTarget = BrainUtil.getMemory(recruit, MemoryModuleType.WALK_TARGET);
        if (walkTarget == null) {
            stopOwnedNavigation(recruit);
            return;
        }

        BlockPos targetPos = walkTarget.getTarget().currentBlockPosition();
        if (targetPos.distManhattan(recruit.blockPosition())
                <= walkTarget.getCloseEnoughDist()) {
            BrainUtil.clearMemory(recruit, MemoryModuleType.WALK_TARGET);
            stopOwnedNavigation(recruit);
            return;
        }

        boolean targetMoved = lastTargetPos == null
                || lastTargetPos.distSqr(targetPos) > MOVED_TARGET_DISTANCE_SQUARED;
        if (!controlsNavigation
                || recruit.getNavigation().isDone()
                || targetMoved
                || recruit.tickCount >= nextRepathTick) {
            startPath(recruit, walkTarget, targetPos);
        } else {
            BrainUtil.setOrClearMemory(recruit.getBrain(), MemoryModuleType.PATH,
                    recruit.getNavigation().getPath());
        }
    }

    @Override
    protected void stop(GalacticRecruitEntity recruit) {
        stopOwnedNavigation(recruit);
    }

    private void startPath(
            GalacticRecruitEntity recruit,
            WalkTarget walkTarget,
            BlockPos targetPos
    ) {
        Path path = recruit.getNavigation().createPath(targetPos, 0);
        lastTargetPos = targetPos.immutable();
        nextRepathTick = recruit.tickCount + REPATH_INTERVAL;
        if (path == null) {
            if (controlsNavigation) {
                recruit.getNavigation().stop();
            }
            controlsNavigation = false;
            BrainUtil.clearMemory(recruit, MemoryModuleType.PATH);
            return;
        }

        controlsNavigation = recruit.getNavigation().moveTo(
                path,
                walkTarget.getSpeedModifier());
        BrainUtil.setOrClearMemory(recruit.getBrain(), MemoryModuleType.PATH,
                controlsNavigation ? path : null);
    }

    private void stopOwnedNavigation(GalacticRecruitEntity recruit) {
        if (controlsNavigation) {
            recruit.getNavigation().stop();
        }
        releaseWithoutStopping(recruit);
    }

    private void releaseWithoutStopping(GalacticRecruitEntity recruit) {
        controlsNavigation = false;
        lastTargetPos = null;
        nextRepathTick = 0;
        BrainUtil.clearMemory(recruit, MemoryModuleType.PATH);
    }
}
