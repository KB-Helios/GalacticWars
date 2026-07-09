package middleearth.lotr.warmod.entity.ai;

import java.util.EnumSet;

import middleearth.lotr.warmod.entity.MiddleEarthRecruitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RecruitCompanionGoal extends Goal {
    private static final double COMFORT_DISTANCE_SQUARED = 16.0;
    private static final double TELEPORT_DISTANCE_SQUARED = 256.0;

    private final MiddleEarthRecruitEntity recruit;
    private final double speedModifier;
    private @Nullable LivingEntity owner;

    public RecruitCompanionGoal(MiddleEarthRecruitEntity recruit, double speedModifier) {
        this.recruit = recruit;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.owner = this.recruit.getRecruitOwner().orElse(null);
        return this.owner != null
                && this.recruit.shouldUseCompanionAi()
                && this.recruit.distanceToSqr(this.findCompanionAnchor(this.owner)) > COMFORT_DISTANCE_SQUARED;
    }

    @Override
    public boolean canContinueToUse() {
        return this.owner != null
                && this.owner.isAlive()
                && this.recruit.shouldUseCompanionAi()
                && this.recruit.distanceToSqr(this.findCompanionAnchor(this.owner)) > COMFORT_DISTANCE_SQUARED;
    }

    @Override
    public void tick() {
        if (this.owner == null) {
            return;
        }
        Vec3 anchor = this.findCompanionAnchor(this.owner);
        double distanceToSqr = this.recruit.distanceToSqr(anchor);
        this.recruit.getLookControl().setLookAt(this.owner, 30.0F, 30.0F);
        if (distanceToSqr > TELEPORT_DISTANCE_SQUARED && !this.recruit.level().isClientSide()) {
            this.recruit.teleportTo(anchor.x(), anchor.y(), anchor.z());
            this.recruit.getNavigation().stop();
            return;
        }
        if (this.recruit.tickCount % 10 == 0) {
            this.recruit.getNavigation().moveTo(anchor.x(), anchor.y(), anchor.z(), this.speedModifier);
        }
    }

    @Override
    public void stop() {
        this.recruit.getNavigation().stop();
        this.owner = null;
    }

    private Vec3 findCompanionAnchor(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 forward = new Vec3(look.x(), 0.0, look.z());
        if (forward.lengthSqr() < 1.0E-4) {
            forward = new Vec3(0.0, 0.0, 1.0);
        } else {
            forward = forward.normalize();
        }
        Vec3 right = new Vec3(-forward.z(), 0.0, forward.x()).normalize();
        double sideOffset = (this.recruit.getId() & 1) == 0 ? 1.75 : -1.75;
        return owner.position()
                .add(forward.scale(-2.5))
                .add(right.scale(sideOffset));
    }
}
