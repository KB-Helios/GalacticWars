package galacticwars.clonewars.entity.ai;

import galacticwars.clonewars.combat.BlasterHeatPolicy;
import galacticwars.clonewars.combat.BlasterItem;
import galacticwars.clonewars.combat.FactionRangedWeaponService;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.recruitment.RecruitDuty;
import galacticwars.clonewars.registry.ModItems;
import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

/** Executes local attack orders for ungrouped ranged recruits. */
public final class RecruitRangedCombatGoal extends Goal {
    private final GalacticRecruitEntity recruit;
    private BlasterHeatPolicy.BlasterHeatState heat = BlasterHeatPolicy.BlasterHeatState.ready();
    private int bowCooldownTicks;

    public RecruitRangedCombatGoal(GalacticRecruitEntity recruit) {
        this.recruit = recruit;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = recruit.getTarget();
        return recruit.isTame()
                && recruit.getRecruitDuty() == RecruitDuty.SOLDIER
                && !recruit.hasAuthoritativeArmyGroup()
                && target != null
                && target.isAlive()
                && FactionRangedWeaponService.supportsRecruitRangedCombat(recruit.getMainHandItem());
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = recruit.getTarget();
        if (target == null || !(recruit.level() instanceof ServerLevel level)) {
            return;
        }
        heat = BlasterHeatPolicy.tick(heat);
        bowCooldownTicks = Math.max(0, bowCooldownTicks - 1);
        recruit.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double range = Math.max(4.0D, recruit.getAttributeValue(Attributes.FOLLOW_RANGE));
        if (recruit.distanceToSqr(target) > range * range || !recruit.getSensing().hasLineOfSight(target)) {
            recruit.getNavigation().moveTo(target, 1.05D);
            return;
        }

        recruit.getNavigation().stop();
        recruit.setAggressive(true);
        ItemStack weapon = recruit.getMainHandItem();
        if (weapon.getItem() instanceof BlasterItem blaster && BlasterHeatPolicy.canFire(heat)) {
            blaster.fireAt(level, recruit, target, weapon);
            heat = BlasterHeatPolicy.afterShot(heat);
        } else if (weapon.is(ModItems.NIGHTSISTER_BOW.get()) && bowCooldownTicks == 0) {
            FactionRangedWeaponService.fireNightsisterBow(level, recruit, target, weapon);
            bowCooldownTicks = 24;
        }
    }

    @Override
    public void stop() {
        recruit.getNavigation().stop();
        recruit.setAggressive(false);
    }
}
