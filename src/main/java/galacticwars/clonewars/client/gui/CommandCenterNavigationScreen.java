package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.menu.CommandCenterNavigationMenu;
import galacticwars.clonewars.progression.LaunchContentCatalog;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CommandCenterNavigationScreen extends Screen implements MenuAccess<CommandCenterNavigationMenu> {
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 6;
    private final CommandCenterNavigationMenu menu;

    public CommandCenterNavigationScreen(
            CommandCenterNavigationMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(title);
        this.menu = menu;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - BUTTON_WIDTH) / 2;
        int firstY = Math.max(42, (this.height - LaunchContentCatalog.PLANETS.size()
                * (BUTTON_HEIGHT + GAP)) / 2);
        for (int index = 0; index < LaunchContentCatalog.PLANETS.size(); index++) {
            String planetId = LaunchContentCatalog.PLANETS.get(index);
            int buttonId = index;
            this.addRenderableWidget(Button.builder(
                            Component.translatable("planet.galacticwars." + planetId),
                            button -> this.selectPlanet(buttonId))
                    .bounds(x, firstY + index * (BUTTON_HEIGHT + GAP), BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }
    }

    private void selectPlanet(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
        this.onClose();
    }

    @Override
    public CommandCenterNavigationMenu getMenu() {
        return menu;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(this.font, this.title, (this.width - this.font.width(this.title)) / 2, 16, 0xE5F6FF);
        Component hint = Component.translatable("screen.galacticwars.navigation.hint");
        graphics.text(this.font, hint, (this.width - this.font.width(hint)) / 2, 29, 0x9CA3AF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
        super.onClose();
    }
}
