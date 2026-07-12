package galacticwars.clonewars.combat;

import galacticwars.clonewars.Config;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.faction.FactionRelation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

public final class BlasterCombatEvents {
    private BlasterCombatEvents() {
    }

    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)
                || !FactionRangedWeaponService.isProtectedFactionProjectile(arrow.getWeaponItem())
                || !(arrow.getOwner() instanceof LivingEntity shooter)
                || !(event.getRayTraceResult() instanceof EntityHitResult hit)) {
            return;
        }
        Entity target = hit.getEntity();
        boolean blocked = target instanceof Player player
                ? blocksPlayerHit(shooter, player)
                : target instanceof GalacticRecruitEntity recruit && blocksRecruitHit(shooter, recruit);
        if (blocked) {
            event.setCanceled(true);
            arrow.discard();
        }
    }

    private static boolean blocksPlayerHit(LivingEntity shooter, Player player) {
        if (shooter instanceof ServerPlayer) {
            return BlasterFriendlyFirePolicy.blocksHit(
                    true, false, FactionRelation.NEUTRAL,
                    Config.ALLOW_BLASTER_FRIENDLY_FIRE.getAsBoolean(),
                    Config.ALLOW_BLASTER_PVP.getAsBoolean());
        }
        if (!(shooter instanceof GalacticRecruitEntity recruit)) {
            return true;
        }
        return BlasterFriendlyFirePolicy.blocksRecruitHitOnPlayer(
                recruit.isOwnedBy(player),
                recruit.factionRelationTo(player),
                Config.ALLOW_BLASTER_PVP.getAsBoolean());
    }

    private static boolean blocksRecruitHit(LivingEntity shooter, GalacticRecruitEntity recruit) {
        boolean sameOwner = shooter instanceof ServerPlayer player && recruit.isOwnedBy(player)
                || shooter instanceof GalacticRecruitEntity other && sameOwner(other, recruit);
        FactionRelation relation = relation(shooter, recruit);
        return BlasterFriendlyFirePolicy.blocksHit(false, sameOwner, relation,
                Config.ALLOW_BLASTER_FRIENDLY_FIRE.getAsBoolean(),
                Config.ALLOW_BLASTER_PVP.getAsBoolean());
    }

    private static FactionRelation relation(LivingEntity shooter, GalacticRecruitEntity recruit) {
        if (shooter instanceof GalacticRecruitEntity other) {
            return other.factionRelationTo(recruit);
        }
        return shooter instanceof ServerPlayer player
                ? recruit.factionRelationTo(player)
                : FactionRelation.NEUTRAL;
    }

    private static boolean sameOwner(GalacticRecruitEntity first, GalacticRecruitEntity second) {
        LivingEntity firstOwner = first.getOwner();
        LivingEntity secondOwner = second.getOwner();
        return firstOwner != null && secondOwner != null && firstOwner.getUUID().equals(secondOwner.getUUID());
    }
}
