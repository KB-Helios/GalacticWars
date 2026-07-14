package galacticwars.clonewars.conquest;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.faction.FactionRelation;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.progression.LaunchContentCatalog;
import galacticwars.clonewars.recruitment.NpcServiceBranch;
import galacticwars.clonewars.registry.ModBlocks;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.EntitySpawnReason;
import galacticwars.clonewars.registry.ModEntityTypes;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = GalacticWars.MODID)
public final class ConquestRuntimeEvents {
    private ConquestRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) return;
        for (LaunchContentDefinitions.ConquestRegionDefinition region
                : LaunchContentCatalog.data().conquestRegions().values()) {
            tickRegion(event.getServer(), region);
        }
    }

    private static void tickRegion(MinecraftServer server, LaunchContentDefinitions.ConquestRegionDefinition region) {
        var planet = LaunchContentCatalog.data().planets().get(region.planetId());
        if (planet == null) return;
        ServerLevel level;
        try {
            level = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                    Identifier.parse(planet.dimensionId())));
        } catch (RuntimeException exception) {
            return;
        }
        if (level == null) return;
        ConquestSavedData data = ConquestSavedData.get(level);
        ConquestControlState state = data.state(region.id()).orElseGet(() -> {
            BlockPos beacon = resolveBeacon(level, region);
            ConquestControlState created = new ConquestControlState(region.id(), planet.dimensionId(),
                    beacon.getX(), beacon.getY(), beacon.getZ(), namespacedFaction(region.defenderFaction()),
                    "", "", 0, 0L);
            if (level.getBlockState(beacon).canBeReplaced()) {
                level.setBlockAndUpdate(beacon, ModBlocks.CONTROL_BEACON.get().defaultBlockState());
            }
            data.put(created);
            return created;
        });
        BlockPos beacon = new BlockPos(state.beaconX(), state.beaconY(), state.beaconZ());
        spawnControlPatrol(level, state, beacon);
        ConquestCaptureService.tick(level, region, beacon);
    }

    private static String namespacedFaction(String factionId) {
        return factionId == null || factionId.isBlank() || factionId.indexOf(':') >= 0
                ? (factionId == null ? "" : factionId)
                : "galacticwars:" + factionId;
    }

    private static BlockPos resolveBeacon(ServerLevel level, LaunchContentDefinitions.ConquestRegionDefinition region) {
        KingdomSavedData kingdoms = KingdomSavedData.get(level);
        String dimension = level.dimension().identifier().toString();
        for (int ring = 0; ring <= 8; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (ring > 0 && Math.abs(dx) != ring && Math.abs(dz) != ring) continue;
                    int x = region.landmarkX() + dx * 16;
                    int z = region.landmarkZ() + dz * 16;
                    if (kingdoms.claimAt(dimension, new ChunkPos(x >> 4, z >> 4)).isPresent()) continue;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos candidate = new BlockPos(x, y, z);
                    if (level.getBlockState(candidate).canBeReplaced()) return candidate;
                }
            }
        }
        return new BlockPos(region.landmarkX(),
                level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        region.landmarkX(), region.landmarkZ()), region.landmarkZ());
    }

    public static boolean arrivalClear(ServerLevel level, ServerPlayer player, BlockPos arrival) {
        return level.getEntitiesOfClass(GalacticRecruitEntity.class,
                        new net.minecraft.world.phys.AABB(arrival).inflate(24.0D),
                        recruit -> recruit.isAlive() && recruit.getServiceBranch() == NpcServiceBranch.MILITARY
                                && recruit.factionRelationTo(player) == FactionRelation.ENEMY)
                .isEmpty();
    }

    private static void spawnControlPatrol(
            ServerLevel level, ConquestControlState state, BlockPos beacon
    ) {
        if (state.controllingFaction().isEmpty() || level.getGameTime() % 600L != 0L
                || level.players().stream().noneMatch(player -> player.blockPosition().distSqr(beacon) <= 16384.0D)) {
            return;
        }
        UUID patrolId = UUID.nameUUIDFromBytes(("conquest-patrol:" + state.regionId())
                .getBytes(StandardCharsets.UTF_8));
        int present = level.getEntitiesOfClass(GalacticRecruitEntity.class,
                        new net.minecraft.world.phys.AABB(beacon).inflate(64.0D),
                        recruit -> patrolId.equals(recruit.getFactionOutpostId())).size();
        if (present >= 3) return;
        EntityType<GalacticRecruitEntity> type = patrolType(state.controllingFaction());
        if (type == null) return;
        GalacticRecruitEntity recruit = type.create(level, EntitySpawnReason.EVENT);
        if (recruit == null) return;
        int offset = 3 + present * 2;
        recruit.setPos(beacon.getX() + offset + 0.5D, beacon.getY(), beacon.getZ() + 0.5D);
        recruit.initializeFromSpawnEgg();
        recruit.initializeNaturalFactionNpc(patrolId, NpcServiceBranch.MILITARY, beacon, 48);
        level.addFreshEntity(recruit);
    }

    private static EntityType<GalacticRecruitEntity> patrolType(String factionId) {
        String path = factionId.contains(":")
                ? factionId.substring(factionId.indexOf(':') + 1) : factionId;
        return switch (path) {
            case "republic" -> ModEntityTypes.CLONE_TROOPER.get();
            case "separatist" -> ModEntityTypes.B1_BATTLE_DROID.get();
            case "mandalorian" -> ModEntityTypes.MANDALORIAN_WARRIOR.get();
            case "hutt_cartel" -> ModEntityTypes.HUTT_ENFORCER.get();
            case "nightsister" -> ModEntityTypes.NIGHTSISTER_ACOLYTE.get();
            default -> null;
        };
    }
}
