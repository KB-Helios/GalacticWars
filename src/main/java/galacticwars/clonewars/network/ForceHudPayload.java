package galacticwars.clonewars.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record ForceHudPayload(int energy, int cooldown1, int cooldown2, int cooldown3) {
    public static final StreamCodec<RegistryFriendlyByteBuf, ForceHudPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.energy());
                buffer.writeVarInt(payload.cooldown1());
                buffer.writeVarInt(payload.cooldown2());
                buffer.writeVarInt(payload.cooldown3());
            }, buffer -> new ForceHudPayload(buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt()));
}
