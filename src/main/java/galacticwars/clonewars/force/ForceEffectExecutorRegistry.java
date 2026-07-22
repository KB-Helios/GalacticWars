package galacticwars.clonewars.force;

import galacticwars.clonewars.Config;
import galacticwars.clonewars.combat.BlasterBoltEntity;
import galacticwars.clonewars.combat.LightsaberDeflectionService;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.faction.FactionRelation;
import galacticwars.clonewars.vehicle.GalacticVehicleEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/** Concrete bounded Force executors shared by player and NPC casts. */
public final class ForceEffectExecutorRegistry {
    private static final Map<String, ForceEffectExecutor> EXECUTORS = executors();

    private ForceEffectExecutorRegistry() {
    }

    public static ForceEffectReport execute(ForceEffectContext context) {
        ForceEffectExecutor executor = EXECUTORS.get(context.ability().executor());
        return executor == null
                ? ForceEffectReport.failed("missing_force_executor")
                : executor.execute(context);
    }

    private static Map<String, ForceEffectExecutor> executors() {
        Map<String, ForceEffectExecutor> result = new LinkedHashMap<>();
        result.put("push", context -> impulse(context, false, 1.45D));
        result.put("pull", context -> impulse(context, true, 1.25D));
        result.put("leap", context -> selfMovement(context, 0.9D, 0.85D));
        result.put("dash", context -> selfMovement(context, 1.6D, 0.08D));
        result.put("guard", ForceEffectExecutorRegistry::guard);
        result.put("repulse", context -> impulse(context, false, 2.0D));
        result.put("guardian_stand", context -> defensive(context, true));
        result.put("stasis", context -> restrain(context, false, 0.0F));
        result.put("mind_trick", ForceEffectExecutorRegistry::mindTrick);
        result.put("healing_meditation", context -> healing(context, false));
        result.put("balance_wave", ForceEffectExecutorRegistry::balanceWave);
        result.put("saber_frenzy", context -> saberBuff(context, true));
        result.put("choke", context -> restrain(context, true, 2.0F));
        result.put("throw", context -> impulse(context, false, 2.45D));
        result.put("assault", context -> assault(context, false));
        result.put("lightning", context -> lightning(context, false));
        result.put("chain_lightning", context -> lightning(context, true));
        result.put("crush", context -> restrain(context, true, 4.0F));
        result.put("maelstrom", ForceEffectExecutorRegistry::maelstrom);
        result.put("blade_dance", context -> meleeArea(context, 5.0F));
        result.put("spirit_ward", ForceEffectExecutorRegistry::guard);
        result.put("shadow_hunt", context -> assault(context, true));
        result.put("hex", ForceEffectExecutorRegistry::hex);
        result.put("spirit_snare", context -> restrain(context, false, 1.0F));
        result.put("ichor_bolt", context -> directDamage(context, 6.0F, "ichor"));
        result.put("life_weave", context -> healing(context, true));
        result.put("ritual_storm", ForceEffectExecutorRegistry::ritualStorm);
        return Map.copyOf(result);
    }

    private static ForceEffectReport impulse(
            ForceEffectContext context, boolean pull, double requested
    ) {
        ArrayList<UUID> affected = new ArrayList<>();
        double movement = 0.0D;
        double chargeScale = 0.55D + 0.75D * chargeRatio(context);
        for (Entity target : context.targets()) {
            if (!physicalTargetAllowed(context.caster(), target)) continue;
            Vec3 direction = pull
                    ? context.caster().position().subtract(target.position())
                    : target.position().subtract(context.caster().position());
            if (!pull && context.ability().target().equals("cone")) {
                direction = context.caster().getLookAngle();
            }
            if (direction.lengthSqr() < 0.01D) continue;
            double strength = ForcePhysicsRules.impulseForMass(
                    requested * chargeScale, mass(target), knockbackResistance(target), bossAnchored(target));
            if (strength <= 0.0D) continue;
            Vec3 impulse = direction.normalize().scale(strength).add(0.0D, pull ? 0.12D : 0.22D, 0.0D);
            applyVelocity(target, impulse);
            ForceCollisionDamageService.track(context.level(), context.caster(), target, mass(target));
            affected.add(target.getUUID());
            movement += strength;
        }
        return report(affected, movement, 0.0D, true, "telekinesis");
    }

