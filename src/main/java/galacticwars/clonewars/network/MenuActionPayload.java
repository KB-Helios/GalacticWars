package galacticwars.clonewars.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Replay-addressable request for server-owned menu actions. */
public record MenuActionPayload(UUID replayId, int containerId, int actionId) {
    public static final StreamCodec<RegistryFriendlyByteBuf, MenuActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.replayId());
                buffer.writeVarInt(payload.containerId());
                buffer.writeVarInt(payload.actionId());
            }, buffer -> new MenuActionPayload(
                    buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt()));
}
