package galacticwars.clonewars.world;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.army.ArmyTravelService;
import galacticwars.clonewars.kingdom.KingdomRecord;
import galacticwars.clonewars.kingdom.KingdomPermission;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.kingdom.KingdomActionId;
import galacticwars.clonewars.kingdom.KingdomGameplayAction;
import galacticwars.clonewars.kingdom.KingdomGameplayResult;
import galacticwars.clonewars.kingdom.KingdomGameplayRuntimeService;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.progression.ProgressionState;
import galacticwars.clonewars.settlement.CommandCenterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PlanetTravelService {
    private PlanetTravelService() {
    }

    public static TravelResult travel(ServerPlayer player, String planetId) {
        ServerLevel source = player.level();
        KingdomSavedData kingdoms = KingdomSavedData.get(source);
        ProgressionSavedData progression = ProgressionSavedData.get(source);
        ProgressionState state = progression.state(player.getUUID());
        CommandCenterBlockEntity hall = resolveCommandCenter(player, kingdoms).orElse(null);
        ResourceKey<Level> destinationKey = dimensionKey(planetId);
        ServerLevel destination = destinationKey == null ? null : source.getServer().getLevel(destinationKey);
        PlanetTravelPolicy.TravelAuthorization authorization = PlanetTravelPolicy.authorize(
                planetId,
                hall != null && hall.canUse(player, KingdomPermission.TRAVEL),
                hall != null,
                !state.factionId().isEmpty(),
                state.unlocks().contains("planet_travel"),
                hall != null && hall.upkeepPaid(),
                destination != null,
                destinationKey != null && source.dimension().equals(destinationKey));
        if (!authorization.accepted()) {
            return TravelResult.rejected(authorization.reason());
        }

        Optional<BlockPos> arrival = PlanetArrivalService.findOrCreate(destination);
        if (arrival.isEmpty()) {
            return TravelResult.rejected("safe_arrival_unavailable");
        }
        BlockPos target = arrival.orElseThrow();
        ArmyTravelService.TravelPlan squadTravel = ArmyTravelService.prepare(
                kingdoms, player, destination, target);
        if (!squadTravel.accepted()) {
            return TravelResult.rejected(squadTravel.reason());
        }
        if (!squadTravel.reserve()) {
            return TravelResult.rejected("squad_transfer_conflict");
        }
        boolean teleported = player.teleportTo(
                destination,
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                Set.<Relative>of(),
                player.getYRot(),
                player.getXRot(),
                false);
        if (!teleported) {
            if (!squadTravel.rollback(source.getGameTime())) {
                GalacticWars.LOGGER.error("Failed to roll back squad travel after player teleport failure for {}",
                        player.getGameProfile().name());
            }
            return TravelResult.rejected("teleport_failed");
        }
        squadTravel.commit();
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(
                LevelData.RespawnData.of(destinationKey, target, player.getYRot(), player.getXRot()), true), false);

        ProgressionState afterTravel = progression.state(player.getUUID());
        if (!afterTravel.hasSubject(ProgressionEventType.PLANET_VISITED, planetId)) {
            KingdomGameplayResult visit = KingdomGameplayRuntimeService.applyProgression(
                    progression, new KingdomGameplayAction(
                            KingdomActionId.of("planet_visited", player.getUUID(), planetId),
                            player.getUUID(), ProgressionEventType.PLANET_VISITED, planetId, 1));
            if (!visit.accepted()) {
                GalacticWars.LOGGER.error("Planet visit progression rejected after successful travel for {}: {}",
                        player.getGameProfile().name(), visit.reason());
            }
        }
        return TravelResult.accepted(planetId, target, squadTravel.transfersSquad());
    }

    public static boolean hasActiveCommandCenter(ServerPlayer player) {
        return resolveCommandCenter(player, KingdomSavedData.get(player.level())).isPresent();
    }

    private static Optional<CommandCenterBlockEntity> resolveCommandCenter(
            ServerPlayer player,
            KingdomSavedData kingdoms
    ) {
        KingdomRecord kingdom = kingdoms.kingdomForPlayer(player.getUUID()).orElse(null);
        if (kingdom == null || !kingdom.allows(player.getUUID(), KingdomPermission.TRAVEL)
                || !kingdoms.isHallActive(kingdom.ownerId())) {
            return Optional.empty();
        }
        var settlement = kingdom.settlement();
        ResourceKey<Level> hallDimension = ResourceKey.create(
                Registries.DIMENSION, Identifier.parse(settlement.dimensionId()));
        ServerLevel hallLevel = player.level().getServer().getLevel(hallDimension);
        if (hallLevel == null) {
            return Optional.empty();
        }
        BlockPos hallPos = new BlockPos(settlement.hallX(), settlement.hallY(), settlement.hallZ());
        hallLevel.getChunkAt(hallPos);
        if (!(hallLevel.getBlockEntity(hallPos) instanceof CommandCenterBlockEntity hall)
                || !hall.canUse(player, KingdomPermission.TRAVEL)) {
            return Optional.empty();
        }
        return Optional.of(hall);
    }

    private static ResourceKey<Level> dimensionKey(String planetId) {
        if (!galacticwars.clonewars.progression.LaunchContentCatalog.PLANETS.contains(planetId)) {
            return null;
        }
        return ResourceKey.create(Registries.DIMENSION,
                Identifier.fromNamespaceAndPath(GalacticWars.MODID, planetId));
    }

    public record TravelResult(
            boolean accepted,
            String reason,
            String planetId,
            BlockPos arrival,
            boolean squadTransferred
    ) {
        private static TravelResult accepted(String planetId, BlockPos arrival, boolean squadTransferred) {
            return new TravelResult(true, "accepted", planetId, arrival, squadTransferred);
        }

        private static TravelResult rejected(String reason) {
            return new TravelResult(false, reason, "", BlockPos.ZERO, false);
        }
    }

}
