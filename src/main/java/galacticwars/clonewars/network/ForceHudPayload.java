package galacticwars.clonewars.network;

import galacticwars.clonewars.GalacticWars;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Bounded authoritative Force HUD snapshot; never contains the complete datapack catalog. */
public record ForceHudPayload(
        int energy,
        int rank,
        int masteryExperience,
        int unspentPoints,
        String tradition,
        List<String> abilities,
        int cooldown1,
        int cooldown2,
        int cooldown3,
        int activeSlot,
        int activeMode,
        int activeTicks,
        int targetValidityMask,
        String failureReason
) implements CustomPacketPayload {
    private static final int MAX_ID_LENGTH = 64;

    public ForceHudPayload(int energy, int cooldown1, int cooldown2, int cooldown3) {
        this(energy, 0, 0, 0, "", List.of(), cooldown1, cooldown2, cooldown3,
                -1, 0, 0, 0, "");
    }

    public ForceHudPayload {
        abilities = List.copyOf(abilities);
        if (energy < 0 || energy > 100 || rank < 0 || rank > 10
                || masteryExperience < 0 || masteryExperience > 320
                || unspentPoints < 0 || unspentPoints > 9 || abilities.size() > 3
                || tradition.length() > MAX_ID_LENGTH || failureReason.length() > MAX_ID_LENGTH
                || abilities.stream().anyMatch(value -> value.length() > MAX_ID_LENGTH)
                || cooldown1 < 0 || cooldown2 < 0 || cooldown3 < 0
                || activeSlot < -1 || activeSlot > 2 || activeMode < 0 || activeMode > 2
                || activeTicks < 0 || activeTicks > 100
                || targetValidityMask < 0 || targetValidityMask > 7) {
            throw new IllegalArgumentException("Invalid bounded Force HUD snapshot");
        }
    }

    public static final Type<ForceHudPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "force_hud"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ForceHudPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.energy());
                buffer.writeVarInt(payload.rank());
                buffer.writeVarInt(payload.masteryExperience());
                buffer.writeVarInt(payload.unspentPoints());
                buffer.writeUtf(payload.tradition(), MAX_ID_LENGTH);
                buffer.writeVarInt(payload.abilities().size());
                payload.abilities().forEach(value -> buffer.writeUtf(value, MAX_ID_LENGTH));
                buffer.writeVarInt(payload.cooldown1());
                buffer.writeVarInt(payload.cooldown2());
                buffer.writeVarInt(payload.cooldown3());
                buffer.writeVarInt(payload.activeSlot());
                buffer.writeVarInt(payload.activeMode());
                buffer.writeVarInt(payload.activeTicks());
                buffer.writeVarInt(payload.targetValidityMask());
                buffer.writeUtf(payload.failureReason(), MAX_ID_LENGTH);
            },
            buffer -> {
                int energy = buffer.readVarInt();
                int rank = buffer.readVarInt();
                int experience = buffer.readVarInt();
                int points = buffer.readVarInt();
                String tradition = buffer.readUtf(MAX_ID_LENGTH);
                int count = Math.max(0, Math.min(3, buffer.readVarInt()));
                ArrayList<String> abilities = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    abilities.add(buffer.readUtf(MAX_ID_LENGTH));
                }
                return new ForceHudPayload(
                        energy, rank, experience, points, tradition, abilities,
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readUtf(MAX_ID_LENGTH));
            });

    public boolean targetValid(int slot) {
        return slot >= 0 && slot < 3 && (targetValidityMask & (1 << slot)) != 0;
    }

    @Override
    public Type<ForceHudPayload> type() {
        return TYPE;
    }
}
