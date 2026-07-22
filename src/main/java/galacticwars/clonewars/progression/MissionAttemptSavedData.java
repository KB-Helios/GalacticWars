package galacticwars.clonewars.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.data.SavedDataSchemaPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Persistent server-owned mission phase, retry, target, and hold progress. */
public final class MissionAttemptSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;
    private static final int MAX_PLAYERS = 1_024;
    private static final Set<String> PHASES = Set.of(
            "objectives", "ready", "hold", "cooldown", "complete");

    private static final Codec<MissionAttempt> ATTEMPT_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUIDUtil.CODEC.fieldOf("player_id").forGetter(MissionAttempt::playerId),
                    Codec.STRING.fieldOf("mission_id").forGetter(MissionAttempt::missionId),
                    Codec.INT.fieldOf("attempt").forGetter(MissionAttempt::attempt),
                    Codec.STRING.fieldOf("phase").forGetter(MissionAttempt::phase),
                    Codec.STRING.fieldOf("target_dimension").forGetter(MissionAttempt::targetDimension),
                    BlockPos.CODEC.fieldOf("target").forGetter(MissionAttempt::target),
                    Codec.LONG.fieldOf("started_at").forGetter(MissionAttempt::startedAt),
                    Codec.LONG.optionalFieldOf("retry_at", 0L).forGetter(MissionAttempt::retryAt),
                    Codec.INT.optionalFieldOf("hold_ticks", 0).forGetter(MissionAttempt::holdTicks),
                    Codec.INT.optionalFieldOf("absent_ticks", 0).forGetter(MissionAttempt::absentTicks),
                    Codec.BOOL.optionalFieldOf("wave_spawned", false).forGetter(MissionAttempt::waveSpawned),
                    Codec.STRING.optionalFieldOf("failure_reason", "").forGetter(MissionAttempt::failureReason),
                    Codec.LONG.optionalFieldOf("last_feedback_at", 0L).forGetter(MissionAttempt::lastFeedbackAt)
            ).apply(instance, MissionAttempt::new));

    public static final Codec<MissionAttemptSavedData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("schema_version", SCHEMA_VERSION)
                            .forGetter(data -> SCHEMA_VERSION),
                    ATTEMPT_CODEC.listOf().optionalFieldOf("attempts", List.of())
                            .forGetter(MissionAttemptSavedData::serialized)
            ).apply(instance, MissionAttemptSavedData::new));

    public static final SavedDataType<MissionAttemptSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "mission_attempts"),
            MissionAttemptSavedData::new, CODEC, null);

    private final Map<UUID, MissionAttempt> attempts = new LinkedHashMap<>();

    public MissionAttemptSavedData() {
    }

    private MissionAttemptSavedData(int schemaVersion, List<MissionAttempt> loaded) {
        SavedDataSchemaPolicy.migrate(schemaVersion, SCHEMA_VERSION, "mission_attempts");
        loaded.stream().limit(MAX_PLAYERS)
                .forEach(attempt -> attempts.putIfAbsent(attempt.playerId(), attempt));
    }

    public static MissionAttemptSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<MissionAttempt> forPlayer(UUID playerId) {
        return Optional.ofNullable(attempts.get(playerId));
    }

    public void put(MissionAttempt attempt) {
        attempts.put(attempt.playerId(), attempt);
        setDirty();
    }

    public void remove(UUID playerId) {
        if (attempts.remove(playerId) != null) {
            setDirty();
        }
    }

    private List<MissionAttempt> serialized() {
        return List.copyOf(attempts.values());
    }

    public record MissionAttempt(
            UUID playerId,
            String missionId,
            int attempt,
            String phase,
            String targetDimension,
            BlockPos target,
            long startedAt,
            long retryAt,
            int holdTicks,
            int absentTicks,
            boolean waveSpawned,
            String failureReason,
            long lastFeedbackAt
    ) {
        public MissionAttempt {
            if (playerId == null || missionId == null || missionId.isBlank()
                    || missionId.length() > 128 || attempt < 1 || attempt > 1_024
                    || !PHASES.contains(phase)
                    || targetDimension == null || targetDimension.isBlank()
                    || targetDimension.length() > 256 || target == null
                    || startedAt < 0L || retryAt < 0L || holdTicks < 0 || holdTicks > 72_000
                    || absentTicks < 0 || absentTicks > 72_000 || lastFeedbackAt < 0L
                    || failureReason == null || failureReason.length() > 64) {
                throw new IllegalArgumentException("Invalid persisted mission attempt");
            }
        }

        public MissionAttempt phase(String nextPhase) {
            return new MissionAttempt(playerId, missionId, attempt, nextPhase, targetDimension,
                    target, startedAt, retryAt, holdTicks, absentTicks, waveSpawned,
                    failureReason, lastFeedbackAt);
        }

        public MissionAttempt holding(boolean spawned) {
            return new MissionAttempt(playerId, missionId, attempt, "hold", targetDimension,
                    target, startedAt, retryAt, holdTicks, 0, spawned,
                    "", lastFeedbackAt);
        }

        public MissionAttempt advanceHold(int ticks) {
            return new MissionAttempt(playerId, missionId, attempt, "hold", targetDimension,
                    target, startedAt, retryAt, Math.min(72_000, holdTicks + ticks), 0,
                    waveSpawned, "", lastFeedbackAt);
        }

        public MissionAttempt absent(int ticks) {
            return new MissionAttempt(playerId, missionId, attempt, phase, targetDimension,
                    target, startedAt, retryAt, holdTicks,
                    Math.min(72_000, absentTicks + ticks), waveSpawned,
                    failureReason, lastFeedbackAt);
        }

        public MissionAttempt present() {
            if (absentTicks == 0) {
                return this;
            }
            return new MissionAttempt(playerId, missionId, attempt, phase, targetDimension,
                    target, startedAt, retryAt, holdTicks, 0, waveSpawned,
                    failureReason, lastFeedbackAt);
        }

        public MissionAttempt feedback(long gameTime) {
            return new MissionAttempt(playerId, missionId, attempt, phase, targetDimension,
                    target, startedAt, retryAt, holdTicks, absentTicks, waveSpawned,
                    failureReason, Math.max(0L, gameTime));
        }

        public MissionAttempt failed(long retryGameTime, String reason) {
            return new MissionAttempt(playerId, missionId, attempt, "cooldown", targetDimension,
                    target, startedAt, Math.max(0L, retryGameTime), 0, 0, false,
                    reason, lastFeedbackAt);
        }

        public MissionAttempt restarted(int nextAttempt, long gameTime) {
            return new MissionAttempt(playerId, missionId, nextAttempt, "objectives",
                    targetDimension, target, Math.max(0L, gameTime), 0L,
                    0, 0, false, "", lastFeedbackAt);
        }

        public MissionAttempt complete() {
            return new MissionAttempt(playerId, missionId, attempt, "complete", targetDimension,
                    target, startedAt, 0L, holdTicks, 0, waveSpawned,
                    "", lastFeedbackAt);
        }
    }
}
