package galacticwars.clonewars.workforce.logistics;

import net.minecraft.world.item.ItemStack;

/**
 * Minimal physical-inventory boundary used by the common logistics engine.
 *
 * <p>Implementations must make {@link #setStack(int, ItemStack)} a reliable replacement operation for
 * valid slots. The transaction engine never mutates stacks returned by {@link #getStack(int)}.</p>
 */
public interface LogisticsInventory {
    int size();

    ItemStack getStack(int slot);

    default boolean canExtract(int slot, ItemStack stack) {
        return true;
    }

    default boolean canInsert(int slot, ItemStack stack) {
        return true;
    }

    default int maxStackSize(int slot, ItemStack stack) {
        return stack.getMaxStackSize();
    }

    void setStack(int slot, ItemStack stack);

    default void setChanged() {
    }

    /** Reference-identity token used to reject two endpoint views over the same physical inventory. */
    default Object transactionIdentity() {
        return this;
    }
}
