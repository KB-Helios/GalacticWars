package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.menu.MerchantTradeMenu;
import galacticwars.clonewars.progression.LaunchContentCatalog;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.UUID;
import galacticwars.clonewars.network.MenuActionPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class MerchantTradeScreen extends Screen implements MenuAccess<MerchantTradeMenu> {
    private final MerchantTradeMenu menu;

    public MerchantTradeScreen(MerchantTradeMenu menu, Inventory inventory, Component title) {
        super(title);
        this.menu = menu;
    }

    @Override
    protected void init() {
        int width = 220;
        int x = (this.width - width) / 2;
        int y = Math.max(24, (this.height - menu.tradeIds().size() * 22) / 2);
        for (int index = 0; index < menu.tradeIds().size(); index++) {
            String tradeId = menu.tradeIds().get(index);
            LaunchContentDefinitions.TradeDefinition trade = LaunchContentCatalog.trades().get(tradeId);
            Component label = trade == null
                    ? Component.literal(tradeId)
                    : Component.translatable("screen.galacticwars.trade.offer",
                            Component.translatable("item." + trade.itemId().replace(':', '.')),
                            trade.itemCount(), trade.price());
            int buttonId = index;
            this.addRenderableWidget(Button.builder(label, button -> {
                        ClientPacketDistributor.sendToServer(new MenuActionPayload(
                                UUID.randomUUID(), menu.containerId, buttonId));
                    }).bounds(x, y + index * 22, width, 20).build());
        }
    }

    @Override
    public MerchantTradeMenu getMenu() {
        return menu;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
