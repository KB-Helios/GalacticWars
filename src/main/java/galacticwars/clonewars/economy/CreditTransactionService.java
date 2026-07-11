package galacticwars.clonewars.economy;

import galacticwars.clonewars.registry.ModItems;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class CreditTransactionService {
    private CreditTransactionService() {
    }

    public static int playerBalance(ServerPlayer player) {
        return count(player.getInventory().getNonEquipmentItems());
    }

    public static boolean withdrawPlayer(ServerPlayer player, int amount) {
        if (amount < 0) return false;
        if (amount == 0 || player.hasInfiniteMaterials()) return true;
        boolean committed = withdraw(player.getInventory().getNonEquipmentItems(), amount);
        if (committed) player.getInventory().setChanged();
        return committed;
    }

    public static void refundPlayer(ServerPlayer player, int amount) {
        if (amount <= 0 || player.hasInfiniteMaterials()) return;
        ItemStack remainder = new ItemStack(ModItems.CREDIT_CHIP.get(), amount);
        player.getInventory().add(remainder);
        if (!remainder.isEmpty() && player.level() instanceof ServerLevel level) {
            player.spawnAtLocation(level, remainder);
        }
    }

    public static int containerBalance(Container container) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(ModItems.CREDIT_CHIP.get())) total = Math.addExact(total, stack.getCount());
        }
        return total;
    }

    public static boolean withdrawContainer(Container container, int amount) {
        if (amount < 0 || containerBalance(container) < amount) return false;
        if (amount == 0) return true;
        int remaining = amount;
        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.is(ModItems.CREDIT_CHIP.get())) continue;
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
            if (stack.isEmpty()) container.setItem(slot, ItemStack.EMPTY);
        }
        container.setChanged();
        return remaining == 0;
    }

    public static int depositContainer(Container container, int amount) {
        int remaining = Math.max(0, amount);
        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                int inserted = Math.min(remaining, ModItems.CREDIT_CHIP.get().getDefaultMaxStackSize());
                container.setItem(slot, new ItemStack(ModItems.CREDIT_CHIP.get(), inserted));
                remaining -= inserted;
            } else if (stack.is(ModItems.CREDIT_CHIP.get()) && stack.getCount() < stack.getMaxStackSize()) {
                int inserted = Math.min(remaining, stack.getMaxStackSize() - stack.getCount());
                stack.grow(inserted);
                remaining -= inserted;
            }
        }
        int inserted = Math.max(0, amount) - remaining;
        if (inserted > 0) container.setChanged();
        return inserted;
    }

    private static int count(List<ItemStack> stacks) {
        int total = 0;
        for (ItemStack stack : stacks) {
            if (stack.is(ModItems.CREDIT_CHIP.get())) total = Math.addExact(total, stack.getCount());
        }
        return total;
    }

    private static boolean withdraw(List<ItemStack> stacks, int amount) {
        if (count(stacks) < amount) return false;
        int remaining = amount;
        for (int slot = 0; slot < stacks.size() && remaining > 0; slot++) {
            ItemStack stack = stacks.get(slot);
            if (!stack.is(ModItems.CREDIT_CHIP.get())) continue;
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
            if (stack.isEmpty()) stacks.set(slot, ItemStack.EMPTY);
        }
        return remaining == 0;
    }
}
