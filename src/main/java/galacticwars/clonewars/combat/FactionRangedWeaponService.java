package galacticwars.clonewars.combat;

import galacticwars.clonewars.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public final class FactionRangedWeaponService {
    private static final double NIGHTSISTER_BOW_DAMAGE = 5.0D;
    private static final float NIGHTSISTER_BOW_VELOCITY = 2.8F;
    private static final float NIGHTSISTER_BOW_INACCURACY = 0.8F;

    private FactionRangedWeaponService() {
    }

    public static boolean supportsRecruitRangedCombat(@Nullable ItemStack weapon) {
        return weapon != null
                && (weapon.getItem() instanceof BlasterItem || weapon.is(ModItems.NIGHTSISTER_BOW.get()));
    }

    public static boolean isProtectedFactionProjectile(@Nullable ItemStack weapon) {
        return supportsRecruitRangedCombat(weapon);
    }

    public static void fireNightsisterBow(
            ServerLevel level,
            LivingEntity shooter,
            LivingEntity target,
            ItemStack weapon
    ) {
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 0.8F,
                1.0F + level.getRandom().nextFloat() * 0.1F);
        Arrow arrow = new Arrow(level, shooter, new ItemStack(Items.ARROW), weapon);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setBaseDamage(NIGHTSISTER_BOW_DAMAGE);
        double targetY = target.getY() + target.getBbHeight() * 0.6D;
        arrow.shoot(target.getX() - shooter.getX(), targetY - arrow.getY(), target.getZ() - shooter.getZ(),
                NIGHTSISTER_BOW_VELOCITY, NIGHTSISTER_BOW_INACCURACY);
        level.addFreshEntity(arrow);
    }
}
