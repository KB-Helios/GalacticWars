package galacticwars.clonewars.world;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.recruitment.NpcServiceBranch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

@EventBusSubscriber(modid = GalacticWars.MODID)
public final class FactionNaturalSpawnEvents {
    private FactionNaturalSpawnEvents() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof GalacticRecruitEntity recruit)
                || event.getLevel().getLevel().dimension() != Level.OVERWORLD
                || event.getSpawnType() != EntitySpawnReason.NATURAL
                        && event.getSpawnType() != EntitySpawnReason.CHUNK_GENERATION) {
            return;
        }
        String entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(recruit.getType()).toString();
        OverworldFactionSpawnProfile profile = GameplayDataManager.snapshot()
                .overworldSpawnProfileForEntity(entityTypeId).orElse(null);
        if (profile == null) {
            event.setSpawnCancelled(true);
            return;
        }
        NpcServiceBranch branch = profile.branchFor(entityTypeId);
        ServerLevel level = event.getLevel().getLevel();
        BlockPos position = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        FactionOutpostRecord outpost = FactionOutpostSavedData.get(level).assignNaturalNpc(
                recruit.getUUID(), profile, branch,
                level.dimension().identifier().toString(), position, level.getGameTime()).orElse(null);
        if (outpost == null) {
            event.setSpawnCancelled(true);
            return;
        }
        recruit.initializeNaturalFactionNpc(
                outpost.id(), branch, FactionOutpostMarkerService.shelterCenter(outpost), outpost.radius());
        FactionOutpostSavedData data = FactionOutpostSavedData.get(level);
        if (!data.siteGenerated(outpost.id()) && FactionOutpostMarkerService.generate(level, outpost)) {
            data.markSiteGenerated(outpost.id());
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof GalacticRecruitEntity recruit)
                || !(event.getLevel() instanceof ServerLevel level)
                || recruit.getFactionOutpostId() == null
                || recruit.getRemovalReason() == null
                || !recruit.getRemovalReason().shouldDestroy()) {
            return;
        }
        FactionOutpostSavedData.get(level).removeNpc(recruit.getUUID(), level.getGameTime());
    }
}
