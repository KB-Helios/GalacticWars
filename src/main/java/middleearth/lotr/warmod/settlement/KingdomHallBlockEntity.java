package middleearth.lotr.warmod.settlement;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import middleearth.lotr.warmod.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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

public final class KingdomHallBlockEntity extends BaseContainerBlockEntity {
    public static final int CONTAINER_SIZE = 54;
    private static final List<String> FACTIONS = List.of(
            "kingdomwarsmiddleearth:gondor",
            "kingdomwarsmiddleearth:rohan",
            "kingdomwarsmiddleearth:mordor",
            "kingdomwarsmiddleearth:dwarf",
            "kingdomwarsmiddleearth:elf");

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private @Nullable UUID ownerId;
    private String factionId = FACTIONS.getFirst();
    private long lastUpkeepGameTime;
    private boolean upkeepPaid = true;

    public KingdomHallBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.KINGDOM_HALL.get(), pos, state);
    }

    public @Nullable UUID ownerId() {
        return ownerId;
    }

    public boolean isOwner(Player player) {
        return ownerId != null && ownerId.equals(player.getUUID());
    }

    public boolean claim(Player player) {
        if (ownerId != null && !ownerId.equals(player.getUUID())) {
            return false;
        }
        if (ownerId == null) {
            ownerId = player.getUUID();
            this.setChangedAndSync();
        }
        return true;
    }

    public String factionId() {
        return factionId;
    }

    public String cycleFaction() {
        int next = (FACTIONS.indexOf(factionId) + 1) % FACTIONS.size();
        return this.setFaction(FACTIONS.get(next));
    }

    public String setFaction(String factionId) {
        if (!FACTIONS.contains(factionId)) {
            throw new IllegalArgumentException("unsupported faction " + factionId);
        }
        this.factionId = factionId;
        this.setChangedAndSync();
        return this.factionId;
    }

    public int treasuryEmeralds() {
        int total = 0;
        for (ItemStack stack : items) {
            if (stack.is(Items.EMERALD)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public boolean upkeepPaid() {
        return upkeepPaid;
    }

    public boolean chargeDailyUpkeep(long gameTime, int population) {
        if (gameTime - this.lastUpkeepGameTime < 24000) {
            return this.upkeepPaid;
        }
        int upkeep = Math.max(1, population);
        this.upkeepPaid = this.reserveEmeralds(upkeep);
        this.lastUpkeepGameTime = gameTime;
        this.setChangedAndSync();
        return this.upkeepPaid;
    }

    public boolean reserveEmeralds(int amount) {
        if (amount < 0 || treasuryEmeralds() < amount) {
            return false;
        }
        int remaining = amount;
        for (int slot = 0; slot < items.size(); slot++) {
            if (remaining == 0) {
                break;
            }
            ItemStack stack = items.get(slot);
            if (stack.is(Items.EMERALD)) {
                int removed = Math.min(remaining, stack.getCount());
                stack.shrink(removed);
                remaining -= removed;
                if (stack.isEmpty()) {
                    items.set(slot, ItemStack.EMPTY);
                }
            }
        }
        this.setChanged();
        return true;
    }

    public int refundEmeralds(int amount) {
        int remaining = Math.max(0, amount);
        for (int slot = 0; slot < items.size() && remaining > 0; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                int inserted = Math.min(remaining, Items.EMERALD.getDefaultMaxStackSize());
                items.set(slot, new ItemStack(Items.EMERALD, inserted));
                remaining -= inserted;
            } else if (stack.is(Items.EMERALD) && stack.getCount() < stack.getMaxStackSize()) {
                int inserted = Math.min(remaining, stack.getMaxStackSize() - stack.getCount());
                stack.grow(inserted);
                remaining -= inserted;
            }
        }
        if (remaining != amount) {
            this.setChanged();
        }
        return amount - remaining;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.kingdomwarsmiddleearth.kingdom_hall");
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
        return super.canOpen(player) && this.isOwner(player);
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
        this.ownerId = input.read("KingdomOwner", UUIDUtil.CODEC).orElse(null);
        this.factionId = input.getStringOr("KingdomFaction", FACTIONS.getFirst());
        if (!FACTIONS.contains(this.factionId)) {
            this.factionId = FACTIONS.getFirst();
        }
        this.lastUpkeepGameTime = Math.max(0L, input.getLongOr("LastUpkeepGameTime", 0L));
        this.upkeepPaid = input.getBooleanOr("UpkeepPaid", true);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.storeNullable("KingdomOwner", UUIDUtil.CODEC, this.ownerId);
        output.putString("KingdomFaction", this.factionId);
        output.putLong("LastUpkeepGameTime", this.lastUpkeepGameTime);
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
}
