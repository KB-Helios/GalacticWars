package galacticwars.clonewars.menu;

import galacticwars.clonewars.kingdom.CommandCenterDashboardState;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState.BuildSummary;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState.BlueprintSummary;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState.ClaimSummary;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState.CombatTargetSummary;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState.DiplomacyProposalSummary;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState.ForeignKingdomSummary;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState.InviteSummary;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState.MemberSummary;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState.MaterialSummary;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState.NearbyPlayerSummary;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState.ObjectiveSummary;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState.PositionSummary;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState.QuestSummary;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState.SquadSummary;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState.WorkerSummary;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState.WorkOrderSummary;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Bounded wire format for Command Center dashboard snapshots. */
public final class CommandCenterDashboardCodec {
    private static final int MAX_STRING = 128;
    private static final int MAX_ENTRIES = 64;
    private static final int MAX_QUESTS = 16;
    private static final int MAX_OBJECTIVES = 16;

    private CommandCenterDashboardCodec() {
    }

    public static void write(RegistryFriendlyByteBuf buffer, CommandCenterDashboardState state) {
        buffer.writeVarLong(state.generatedGameTime());
        buffer.writeBoolean(state.kingdomAvailable());
        buffer.writeUUID(state.actorId());
        buffer.writeUUID(state.kingdomId());
        writeString(buffer, state.factionId());
        writeString(buffer, state.actorRole());
        buffer.writeVarInt(state.treasuryCredits());
        buffer.writeVarInt(state.pendingRewardCredits());
        buffer.writeBoolean(state.upkeepPaid());
        buffer.writeVarInt(state.recruitCount());
        buffer.writeVarInt(state.housingCapacity());
        buffer.writeVarInt(state.settlementCount());
        buffer.writeVarInt(state.claimCount());
        buffer.writeBoolean(state.commanderAssigned());
        buffer.writeBoolean(state.campaignVictory());
        buffer.writeVarInt(state.veteranVehicleDeployments());
        buffer.writeVarInt(state.veteranTrades());
        buffer.writeVarInt(state.veteranRegionCaptures());
        writeList(buffer, state.commandCandidateIds(), MAX_ENTRIES,
                (target, value) -> target.writeUUID(value));
        writeList(buffer, state.claims(), MAX_ENTRIES, CommandCenterDashboardCodec::writeClaim);
        writeList(buffer, state.squads(), MAX_ENTRIES, CommandCenterDashboardCodec::writeSquad);
        writeList(buffer, state.combatTargets(), MAX_ENTRIES,
                CommandCenterDashboardCodec::writeCombatTarget);
        writeList(buffer, state.blueprints(), MAX_ENTRIES, CommandCenterDashboardCodec::writeBlueprint);
        writeList(buffer, state.constructionBuilderIds(), MAX_ENTRIES,
                (target, value) -> target.writeUUID(value));
        writeList(buffer, state.builds(), MAX_ENTRIES, CommandCenterDashboardCodec::writeBuild);
        writeList(buffer, state.workOrders(), MAX_ENTRIES, CommandCenterDashboardCodec::writeWorkOrder);
        writeList(buffer, state.workers(), MAX_ENTRIES, CommandCenterDashboardCodec::writeWorker);
        writeList(buffer, state.campaign(), MAX_QUESTS, CommandCenterDashboardCodec::writeQuest);
        writeList(buffer, state.nearbyPlayers(), 16,
                CommandCenterDashboardCodec::writeNearbyPlayer);
        writeList(buffer, state.members(), MAX_ENTRIES, CommandCenterDashboardCodec::writeMember);
        writeList(buffer, state.foreignKingdoms(), MAX_ENTRIES,
                CommandCenterDashboardCodec::writeForeignKingdom);
        writeList(buffer, state.invites(), MAX_ENTRIES, CommandCenterDashboardCodec::writeInvite);
        writeList(buffer, state.diplomacyProposals(), MAX_ENTRIES,
                CommandCenterDashboardCodec::writeDiplomacyProposal);
    }

