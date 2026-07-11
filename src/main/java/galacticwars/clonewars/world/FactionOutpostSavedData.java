package galacticwars.clonewars.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.recruitment.NpcServiceBranch;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.LinkedHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class FactionOutpostSavedData extends SavedData {
    private static final Codec<FactionOutpostRecord> OUTPOST_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(FactionOutpostRecord::id),
            Codec.STRING.fieldOf("faction_id").forGetter(FactionOutpostRecord::factionId),
            Codec.STRING.fieldOf("dimension").forGetter(FactionOutpostRecord::dimensionId),
            Codec.INT.fieldOf("x").forGetter(FactionOutpostRecord::x),
            Codec.INT.fieldOf("y").forGetter(FactionOutpostRecord::y),
            Codec.INT.fieldOf("z").forGetter(FactionOutpostRecord::z),
            Codec.intRange(16, 512).fieldOf("radius").forGetter(FactionOutpostRecord::radius),
            UUIDUtil.CODEC.listOf().optionalFieldOf("military_npcs", List.of())
                    .forGetter(FactionOutpostRecord::militaryNpcIds),
            UUIDUtil.CODEC.listOf().optionalFieldOf("civilian_npcs", List.of())
                    .forGetter(FactionOutpostRecord::civilianNpcIds),
            Codec.LONG.optionalFieldOf("last_activity", 0L).forGetter(FactionOutpostRecord::lastActivityGameTime)
    ).apply(instance, FactionOutpostRecord::new));

    public static final Codec<FactionOutpostSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", 1).forGetter(data -> 1),
            OUTPOST_CODEC.listOf().optionalFieldOf("outposts", List.of()).forGetter(FactionOutpostSavedData::outposts),
            UUIDUtil.CODEC.listOf().optionalFieldOf("generated_sites", List.of())
                    .forGetter(data -> List.copyOf(data.generatedSiteIds))
    ).apply(instance, FactionOutpostSavedData::new));

    public static final SavedDataType<FactionOutpostSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "overworld_faction_outposts"),
            FactionOutpostSavedData::new,
            CODEC);

    private final Map<UUID, FactionOutpostRecord> outpostsById = new LinkedHashMap<>();
    private final Map<UUID, UUID> outpostIdsByNpc = new LinkedHashMap<>();
    private final LinkedHashSet<UUID> generatedSiteIds = new LinkedHashSet<>();

    public FactionOutpostSavedData() {
    }

    private FactionOutpostSavedData(
            int schemaVersion,
            List<FactionOutpostRecord> outposts,
            List<UUID> generatedSiteIds
    ) {
        if (schemaVersion > 1) throw new IllegalArgumentException("unsupported faction outpost schema " + schemaVersion);
        outposts.forEach(this::index);
        this.generatedSiteIds.addAll(generatedSiteIds);
        this.generatedSiteIds.retainAll(outpostsById.keySet());
    }

    public static FactionOutpostSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public List<FactionOutpostRecord> outposts() {
        return List.copyOf(outpostsById.values());
    }

    public Optional<FactionOutpostRecord> outpost(UUID id) {
        return Optional.ofNullable(outpostsById.get(id));
    }

    public Optional<FactionOutpostRecord> outpostForNpc(UUID npcId) {
        return Optional.ofNullable(outpostIdsByNpc.get(npcId)).flatMap(this::outpost);
    }

    public boolean siteGenerated(UUID outpostId) {
        return generatedSiteIds.contains(outpostId);
    }

    public void markSiteGenerated(UUID outpostId) {
        if (outpostsById.containsKey(outpostId) && generatedSiteIds.add(outpostId)) {
            this.setDirty();
        }
    }

    public Optional<FactionOutpostRecord> assignNaturalNpc(
            UUID npcId,
            OverworldFactionSpawnProfile profile,
            NpcServiceBranch branch,
            String dimensionId,
            BlockPos position,
            long gameTime
    ) {
        Optional<FactionOutpostRecord> existing = outpostForNpc(npcId);
        if (existing.isPresent()) return existing;
        FactionOutpostRecord nearest = outpostsById.values().stream()
                .filter(outpost -> outpost.dimensionId().equals(dimensionId)
                        && outpost.factionId().equals(profile.factionId()))
                .filter(outpost -> outpost.distanceSquared(position.getX(), position.getZ())
                        <= (long) outpost.radius() * outpost.radius())
                .min(java.util.Comparator.comparingLong(
                        outpost -> outpost.distanceSquared(position.getX(), position.getZ())))
                .orElse(null);
        if (nearest == null) {
            boolean overlaps = outpostsById.values().stream()
                    .filter(outpost -> outpost.dimensionId().equals(dimensionId))
                    .anyMatch(outpost -> outpost.distanceSquared(position.getX(), position.getZ())
                            < (long) profile.minimumOutpostSpacing() * profile.minimumOutpostSpacing());
            if (overlaps) return Optional.empty();
            nearest = FactionOutpostRecord.create(
                    profile.factionId(), dimensionId, position.getX(), position.getY(), position.getZ(),
                    profile.outpostRadius(), gameTime);
        }
        int population = branch == NpcServiceBranch.MILITARY
                ? nearest.militaryNpcIds().size() : nearest.civilianNpcIds().size();
        int capacity = branch == NpcServiceBranch.MILITARY
                ? profile.militaryCapacity() : profile.civilianCapacity();
        if (population >= capacity) return Optional.empty();
        FactionOutpostRecord updated = nearest.withNpc(npcId, branch, gameTime);
        index(updated);
        this.setDirty();
        return Optional.of(updated);
    }

    public boolean removeNpc(UUID npcId, long gameTime) {
        UUID outpostId = outpostIdsByNpc.remove(npcId);
        FactionOutpostRecord outpost = outpostId == null ? null : outpostsById.get(outpostId);
        if (outpost == null) return false;
        index(outpost.withoutNpc(npcId, gameTime));
        this.setDirty();
        return true;
    }

    private void index(FactionOutpostRecord outpost) {
        FactionOutpostRecord previous = outpostsById.put(outpost.id(), outpost);
        if (previous != null) {
            previous.militaryNpcIds().forEach(id -> outpostIdsByNpc.remove(id, previous.id()));
            previous.civilianNpcIds().forEach(id -> outpostIdsByNpc.remove(id, previous.id()));
        }
        outpost.militaryNpcIds().forEach(id -> outpostIdsByNpc.put(id, outpost.id()));
        outpost.civilianNpcIds().forEach(id -> outpostIdsByNpc.put(id, outpost.id()));
    }
}
