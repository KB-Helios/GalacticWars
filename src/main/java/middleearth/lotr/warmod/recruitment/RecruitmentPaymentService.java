package middleearth.lotr.warmod.recruitment;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class RecruitmentPaymentService {
    private RecruitmentPaymentService() {
    }

    public static int emeraldCount(ServerPlayer player) {
        return player.getInventory().getNonEquipmentItems().stream()
                .filter(stack -> stack.is(Items.EMERALD))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    public static boolean withdrawEmeralds(ServerPlayer player, int amount) {
        if (amount < 0) {
            return false;
        }
        if (amount == 0 || player.hasInfiniteMaterials()) {
            return true;
        }
        if (emeraldCount(player) < amount) {
            return false;
        }
        int removed = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.is(Items.EMERALD)) {
                continue;
            }
            int taken = Math.min(amount - removed, stack.getCount());
            stack.shrink(taken);
            removed += taken;
            if (removed == amount) {
                break;
            }
        }
        player.getInventory().setChanged();
        if (removed == amount) {
            return true;
        }
        refundEmeralds(player, removed);
        return false;
    }

    public static void refundEmeralds(ServerPlayer player, int amount) {
        if (amount <= 0 || player.hasInfiniteMaterials()) {
            return;
        }
        ItemStack refund = new ItemStack(Items.EMERALD, amount);
        player.getInventory().add(refund);
        if (!refund.isEmpty() && player.level() instanceof ServerLevel level) {
            player.spawnAtLocation(level, refund);
        }
    }
}
