package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.menu.MerchantTradeMenu;
import galacticwars.clonewars.progression.LaunchContentCatalog;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import java.util.UUID;
import galacticwars.clonewars.network.GalacticNetwork;
import galacticwars.clonewars.network.MenuActionPayload;

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
                            tradeItemName(trade.itemId()),
                            trade.itemCount(), trade.price());
            int buttonId = index;
            this.addRenderableWidget(Button.builder(label, button -> {
                        GalacticNetwork.CHANNEL.sendToServer(new MenuActionPayload(
                                UUID.randomUUID(), menu.containerId, buttonId));
                    }).bounds(x, y + index * 22, width, 20).build());
        }
    }

    private static Component tradeItemName(String itemId) {
        try {
            Identifier id = Identifier.parse(itemId);
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item != null && id.equals(BuiltInRegistries.ITEM.getKey(item))) {
                return Component.translatable(item.getDescriptionId());
            }
        } catch (RuntimeException ignored) {
            // Invalid datapack content is rendered as its raw id instead of crashing the client.
        }
        return Component.literal(itemId);
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
