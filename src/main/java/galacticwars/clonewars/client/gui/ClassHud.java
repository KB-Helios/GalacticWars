package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.client.ClassClientState;
import galacticwars.clonewars.client.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Compact class resource, rank and active-slot HUD. */
public final class ClassHud {
    private ClassHud() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (!ClassClientState.visible() || Minecraft.getInstance().player == null) {
            return;
        }
        var state = ClassClientState.snapshot();
        double scale = ClientConfig.HUD_SCALE_PERCENT.get() / 100.0D;
        int baseWidth = 100;
        int width = (int) Math.round(baseWidth * scale);
        int baseLeft = 12;
        int baseTopOffset = -56;
        int left = baseLeft + ClientConfig.HUD_HORIZONTAL_OFFSET.get();
        int top = graphics.guiHeight() + baseTopOffset + ClientConfig.HUD_VERTICAL_OFFSET.get();
        int barHeight = (int) Math.round(8 * scale);
        int barPadding = (int) Math.round(1 * scale);
        int borderWidth = (int) Math.round(2 * scale);
        graphics.fill(left, top, left + width + borderWidth, top + barHeight, 0xAA080C12);
        graphics.fill(left + barPadding, top + barPadding,
                left + barPadding + (width * state.resource() / 100), top + barHeight - barPadding, 0xFFFFB84D);
        int labelOffsetAbove = (int) Math.round(11 * scale);
        int labelOffsetBelow = (int) Math.round(10 * scale);
        HudRenderTransforms.text(graphics, Minecraft.getInstance().font,
                Component.translatable(
                        "hud.galacticwars.class",
                        humanize(state.classId()),
                        state.rank(),
                        state.resource(),
                        slot(state.ability1Id(), state.cooldown1()),
                        state.ability2Id().isBlank() && state.rank() < 3
                                ? Component.translatable(
                                        "hud.galacticwars.class.locked_rank", 3).getString()
                                : slot(state.ability2Id(), state.cooldown2())),
                left, top - labelOffsetAbove, 0xFFFFE4B8, scale);
        if (state.experienceForNextRank() > 0L) {
            HudRenderTransforms.text(graphics, Minecraft.getInstance().font,
                    Component.translatable("hud.galacticwars.class.progress",
                            state.experience(), state.experienceForNextRank(),
                            state.nextMilestoneRank()),
                    left, top + labelOffsetBelow, 0xFFB8C5D6, scale);
        }
    }

    private static String slot(String abilityId, int cooldown) {
        return abilityId.isBlank() ? "-" : humanize(abilityId) + " " + cooldown;
    }

    private static String humanize(String identifier) {
        String path = identifier.substring(identifier.indexOf(':') + 1);
        String[] words = path.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
