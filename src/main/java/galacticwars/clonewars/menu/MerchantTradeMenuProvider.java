package galacticwars.clonewars.menu;

import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.progression.LaunchContentCatalog;
import galacticwars.clonewars.progression.ProgressionSavedData;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class MerchantTradeMenuProvider implements MenuProvider {
    private final GalacticRecruitEntity merchant;

    public MerchantTradeMenuProvider(GalacticRecruitEntity merchant) {
        this.merchant = merchant;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.galacticwars.merchant_trade");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MerchantTradeMenu(containerId, inventory, merchant.getId(), offers(player));
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        List<String> offers = menu instanceof MerchantTradeMenu tradeMenu ? tradeMenu.tradeIds() : List.of();
        buffer.writeVarInt(merchant.getId());
        buffer.writeVarInt(offers.size());
        offers.forEach(buffer::writeUtf);
    }

    private List<String> offers(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return List.of();
        }
        String faction = merchant.factionIdForGameplay();
        String path = faction.contains(":") ? faction.substring(faction.indexOf(':') + 1) : faction;
        return LaunchContentCatalog.trades().values().stream()
                .filter(trade -> trade.factionId().equals(path))
                .map(trade -> trade.id())
                .sorted()
                .limit(MerchantTradeMenu.MAX_OFFERS)
                .toList();
    }
}
