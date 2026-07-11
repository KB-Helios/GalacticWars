package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.menu.FactionSelectionMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FactionSelectionScreen extends Screen implements MenuAccess<FactionSelectionMenu> {
    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 24;
    private static final int GAP = 7;
    private final FactionSelectionMenu menu;

    public FactionSelectionScreen(FactionSelectionMenu menu, Inventory inventory, Component title) {
        super(title);
        this.menu = menu;
    }

    @Override
    protected void init() {
        super.init();
        StringWidget heading = new StringWidget(0, 15, this.width, 16, this.title, this.font);
        heading.setFGColor(0xFFE082);
        this.addRenderableWidget(heading);
        StringWidget subtitle = new StringWidget(
                0, 32, this.width, 14,
                Component.translatable("screen.galacticwars.faction_selection.subtitle"), this.font);
        subtitle.setFGColor(0xD7E7F5);
        this.addRenderableWidget(subtitle);
        StringWidget warning = new StringWidget(
                0, this.height - 21, this.width, 14,
                Component.translatable("screen.galacticwars.faction_selection.warning"), this.font);
        warning.setFGColor(0xAAB7C4);
        this.addRenderableWidget(warning);
        int x = (this.width - BUTTON_WIDTH) / 2;
        int firstY = Math.max(58, (this.height - FactionSelectionMenu.FACTION_IDS.size()
                * (BUTTON_HEIGHT + GAP)) / 2);
        for (int index = 0; index < FactionSelectionMenu.FACTION_IDS.size(); index++) {
            String factionId = FactionSelectionMenu.FACTION_IDS.get(index);
            int buttonId = index;
            this.addRenderableWidget(Button.builder(
                            Component.translatable(FactionSelectionMenu.factionTranslation(factionId)),
                            button -> this.selectFaction(buttonId))
                    .bounds(x, firstY + index * (BUTTON_HEIGHT + GAP), BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }
    }

    private void selectFaction(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    @Override
    public FactionSelectionMenu getMenu() {
        return menu;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
