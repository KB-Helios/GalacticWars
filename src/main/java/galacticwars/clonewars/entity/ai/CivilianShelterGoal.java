package galacticwars.clonewars.entity.ai;

import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.recruitment.NpcServiceBranch;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

/** Returns natural civilians to their outpost home at night or when hostile troops approach. */
public final class CivilianShelterGoal extends Goal {
    private static final double DANGER_RADIUS = 18.0D;
    private final GalacticRecruitEntity civilian;

    public CivilianShelterGoal(GalacticRecruitEntity civilian) {
        this.civilian = civilian;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return isNaturalCivilian() && civilian.hasHome()
                && (civilian.level().isDarkOutside() || dangerNearby());
    }

    @Override
    public boolean canContinueToUse() {
        return isNaturalCivilian() && civilian.hasHome()
                && !civilian.blockPosition().closerThan(civilian.getHomePosition(), 3.0D)
                && (civilian.level().isDarkOutside() || dangerNearby());
    }

    @Override
    public void start() {
        civilian.setTarget(null);
        moveHome();
    }

    @Override
    public void tick() {
        if (civilian.getNavigation().isDone()) {
            moveHome();
        }
    }

    private void moveHome() {
        var home = civilian.getHomePosition();
        civilian.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 1.1D);
    }

    private boolean dangerNearby() {
        return !civilian.level().getEntitiesOfClass(
                GalacticRecruitEntity.class,
                civilian.getBoundingBox().inflate(DANGER_RADIUS),
                candidate -> candidate != civilian
                        && candidate.getServiceBranch() == NpcServiceBranch.MILITARY
                        && civilian.isHostileFactionRecruit(candidate)).isEmpty();
    }

    private boolean isNaturalCivilian() {
        return !civilian.isTame()
                && civilian.getFactionOutpostId() != null
                && civilian.getServiceBranch() == NpcServiceBranch.CIVILIAN;
    }
}
