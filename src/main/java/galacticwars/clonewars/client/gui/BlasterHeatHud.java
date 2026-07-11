package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.combat.BlasterHeatPolicy;
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
        int width = SEGMENT_COUNT * SEGMENT_WIDTH + (SEGMENT_COUNT - 1) * SEGMENT_GAP;
        int left = (graphics.guiWidth() - width) / 2;
        int top = graphics.guiHeight() - 39;
        int activeColor = overheated ? 0xFFFF3D32 : heat.heatFraction() >= 0.66F ? 0xFFFFA726 : 0xFF35C9FF;

        graphics.fill(left - 3, top - 2, left + width + 3, top + BAR_HEIGHT + 2, 0xAA080C12);
        for (int segment = 0; segment < SEGMENT_COUNT; segment++) {
            int x = left + segment * (SEGMENT_WIDTH + SEGMENT_GAP);
            graphics.fill(x, top, x + SEGMENT_WIDTH, top + BAR_HEIGHT,
                    segment < heatedSegments ? activeColor : 0xFF34404D);
        }

        Component label = Component.translatable(overheated
                ? "hud.galacticwars.blaster.overheated"
                : "hud.galacticwars.blaster.heat");
        graphics.centeredText(minecraft.font, label, graphics.guiWidth() / 2, top - 10,
                overheated ? 0xFFFF665C : 0xFFD7E7F5);
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