    public static CommandCenterDashboardState read(RegistryFriendlyByteBuf buffer) {
        return new CommandCenterDashboardState(
                buffer.readVarLong(),
                buffer.readBoolean(),
                buffer.readUUID(),
                buffer.readUUID(),
                readString(buffer),
                readString(buffer),
                nonNegative(buffer.readVarInt(), "treasuryCredits"),
                nonNegative(buffer.readVarInt(), "pendingRewardCredits"),
                buffer.readBoolean(),
                nonNegative(buffer.readVarInt(), "recruitCount"),
                nonNegative(buffer.readVarInt(), "housingCapacity"),
                nonNegative(buffer.readVarInt(), "settlementCount"),
                nonNegative(buffer.readVarInt(), "claimCount"),
                buffer.readBoolean(),
                buffer.readBoolean(),
                nonNegative(buffer.readVarInt(), "veteranVehicleDeployments"),
                nonNegative(buffer.readVarInt(), "veteranTrades"),
                nonNegative(buffer.readVarInt(), "veteranRegionCaptures"),
                readList(buffer, MAX_ENTRIES, target -> target.readUUID()),
                readList(buffer, MAX_ENTRIES, CommandCenterDashboardCodec::readClaim),
                readList(buffer, MAX_ENTRIES, CommandCenterDashboardCodec::readSquad),
                readList(buffer, MAX_ENTRIES, CommandCenterDashboardCodec::readCombatTarget),
                readList(buffer, MAX_ENTRIES, CommandCenterDashboardCodec::readBlueprint),
                readList(buffer, MAX_ENTRIES, target -> target.readUUID()),
                readList(buffer, MAX_ENTRIES, CommandCenterDashboardCodec::readBuild),
                readList(buffer, MAX_ENTRIES, CommandCenterDashboardCodec::readWorkOrder),
                readList(buffer, MAX_ENTRIES, CommandCenterDashboardCodec::readWorker),
                readList(buffer, MAX_QUESTS, CommandCenterDashboardCodec::readQuest),
                readList(buffer, 16, CommandCenterDashboardCodec::readNearbyPlayer),
                readList(buffer, MAX_ENTRIES, CommandCenterDashboardCodec::readMember),
                readList(buffer, MAX_ENTRIES, CommandCenterDashboardCodec::readForeignKingdom),
                readList(buffer, MAX_ENTRIES, CommandCenterDashboardCodec::readInvite),
                readList(buffer, MAX_ENTRIES, CommandCenterDashboardCodec::readDiplomacyProposal));
    }

    private static void writeSquad(RegistryFriendlyByteBuf buffer, SquadSummary value) {
        buffer.writeUUID(value.id());
        writeString(buffer, value.name());
        writeOptionalUuid(buffer, value.commanderId());
        writeList(buffer, value.memberIds(), MAX_ENTRIES,
                (target, memberId) -> target.writeUUID(memberId));
        buffer.writeVarInt(value.unitCount());
        writeString(buffer, value.order());
        writeString(buffer, value.formation());
        writeString(buffer, value.lifecycle());
        buffer.writeVarInt(value.supplyUnits());
    }

    private static SquadSummary readSquad(RegistryFriendlyByteBuf buffer) {
        return new SquadSummary(
                buffer.readUUID(), readString(buffer), readOptionalUuid(buffer),
                readList(buffer, MAX_ENTRIES, target -> target.readUUID()),
                nonNegative(buffer.readVarInt(), "unitCount"), readString(buffer), readString(buffer),
                readString(buffer), nonNegative(buffer.readVarInt(), "supplyUnits"));
    }

    private static void writeCombatTarget(
            RegistryFriendlyByteBuf buffer, CombatTargetSummary value
    ) {
        buffer.writeUUID(value.entityId());
        writeString(buffer, value.displayName());
        writeString(buffer, value.factionId());
        buffer.writeVarInt(value.distanceBlocks());
    }

    private static CombatTargetSummary readCombatTarget(RegistryFriendlyByteBuf buffer) {
        return new CombatTargetSummary(
                buffer.readUUID(), readString(buffer), readString(buffer),
                nonNegative(buffer.readVarInt(), "combatTargetDistance"));
    }

