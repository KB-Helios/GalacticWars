package galacticwars.clonewars.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record VehicleInputPayload(
        UUID replayId, int entityId, float forward, float strafe,
        boolean ascend, boolean descend, boolean fire
) {
    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleInputPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.replayId());
                buffer.writeVarInt(payload.entityId());
                buffer.writeFloat(payload.forward());
                buffer.writeFloat(payload.strafe());
                buffer.writeBoolean(payload.ascend());
                buffer.writeBoolean(payload.descend());
                buffer.writeBoolean(payload.fire());
            }, buffer -> new VehicleInputPayload(buffer.readUUID(), buffer.readVarInt(),
                buffer.readFloat(), buffer.readFloat(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean()));
}
