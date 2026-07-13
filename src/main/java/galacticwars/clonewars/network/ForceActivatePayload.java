package galacticwars.clonewars.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record ForceActivatePayload(UUID activationId, int slot) {
    public static final StreamCodec<RegistryFriendlyByteBuf, ForceActivatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.activationId());
                buffer.writeVarInt(payload.slot());
            },
            buffer -> new ForceActivatePayload(buffer.readUUID(), buffer.readVarInt()));
}
