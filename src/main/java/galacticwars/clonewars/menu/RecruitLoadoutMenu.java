package galacticwars.clonewars.menu;

import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.registry.ModMenuTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.network.FriendlyByteBuf;

/** Server-authoritative recruit equipment and shared physical cargo inventory. */
public final class RecruitLoadoutMenu extends AbstractContainerMenu {
    public static final int EQUIPMENT_SLOT_COUNT = 6;
    public static final int CARGO_SLOT_COUNT = 9;
    public static final int RECRUIT_SLOT_COUNT = EQUIPMENT_SLOT_COUNT + CARGO_SLOT_COUNT;
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int PLAYER_INVENTORY_START = RECRUIT_SLOT_COUNT;
    public static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + PLAYER_SLOT_COUNT;

    private static final int CARGO_SLOT_START = EQUIPMENT_SLOT_COUNT;
    private static final int CARGO_SLOT_END = RECRUIT_SLOT_COUNT;
    private static final int PLAYER_EXTENDED_END = PLAYER_INVENTORY_START + 27;
    private static final EquipmentSlot[] EQUIPMENT_SLOTS = {
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private final Level level;
    private final int recruitEntityId;
    private final Container equipment;
    private final Container cargo;
    private final GalacticRecruitEntity serverRecruit;

    /** Client constructor. Slot contents are populated by vanilla menu synchronization. */
    public RecruitLoadoutMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readVarInt());
    }

    private RecruitLoadoutMenu(int containerId, Inventory inventory, int recruitEntityId) {
        this(
                containerId,
                inventory,
                recruitEntityId,
                new SimpleContainer(EQUIPMENT_SLOT_COUNT),
                new SimpleContainer(CARGO_SLOT_COUNT),
                null);
    }

    /** Server constructor. Both container views mutate the live recruit state. */
    public RecruitLoadoutMenu(
            int containerId,
            Inventory inventory,
            GalacticRecruitEntity recruit
    ) {
        this(
                containerId,
                inventory,
                recruit.getId(),
                new RecruitEquipmentContainer(recruit),
                recruit.createCargoContainer(),
                recruit);
    }

    private RecruitLoadoutMenu(
            int containerId,
            Inventory inventory,
            int recruitEntityId,
            Container equipment,
            Container cargo,
            GalacticRecruitEntity serverRecruit
    ) {
        super(ModMenuTypes.RECRUIT_LOADOUT.get(), containerId);
        checkContainerSize(equipment, EQUIPMENT_SLOT_COUNT);
        checkContainerSize(cargo, CARGO_SLOT_COUNT);
        this.level = inventory.player.level();
        this.recruitEntityId = recruitEntityId;
        this.equipment = equipment;
        this.cargo = cargo;
        this.serverRecruit = serverRecruit;

        for (int slot = 0; slot < EQUIPMENT_SLOT_COUNT; slot++) {
            this.addSlot(new RecruitEquipmentSlot(
                    equipment,
                    slot,
                    8 + slot * 18,
                    20,
                    EQUIPMENT_SLOTS[slot],
                    serverRecruit));
        }
        for (int slot = 0; slot < CARGO_SLOT_COUNT; slot++) {
            this.addSlot(new Slot(cargo, slot, 8 + slot * 18, 64));
        }
        this.addStandardInventorySlots(inventory, 8, 112);

        if (serverRecruit != null) {
            equipment.startOpen(inventory.player);
            cargo.startOpen(inventory.player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot sourceSlot = this.slots.get(slotIndex);
        if (!sourceSlot.hasItem() || !sourceSlot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack original = sourceStack.copy();
        int startingCount = sourceStack.getCount();

        if (slotIndex < RECRUIT_SLOT_COUNT) {
            if (!this.moveItemStackTo(
                    sourceStack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int equipmentTarget = this.equipmentTarget(sourceStack);
            if (equipmentTarget >= 0) {
                this.moveItemStackTo(sourceStack, equipmentTarget, equipmentTarget + 1, false);
            }
            if (!sourceStack.isEmpty()) {
                this.moveItemStackTo(sourceStack, CARGO_SLOT_START, CARGO_SLOT_END, false);
            }
            if (sourceStack.getCount() == startingCount) {
                boolean movedWithinPlayerInventory;
                if (slotIndex < PLAYER_EXTENDED_END) {
                    movedWithinPlayerInventory = this.moveItemStackTo(
                            sourceStack, PLAYER_EXTENDED_END, PLAYER_INVENTORY_END, false);
                } else {
                    movedWithinPlayerInventory = this.moveItemStackTo(
                            sourceStack, PLAYER_INVENTORY_START, PLAYER_EXTENDED_END, false);
                }
                if (!movedWithinPlayerInventory) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.setByPlayer(ItemStack.EMPTY, original);
        } else {
            sourceSlot.setChanged();
        }
        if (sourceStack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        sourceSlot.onTake(player, sourceStack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        Entity entity = this.level.getEntity(this.recruitEntityId);
        return player.level() == this.level
                && entity instanceof GalacticRecruitEntity recruit
                && (this.serverRecruit == null || recruit == this.serverRecruit)
                && recruit.isAlive()
                && player.distanceToSqr(recruit) <= 64.0D
                && (this.serverRecruit == null || recruit.canPlayerManageLogistics(player));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.serverRecruit != null) {
            this.equipment.stopOpen(player);
            this.cargo.stopOpen(player);
        }
    }

    public int recruitEntityId() {
        return recruitEntityId;
    }

    private int equipmentTarget(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return -1;
        }
        for (int slot = 0; slot < EQUIPMENT_SLOT_COUNT; slot++) {
            if (EQUIPMENT_SLOTS[slot] == equippable.slot()
                    && this.slots.get(slot).mayPlace(stack)) {
                return slot;
            }
        }
        return -1;
    }

    private static final class RecruitEquipmentSlot extends Slot {
        private final EquipmentSlot equipmentSlot;
        private final GalacticRecruitEntity owner;

        private RecruitEquipmentSlot(
                Container container,
                int containerSlot,
                int x,
                int y,
                EquipmentSlot equipmentSlot,
                GalacticRecruitEntity owner
        ) {
            super(container, containerSlot, x, y);
            this.equipmentSlot = equipmentSlot;
            this.owner = owner;
        }

        @Override
        public void setByPlayer(ItemStack stack, ItemStack previous) {
            if (this.owner != null) {
                this.owner.onEquipItem(this.equipmentSlot, previous, stack);
            }
            super.setByPlayer(stack, previous);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (this.equipmentSlot.getType() == EquipmentSlot.Type.HAND) {
                return true;
            }
            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
            return equippable != null
                    && equippable.slot() == this.equipmentSlot
                    && (this.owner == null
                    || equippable.canBeEquippedBy(this.owner.typeHolder()));
        }

        @Override
        public boolean mayPickup(Player player) {
            ItemStack stack = this.getItem();
            if (this.equipmentSlot.isArmor()
                    && !stack.isEmpty()
                    && !player.isCreative()
                    && EnchantmentHelper.has(
                    stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
                return false;
            }
            return super.mayPickup(player);
        }

        @Override
        public int getMaxStackSize() {
            return this.equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR
                    ? 1 : super.getMaxStackSize();
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return this.equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR
                    ? 1 : super.getMaxStackSize(stack);
        }

        @Override
        public Identifier getNoItemIcon() {
            return switch (this.equipmentSlot) {
                case OFFHAND -> InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;
                case HEAD -> InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
                case CHEST -> InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
                case LEGS -> InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
                case FEET -> InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
                default -> null;
            };
        }
    }

    private static final class RecruitEquipmentContainer implements Container {
        private final GalacticRecruitEntity recruit;

        private RecruitEquipmentContainer(GalacticRecruitEntity recruit) {
            this.recruit = recruit;
        }

        @Override
        public int getContainerSize() {
            return EQUIPMENT_SLOT_COUNT;
        }

        @Override
        public boolean isEmpty() {
            for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
                if (!this.recruit.getItemBySlot(slot).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return validSlot(slot)
                    ? this.recruit.getItemBySlot(EQUIPMENT_SLOTS[slot])
                    : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack existing = this.getItem(slot);
            if (existing.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            int removedCount = Math.min(amount, existing.getCount());
            ItemStack removed = existing.copyWithCount(removedCount);
            ItemStack remainder = existing.copy();
            remainder.shrink(removedCount);
            this.setItem(slot, remainder);
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack existing = this.getItem(slot);
            if (!existing.isEmpty()) {
                this.recruit.setItemSlot(EQUIPMENT_SLOTS[slot], ItemStack.EMPTY);
            }
            return existing;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (!validSlot(slot)) {
                throw new IndexOutOfBoundsException("equipment slot " + slot);
            }
            EquipmentSlot equipmentSlot = EQUIPMENT_SLOTS[slot];
            int maxCount = equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR
                    ? 1 : stack.getMaxStackSize();
            ItemStack stored = stack.isEmpty()
                    ? ItemStack.EMPTY
                    : stack.copyWithCount(Math.min(stack.getCount(), maxCount));
            this.recruit.setItemSlot(equipmentSlot, stored);
        }

        @Override
        public void setChanged() {
            this.recruit.markLoadoutChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return player.level() == this.recruit.level()
                    && this.recruit.isAlive()
                    && player.distanceToSqr(this.recruit) <= 64.0D
                    && this.recruit.canPlayerManageLogistics(player);
        }

        @Override
        public void clearContent() {
            for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
                this.recruit.setItemSlot(slot, ItemStack.EMPTY);
            }
        }

        private static boolean validSlot(int slot) {
            return slot >= 0 && slot < EQUIPMENT_SLOT_COUNT;
        }
    }
}
