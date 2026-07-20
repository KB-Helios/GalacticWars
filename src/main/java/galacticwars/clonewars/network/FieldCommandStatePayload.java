package galacticwars.clonewars.network;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.army.ArmyGroupRecord;
import galacticwars.clonewars.army.FieldCommandResult;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Bounded S2C projection for the player-owned field command panel. */
public record FieldCommandStatePayload(
        UUID replayId,
        FieldCommandResult result,
        List<Squad> squads,
        boolean markedBlockAvailable,
        boolean markedEntityAvailable
) implements CustomPacketPayload {
    public static final int MAX_SQUADS = 64;
    /** A legal 48-code-unit squad name can require 192 UTF-8 bytes. */
    public static final int MAX_TEXT_BYTES = 192;
    public static final Type<FieldCommandStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "field_command_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FieldCommandStatePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeUUID(payload.replayId());
                        buffer.writeVarInt(payload.result().wireId());
                        buffer.writeBoolean(payload.markedBlockAvailable());
                        buffer.writeBoolean(payload.markedEntityAvailable());
                        buffer.writeVarInt(payload.squads().size());
                        payload.squads().forEach(squad -> squad.write(buffer));
                    },
                    buffer -> {
                        UUID replayId = buffer.readUUID();
                        FieldCommandResult result = FieldCommandResult.fromWireId(buffer.readVarInt());
                        boolean markedBlockAvailable = buffer.readBoolean();
                        boolean markedEntityAvailable = buffer.readBoolean();
                        int size = buffer.readVarInt();
                        if (size < 0 || size > MAX_SQUADS) {
                            throw new IllegalArgumentException("field command squad count exceeds " + MAX_SQUADS);
                        }
                        ArrayList<Squad> squads = new ArrayList<>(size);
                        for (int index = 0; index < size; index++) {
                            squads.add(Squad.read(buffer));
                        }
                        return new FieldCommandStatePayload(
                                replayId, result, squads, markedBlockAvailable, markedEntityAvailable);
                    });

    public FieldCommandStatePayload {
        Objects.requireNonNull(replayId, "replayId");
        Objects.requireNonNull(result, "result");
        squads = List.copyOf(Objects.requireNonNull(squads, "squads"));
        if (squads.size() > MAX_SQUADS) {
            throw new IllegalArgumentException("field command squad count exceeds " + MAX_SQUADS);
        }
        if (markedBlockAvailable && markedEntityAvailable) {
            throw new IllegalArgumentException("a command marker cannot expose block and entity targets together");
        }
    }

    public static FieldCommandStatePayload awaitingServer() {
        return new FieldCommandStatePayload(
                new UUID(0L, 0L), FieldCommandResult.KINGDOM_UNAVAILABLE, List.of(), false, false);
    }

    @Override
    public Type<FieldCommandStatePayload> type() {
        return TYPE;
    }

    public record Squad(
            UUID id,
            String name,
            int unitCount,
            String order,
            String formation,
            String lifecycle,
            Optional<Patrol> patrol
    ) {
        /** Compatibility constructor for clients/tests that only need the non-patrol roster projection. */
        public Squad(UUID id, String name, int unitCount, String order, String formation, String lifecycle) {
            this(id, name, unitCount, order, formation, lifecycle, Optional.empty());
        }

        public Squad {
            Objects.requireNonNull(id, "id");
            name = boundedText(name, "name");
            order = boundedText(order, "order");
            formation = boundedText(formation, "formation");
            lifecycle = boundedText(lifecycle, "lifecycle");
            patrol = patrol == null ? Optional.empty() : patrol;
            if (unitCount < 0 || unitCount > 4096) {
                throw new IllegalArgumentException("field command squad unit count is outside the supported range");
            }
        }

        public static Squad from(ArmyGroupRecord group) {
            return new Squad(
                    group.id(),
                    group.name(),
                    group.memberIds().size() + (group.commanderId().isPresent() ? 1 : 0),
                    group.order().type().name(),
                    group.order().formation().name(),
                    group.simulation().lifecycleState().name(),
                    group.effectivePatrolPlan().map(plan -> new Patrol(plan.name(), plan.waypoints().size())));
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeUUID(id);
            buffer.writeUtf(name, MAX_TEXT_BYTES);
            buffer.writeVarInt(unitCount);
            buffer.writeUtf(order, MAX_TEXT_BYTES);
            buffer.writeUtf(formation, MAX_TEXT_BYTES);
            buffer.writeUtf(lifecycle, MAX_TEXT_BYTES);
            buffer.writeBoolean(patrol.isPresent());
            patrol.ifPresent(value -> value.write(buffer));
        }

        private static Squad read(RegistryFriendlyByteBuf buffer) {
            return new Squad(
                    buffer.readUUID(),
                    buffer.readUtf(MAX_TEXT_BYTES),
                    buffer.readVarInt(),
                    buffer.readUtf(MAX_TEXT_BYTES),
                    buffer.readUtf(MAX_TEXT_BYTES),
                    buffer.readUtf(MAX_TEXT_BYTES),
                    buffer.readBoolean() ? Optional.of(Patrol.read(buffer)) : Optional.empty());
        }

        private static String boundedText(String value, String label) {
            String normalized = Objects.requireNonNull(value, label).trim();
            if (normalized.isEmpty() || normalized.chars().anyMatch(Character::isISOControl)
                    || normalized.getBytes(StandardCharsets.UTF_8).length > MAX_TEXT_BYTES) {
                throw new IllegalArgumentException(label + " is not safe bounded text");
            }
            return normalized;
        }

        /** Small server-owned projection used to constrain the local route editor. */
        public record Patrol(String name, int waypointCount) {
            public Patrol {
                name = boundedText(name, "patrol name");
                if (waypointCount < 2 || waypointCount > 32) {
                    throw new IllegalArgumentException("field command patrol waypoint count is outside the supported range");
                }
            }

            private void write(RegistryFriendlyByteBuf buffer) {
                buffer.writeUtf(name, MAX_TEXT_BYTES);
                buffer.writeVarInt(waypointCount);
            }

            private static Patrol read(RegistryFriendlyByteBuf buffer) {
                return new Patrol(buffer.readUtf(MAX_TEXT_BYTES), buffer.readVarInt());
            }
        }
    }
}
