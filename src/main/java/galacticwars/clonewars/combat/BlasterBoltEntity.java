package galacticwars.clonewars.combat;

import galacticwars.clonewars.registry.ModEntityTypes;
import galacticwars.clonewars.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class BlasterBoltEntity extends AbstractArrow implements ItemSupplier {
    private static final int MAX_LIFETIME_TICKS = 100;

    public BlasterBoltEntity(EntityType<? extends BlasterBoltEntity> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
        this.setNoGravity(true);
    }

    public BlasterBoltEntity(Level level, LivingEntity shooter, ItemStack weapon, double damage) {
        super(ModEntityTypes.BLASTER_BOLT.get(), shooter, level,
                new ItemStack(ModItems.BLASTER_BOLT.get()), weapon.copy());
        this.pickup = Pickup.DISALLOWED;
        this.setNoGravity(true);
        this.setBaseDamage(damage);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount >= MAX_LIFETIME_TICKS) {
            this.discard();
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }

    @Override
    protected float getAirDrag() {
        return 1.0F;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.BLASTER_BOLT.get());
    }

    public boolean deflectTowardOwner(LivingEntity defender) {
        Entity previousOwner = this.getOwner();
        if (!canDeflectTowardOwner(defender)) {
            return false;
        }
        Vec3 origin = defender.getEyePosition();
        Vec3 direction = previousOwner.getEyePosition().subtract(origin);
        if (direction.lengthSqr() < 0.01D) {
            direction = this.getDeltaMovement().scale(-1.0D);
        }
        if (direction.lengthSqr() < 0.01D) {
            return false;
        }
        Vec3 normalized = direction.normalize();
        this.setOwner(defender);
        this.setPos(origin.add(normalized.scale(0.75D)));
        this.shoot(normalized.x, normalized.y, normalized.z, 3.6F, 0.0F);
        return true;
    }

    public boolean deflectAlongLook(LivingEntity defender) {
        if (!canDeflectTowardOwner(defender)) return false;
        Vec3 direction = defender.getLookAngle();
        if (direction.lengthSqr() < 0.01D) return false;
        Vec3 normalized = direction.normalize();
        this.setOwner(defender);
        this.setPos(defender.getEyePosition().add(normalized.scale(0.75D)));
        this.shoot(normalized.x, normalized.y, normalized.z, 3.6F, 0.0F);
        return true;
    }

    public boolean canDeflectTowardOwner(LivingEntity defender) {
        Entity previousOwner = this.getOwner();
        if (previousOwner == null || previousOwner == defender) {
            return false;
        }
        Vec3 direction = previousOwner.getEyePosition().subtract(defender.getEyePosition());
        return direction.lengthSqr() >= 0.01D || this.getDeltaMovement().lengthSqr() >= 0.01D;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (BlasterCombatEvents.handleProjectileImpact(this, hit.getEntity())) {
            return;
        }
        super.onHitEntity(hit);
        this.emitImpact();
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        this.emitImpact();
        this.discard();
    }

    private void emitImpact() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    this.getX(), this.getY(), this.getZ(),
                    6, 0.08D, 0.08D, 0.08D, 0.02D);
        }
    }
}
