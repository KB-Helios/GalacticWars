package middleearth.lotr.warmod.kingdom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
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
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final Codec<KingdomSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION).forGetter(KingdomSavedData::schemaVersion),
            KingdomCodecs.KINGDOM_RECORD.listOf().optionalFieldOf("kingdoms", List.of()).forGetter(KingdomSavedData::kingdoms)
    ).apply(instance, KingdomSavedData::new));
    public static final SavedDataType<KingdomSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(KingdomWarsMiddleEarth.MODID, "kingdoms"),
            KingdomSavedData::new,
            CODEC);

    private final int schemaVersion;
    private final Map<UUID, KingdomRecord> kingdomsByOwner = new LinkedHashMap<>();

    public KingdomSavedData() {
        this(CURRENT_SCHEMA_VERSION, List.of());
    }

    private KingdomSavedData(int schemaVersion, List<KingdomRecord> kingdoms) {
        this.schemaVersion = Math.max(CURRENT_SCHEMA_VERSION, schemaVersion);
        for (KingdomRecord kingdom : kingdoms) {
            this.kingdomsByOwner.putIfAbsent(kingdom.ownerId(), kingdom);
        }
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
