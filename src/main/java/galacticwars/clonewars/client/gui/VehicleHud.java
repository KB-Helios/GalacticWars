package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.vehicle.GalacticVehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class VehicleHud {
    private VehicleHud() {}
    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.getVehicle() instanceof GalacticVehicleEntity vehicle)) return;
        int left = graphics.guiWidth() - 154;
        int top = graphics.guiHeight() - 38;
        graphics.fill(left, top, left + 142, top + 26, 0xBB080C12);
        graphics.text(minecraft.font, Component.translatable("hud.galacticwars.vehicle",
                vehicle.health(), vehicle.maxHealthForHud(), vehicle.fuel(), vehicle.fuelCapacityForHud()),
                left + 6, top + 8, 0xFFBDEBFF);
    }
}
