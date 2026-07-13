package galacticwars.clonewars.kingdom;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = GalacticWars.MODID)
public final class SiegeRuntimeEvents {
    private SiegeRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) return;
        KingdomSavedData data = KingdomSavedData.get(event.getServer().overworld());
        for (KingdomSiege siege : data.sieges()) {
            if (siege.state() != SiegeState.ACTIVE) continue;
            KingdomClaim claim = data.kingdom(siege.defenderKingdomId()).stream()
                    .flatMap(kingdom -> kingdom.claims().stream())
                    .filter(candidate -> candidate.id().equals(siege.claimId())).findFirst().orElse(null);
            if (claim == null) continue;
            ServerLevel level;
            try {
                level = event.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,
                        Identifier.parse(claim.dimensionId())));
            } catch (RuntimeException exception) {
                continue;
            }
            if (level == null) continue;
            BlockPos center = new BlockPos((claim.center().x() << 4) + 8,
                    level.getSeaLevel(), (claim.center().z() << 4) + 8);
            List<GalacticRecruitEntity> nearby = level.getEntitiesOfClass(GalacticRecruitEntity.class,
                    new net.minecraft.world.phys.AABB(center).inflate(64.0D), GalacticRecruitEntity::isAlive);
            Set<UUID> attackerRoster = data.kingdom(siege.attackerKingdomId()).stream()
                    .flatMap(kingdom -> kingdom.npcRoster().stream())
                    .filter(npc -> npc.serviceBranch() == galacticwars.clonewars.recruitment.NpcServiceBranch.MILITARY)
                    .map(KingdomNpcRecord::recruitId).collect(Collectors.toSet());
            Set<UUID> defenderRoster = data.kingdom(siege.defenderKingdomId()).stream()
                    .flatMap(kingdom -> kingdom.npcRoster().stream())
                    .filter(npc -> npc.serviceBranch() == galacticwars.clonewars.recruitment.NpcServiceBranch.MILITARY)
                    .map(KingdomNpcRecord::recruitId).collect(Collectors.toSet());
            List<UUID> attackers = nearby.stream().map(GalacticRecruitEntity::getUUID)
                    .filter(attackerRoster::contains).toList();
            List<UUID> defenders = nearby.stream().map(GalacticRecruitEntity::getUUID)
                    .filter(defenderRoster::contains).toList();
            data.progressSiege(siege.id(), attackers.size(), defenders.size(),
                    level.getGameTime(), attackers, defenders);
        }
    }
}
