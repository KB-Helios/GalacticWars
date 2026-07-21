package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.menu.RecruitLoadoutMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Compact in-world equipment and cargo screen backed by the live recruit menu. */
public final class RecruitLoadoutScreen extends AbstractContainerScreen<RecruitLoadoutMenu> {
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 194;
    private static final int SCREEN_SHADE = 0x98070B12;
    private static final int PANEL_COLOR = 0xF018202B;
    private static final int PANEL_BORDER = 0xFF53657A;
    private static final int SLOT_BORDER = 0xFF65788C;
    private static final int SLOT_BACKGROUND = 0xFF0D141D;
    private static final int LABEL_COLOR = 0xFFE8EEF5;
    private static final int MUTED_COLOR = 0xFFAAB7C4;

    public RecruitLoadoutScreen(
            RecruitLoadoutMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.inventoryLabelY = 100;
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        graphics.fill(0, 0, this.width, this.height, SCREEN_SHADE);
        graphics.fill(
                this.leftPos,
                this.topPos,
                this.leftPos + this.imageWidth,
                this.topPos + this.imageHeight,
                PANEL_COLOR);
        this.drawBorder(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, SCREEN_SHADE);
        graphics.fill(
                this.leftPos,
                this.topPos,
                this.leftPos + this.imageWidth,
                this.topPos + this.imageHeight,
                PANEL_COLOR);
        this.drawBorder(graphics);
        for (Slot slot : this.menu.slots) {
            this.drawSlotFrame(graphics, slot);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY,
                LABEL_COLOR, false);
        graphics.text(
                this.font,
                Component.translatable("screen.galacticwars.recruit.loadout.cargo"),
                8,
                51,
                MUTED_COLOR,
                false);
        graphics.text(
                this.font,
                this.playerInventoryTitle,
                this.inventoryLabelX,
                this.inventoryLabelY,
                MUTED_COLOR,
                false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawBorder(GuiGraphicsExtractor graphics) {
        int right = this.leftPos + this.imageWidth;
        int bottom = this.topPos + this.imageHeight;
        graphics.fill(this.leftPos, this.topPos, right, this.topPos + 1, PANEL_BORDER);
        graphics.fill(this.leftPos, bottom - 1, right, bottom, PANEL_BORDER);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + 1, bottom, PANEL_BORDER);
        graphics.fill(right - 1, this.topPos, right, bottom, PANEL_BORDER);
    }

    private void drawSlotFrame(GuiGraphicsExtractor graphics, Slot slot) {
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_BORDER);
        graphics.fill(x, y, x + 16, y + 16, SLOT_BACKGROUND);
    }
}
