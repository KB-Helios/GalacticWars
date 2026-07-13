package galacticwars.clonewars.classes;

import galacticwars.clonewars.ability.AbilityDefinition;
import galacticwars.clonewars.combat.BlasterBoltEntity;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Concrete executors for every enabled launch class ability. */
public final class ClassAbilityEffectRegistry {
    private ClassAbilityEffectRegistry() {
    }

    public static boolean registered(String abilityId) {
        return ClassAbilityExecutorCatalog.registered(abilityId);
    }

    public static boolean execute(
            ServerLevel level,
            GalacticRecruitEntity actor,
            AbilityDefinition ability,
            LivingEntity target
    ) {
        return switch (ClassAbilityExecutorCatalog.family(ability.id().toString())) {
            case PROJECTILE -> projectile(level, actor, target, ability);
            case MARK -> debuff(actor, target, ability);
            case MOBILITY -> mobility(actor, target);
            case DEFENSIVE -> defensive(actor);
            case SUPPORT -> support(level, actor, ability.range());
            case PASSIVE -> passive(actor, ability.id().toString());
        };
    }

    private static boolean projectile(
            ServerLevel level, GalacticRecruitEntity actor,
            LivingEntity target, AbilityDefinition ability
    ) {
        if (target == null) return false;
        Vec3 direction = target.getEyePosition().subtract(actor.getEyePosition());
        if (direction.lengthSqr() < 0.01D) return false;
        BlasterBoltEntity bolt = new BlasterBoltEntity(level, actor,
                actor.getMainHandItem().isEmpty() ? ItemStack.EMPTY : actor.getMainHandItem(),
                path(ability.id().toString()).contains("heavy") || path(ability.id().toString()).contains("barrage")
                        ? 8.0D : 5.0D);
        bolt.setPos(actor.getEyePosition().add(direction.normalize().scale(0.7D)));
        bolt.shoot(direction.x, direction.y, direction.z, 3.2F, 0.5F);
        return level.addFreshEntity(bolt);
    }

    private static boolean debuff(
            GalacticRecruitEntity actor, LivingEntity target, AbilityDefinition ability
    ) {
        if (target == null) return false;
        String id = path(ability.id().toString());
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), actor);
        if (id.equals("target_disruption") || id.equals("crippling_shot")) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1), actor);
        } else if (id.equals("intimidation")) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), actor);
        } else if (id.equals("smoke_charge")) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), actor);
        }
        return true;
    }

    private static boolean mobility(GalacticRecruitEntity actor, LivingEntity target) {
        Vec3 direction = target == null
                ? actor.getLookAngle() : target.position().subtract(actor.position());
        if (direction.lengthSqr() < 0.01D) return false;
        Vec3 normalized = direction.normalize();
        actor.push(normalized.x * 0.9D, 0.18D, normalized.z * 0.9D);
        actor.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 1));
        return true;
    }

    private static boolean defensive(GalacticRecruitEntity actor) {
        actor.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 1));
        actor.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0));
        return true;
    }

    private static boolean support(ServerLevel level, GalacticRecruitEntity actor, double range) {
        for (GalacticRecruitEntity ally : level.getEntitiesOfClass(
                GalacticRecruitEntity.class, actor.getBoundingBox().inflate(Math.max(4.0D, range)),
                recruit -> recruit.isAlive() && recruit.factionRelationTo(actor)
                        != galacticwars.clonewars.faction.FactionRelation.ENEMY)) {
            ally.heal(2.0F);
            ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0));
        }
        return true;
    }

    private static boolean passive(GalacticRecruitEntity actor, String abilityId) {
        String id = path(abilityId);
        if (id.contains("mobility") || id.contains("patience")) {
            actor.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, false, false));
        } else if (id.contains("resistance") || id.contains("ward") || id.contains("beskar")) {
            actor.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0, false, false));
        } else if (id.contains("steady") || id.contains("focus")) {
            actor.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 0, false, false));
        } else {
            actor.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false));
        }
        return true;
    }

    private static String path(String id) {
        int separator = id.indexOf(':');
        return separator < 0 ? id : id.substring(separator + 1);
    }
}