    private static void writeClaim(RegistryFriendlyByteBuf buffer, ClaimSummary value) {
        buffer.writeUUID(value.id());
        writeString(buffer, value.dimensionId());
        buffer.writeVarInt(value.centerChunkX());
        buffer.writeVarInt(value.centerChunkZ());
        buffer.writeVarInt(value.chunkCount());
        buffer.writeBoolean(value.capital());
    }

    private static ClaimSummary readClaim(RegistryFriendlyByteBuf buffer) {
        return new ClaimSummary(
                buffer.readUUID(), readString(buffer), buffer.readVarInt(), buffer.readVarInt(),
                nonNegative(buffer.readVarInt(), "claimChunkCount"), buffer.readBoolean());
    }

    private static void writeBlueprint(RegistryFriendlyByteBuf buffer, BlueprintSummary value) {
        buffer.writeUUID(value.targetId());
        writeString(buffer, value.blueprintId());
        writeString(buffer, value.displayName());
        writeList(buffer, value.allowedRotations(), 4,
                (target, rotation) -> target.writeVarInt(rotation));
        buffer.writeVarInt(value.placementCount());
        writeList(buffer, value.materials(), MAX_ENTRIES,
                CommandCenterDashboardCodec::writeMaterial);
        buffer.writeVarInt(value.housingReward());
        buffer.writeVarInt(value.storageSlotReward());
        buffer.writeVarInt(value.commanderSlotReward());
    }

    private static BlueprintSummary readBlueprint(RegistryFriendlyByteBuf buffer) {
        return new BlueprintSummary(
                buffer.readUUID(), readString(buffer), readString(buffer),
                readList(buffer, 4, target -> target.readVarInt()),
                nonNegative(buffer.readVarInt(), "blueprintPlacementCount"),
                readList(buffer, MAX_ENTRIES, CommandCenterDashboardCodec::readMaterial),
                nonNegative(buffer.readVarInt(), "blueprintHousingReward"),
                nonNegative(buffer.readVarInt(), "blueprintStorageReward"),
                nonNegative(buffer.readVarInt(), "blueprintCommanderReward"));
    }

    private static void writeMaterial(RegistryFriendlyByteBuf buffer, MaterialSummary value) {
        writeString(buffer, value.itemId());
        buffer.writeVarInt(value.count());
    }

    private static MaterialSummary readMaterial(RegistryFriendlyByteBuf buffer) {
        return new MaterialSummary(
                readString(buffer), nonNegative(buffer.readVarInt(), "blueprintMaterialCount"));
    }

    private static void writeBuild(RegistryFriendlyByteBuf buffer, BuildSummary value) {
        buffer.writeUUID(value.id());
        writeString(buffer, value.blueprintId());
        writeString(buffer, value.state());
        buffer.writeVarInt(value.completedPlacements());
        buffer.writeVarInt(value.totalPlacements());
        writeString(buffer, value.blockedReason());
    }

    private static BuildSummary readBuild(RegistryFriendlyByteBuf buffer) {
        return new BuildSummary(
                buffer.readUUID(), readString(buffer), readString(buffer),
                nonNegative(buffer.readVarInt(), "completedPlacements"),
                nonNegative(buffer.readVarInt(), "totalPlacements"), readString(buffer));
    }

    private static void writeWorkOrder(RegistryFriendlyByteBuf buffer, WorkOrderSummary value) {
        buffer.writeUUID(value.id());
        writeString(buffer, value.type());
        writeString(buffer, value.state());
        writeOptionalUuid(buffer, value.assignedRecruitId());
        buffer.writeVarInt(value.completedQuantity());
        buffer.writeVarInt(value.quantity());
        writeString(buffer, value.blockedReason());
    }

    private static WorkOrderSummary readWorkOrder(RegistryFriendlyByteBuf buffer) {
        return new WorkOrderSummary(
                buffer.readUUID(), readString(buffer), readString(buffer), readOptionalUuid(buffer),
                nonNegative(buffer.readVarInt(), "completedQuantity"),
                nonNegative(buffer.readVarInt(), "quantity"), readString(buffer));
    }

