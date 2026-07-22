package galacticwars.clonewars.network;

import galacticwars.clonewars.GalacticWars;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Bounded shrine view containing only the player's 13-node tradition. */
public record ForceProgressionPayload(
        BlockPos shrinePos,
        String tradition,
        int rank,
        int masteryExperience,
        int unspentPoints,
        List<String> learnedNodes,
        List<String> equippedAbilities,
        List<NodeEntry> nodes,
        int respecCost
) implements CustomPacketPayload {
    private static final int MAX_ID = 64;

    public ForceProgressionPayload {
        learnedNodes = List.copyOf(learnedNodes);
        equippedAbilities = List.copyOf(equippedAbilities);
        nodes = List.copyOf(nodes);
        if (tradition.length() > MAX_ID || rank < 1 || rank > 10
                || masteryExperience < 0 || masteryExperience > 320
                || unspentPoints < 0 || unspentPoints > 9
                || learnedNodes.size() > 13 || equippedAbilities.size() > 3
                || nodes.size() != 13 || respecCost < 0 || respecCost > 10_000) {
            throw new IllegalArgumentException("Invalid bounded Force progression payload");
        }
    }

    public static final Type<ForceProgressionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "force_progression"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ForceProgressionPayload> STREAM_CODEC =
            StreamCodec.of(ForceProgressionPayload::encode, ForceProgressionPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, ForceProgressionPayload payload) {
        buffer.writeLong(payload.shrinePos().asLong());
        buffer.writeUtf(payload.tradition(), MAX_ID);
        buffer.writeVarInt(payload.rank());
        buffer.writeVarInt(payload.masteryExperience());
        buffer.writeVarInt(payload.unspentPoints());
        writeStrings(buffer, payload.learnedNodes(), 13);
        writeStrings(buffer, payload.equippedAbilities(), 3);
        buffer.writeVarInt(payload.nodes().size());
        payload.nodes().forEach(node -> {
            buffer.writeUtf(node.id(), MAX_ID);
            buffer.writeUtf(node.branch(), MAX_ID);
            buffer.writeVarInt(node.tier());
            buffer.writeVarInt(node.pointCost());
            buffer.writeUtf(node.abilityId(), MAX_ID);
            buffer.writeBoolean(node.passive());
            writeStrings(buffer, node.prerequisites(), 4);
        });
        buffer.writeVarInt(payload.respecCost());
    }

    private static ForceProgressionPayload decode(RegistryFriendlyByteBuf buffer) {
        BlockPos position = BlockPos.of(buffer.readLong());
        String tradition = buffer.readUtf(MAX_ID);
        int rank = buffer.readVarInt();
        int experience = buffer.readVarInt();
        int points = buffer.readVarInt();
        List<String> learned = readStrings(buffer, 13);
        List<String> equipped = readStrings(buffer, 3);
        int count = Math.max(0, Math.min(13, buffer.readVarInt()));
        ArrayList<NodeEntry> nodes = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            nodes.add(new NodeEntry(
                    buffer.readUtf(MAX_ID), buffer.readUtf(MAX_ID), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readUtf(MAX_ID), buffer.readBoolean(),
                    readStrings(buffer, 4)));
        }
        return new ForceProgressionPayload(position, tradition, rank, experience, points,
                learned, equipped, nodes, buffer.readVarInt());
    }

    private static void writeStrings(
            RegistryFriendlyByteBuf buffer, List<String> values, int maximum
    ) {
        int count = Math.min(maximum, values.size());
        buffer.writeVarInt(count);
        for (int index = 0; index < count; index++) buffer.writeUtf(values.get(index), MAX_ID);
    }

    private static List<String> readStrings(RegistryFriendlyByteBuf buffer, int maximum) {
        int count = Math.max(0, Math.min(maximum, buffer.readVarInt()));
        ArrayList<String> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) result.add(buffer.readUtf(MAX_ID));
        return List.copyOf(result);
    }

    @Override
    public Type<ForceProgressionPayload> type() {
        return TYPE;
    }

    public record NodeEntry(
            String id,
            String branch,
            int tier,
            int pointCost,
            String abilityId,
            boolean passive,
            List<String> prerequisites
    ) {
        public NodeEntry {
            prerequisites = List.copyOf(prerequisites);
            if (id.length() > MAX_ID || branch.length() > MAX_ID || abilityId.length() > MAX_ID
                    || tier < 0 || tier > 5 || pointCost < 0 || pointCost > 1
                    || prerequisites.size() > 4) {
                throw new IllegalArgumentException("Invalid Force node payload");
            }
        }
    }
}
