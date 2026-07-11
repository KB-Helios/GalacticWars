package galacticwars.clonewars.entity.ai;

import galacticwars.clonewars.entity.GalacticRecruitEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

/** Keeps natural faction civilians visibly occupied at their settlement during daylight. */
public final class NaturalCivilianWorkGoal extends Goal {
    private final GalacticRecruitEntity civilian;

    public NaturalCivilianWorkGoal(GalacticRecruitEntity civilian) {
        this.civilian = civilian;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return civilian.isNaturalFactionCivilian()
                && civilian.hasHome()
                && civilian.getWorkerProfession().isPresent()
                && civilian.level().isBrightOutside();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        moveToWorkstation();
    }

    @Override
    public void tick() {
        var workstation = civilian.naturalWorkstationPosition();
        civilian.getLookControl().setLookAt(
                workstation.getX() + 0.5D, workstation.getY() + 0.5D, workstation.getZ() + 0.5D);
        if (civilian.blockPosition().closerThan(workstation, 2.5D)) {
            civilian.getNavigation().stop();
            civilian.tryProduceNaturalSettlementSupplies();
        } else if (civilian.getNavigation().isDone()) {
            moveToWorkstation();
        }
    }

    private void moveToWorkstation() {
        var target = civilian.naturalWorkstationPosition();
        civilian.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.85D);
    }
}