    private static void writeWorker(RegistryFriendlyByteBuf buffer, WorkerSummary value) {
        buffer.writeUUID(value.entityId());
        writeString(buffer, value.displayName());
        writeString(buffer, value.profession());
        writeString(buffer, value.command());
        writeString(buffer, value.phase());
        writeString(buffer, value.reasonCode());
        writeOptionalPosition(buffer, value.worksite());
        buffer.writeVarInt(value.workRadius());
        writeOptionalPosition(buffer, value.storage());
        writeOptionalPosition(buffer, value.activeTarget());
        writeOptionalUuid(buffer, value.workOrderId());
        buffer.writeVarInt(value.carriedItemCount());
        buffer.writeVarInt(value.storageItemCount());
        buffer.writeVarInt(value.distanceBlocks());
    }

    private static WorkerSummary readWorker(RegistryFriendlyByteBuf buffer) {
        return new WorkerSummary(
                buffer.readUUID(), readString(buffer), readString(buffer), readString(buffer),
                readString(buffer), readString(buffer), readOptionalPosition(buffer),
                nonNegative(buffer.readVarInt(), "workerRadius"), readOptionalPosition(buffer),
                readOptionalPosition(buffer), readOptionalUuid(buffer),
                nonNegative(buffer.readVarInt(), "workerCarriedItems"),
                nonNegative(buffer.readVarInt(), "workerStorageItems"),
                nonNegative(buffer.readVarInt(), "workerDistance"));
    }

    private static void writeOptionalPosition(
            RegistryFriendlyByteBuf buffer, Optional<PositionSummary> value
    ) {
        Optional<PositionSummary> normalized = value == null ? Optional.empty() : value;
        buffer.writeBoolean(normalized.isPresent());
        normalized.ifPresent(position -> {
            writeString(buffer, position.dimensionId());
            buffer.writeVarInt(position.x());
            buffer.writeVarInt(position.y());
            buffer.writeVarInt(position.z());
        });
    }