    private static ForceEffectReport selfMovement(
            ForceEffectContext context, double horizontal, double vertical
    ) {
        Vec3 look = context.caster().getLookAngle().normalize();
        double scale = 0.55D + 0.75D * chargeRatio(context);
        applyVelocity(context.caster(), new Vec3(
                look.x * horizontal * scale,
                Math.max(vertical, look.y * horizontal * 0.3D) * scale,
                look.z * horizontal * scale));
        return new ForceEffectReport(true, "accepted", List.of(context.caster().getUUID()),
                horizontal * scale, 0.0D, false, List.of("movement"));
    }

    private static ForceEffectReport guard(ForceEffectContext context) {
        if (!LightsaberDeflectionService.holdsLightsaber(context.caster())) {
            return ForceEffectReport.failed("lightsaber_required");
        }
        ArrayList<UUID> affected = new ArrayList<>();
        for (Entity target : context.targets()) {
            if (!(target instanceof Projectile projectile)) continue;
            Vec3 toProjectile = projectile.position().subtract(context.caster().getEyePosition());
            if (toProjectile.lengthSqr() > 0.01D
                    && toProjectile.normalize().dot(context.caster().getLookAngle()) < 0.45D) continue;
            boolean deflected;
            if (projectile instanceof BlasterBoltEntity bolt) {
                deflected = bolt.deflectTowardOwner(context.caster());
            } else {
                Vec3 reflected = projectile.getDeltaMovement().scale(-1.0D);
                deflected = reflected.lengthSqr() > 0.01D;
                if (deflected) {
                    projectile.setOwner(context.caster());
                    projectile.setDeltaMovement(reflected.normalize().scale(
                            Math.min(ForcePhysicsRules.MAX_VELOCITY,
                                    Math.max(0.8D, reflected.length()))));
                }
            }
            if (deflected) affected.add(projectile.getUUID());
        }
        if (affected.isEmpty()) {
            return new ForceEffectReport(true, "accepted", List.of(),
                    0.0D, 0.0D, false, List.of("guard"));
        }
        return report(affected, affected.size(), 0.0D, true, "deflection");
    }

