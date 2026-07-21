package galacticwars.clonewars.workforce.logistics;

import java.util.Objects;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Loader-neutral adapter for Minecraft's common {@link Container} contract. */
public final class MinecraftContainerInventory implements LogisticsInventory {
    private final Container container;

    public MinecraftContainerInventory(Container container) {
        this.container = Objects.requireNonNull(container, "container");
    }

    public Container container() {
        return container;
    }

    @Override
    public int size() {
        return container.getContainerSize();
    }

    @Override
    public ItemStack getStack(int slot) {
        return container.getItem(slot);
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack) {
        return container.canPlaceItem(slot, stack);
    }

    @Override
    public int maxStackSize(int slot, ItemStack stack) {
        return Math.min(container.getMaxStackSize(stack), stack.getMaxStackSize());
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        container.setItem(slot, stack);
    }

    @Override
    public void setChanged() {
        container.setChanged();
    }

    @Override
    public Object transactionIdentity() {
        return container;
    }
}
