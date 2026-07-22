package galacticwars.clonewars.force;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import galacticwars.clonewars.GalacticWars;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Persistent source-state ledger. Missing proxies are always restored from this data. */
public final class ForceBlockRestorationSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;
    private static final Codec<LiftedBlockRecord> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("proxy_id").forGetter(LiftedBlockRecord::proxyId),
            UUIDUtil.CODEC.fieldOf("caster_id").forGetter(LiftedBlockRecord::casterId),
            Codec.STRING.fieldOf("dimension").forGetter(LiftedBlockRecord::dimensionId),
            BlockPos.CODEC.fieldOf("source").forGetter(LiftedBlockRecord::sourcePos),
            BlockState.CODEC.fieldOf("state").forGetter(LiftedBlockRecord::sourceState),
            Codec.LONG.fieldOf("expires_at").forGetter(LiftedBlockRecord::expiresAt),
            Codec.LONG.optionalFieldOf("released_at", 0L).forGetter(LiftedBlockRecord::releasedAt),
            Codec.DOUBLE.fieldOf("x").forGetter(LiftedBlockRecord::x),
            Codec.DOUBLE.fieldOf("y").forGetter(LiftedBlockRecord::y),
            Codec.DOUBLE.fieldOf("z").forGetter(LiftedBlockRecord::z),
            Codec.DOUBLE.optionalFieldOf("dx", 0.0D).forGetter(LiftedBlockRecord::dx),
            Codec.DOUBLE.optionalFieldOf("dy", 0.0D).forGetter(LiftedBlockRecord::dy),
            Codec.DOUBLE.optionalFieldOf("dz", 0.0D).forGetter(LiftedBlockRecord::dz)
    ).apply(instance, LiftedBlockRecord::new));
    public static final Codec<ForceBlockRestorationSavedData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("schema_version", SCHEMA_VERSION)
                            .forGetter(data -> SCHEMA_VERSION),
                    ENTRY_CODEC.listOf().optionalFieldOf("entries", List.of())
                            .forGetter(ForceBlockRestorationSavedData::serialized)
            ).apply(instance, ForceBlockRestorationSavedData::new));
    public static final SavedDataType<ForceBlockRestorationSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "force_block_restoration"),
            ForceBlockRestorationSavedData::new, CODEC, null);

    private final Map<UUID, LiftedBlockRecord> entries = new LinkedHashMap<>();

    public ForceBlockRestorationSavedData() {
    }

    private ForceBlockRestorationSavedData(int schemaVersion, List<LiftedBlockRecord> loaded) {
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported Force block ledger schema " + schemaVersion);
        }
        loaded.stream().limit(ForcePhysicsRules.MAX_LIFTED_BLOCKS)
                .forEach(entry -> entries.putIfAbsent(entry.proxyId(), entry));
    }

    public static ForceBlockRestorationSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public List<LiftedBlockRecord> entries() {
        return List.copyOf(entries.values());
    }

    public LiftedBlockRecord forCaster(UUID casterId) {
        return entries.values().stream().filter(entry -> entry.casterId().equals(casterId))
                .findFirst().orElse(null);
    }

    public boolean add(LiftedBlockRecord entry) {
        if (entries.size() >= ForcePhysicsRules.MAX_LIFTED_BLOCKS
                || forCaster(entry.casterId()) != null || entries.containsKey(entry.proxyId())) return false;
        entries.put(entry.proxyId(), entry);
        setDirty();
        return true;
    }

    public void replace(LiftedBlockRecord entry) {
        if (!entries.containsKey(entry.proxyId())) return;
        entries.put(entry.proxyId(), entry);
        setDirty();
    }

    public boolean rekey(UUID previousProxyId, LiftedBlockRecord entry) {
        if (!entries.containsKey(previousProxyId) || entries.containsKey(entry.proxyId())) return false;
        entries.remove(previousProxyId);
        entries.put(entry.proxyId(), entry);
        setDirty();
        return true;
    }

    public void remove(UUID proxyId) {
        if (entries.remove(proxyId) != null) setDirty();
    }

    private List<LiftedBlockRecord> serialized() {
        return entries();
    }

    public record LiftedBlockRecord(
            UUID proxyId,
            UUID casterId,
            String dimensionId,
            BlockPos sourcePos,
            BlockState sourceState,
            long expiresAt,
            long releasedAt,
            double x,
            double y,
            double z,
            double dx,
            double dy,
            double dz
    ) {
        public LiftedBlockRecord {
            if (dimensionId.isBlank() || expiresAt < 0L
                    || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Double.isFinite(dx) || !Double.isFinite(dy) || !Double.isFinite(dz)) {
                throw new IllegalArgumentException("Invalid lifted block ledger entry");
            }
        }

        public LiftedBlockRecord released(long gameTime, double velocityX, double velocityY, double velocityZ) {
            return new LiftedBlockRecord(proxyId, casterId, dimensionId, sourcePos, sourceState,
                    expiresAt, Math.max(1L, gameTime), x, y, z,
                    velocityX, velocityY, velocityZ);
        }

        public LiftedBlockRecord withProxyId(UUID newProxyId) {
            return new LiftedBlockRecord(newProxyId, casterId, dimensionId, sourcePos, sourceState,
                    expiresAt, releasedAt, x, y, z, dx, dy, dz);
        }

        public LiftedBlockRecord moved(double nextX, double nextY, double nextZ) {
            return new LiftedBlockRecord(proxyId, casterId, dimensionId, sourcePos, sourceState,
                    expiresAt, releasedAt, nextX, nextY, nextZ, dx, dy, dz);
        }
    }
}
