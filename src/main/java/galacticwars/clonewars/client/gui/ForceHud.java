package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.client.ForceClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class ForceHud {
    private ForceHud() {}
    public static void render(GuiGraphicsExtractor graphics) {
        if (!ForceClientState.visible() || Minecraft.getInstance().player == null) return;
        var state = ForceClientState.snapshot();
        int width = 100;
        int left = 12;
        int top = graphics.guiHeight() - 34;
        graphics.fill(left, top, left + width + 2, top + 8, 0xAA080C12);
        graphics.fill(left + 1, top + 1, left + 1 + state.energy(), top + 7, 0xFF7F6CFF);
        graphics.text(Minecraft.getInstance().font,
                Component.translatable("hud.galacticwars.force", state.energy(),
                        state.cooldown1(), state.cooldown2(), state.cooldown3()),
                left, top - 11, 0xFFE6E0FF);
    }
}
