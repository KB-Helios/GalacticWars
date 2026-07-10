package middleearth.lotr.warmod.kingdom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntUnaryOperator;
import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class KingdomSavedData extends SavedData {
    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final Codec<KingdomSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION).forGetter(KingdomSavedData::schemaVersion),
            KingdomCodecs.KINGDOM_RECORD.listOf().optionalFieldOf("kingdoms", List.of()).forGetter(KingdomSavedData::kingdoms),
            net.minecraft.core.UUIDUtil.CODEC.listOf().optionalFieldOf("inactive_hall_owners", List.of())
                    .forGetter(data -> List.copyOf(data.inactiveHallOwners))
    ).apply(instance, KingdomSavedData::new));
    public static final SavedDataType<KingdomSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(KingdomWarsMiddleEarth.MODID, "kingdoms"),
            KingdomSavedData::new,
            CODEC);

    private final int schemaVersion;
    private final Map<UUID, KingdomRecord> kingdomsByOwner = new LinkedHashMap<>();
    private final LinkedHashSet<UUID> inactiveHallOwners = new LinkedHashSet<>();

    public KingdomSavedData() {
        this(CURRENT_SCHEMA_VERSION, List.of(), List.of());
    }

    private KingdomSavedData(int schemaVersion, List<KingdomRecord> kingdoms, List<UUID> inactiveHallOwners) {
        this.schemaVersion = Math.max(CURRENT_SCHEMA_VERSION, schemaVersion);
        for (KingdomRecord kingdom : kingdoms) {
            this.kingdomsByOwner.putIfAbsent(kingdom.ownerId(), kingdom);
        }
        this.inactiveHallOwners.addAll(inactiveHallOwners);
        this.inactiveHallOwners.retainAll(this.kingdomsByOwner.keySet());
    }

    public static KingdomSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public List<KingdomRecord> kingdoms() {
        return List.copyOf(kingdomsByOwner.values());
    }

    public Optional<KingdomRecord> kingdomForOwner(UUID ownerId) {
        return Optional.ofNullable(kingdomsByOwner.get(ownerId));
    }

    public boolean isHallActive(UUID ownerId) {
        return kingdomsByOwner.containsKey(ownerId) && !inactiveHallOwners.contains(ownerId);
    }

    public KingdomRecord foundKingdom(UUID ownerId, String factionId, String dimensionId, BlockPos hallPos) {
        KingdomRecord existing = kingdomsByOwner.get(ownerId);
        if (existing != null) {
            return existing;
        }
        KingdomRecord kingdom = new KingdomRecord(
                UUID.randomUUID(),
                ownerId,
                factionId,
                SettlementRecord.create(dimensionId, hallPos.getX(), hallPos.getY(), hallPos.getZ()));
        kingdomsByOwner.put(ownerId, kingdom);
        this.setDirty();
        return kingdom;
    }

    public Optional<KingdomRecord> activateHall(
            UUID ownerId,
            String factionId,
            String dimensionId,
            BlockPos hallPos
    ) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(hallPos, "hallPos");
        KingdomRecord existing = kingdomsByOwner.get(ownerId);
        if (existing == null) {
            return Optional.of(foundKingdom(ownerId, factionId, dimensionId, hallPos));
        }
        SettlementRecord settlement = existing.settlement();
        boolean sameHall = settlement.dimensionId().equals(dimensionId)
                && settlement.hallX() == hallPos.getX()
                && settlement.hallY() == hallPos.getY()
                && settlement.hallZ() == hallPos.getZ();
        if (!inactiveHallOwners.contains(ownerId)) {
            return sameHall ? Optional.of(existing) : Optional.empty();
        }
        KingdomRecord relocated = existing.withSettlement(
                settlement.withHallLocation(dimensionId, hallPos.getX(), hallPos.getY(), hallPos.getZ()));
        kingdomsByOwner.put(ownerId, relocated);
        inactiveHallOwners.remove(ownerId);
        this.setDirty();
        return Optional.of(relocated);
    }

    public boolean deactivateHall(UUID ownerId, String dimensionId, BlockPos hallPos) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null || inactiveHallOwners.contains(ownerId)) {
            return false;
        }
        SettlementRecord settlement = kingdom.settlement();
        if (!settlement.dimensionId().equals(dimensionId)
                || settlement.hallX() != hallPos.getX()
                || settlement.hallY() != hallPos.getY()
                || settlement.hallZ() != hallPos.getZ()) {
            return false;
        }
        inactiveHallOwners.add(ownerId);
        this.setDirty();
        return true;
    }

    public boolean registerRecruit(UUID ownerId, UUID recruitId) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null || !kingdom.settlement().hasHousingSpace()) {
            return false;
        }
        SettlementRecord updated = kingdom.settlement().withRecruit(recruitId);
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return true;
    }

    public boolean unregisterRecruit(UUID ownerId, UUID recruitId) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return false;
        }
        SettlementRecord updated = kingdom.settlement().withoutRecruit(recruitId);
        if (updated == kingdom.settlement()) {
            return false;
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return true;
    }

    public int cancelActiveCampaigns(UUID ownerId, String reasonCode) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return 0;
        }
        SettlementRecord updated = kingdom.settlement();
        int cancelled = 0;
        for (RecruitmentCampaign campaign : kingdom.settlement().recruitmentCampaigns()) {
            if (campaign.active()) {
                updated = updated.replaceCampaign(campaign.cancel(reasonCode));
                cancelled++;
            }
        }
        if (cancelled > 0) {
            kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
            this.setDirty();
        }
        return cancelled;
    }

    public boolean changeFaction(UUID ownerId, String factionId) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return true;
        }
        if (!kingdom.settlement().recruitIds().isEmpty() || kingdom.settlement().hasActiveCampaign()) {
            return false;
        }
        kingdomsByOwner.put(ownerId, kingdom.withFaction(factionId));
        this.setDirty();
        return true;
    }

    public boolean promoteCommander(UUID ownerId, UUID recruitId) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null || kingdom.settlement().commanderId().isPresent()
                || !kingdom.settlement().hasCommanderSlot()
                || !kingdom.settlement().containsRecruit(recruitId)) {
            return false;
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(kingdom.settlement().withCommander(recruitId)));
        this.setDirty();
        return true;
    }

    public boolean completeBuildProject(
            UUID ownerId,
            BuildProject project,
            int housingReward,
            String worksiteType,
            int worksiteCapacity
    ) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return false;
        }
        SettlementRecord updated = kingdom.settlement().withCompletedProject(
                project, housingReward, worksiteType, worksiteCapacity);
        if (updated == kingdom.settlement()) {
            return false;
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return true;
    }

    public boolean clearCommander(UUID ownerId, UUID recruitId) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return false;
        }
        SettlementRecord updated = kingdom.settlement().withoutCommander(recruitId);
        if (updated == kingdom.settlement()) {
            return false;
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return true;
    }

    public boolean updateCommanderPolicy(UUID ownerId, int expectedRevision, CommanderPolicy policy) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null || kingdom.settlement().revision() != expectedRevision) {
            return false;
        }
        kingdomsByOwner.put(ownerId,
                kingdom.withSettlement(kingdom.settlement().withCommanderPolicy(policy)));
        this.setDirty();
        return true;
    }

    public boolean beginCampaign(UUID ownerId, RecruitmentCampaignDecision decision) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null || !decision.accepted()) {
            return false;
        }
        RecruitmentCampaign campaign = decision.campaign().orElseThrow();
        kingdomsByOwner.put(ownerId,
                kingdom.withSettlement(kingdom.settlement().withCampaign(campaign)));
        this.setDirty();
        return true;
    }

    public boolean replaceCampaign(UUID ownerId, RecruitmentCampaign campaign) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return false;
        }
        SettlementRecord updated = kingdom.settlement().replaceCampaign(campaign);
        if (updated == kingdom.settlement()) {
            return false;
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return true;
    }

    public int applyPendingCampaignRefunds(UUID ownerId, IntUnaryOperator refundSink) {
        Objects.requireNonNull(refundSink, "refundSink");
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return 0;
        }
        SettlementRecord updated = kingdom.settlement();
        int totalRefunded = 0;
        for (RecruitmentCampaign campaign : kingdom.settlement().recruitmentCampaigns()) {
            if (!campaign.refundPending()) {
                continue;
            }
            int refunded = Math.max(0, Math.min(campaign.reservedCost(), refundSink.applyAsInt(campaign.reservedCost())));
            if (refunded > 0) {
                updated = updated.replaceCampaign(campaign.applyRefund(refunded));
                totalRefunded = Math.addExact(totalRefunded, refunded);
            }
        }
        if (totalRefunded > 0) {
            kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
            this.setDirty();
        }
        return totalRefunded;
    }
}
