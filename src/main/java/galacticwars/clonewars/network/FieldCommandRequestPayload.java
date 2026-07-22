package galacticwars.clonewars.network;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.army.ArmyPatrolPlan;
import galacticwars.clonewars.army.FieldCommandAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Bounded, replay-addressable C2S request from the field command screen. */
public record FieldCommandRequestPayload(
        UUID replayId,
        FieldCommandAction action,
        List<UUID> groupIds,
        String patrolRouteName,
        int patrolWaypointIndex,
        int patrolWaypointWaitTicks,
        String optionId
) implements CustomPacketPayload {
    public static final int MAX_GROUPS = 8;
    public static final int MAX_PATROL_ROUTE_NAME_BYTES = 128;
    public static final int MAX_PATROL_WAYPOINTS = 32;
    public static final int MAX_OPTION_ID_BYTES = 64;
    public static final Type<FieldCommandRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "field_command_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FieldCommandRequestPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeUUID(payload.replayId());
                        buffer.writeVarInt(payload.action().wireId());
                        buffer.writeVarInt(payload.groupIds().size());
                        payload.groupIds().forEach(buffer::writeUUID);
                        buffer.writeUtf(payload.patrolRouteName(), MAX_PATROL_ROUTE_NAME_BYTES);
                        buffer.writeVarInt(payload.patrolWaypointIndex());
                        buffer.writeVarInt(payload.patrolWaypointWaitTicks());
                        buffer.writeUtf(payload.optionId(), MAX_OPTION_ID_BYTES);
                    },
                    buffer -> {
                        UUID replayId = buffer.readUUID();
                        FieldCommandAction action = FieldCommandAction.fromWireId(buffer.readVarInt());
                        int size = buffer.readVarInt();
                        if (size < 0 || size > MAX_GROUPS) {
                            throw new IllegalArgumentException("field command group count exceeds " + MAX_GROUPS);
                        }
                        ArrayList<UUID> groupIds = new ArrayList<>(size);
                        for (int index = 0; index < size; index++) {
                            groupIds.add(buffer.readUUID());
                        }
                        return new FieldCommandRequestPayload(
                                replayId,
                                action,
                                groupIds,
                                buffer.readUtf(MAX_PATROL_ROUTE_NAME_BYTES),
                                buffer.readVarInt(),
                                buffer.readVarInt(),
                                buffer.readUtf(MAX_OPTION_ID_BYTES));
                    });

    /** Compatibility constructor for non-patrol commands and existing call sites. */
    public FieldCommandRequestPayload(UUID replayId, FieldCommandAction action, List<UUID> groupIds) {
        this(replayId, action, groupIds, "", 0, 0, "");
    }

    /** Compatibility constructor for the original patrol-aware request shape. */
    public FieldCommandRequestPayload(
            UUID replayId,
            FieldCommandAction action,
            List<UUID> groupIds,
            String patrolRouteName,
            int patrolWaypointIndex,
            int patrolWaypointWaitTicks
    ) {
        this(replayId, action, groupIds, patrolRouteName, patrolWaypointIndex,
                patrolWaypointWaitTicks, "");
    }

    public FieldCommandRequestPayload {
        Objects.requireNonNull(replayId, "replayId");
        Objects.requireNonNull(action, "action");
        groupIds = List.copyOf(Objects.requireNonNull(groupIds, "groupIds"));
        if (groupIds.size() > MAX_GROUPS || groupIds.stream().anyMatch(Objects::isNull)
                || new HashSet<>(groupIds).size() != groupIds.size()) {
            throw new IllegalArgumentException("field command selection must contain up to "
                    + MAX_GROUPS + " unique squads");
        }
        if (action == FieldCommandAction.REFRESH ? !groupIds.isEmpty() : groupIds.isEmpty()) {
            throw new IllegalArgumentException("field command action has an invalid squad selection");
        }
        patrolRouteName = Objects.requireNonNull(patrolRouteName, "patrolRouteName").trim();
        if (patrolRouteName.length() > ArmyPatrolPlan.MAX_NAME_LENGTH
                || patrolRouteName.chars().anyMatch(Character::isISOControl)
                || patrolRouteName.getBytes(StandardCharsets.UTF_8).length > MAX_PATROL_ROUTE_NAME_BYTES) {
            throw new IllegalArgumentException("field command patrol route name is not safe bounded text");
        }
        if (patrolWaypointIndex < 0 || patrolWaypointIndex >= MAX_PATROL_WAYPOINTS) {
            throw new IllegalArgumentException("field command patrol waypoint index is outside the supported range");
        }
        if (patrolWaypointWaitTicks < 0
                || patrolWaypointWaitTicks > ArmyPatrolPlan.MAX_FIELD_COMMAND_WAIT_TICKS) {
            throw new IllegalArgumentException("field command patrol wait is outside the supported range");
        }
        optionId = Objects.requireNonNull(optionId, "optionId").trim().toLowerCase(java.util.Locale.ROOT);
        if (optionId.chars().anyMatch(character -> !Character.isLetterOrDigit(character) && character != '_')
                || optionId.getBytes(StandardCharsets.UTF_8).length > MAX_OPTION_ID_BYTES) {
            throw new IllegalArgumentException("field command option is not a safe bounded identifier");
        }
        boolean optionRequired = action == FieldCommandAction.SET_FORMATION
                || action == FieldCommandAction.SET_ENGAGEMENT
                || action == FieldCommandAction.SET_TARGET_PRIORITY
                || action == FieldCommandAction.SET_RANGED_FIRE;
        if (optionRequired == optionId.isEmpty()) {
            throw new IllegalArgumentException("field command action has an invalid option identifier");
        }
    }

    @Override
    public Type<FieldCommandRequestPayload> type() {
        return TYPE;
    }
}
