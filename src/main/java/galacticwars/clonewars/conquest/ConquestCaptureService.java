package galacticwars.clonewars.conquest;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.faction.FactionRelation;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.progression.GalacticSystemsService;
import galacticwars.clonewars.progression.ProgressionEvent;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.recruitment.NpcServiceBranch;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/** Server-authoritative, world-present capture transaction shared by runtime ticks and GameTests. */
public final class ConquestCaptureService {
    private ConquestCaptureService() {
    }

    public static CaptureResult tick(
            ServerLevel level,
            LaunchContentDefinitions.ConquestRegionDefinition region,
            BlockPos beacon
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(beacon, "beacon");
        ConquestSavedData data = ConquestSavedData.get(level);
        ConquestControlState state = data.state(region.id()).orElse(null);
        if (state == null) {
            return CaptureResult.rejected("region_not_initialized", 0, "");
        }
        String normalizedFaction = namespacedFaction(state.controllingFaction());
        if (!normalizedFaction.equals(state.controllingFaction())) {
            state = state.withControllingFaction(normalizedFaction);
            data.put(state);
        }
        ServerPlayer player = level.players().stream()
                .filter(candidate -> candidate.blockPosition().distSqr(beacon)
                        <= (double) region.captureRadius() * region.captureRadius())
                .filter(candidate -> ownedMilitaryStrength(
                        level, candidate, beacon, region.captureRadius()) > 0)
                .findFirst().orElse(null);
        if (player == null) {
            int progress = Math.max(0, state.progress() - 10);
            if (progress != state.progress()) {
                state = state.withProgress("", progress);
                data.put(state);
            }
            return CaptureResult.accepted(false, "awaiting_commander", state);
        }
        String playerFaction = namespacedFaction(
                ProgressionSavedData.get(level).state(player.getUUID()).factionId());
            if (state.progress() > 0) {
                state = state.withProgress("", 0);
                data.put(state);
            }
            return CaptureResult.accepted(false, "already_controlled", state);
        }
        int friendly = ownedMilitaryStrength(level, player, beacon, region.captureRadius());
        int defenders = defenderStrength(level, player, beacon, region.captureRadius());
        if (defenders >= friendly) {
            return CaptureResult.accepted(false, "defenders_holding", state);
        }
        int progress = Math.min(
                region.captureTicks(), state.progress() + (friendly - defenders) * 20);
        ConquestControlState progressed = state.withProgress(player.getUUID().toString(), progress);
        if (progress < region.captureTicks()) {
            data.put(progressed);
            return CaptureResult.accepted(false, "capturing", progressed);
        }
        ConquestControlState captured = progressed.captured(playerFaction, playerKingdom);
        UUID eventId = UUID.nameUUIDFromBytes(("conquest:" + region.id() + ":"
                + player.getUUID() + ":" + captured.revision()).getBytes(StandardCharsets.UTF_8));
        ProgressionSavedData progression = ProgressionSavedData.get(level);
        GalacticSystemsService.SystemDecision gate = GalacticSystemsService.captureRegion(
                progression.state(player.getUUID()), eventId, region.id());
        if (!gate.accepted()) {
            ConquestControlState held = progressed.withProgress(
                    "", Math.max(0, region.captureTicks() - 20));
            data.put(held);
            return CaptureResult.rejected(gate.reason(), held.progress(), held.controllingFaction());
        }
        var committed = progression.apply(new ProgressionEvent(
                eventId, player.getUUID(), ProgressionEventType.REGION_CAPTURED, region.id(), 1));
        if (!committed.accepted()) {
            return CaptureResult.rejected(
                    committed.reason(), progressed.progress(), progressed.controllingFaction());
        }
        data.put(captured);
        return CaptureResult.accepted(true, "captured", captured);
    }

    private static int ownedMilitaryStrength(
            ServerLevel level, ServerPlayer player, BlockPos beacon, int radius
    ) {
        return level.getEntitiesOfClass(GalacticRecruitEntity.class,
                new AABB(beacon).inflate(radius),
                recruit -> recruit.isAlive() && recruit.isOwnedBy(player)
                        && recruit.getServiceBranch() == NpcServiceBranch.MILITARY).size();
    }

    private static int defenderStrength(
            ServerLevel level, ServerPlayer player, BlockPos beacon, int radius
    ) {
        return level.getEntitiesOfClass(GalacticRecruitEntity.class,
                new AABB(beacon).inflate(radius),
                recruit -> recruit.isAlive()
                        && recruit.getServiceBranch() == NpcServiceBranch.MILITARY
                        && recruit.factionRelationTo(player) == FactionRelation.ENEMY).size();
    }

    private static String namespacedFaction(String factionId) {
        return factionId == null || factionId.isBlank() || factionId.indexOf(':') >= 0
                ? (factionId == null ? "" : factionId)
                : "galacticwars:" + factionId;
    }

    public record CaptureResult(
            boolean accepted,
            boolean captured,
            String reason,
            int progress,
            String controllingFaction
    ) {
        public CaptureResult {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(controllingFaction, "controllingFaction");
            if (progress < 0) {
                throw new IllegalArgumentException("progress cannot be negative");
            }
        }

        private static CaptureResult accepted(
                boolean captured, String reason, ConquestControlState state
        ) {
            return new CaptureResult(
                    true, captured, reason, state.progress(), state.controllingFaction());
        }

        private static CaptureResult rejected(String reason, int progress, String faction) {
            return new CaptureResult(false, false, reason, progress, faction);
        }
    }
}