    private static ForceEffectReport defensive(ForceEffectContext context, boolean allies) {
        context.caster().addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
                Math.max(20, context.ability().durationTicks()), 1));
        ArrayList<UUID> affected = new ArrayList<>();
        affected.add(context.caster().getUUID());
        if (allies) {
            for (Entity target : context.targets()) {
                if (target instanceof LivingEntity living && allied(context.caster(), living)) {
                    living.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0));
                    affected.add(living.getUUID());
                }
            }
        }
        return report(affected, 0.0D, 0.0D, false, "ward");
    }

    private static ForceEffectReport restrain(
            ForceEffectContext context, boolean levitate, float damage
    ) {
        ArrayList<UUID> affected = new ArrayList<>();
        double dealt = 0.0D;
        for (Entity entity : context.targets()) {
            if (!(entity instanceof LivingEntity target) || !hostile(context.caster(), target)) continue;
            target.addEffect(new MobEffectInstance(
                    levitate ? MobEffects.LEVITATION : MobEffects.SLOWNESS, 12,
                    levitate ? 0 : 4), context.caster());
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 1), context.caster());
            if (damage > 0.0F && damage(context, target, damage)) dealt += damage;
            affected.add(target.getUUID());
        }
        return report(affected, levitate ? affected.size() * 0.1D : 0.0D,
                dealt, true, levitate ? "grip" : "snare");
    }

    private static ForceEffectReport mindTrick(ForceEffectContext context) {
        ArrayList<UUID> affected = new ArrayList<>();
        for (Entity entity : context.targets()) {
            if (!(entity instanceof LivingEntity target) || !hostile(context.caster(), target)) continue;
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), context.caster());
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), context.caster());
            affected.add(target.getUUID());
        }
        return report(affected, 0.0D, 0.0D, true, "mind_trick");
    }

    private static ForceEffectReport healing(ForceEffectContext context, boolean drainEnemy) {
        ArrayList<UUID> affected = new ArrayList<>();
        double healed = 0.0D;
        if (context.caster().getHealth() < context.caster().getMaxHealth()) {
            context.caster().heal(3.0F);
            affected.add(context.caster().getUUID());
            healed += 3.0D;
        }
        for (Entity entity : context.targets()) {
            if (!(entity instanceof LivingEntity target)) continue;
            if (allied(context.caster(), target) && target.getHealth() < target.getMaxHealth()) {
                target.heal(2.0F);
                affected.add(target.getUUID());
                healed += 2.0D;
            } else if (drainEnemy && hostile(context.caster(), target) && damage(context, target, 2.0F)) {
                context.caster().heal(1.0F);
                affected.add(target.getUUID());
                healed += 1.0D;
            }
        }
        return report(affected, 0.0D, drainEnemy ? healed : 0.0D, drainEnemy, "healing");
    }

    private static ForceEffectReport balanceWave(ForceEffectContext context) {
        ForceEffectReport movement = impulse(context, false, 1.65D);
        for (Entity entity : context.targets()) {
            if (entity instanceof LivingEntity target && hostile(context.caster(), target)) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), context.caster());
            }
        }
        return movement;
    }

    private static ForceEffectReport saberBuff(ForceEffectContext context, boolean frenzy) {
        if (!LightsaberDeflectionService.holdsLightsaber(context.caster())) {
            return ForceEffectReport.failed("lightsaber_required");
        }
        context.caster().addEffect(new MobEffectInstance(MobEffects.STRENGTH, 80, frenzy ? 1 : 0));
        context.caster().addEffect(new MobEffectInstance(MobEffects.SPEED, 80, 1));
        return new ForceEffectReport(true, "accepted", List.of(context.caster().getUUID()),
                0.0D, 0.0D, false, List.of("saber"));
    }

    private static ForceEffectReport assault(ForceEffectContext context, boolean shadow) {
        ForceEffectReport movement = selfMovement(context, shadow ? 2.0D : 1.8D, 0.12D);
        ForceEffectReport damage = meleeArea(context, shadow ? 7.0F : 6.0F);
        return merge(movement, damage, "assault");
    }

    private static ForceEffectReport lightning(ForceEffectContext context, boolean chain) {
        ArrayList<UUID> affected = new ArrayList<>();
        double dealt = 0.0D;
        int maximum = chain ? 4 : 1;
        for (Entity entity : context.targets()) {
            if (affected.size() >= maximum) break;
            if (!(entity instanceof LivingEntity target) || !hostile(context.caster(), target)) continue;
            float amount = chain ? 5.0F : 4.0F;
            if (damage(context, target, amount)) dealt += amount;
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1), context.caster());
            affected.add(target.getUUID());
        }
        return report(affected, 0.0D, dealt, true, "lightning");
    }

    private static ForceEffectReport maelstrom(ForceEffectContext context) {
        ForceEffectReport movement = impulse(context, true, 1.2D);
        ForceEffectReport damage = directDamage(context, 3.0F, "maelstrom");
        return merge(movement, damage, "maelstrom");
    }

    private static ForceEffectReport meleeArea(ForceEffectContext context, float amount) {
        return directDamage(context, amount, "melee");
    }

    private static ForceEffectReport hex(ForceEffectContext context) {
        ArrayList<UUID> affected = new ArrayList<>();
        for (Entity entity : context.targets()) {
            if (!(entity instanceof LivingEntity target) || !hostile(context.caster(), target)) continue;
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), context.caster());
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1), context.caster());
            affected.add(target.getUUID());
        }
        return report(affected, 0.0D, 0.0D, true, "hex");
    }

    private static ForceEffectReport ritualStorm(ForceEffectContext context) {
        ForceEffectReport damage = directDamage(context, 4.0F, "ritual_storm");
        for (Entity entity : context.targets()) {
            if (entity instanceof LivingEntity target && hostile(context.caster(), target)) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), context.caster());
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2), context.caster());
            }
        }
        return damage;
    }

    private static ForceEffectReport directDamage(
            ForceEffectContext context, float amount, String cue
    ) {
        ArrayList<UUID> affected = new ArrayList<>();
        double dealt = 0.0D;
        for (Entity entity : context.targets()) {
            if (!(entity instanceof LivingEntity target) || !hostile(context.caster(), target)) continue;
            if (damage(context, target, amount)) {
                dealt += amount;
                affected.add(target.getUUID());
            }
        }
        return report(affected, 0.0D, dealt, true, cue);
    }

    private static boolean damage(ForceEffectContext context, LivingEntity target, float amount) {
        return target.hurtServer(context.level(), context.level().damageSources().magic(), amount);
    }

    private static ForceEffectReport report(
            List<UUID> affected, double movement, double damage,
            boolean progressionEligible, String cue
    ) {
        if (affected.isEmpty()) return ForceEffectReport.failed("force_target_required");
        return new ForceEffectReport(true, "accepted", affected,
                movement, damage, progressionEligible, List.of(cue));
    }

    private static ForceEffectReport merge(
            ForceEffectReport first, ForceEffectReport second, String cue
    ) {
        ArrayList<UUID> ids = new ArrayList<>(first.affectedEntities());
        for (UUID id : second.affectedEntities()) if (!ids.contains(id)) ids.add(id);
        if (ids.size() > ForcePhysicsRules.MAX_AOE_TARGETS) {
            ids.subList(ForcePhysicsRules.MAX_AOE_TARGETS, ids.size()).clear();
        }
        if (ids.isEmpty()) return ForceEffectReport.failed("force_target_required");
        return new ForceEffectReport(true, "accepted", ids,
                first.movement() + second.movement(), first.damage() + second.damage(),
                first.progressionEligible() || second.progressionEligible(), List.of(cue));
    }

    private static double chargeRatio(ForceEffectContext context) {
        int maximum = Math.max(1, context.ability().maxChargeTicks());
        return Math.max(0.0D, Math.min(1.0D, context.activeTicks() / (double) maximum));
    }

    private static double mass(Entity entity) {
        if (entity instanceof Projectile) return 0.25D;
        if (entity instanceof ItemEntity) return 0.35D;
        double volume = Math.max(0.25D,
                entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight());
        if (entity instanceof GalacticVehicleEntity) return Math.max(6.0D, volume * 4.0D);
        return Math.max(0.5D, Math.min(8.0D, volume));
    }

    private static double knockbackResistance(Entity entity) {
        return entity instanceof LivingEntity living
                ? living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) : 0.0D;
    }

    private static boolean bossAnchored(Entity entity) {
        return entity instanceof LivingEntity living && living.getMaxHealth() >= 200.0F;
    }

    private static void applyVelocity(Entity entity, Vec3 impulse) {
        Vec3 next = entity.getDeltaMovement().add(impulse);
        if (next.length() > ForcePhysicsRules.MAX_VELOCITY) {
            next = next.normalize().scale(ForcePhysicsRules.MAX_VELOCITY);
        }
        entity.setDeltaMovement(next);
    }

    private static boolean physicalTargetAllowed(LivingEntity caster, Entity target) {
        if (target instanceof LivingEntity living) return hostile(caster, living);
        if (target instanceof GalacticVehicleEntity vehicle) {
            if (!Config.ALLOW_FORCE_VEHICLE_PHYSICS.getAsBoolean()) return false;
            return !(caster instanceof Player player
                    && vehicle.ownerId().filter(player.getUUID()::equals).isPresent());
        }
        return target instanceof ItemEntity || target instanceof Projectile;
    }

    private static boolean hostile(LivingEntity caster, LivingEntity target) {
        if (target == caster || !target.isAlive() || target.isInvulnerable()) return false;
        if (target instanceof Player && !Config.ALLOW_FORCE_PVP.getAsBoolean()) return false;
        if (caster instanceof Player player && target instanceof GalacticRecruitEntity recruit) {
            return !recruit.isOwnedBy(player)
                    && recruit.factionRelationTo(player) == FactionRelation.ENEMY;
        }
        if (caster instanceof GalacticRecruitEntity recruit && target instanceof Player player) {
            return Config.ALLOW_FORCE_PVP.getAsBoolean()
                    && recruit.factionRelationTo(player) == FactionRelation.ENEMY;
        }
        if (caster instanceof GalacticRecruitEntity recruit
                && target instanceof GalacticRecruitEntity other) {
            return recruit.isHostileFactionRecruit(other);
        }
        return target instanceof Monster || target instanceof Player;
    }

    private static boolean allied(LivingEntity caster, LivingEntity target) {
        if (target == caster) return true;
        if (caster instanceof Player player && target instanceof GalacticRecruitEntity recruit) {
            return recruit.isOwnedBy(player)
                    || recruit.factionRelationTo(player) != FactionRelation.ENEMY;
        }
        if (caster instanceof GalacticRecruitEntity recruit
                && target instanceof GalacticRecruitEntity other) {
            return !recruit.isHostileFactionRecruit(other);
        }
        return caster.getTeam() != null && caster.isAlliedTo(target);
    }
}
