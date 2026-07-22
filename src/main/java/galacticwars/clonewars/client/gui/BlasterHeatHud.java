package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.combat.BlasterHeatPolicy;
import galacticwars.clonewars.client.ClientConfig;
import galacticwars.clonewars.combat.BlasterItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class BlasterHeatHud {
    private static final int SEGMENT_COUNT = BlasterHeatPolicy.SHOTS_BEFORE_OVERHEAT;
    private static final int SEGMENT_WIDTH = 8;
    private static final int SEGMENT_GAP = 2;
    private static final int BAR_HEIGHT = 5;

    private BlasterHeatHud() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        ItemStack weapon = heldBlaster(minecraft);
        if (weapon.isEmpty()) {
            return;
        }

        BlasterHeatPolicy.BlasterHeatState heat = BlasterItem.heat(weapon);
        boolean overheated = heat.overheatTicks() > 0;
        int heatedSegments = overheated ? SEGMENT_COUNT : SEGMENT_COUNT - heat.shotsRemaining();
        double scale = ClientConfig.HUD_SCALE_PERCENT.get() / 100.0D;
        int segmentWidth = (int) Math.round(SEGMENT_WIDTH * scale);
        int segmentGap = (int) Math.round(SEGMENT_GAP * scale);
        int barHeight = (int) Math.round(BAR_HEIGHT * scale);
        int width = SEGMENT_COUNT * segmentWidth + (SEGMENT_COUNT - 1) * segmentGap;
        int left = (graphics.guiWidth() - width) / 2 + ClientConfig.HUD_HORIZONTAL_OFFSET.get();
        int top = graphics.guiHeight() - 39 + ClientConfig.HUD_VERTICAL_OFFSET.get();
        int activeColor = overheated ? 0xFFFF3D32 : heat.heatFraction() >= 0.66F ? 0xFFFFA726 : 0xFF35C9FF;

        int padding = (int) Math.round(3 * scale);
        int verticalPadding = (int) Math.round(2 * scale);
        graphics.fill(left - padding, top - verticalPadding, left + width + padding, top + barHeight + verticalPadding, 0xAA080C12);
        for (int segment = 0; segment < SEGMENT_COUNT; segment++) {
            int x = left + segment * (segmentWidth + segmentGap);
            graphics.fill(x, top, x + segmentWidth, top + barHeight,
                    segment < heatedSegments ? activeColor : 0xFF34404D);
        }

        Component label = Component.translatable(overheated
                ? "hud.galacticwars.blaster.overheated"
                : "hud.galacticwars.blaster.heat");
        int labelOffset = (int) Math.round(10 * scale);
        HudRenderTransforms.centeredText(graphics, minecraft.font, label,
                graphics.guiWidth() / 2 + ClientConfig.HUD_HORIZONTAL_OFFSET.get(),
                top - labelOffset, overheated ? 0xFFFF665C : 0xFFD7E7F5, scale);
    }

    private static ItemStack heldBlaster(Minecraft minecraft) {
        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (mainHand.getItem() instanceof BlasterItem) {
            return mainHand;
        }
        ItemStack offHand = minecraft.player.getOffhandItem();
        return offHand.getItem() instanceof BlasterItem ? offHand : ItemStack.EMPTY;
    }
}
