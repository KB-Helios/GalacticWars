package galacticwars.clonewars.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.data.SavedDataSchemaPolicy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class ForceSavedData extends SavedData {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Codec<PlayerForceState> PLAYER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("player_id").forGetter(PlayerForceState::playerId),
            Codec.STRING.optionalFieldOf("path", "").forGetter(PlayerForceState::path),
            Codec.INT.optionalFieldOf("energy", ForceRuntimeState.MAX_ENERGY).forGetter(PlayerForceState::energy),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("cooldowns", Map.of())
                    .forGetter(PlayerForceState::cooldowns),
            UUIDUtil.CODEC.listOf().optionalFieldOf("processed_activations", List.of())
                    .forGetter(PlayerForceState::processedActivations)
    ).apply(instance, PlayerForceState::new));
    public static final Codec<ForceSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION)
                    .forGetter(data -> CURRENT_SCHEMA_VERSION),
            PLAYER_CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(ForceSavedData::serialized)
    ).apply(instance, ForceSavedData::new));
    public static final SavedDataType<ForceSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "force_state"),
            ForceSavedData::new, CODEC);

    private final Map<UUID, ForceRuntimeState> states = new LinkedHashMap<>();

    public ForceSavedData() {
    }

    private ForceSavedData(int schemaVersion, List<PlayerForceState> players) {
        SavedDataSchemaPolicy.migrate(schemaVersion, CURRENT_SCHEMA_VERSION, "force");
        for (PlayerForceState player : players) {
            states.put(player.playerId(), new ForceRuntimeState(player.path(),
                    Math.max(0, Math.min(ForceRuntimeState.MAX_ENERGY, player.energy())),
                    player.cooldowns(), java.util.Set.copyOf(player.processedActivations())));
        }
    }

    public static ForceSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public ForceRuntimeState state(UUID playerId) {
        return states.getOrDefault(playerId, ForceRuntimeState.full());
    }

    public ForceAbilityRuntimeService.ActivationDecision activate(
            ProgressionState progression, UUID activationId, String abilityId,
            long gameTime, boolean targetsPlayer, boolean allowForcePvp
    ) {
        ForceRuntimeState before = state(progression.playerId());
        ForceAbilityRuntimeService.ActivationDecision decision = ForceAbilityRuntimeService.activate(
                progression, before, activationId, abilityId, gameTime, targetsPlayer, allowForcePvp);
        if (decision.accepted() && decision.state() != before) {
            states.put(progression.playerId(), decision.state());
            setDirty();
        }
        return decision;
    }

    public void regenerate(UUID playerId, int amount) {
        ForceRuntimeState before = state(playerId);
        ForceRuntimeState after = before.regenerate(amount);
        if (!after.equals(before)) {
            states.put(playerId, after);
            setDirty();
        }
    }

    private List<PlayerForceState> serialized() {
        return states.entrySet().stream().map(entry -> new PlayerForceState(
                entry.getKey(), entry.getValue().path(), entry.getValue().energy(),
                entry.getValue().cooldownEnds(), entry.getValue().processedActivationIds().stream().toList()))
                .toList();
    }

    private record PlayerForceState(
            UUID playerId, String path, int energy,
            Map<String, Long> cooldowns, List<UUID> processedActivations
    ) {
        private PlayerForceState {
            cooldowns = Map.copyOf(cooldowns);
            processedActivations = List.copyOf(processedActivations);
        }
    }
}
