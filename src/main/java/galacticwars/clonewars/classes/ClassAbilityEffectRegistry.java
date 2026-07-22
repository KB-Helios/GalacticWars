package galacticwars.clonewars.classes;

import galacticwars.clonewars.Config;
import galacticwars.clonewars.ability.AbilityDefinition;
import galacticwars.clonewars.ability.AbilityActivation;
import galacticwars.clonewars.combat.BlasterBoltEntity;
import galacticwars.clonewars.combat.FactionRangedWeaponService;
import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.faction.FactionRelation;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
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
        return execute(level, (LivingEntity) actor, ability, target);
    }

    /** Executes a validated class ability for either a recruit or a server player. */
    public static boolean execute(
            ServerLevel level,
            LivingEntity actor,
            AbilityDefinition ability,
            LivingEntity target
    ) {
        return switch (ClassAbilityExecutorCatalog.family(ability.id().toString())) {
            case PROJECTILE -> projectile(level, actor, target, ability);
            case MARK -> mark(level, actor, target, ability);
            case MOBILITY -> mobility(actor, target);
            case DEFENSIVE -> defensive(actor);
            case SUPPORT -> support(level, actor, ability.range());
            case PASSIVE -> passive(actor, ability.id().toString());
        };
    }

    /** Returns whether an aimed class ability is allowed to affect this target. */
    public static boolean isValidTarget(
            ServerLevel level,
            LivingEntity actor,
            AbilityDefinition ability,
            LivingEntity target
    ) {
        if (ability.activation() != AbilityActivation.TARGET || target == null) {
            return false;
        }
        return switch (ClassAbilityExecutorCatalog.family(ability.id().toString())) {
            case PROJECTILE, MARK, MOBILITY -> hostileTo(actor, target, level);
            case DEFENSIVE, SUPPORT, PASSIVE -> false;
        };
    }

    private static boolean projectile(
            ServerLevel level, LivingEntity actor,
            LivingEntity target, AbilityDefinition ability
    ) {
        if (target == null) return false;
        Vec3 direction = target.getEyePosition().subtract(actor.getEyePosition());
        if (direction.lengthSqr() < 0.01D) return false;
        ItemStack weapon = projectileWeapon(actor, ability);
        BlasterBoltEntity bolt = new BlasterBoltEntity(level, actor,
                weapon,
                path(ability.id().toString()).contains("heavy") || path(ability.id().toString()).contains("barrage")
                        ? 8.0D : 5.0D);
        bolt.setPos(actor.getEyePosition().add(direction.normalize().scale(0.7D)));
        bolt.shoot(direction.x, direction.y, direction.z, 3.2F, 0.5F);
        boolean spawned = level.addFreshEntity(bolt);
        if (spawned && path(ability.id().toString()).equals("crippling_shot")) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1), actor);
        }
        return spawned;
    }

    private static ItemStack projectileWeapon(LivingEntity actor, AbilityDefinition ability) {
        ItemStack held = actor.getMainHandItem();
        if (FactionRangedWeaponService.supportsRecruitRangedCombat(held)) {
            return held;
        }
        return switch (path(ability.id().toString())) {
            case "networked_volley", "heavy_fire" -> new ItemStack(ModItems.E5_BLASTER.get());
            case "braced_barrage" -> new ItemStack(ModItems.WESTAR_BLASTER.get());
            case "crippling_shot" -> new ItemStack(ModItems.NIGHTSISTER_BOW.get());
            default -> new ItemStack(ModItems.DC15_BLASTER.get());
        };
    }

    private static boolean mark(
            ServerLevel level, LivingEntity actor, LivingEntity target, AbilityDefinition ability
    ) {
        if (ability.activation() != AbilityActivation.AREA) {
            return debuff(actor, target, ability);
        }
        int affected = 0;
        for (LivingEntity candidate : level.getEntitiesOfClass(
                LivingEntity.class,
                actor.getBoundingBox().inflate(Math.max(1.0D, ability.range())),
                candidate -> hostileTo(actor, candidate, level))) {
            if (debuff(actor, candidate, ability)) {
                affected++;
            }
        }
        return affected > 0;
    }

    private static boolean debuff(
            LivingEntity actor, LivingEntity target, AbilityDefinition ability
    ) {
        if (target == null || target == actor || !target.isAlive()) return false;
        String id = path(ability.id().toString());
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), actor);
        if (id.equals("target_disruption")) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1), actor);
        } else if (id.equals("intimidation")) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), actor);
        } else if (id.equals("smoke_charge")) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), actor);
        }
        return true;
    }

    private static boolean mobility(LivingEntity actor, LivingEntity target) {
        Vec3 direction = target == null
                ? actor.getLookAngle() : target.position().subtract(actor.position());
        if (direction.lengthSqr() < 0.01D) return false;
        Vec3 normalized = direction.normalize();
        actor.push(normalized.x * 0.9D, 0.18D, normalized.z * 0.9D);
        actor.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 1));
        return true;
    }

    private static boolean defensive(LivingEntity actor) {
        actor.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 1));
        actor.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0));
        return true;
    }

    private static boolean support(ServerLevel level, LivingEntity actor, double range) {
        actor.heal(2.0F);
        actor.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0));
        for (LivingEntity ally : level.getEntitiesOfClass(
                LivingEntity.class, actor.getBoundingBox().inflate(Math.max(4.0D, range)),
                candidate -> candidate != actor && alliedWith(actor, candidate, level))) {
            ally.heal(2.0F);
            ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0));
        }
        return true;
    }

    private static boolean passive(LivingEntity actor, String abilityId) {
        String id = path(abilityId);
        if (id.contains("mobility") || id.contains("patience")) {
            actor.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, false, false));
        } else if (id.contains("resistance") || id.contains("ward") || id.contains("beskar")) {
            actor.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0, false, false));
        } else if (id.contains("steady") || id.contains("focus") || id.contains("weapon_mastery")) {
            actor.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 0, false, false));
        } else {
            actor.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false));
        }
        return true;
    }

    private static boolean hostileTo(LivingEntity actor, LivingEntity candidate, ServerLevel level) {
        if (candidate == actor || !candidate.isAlive() || candidate.isInvulnerable()
                || candidate.isSpectator()) {
            return false;
        }
        if (actor instanceof GalacticRecruitEntity recruit) {
            if (candidate instanceof GalacticRecruitEntity other) {
                return recruit.isHostileFactionRecruit(other);
            }
            if (candidate instanceof Player player) {
                return Config.ALLOW_CLASS_PVP.getAsBoolean()
                        && recruit.factionRelationTo(player) == FactionRelation.ENEMY;
            }
            return candidate instanceof Monster;
        }
        if (actor instanceof Player player) {
            if (candidate instanceof GalacticRecruitEntity recruit) {
                return recruit.factionRelationTo(player) == FactionRelation.ENEMY;
            }
            if (candidate instanceof Player other) {
                return Config.ALLOW_CLASS_PVP.getAsBoolean()
                        && !other.hasInfiniteMaterials()
                        && playerFactionsAreEnemies(level, player, other);
            }
            return candidate instanceof Monster;
        }
        return candidate instanceof Monster;
    }

    private static boolean alliedWith(LivingEntity actor, LivingEntity candidate, ServerLevel level) {
        if (!candidate.isAlive()) {
            return false;
        }
        if (actor instanceof GalacticRecruitEntity recruit) {
            return candidate instanceof GalacticRecruitEntity other
                    && recruit.factionRelationTo(other) != FactionRelation.ENEMY;
        }
        if (actor instanceof Player player) {
            String playerFaction = ProgressionSavedData.get(level).state(player.getUUID()).factionId();
            if (playerFaction.isBlank()) {
                return false;
            }
            if (candidate instanceof GalacticRecruitEntity recruit) {
                return recruit.isOwnedBy(player) || recruit.factionIdForGameplay().equals(playerFaction);
            }
            if (candidate instanceof Player other) {
                return playerFaction.equals(
                        ProgressionSavedData.get(level).state(other.getUUID()).factionId());
            }
        }
        return false;
    }

    private static boolean playerFactionsAreEnemies(
            ServerLevel level, Player first, Player second
    ) {
        String firstFaction = ProgressionSavedData.get(level).state(first.getUUID()).factionId();
        String secondFaction = ProgressionSavedData.get(level).state(second.getUUID()).factionId();
        return !firstFaction.isBlank() && !secondFaction.isBlank()
                && GameplayDataManager.snapshot().factions().relation(
                        FactionId.of(firstFaction), FactionId.of(secondFaction)) == FactionRelation.ENEMY;
    }

    private static String path(String id) {
        int separator = id.indexOf(':');
        return separator < 0 ? id : id.substring(separator + 1);
    }
}
