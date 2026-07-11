package galacticwars.clonewars.combat;

import galacticwars.clonewars.Config;
import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.faction.FactionRelation;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

public final class BlasterCombatEvents {
    private BlasterCombatEvents() {
    }

    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)
                || !(arrow.getWeaponItem().getItem() instanceof BlasterItem)
                || !(arrow.getOwner() instanceof ServerPlayer shooter)
                || !(event.getRayTraceResult() instanceof EntityHitResult hit)) {
            return;
        }
        Entity target = hit.getEntity();
        boolean blocked = target instanceof Player
                ? BlasterFriendlyFirePolicy.blocksHit(true, false, FactionRelation.NEUTRAL,
                        Config.ALLOW_BLASTER_FRIENDLY_FIRE.getAsBoolean(),
                        Config.ALLOW_BLASTER_PVP.getAsBoolean())
                : target instanceof GalacticRecruitEntity recruit && blocksRecruitHit(shooter, recruit);
        if (blocked) {
            event.setCanceled(true);
            arrow.discard();
        }
    }

    private static boolean blocksRecruitHit(ServerPlayer shooter, GalacticRecruitEntity recruit) {
        boolean sameOwner = recruit.isOwnedBy(shooter);
        FactionRelation relation = relation(shooter, recruit);
        return BlasterFriendlyFirePolicy.blocksHit(false, sameOwner, relation,
                Config.ALLOW_BLASTER_FRIENDLY_FIRE.getAsBoolean(),
                Config.ALLOW_BLASTER_PVP.getAsBoolean());
    }

    private static FactionRelation relation(ServerPlayer shooter, GalacticRecruitEntity recruit) {
        if (!(shooter.level() instanceof ServerLevel level)) {
            return FactionRelation.NEUTRAL;
        }
        return KingdomSavedData.get(level).kingdomForOwner(shooter.getUUID())
                .map(kingdom -> GameplayDataManager.snapshot().factions().relation(
                        FactionId.of(kingdom.factionId()), FactionId.of(recruit.getRecruitFactionId())))
                .orElse(FactionRelation.NEUTRAL);
    }
}
