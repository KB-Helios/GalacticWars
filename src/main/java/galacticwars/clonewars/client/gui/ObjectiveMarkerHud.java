package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.client.ObjectiveMarkerClientState;
import galacticwars.clonewars.client.ClientConfig;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Dimension-aware direction, distance, and next-action campaign guidance. */
public final class ObjectiveMarkerHud {
    private ObjectiveMarkerHud() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var state = ObjectiveMarkerClientState.snapshot();
        if (player == null || !state.active()) {
            return;
        }
        Component action = Component.translatable(
                "screen.galacticwars.operations.objective." + state.objectiveId());
        int offsetX = ClientConfig.HUD_HORIZONTAL_OFFSET.get();
        int offsetY = ClientConfig.HUD_VERTICAL_OFFSET.get();
        double scale = ClientConfig.HUD_SCALE_PERCENT.get() / 100.0D;
        int primaryColor = ClientConfig.HIGH_CONTRAST.get() ? 0xFFFFFFFF : 0xFFE8EEF7;
        int actionY = (int) Math.round(12 * scale);
        drawCentered(graphics, action, actionY + offsetY, offsetX, primaryColor, scale);
        if (!state.targetKnown()) {
            return;
        }
        String currentDimension = player.level().dimension().identifier().toString();
        int markerY = (int) Math.round(24 * scale);
        if (!currentDimension.equals(state.dimensionId())) {
            drawCentered(graphics,
                    Component.translatable("hud.galacticwars.objective.travel",
                            dimensionName(state.dimensionId())), markerY + offsetY, offsetX,
                    0xFFFFC96B, scale);
            return;
        }
        double dx = state.x() + 0.5D - player.getX();
        double dz = state.z() + 0.5D - player.getZ();
        int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double relative = wrapDegrees(targetYaw - player.getYRot());
        String arrow = Math.abs(relative) < 22.5D ? "↑"
                : relative >= 157.5D || relative < -157.5D ? "↓"
                : relative >= 112.5D ? "↙" : relative >= 67.5D ? "←"
                : relative >= 22.5D ? "↖" : relative >= -22.5D ? "↑"
                : relative >= -67.5D ? "↗" : relative >= -112.5D ? "→" : "↘";
        drawCentered(graphics,
                Component.translatable("hud.galacticwars.objective.marker", arrow, distance),
                markerY + offsetY, offsetX, 0xFFFFC96B, scale);
    }

    private static Component dimensionName(String dimensionId) {
        String path = dimensionId.substring(dimensionId.indexOf(':') + 1)
                .toLowerCase(Locale.ROOT);
        return dimensionId.startsWith("galacticwars:")
                ? Component.translatable("planet.galacticwars." + path)
                : Component.translatable("dimension." + dimensionId.replace(':', '.'));
    }

    private static void drawCentered(
            GuiGraphicsExtractor graphics, Component text, int y, int offsetX, int color,
            double scale
    ) {
        HudRenderTransforms.centeredText(graphics, Minecraft.getInstance().font, text,
                graphics.guiWidth() / 2 + offsetX, y, color, scale);
    }

    private static double wrapDegrees(double value) {
        double wrapped = value % 360.0D;
        if (wrapped >= 180.0D) wrapped -= 360.0D;
        if (wrapped < -180.0D) wrapped += 360.0D;
        return wrapped;
    }
}
