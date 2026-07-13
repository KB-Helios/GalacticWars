package galacticwars.clonewars.network;

import galacticwars.clonewars.GalacticWars;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Versioned, replay-addressable request for server-owned menu actions. */
public record MenuActionPayload(UUID replayId, int containerId, int actionId)
        implements CustomPacketPayload {
    private static final int VERSION = 1;
    public static final Type<MenuActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "menu_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MenuActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(VERSION);
                buffer.writeUUID(payload.replayId());
                buffer.writeVarInt(payload.containerId());
                buffer.writeVarInt(payload.actionId());
            }, MenuActionPayload::decode);

    private static MenuActionPayload decode(RegistryFriendlyByteBuf buffer) {
        if (buffer.readVarInt() != VERSION) throw new IllegalArgumentException("Unsupported menu payload");
        return new MenuActionPayload(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt());
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
