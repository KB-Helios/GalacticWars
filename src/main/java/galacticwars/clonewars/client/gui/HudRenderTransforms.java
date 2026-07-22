package galacticwars.clonewars.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Applies the accessibility HUD scale to text as well as panel geometry. */
final class HudRenderTransforms {
    private HudRenderTransforms() {
    }

    static void text(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int x,
            int y,
            int color,
            double scale
    ) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale((float) scale, (float) scale);
        graphics.text(font, text, 0, 0, color);
        graphics.pose().popMatrix();
    }

    static void centeredText(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int centerX,
            int y,
            int color,
            double scale
    ) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, y);
        graphics.pose().scale((float) scale, (float) scale);
        graphics.centeredText(font, text, 0, 0, color);
        graphics.pose().popMatrix();
    }
}
