package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.client.ClientGameplayCatalog;
import galacticwars.clonewars.client.ClientConfig;
import galacticwars.clonewars.vehicle.GalacticVehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class VehicleHud {
    private VehicleHud() {}
    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.getVehicle() instanceof GalacticVehicleEntity vehicle)) return;
        double scale = ClientConfig.HUD_SCALE_PERCENT.get() / 100.0D;
        int baseWidth = 142;
        int baseHeight = 26;
        int width = (int) Math.round(baseWidth * scale);
        int height = (int) Math.round(baseHeight * scale);
        int left = graphics.guiWidth() - (int) Math.round(154 * scale) + ClientConfig.HUD_HORIZONTAL_OFFSET.get();
        int top = graphics.guiHeight() - (int) Math.round(38 * scale) + ClientConfig.HUD_VERTICAL_OFFSET.get();
        var configured = ClientGameplayCatalog.snapshot().vehicle(vehicle.vehicleId()).orElse(null);
        int maximumHealth = configured == null
                ? vehicle.syncedMaximumHealth()
                : Math.max(vehicle.health(), configured.maxHealth());
        int maximumFuel = configured == null
                ? vehicle.syncedFuelCapacity()
                : Math.max(vehicle.fuel(), configured.fuelCapacity());
        graphics.fill(left, top, left + width, top + height, 0xBB080C12);
        int textPadding = (int) Math.round(6 * scale);
        int textY = (int) Math.round(8 * scale);
        HudRenderTransforms.text(graphics, minecraft.font,
                Component.translatable("hud.galacticwars.vehicle",
                        vehicle.health(), maximumHealth, vehicle.fuel(), maximumFuel),
                left + textPadding, top + textY, 0xFFBDEBFF, scale);
    }
}
