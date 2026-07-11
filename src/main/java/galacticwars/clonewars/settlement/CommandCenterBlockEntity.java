package galacticwars.clonewars.settlement;

import galacticwars.clonewars.economy.CreditTransactionService;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.kingdom.KingdomRecord;
import galacticwars.clonewars.kingdom.KingdomActionId;
import galacticwars.clonewars.kingdom.KingdomGameplayAction;
import galacticwars.clonewars.kingdom.KingdomGameplayRuntimeService;
import galacticwars.clonewars.kingdom.KingdomPermission;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.registry.ModBlockEntityTypes;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class CommandCenterBlockEntity extends BaseContainerBlockEntity {
    public static final int CONTAINER_SIZE = 54;
    private static final List<String> FALLBACK_FACTIONS = List.of(
            "galacticwars:republic",
            "galacticwars:mandalorian",
            "galacticwars:separatist",
            "galacticwars:hutt_cartel",
            "galacticwars:nightsister");

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private @Nullable UUID ownerId;
    private String factionId = FALLBACK_FACTIONS.getFirst();
    private long lastUpkeepGameTime;
    private boolean upkeepClockInitialized;
    private boolean upkeepPaid = true;
    private boolean removalPrepared;

    public CommandCenterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.COMMAND_CENTER.get(), pos, state);
    }

    public @Nullable UUID ownerId() {
        return ownerId;
    }

    public boolean isOwner(Player player) {
        return ownerId != null && ownerId.equals(player.getUUID());
    }

    public boolean canUse(Player player, KingdomPermission permission) {
        if (this.isOwner(player)) {
            return true;
        }
        return this.level instanceof ServerLevel serverLevel
                && this.ownerId != null
                && KingdomSavedData.get(serverLevel).kingdomForOwner(this.ownerId)
                        .map(kingdom -> kingdom.allows(player.getUUID(), permission))
                        .orElse(false);
    }

    public boolean claim(Player player) {
        if (ownerId != null && !ownerId.equals(player.getUUID())) {
            return false;
        }
        if (ownerId == null) {
            ownerId = player.getUUID();
            this.lastUpkeepGameTime = this.level == null ? 0L : this.level.getGameTime();
            this.upkeepClockInitialized = true;
            if (this.level instanceof ServerLevel serverLevel) {
                KingdomGameplayRuntimeService.applyProgression(
                        ProgressionSavedData.get(serverLevel),
                        new KingdomGameplayAction(
                                KingdomActionId.of("command_center_claim", player.getUUID(),
                                        serverLevel.dimension().identifier(), this.worldPosition.asLong()),
                                player.getUUID(), ProgressionEventType.BUILDING_COMPLETED,
                                "command_center", 1));
            }
            this.setChangedAndSync();
        }
        return true;
    }

    public String factionId() {
        return factionId;
    }

    public String cycleFaction() {
        List<String> factions = supportedFactions();
        int next = (Math.max(0, factions.indexOf(factionId)) + 1) % factions.size();
        return this.setFaction(factions.get(next));
    }

    public String setFaction(String factionId) {
        if (!supportedFactions().contains(factionId)) {
            throw new IllegalArgumentException("unsupported faction " + factionId);
        }
        this.factionId = factionId;
        this.setChangedAndSync();
        return this.factionId;
    }

    public int treasuryCredits() {
        return CreditTransactionService.containerBalance(this);
    }

    public boolean upkeepPaid() {
        return upkeepPaid;
    }

    public boolean chargeDailyUpkeep(long gameTime, int population) {
        if (this.level instanceof ServerLevel serverLevel) {
            this.settlePendingCampaignRefunds(serverLevel);
        }
        if (!this.upkeepClockInitialized) {
            this.lastUpkeepGameTime = gameTime;
            this.upkeepClockInitialized = true;
            this.setChangedAndSync();
            return this.upkeepPaid;
        }
        if (gameTime - this.lastUpkeepGameTime < 24000) {
            return this.upkeepPaid;
        }
        int upkeepPercent = GameplayDataManager.snapshot().faction(this.factionId)
                .map(definition -> definition.strategy().upkeepPercent()).orElse(100);
        int upkeep = Math.max(1, Math.floorDiv(Math.addExact(
                Math.multiplyExact(Math.max(0, population), upkeepPercent), 99), 100));
        this.upkeepPaid = this.reserveCredits(upkeep);
        this.lastUpkeepGameTime = gameTime;
        this.setChangedAndSync();
        return this.upkeepPaid;
    }

    public boolean reserveCredits(int amount) {
        return CreditTransactionService.withdrawContainer(this, amount);
    }

    public int refundCredits(int amount) {
        return CreditTransactionService.depositContainer(this, amount);
    }

    public int settlePendingCampaignRefunds(ServerLevel level) {
        if (this.ownerId == null) {
            return 0;
        }
        KingdomSavedData data = KingdomSavedData.get(level);
        boolean isAuthoritativeHall = data.isHallActive(this.ownerId)
                && data.kingdomForOwner(this.ownerId)
                .map(KingdomRecord::settlement)
                .filter(settlement -> settlement.dimensionId().equals(level.dimension().identifier().toString()))
                .filter(settlement -> settlement.hallX() == this.worldPosition.getX()
                        && settlement.hallY() == this.worldPosition.getY()
                        && settlement.hallZ() == this.worldPosition.getZ())
                .isPresent();
        return isAuthoritativeHall
                ? data.applyPendingCampaignRefunds(this.ownerId, this::refundCredits)
                : 0;
    }

    public boolean prepareForOwnerRemoval(ServerLevel level) {
        if (this.removalPrepared) {
            return false;
        }
        this.removalPrepared = true;
        return CommandCenterLifecycleService.prepareOwnerRemoval(level, this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level instanceof ServerLevel serverLevel) {
            this.settlePendingCampaignRefunds(serverLevel);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.galacticwars.command_center");
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = Objects.requireNonNull(items, "items");
    }

    @Override
    public boolean canOpen(Player player) {
        return super.canOpen(player) && this.canUse(player, KingdomPermission.USE_STORAGE);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.sixRows(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.ownerId = input.read("CommandCenterOwner", UUIDUtil.CODEC).orElse(null);
        List<String> factions = supportedFactions();
        this.factionId = input.getStringOr("CommandCenterFaction", factions.getFirst());
        if (!factions.contains(this.factionId)) {
            this.factionId = factions.getFirst();
        }
        this.lastUpkeepGameTime = Math.max(0L, input.getLongOr("LastUpkeepGameTime", 0L));
        this.upkeepClockInitialized = input.getBooleanOr(
                "UpkeepClockInitialized",
                this.lastUpkeepGameTime > 0L);
        this.upkeepPaid = input.getBooleanOr("UpkeepPaid", true);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.storeNullable("CommandCenterOwner", UUIDUtil.CODEC, this.ownerId);
        output.putString("CommandCenterFaction", this.factionId);
        output.putLong("LastUpkeepGameTime", this.lastUpkeepGameTime);
        output.putBoolean("UpkeepClockInitialized", this.upkeepClockInitialized);
        output.putBoolean("UpkeepPaid", this.upkeepPaid);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    private void setChangedAndSync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    private static List<String> supportedFactions() {
        List<String> loaded = GameplayDataManager.snapshot().selectableFactions().stream()
                .map(definition -> definition.id().toString())
                .toList();
        return loaded.isEmpty() ? FALLBACK_FACTIONS : loaded;
    }
}
