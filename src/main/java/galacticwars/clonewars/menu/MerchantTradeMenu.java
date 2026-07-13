package galacticwars.clonewars.menu;

import galacticwars.clonewars.economy.PhysicalTradeService;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.registry.ModMenuTypes;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Bounded merchant offers; all costs and rewards are resolved again on the server. */
public final class MerchantTradeMenu extends AbstractContainerMenu {
    public static final int MAX_OFFERS = 32;
    private final Level level;
    private final int merchantEntityId;
    private final List<String> tradeIds;
    private final LinkedHashSet<UUID> processedActionIds = new LinkedHashSet<>();

    public MerchantTradeMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readVarInt(), readOffers(buffer));
    }

    MerchantTradeMenu(int containerId, Inventory inventory, int merchantEntityId, List<String> tradeIds) {
        super(ModMenuTypes.MERCHANT_TRADE.get(), containerId);
        this.level = inventory.player.level();
        this.merchantEntityId = merchantEntityId;
        this.tradeIds = List.copyOf(tradeIds.subList(0, Math.min(MAX_OFFERS, tradeIds.size())));
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        return false;
    }

    public boolean handleReplayAction(ServerPlayer serverPlayer, UUID requestId, int buttonId) {
        if (buttonId < 0 || buttonId >= tradeIds.size()
                || !stillValid(serverPlayer) || !processedActionIds.add(requestId)) {
            return false;
        }
        while (processedActionIds.size() > 64) {
            processedActionIds.remove(processedActionIds.iterator().next());
        }
        PhysicalTradeService.TradeResult result = PhysicalTradeService.purchase(
                serverPlayer, requestId, tradeIds.get(buttonId),
                level.getEntity(merchantEntityId) instanceof GalacticRecruitEntity merchant ? merchant : null);
        serverPlayer.sendSystemMessage(Component.translatable(
                result.accepted() ? "message.galacticwars.trade.accepted" : "message.galacticwars.trade.rejected",
                Component.literal(result.reason())));
        return result.accepted();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        Entity entity = level.getEntity(merchantEntityId);
        return entity instanceof GalacticRecruitEntity recruit
                && recruit.isMerchant()
                && entity.isAlive()
                && player.distanceToSqr(entity) <= 64.0D;
    }

    public List<String> tradeIds() {
        return tradeIds;
    }

    private static List<String> readOffers(RegistryFriendlyByteBuf buffer) {
        int count = Math.min(MAX_OFFERS, Math.max(0, buffer.readVarInt()));
        java.util.ArrayList<String> offers = new java.util.ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            offers.add(buffer.readUtf(128));
        }
        return List.copyOf(offers);
    }
}
