package galacticwars.clonewars.force;

import galacticwars.clonewars.Config;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.faction.FactionRelation;
import galacticwars.clonewars.vehicle.GalacticVehicleEntity;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

/** Server-owned targeting. Clients never supply positions, targets, or force strength. */
public final class ForceTargetingService {
    private ForceTargetingService() {
    }

    public static List<Entity> targets(
            ServerLevel level,
            LivingEntity caster,
            String targetMode,
            double requestedRange
    ) {
        if (targetMode.equals("self")) return List.of(caster);
        double range = ForcePhysicsRules.boundedRange(requestedRange);
        if (range <= 0.0D) return List.of();
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 reach = look.scale(range);
        if (targetMode.equals("ray") || targetMode.equals("held_object")) {
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                    caster, eye, eye.add(reach),
                    caster.getBoundingBox().expandTowards(reach).inflate(1.0D),
                    entity -> eligible(caster, entity) && caster.hasLineOfSight(entity),
                    range * range);
            return hit == null ? List.of() : List.of(hit.getEntity());
        }
        return level.getEntities(
                        caster,
                        caster.getBoundingBox().inflate(range),
                        entity -> eligible(caster, entity)
                                && entity.distanceToSqr(caster) <= range * range
                                && caster.hasLineOfSight(entity)
                                && matchesMode(targetMode, entity)
                                && insideShape(targetMode, eye, look, entity))
                .stream()
                .sorted(Comparator.comparingDouble(caster::distanceToSqr))
                .limit(ForcePhysicsRules.MAX_AOE_TARGETS)
                .toList();
    }

    private static boolean matchesMode(String targetMode, Entity entity) {
        if (targetMode.equals("projectile")) return entity instanceof Projectile;
        return entity instanceof LivingEntity || entity instanceof ItemEntity
                || entity instanceof Projectile || entity instanceof GalacticVehicleEntity;
    }

    private static boolean insideShape(String mode, Vec3 eye, Vec3 look, Entity entity) {
        if (mode.equals("sphere")) return true;
        Vec3 direction = entity.getBoundingBox().getCenter().subtract(eye);
        return direction.lengthSqr() > 0.01D && direction.normalize().dot(look) >= 0.75D;
    }

    private static boolean eligible(LivingEntity caster, Entity entity) {
        if (entity == caster || !entity.isAlive() || entity.isSpectator()) return false;
        if (entity instanceof ServerPlayer && !Config.ALLOW_FORCE_PVP.getAsBoolean()) return false;
        if (entity instanceof GalacticVehicleEntity
                && !Config.ALLOW_FORCE_VEHICLE_PHYSICS.getAsBoolean()) return false;
        if (caster instanceof ServerPlayer player && entity instanceof GalacticRecruitEntity recruit) {
            FactionRelation relation = recruit.factionRelationTo(player);
            if (recruit.isOwnedBy(player) || relation == FactionRelation.SAME
                    || relation == FactionRelation.ALLY) {
                return true; // support executors may use allies; harmful executors re-check relation.
            }
        }
        return true;
    }
}