    private static Optional<PositionSummary> readOptionalPosition(RegistryFriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return Optional.empty();
        }
        return Optional.of(new PositionSummary(
                readString(buffer), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));
    }

    private static void writeQuest(RegistryFriendlyByteBuf buffer, QuestSummary value) {
        writeString(buffer, value.questId());
        buffer.writeBoolean(value.complete());
        buffer.writeVarInt(value.rewardCredits());
        writeList(buffer, value.unlocks(), MAX_OBJECTIVES,
                (target, unlock) -> writeString(target, unlock));
        writeList(buffer, value.objectives(), MAX_OBJECTIVES,
                CommandCenterDashboardCodec::writeObjective);
    }

    private static QuestSummary readQuest(RegistryFriendlyByteBuf buffer) {
        return new QuestSummary(
                readString(buffer), buffer.readBoolean(),
                nonNegative(buffer.readVarInt(), "rewardCredits"),
                readList(buffer, MAX_OBJECTIVES, CommandCenterDashboardCodec::readString),
                readList(buffer, MAX_OBJECTIVES, CommandCenterDashboardCodec::readObjective));
    }

    private static void writeObjective(RegistryFriendlyByteBuf buffer, ObjectiveSummary value) {
        writeString(buffer, value.objectiveId());
        buffer.writeBoolean(value.complete());
    }

    private static ObjectiveSummary readObjective(RegistryFriendlyByteBuf buffer) {
        return new ObjectiveSummary(readString(buffer), buffer.readBoolean());
    }

    private static void writeMember(RegistryFriendlyByteBuf buffer, MemberSummary value) {
        buffer.writeUUID(value.playerId());
        writeString(buffer, value.role());
    }

    private static void writeNearbyPlayer(
            RegistryFriendlyByteBuf buffer, NearbyPlayerSummary value
    ) {
        buffer.writeUUID(value.playerId());
        writeString(buffer, value.displayName());
        buffer.writeVarInt(value.distanceBlocks());
    }

    private static NearbyPlayerSummary readNearbyPlayer(RegistryFriendlyByteBuf buffer) {
        return new NearbyPlayerSummary(
                buffer.readUUID(), readString(buffer),
                nonNegative(buffer.readVarInt(), "nearbyPlayerDistance"));
    }

    private static MemberSummary readMember(RegistryFriendlyByteBuf buffer) {
        return new MemberSummary(buffer.readUUID(), readString(buffer));
    }

    private static void writeForeignKingdom(
            RegistryFriendlyByteBuf buffer, ForeignKingdomSummary value
    ) {
        buffer.writeUUID(value.kingdomId());
        buffer.writeUUID(value.ownerId());
        writeString(buffer, value.factionId());
    }

    private static ForeignKingdomSummary readForeignKingdom(RegistryFriendlyByteBuf buffer) {
        return new ForeignKingdomSummary(buffer.readUUID(), buffer.readUUID(), readString(buffer));
    }

    private static void writeInvite(RegistryFriendlyByteBuf buffer, InviteSummary value) {
        buffer.writeUUID(value.inviteId());
        buffer.writeUUID(value.kingdomId());
        buffer.writeUUID(value.inviterId());
        buffer.writeUUID(value.targetPlayerId());
        writeString(buffer, value.offeredRole());
        buffer.writeVarLong(value.expiresGameTime());
    }

    private static InviteSummary readInvite(RegistryFriendlyByteBuf buffer) {
        return new InviteSummary(
                buffer.readUUID(), buffer.readUUID(), buffer.readUUID(), buffer.readUUID(),
                readString(buffer), nonNegative(buffer.readVarLong(), "inviteExpiry"));
    }

    private static void writeDiplomacyProposal(
            RegistryFriendlyByteBuf buffer, DiplomacyProposalSummary value
    ) {
        buffer.writeUUID(value.proposalId());
        buffer.writeUUID(value.proposerKingdomId());
        buffer.writeUUID(value.targetKingdomId());
        writeString(buffer, value.relation());
        buffer.writeVarLong(value.expiresGameTime());
    }

    private static DiplomacyProposalSummary readDiplomacyProposal(RegistryFriendlyByteBuf buffer) {
        return new DiplomacyProposalSummary(
                buffer.readUUID(), buffer.readUUID(), buffer.readUUID(), readString(buffer),
                nonNegative(buffer.readVarLong(), "proposalExpiry"));
    }

    private static void writeString(RegistryFriendlyByteBuf buffer, String value) {
        buffer.writeUtf(value == null ? "" : value, MAX_STRING);
    }

    private static String readString(RegistryFriendlyByteBuf buffer) {
        return buffer.readUtf(MAX_STRING);
    }

    private static void writeOptionalUuid(RegistryFriendlyByteBuf buffer, Optional<UUID> value) {
        Optional<UUID> normalized = value == null ? Optional.empty() : value;
        buffer.writeBoolean(normalized.isPresent());
        normalized.ifPresent(buffer::writeUUID);
    }

    private static Optional<UUID> readOptionalUuid(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Optional.of(buffer.readUUID()) : Optional.empty();
    }

    private static <T> void writeList(
            RegistryFriendlyByteBuf buffer,
            List<T> values,
            int limit,
            BiConsumer<RegistryFriendlyByteBuf, T> writer
    ) {
        int size = Math.min(values.size(), limit);
        buffer.writeVarInt(size);
        for (int index = 0; index < size; index++) {
            writer.accept(buffer, values.get(index));
        }
    }

    private static <T> List<T> readList(
            RegistryFriendlyByteBuf buffer,
            int limit,
            Function<RegistryFriendlyByteBuf, T> reader
    ) {
        int size = buffer.readVarInt();
        if (size < 0 || size > limit) {
            throw new IllegalArgumentException("dashboard list size exceeds " + limit);
        }
        ArrayList<T> values = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            values.add(reader.apply(buffer));
        }
        return List.copyOf(values);
    }

    private static int nonNegative(int value, String label) {
        if (value < 0) {
            throw new IllegalArgumentException(label + " cannot be negative");
        }
        return value;
    }

    private static long nonNegative(long value, String label) {
        if (value < 0L) {
            throw new IllegalArgumentException(label + " cannot be negative");
        }
        return value;
    }
}
