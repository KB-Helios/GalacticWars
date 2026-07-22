package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.client.ForceClientState;
import galacticwars.clonewars.client.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Compact responsive Force career HUD with energy, mastery, loadout, and cast state. */
public final class ForceHud {
    private static final String[] KEYS = {"Z", "X", "C"};

    private ForceHud() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (!ForceClientState.visible() || Minecraft.getInstance().player == null) return;
        var state = ForceClientState.snapshot();
        double scale = ClientConfig.HUD_SCALE_PERCENT.get() / 100.0D;
        int baseWidth = Math.min(222, Math.max(174, graphics.guiWidth() / 3));
        int width = (int) Math.round(Math.min(333, Math.max(87, baseWidth * scale)));
        boolean hasFailure = !state.failureReason().isBlank();
        int baseHeight = hasFailure ? 69 : 57;
        int height = (int) Math.round(baseHeight * scale);
        int left = 12 + ClientConfig.HUD_HORIZONTAL_OFFSET.get();
        int top = Math.max(4, graphics.guiHeight() - height - 12 + ClientConfig.HUD_VERTICAL_OFFSET.get());
        int accent = switch (state.tradition()) {
            case "sith" -> 0xFFE34848;
            case "nightsister" -> 0xFF56D98C;
            default -> 0xFF7F8FFF;
        };
        graphics.fill(left, top, left + width, top + height, 0xDD080C12);
        int borderHeight = Math.max(1, (int) Math.round(1 * scale));
        graphics.fill(left, top, left + width, top + borderHeight, accent);
        int padding = (int) Math.round(5 * scale);
        int headerY = (int) Math.round(5 * scale);
        HudRenderTransforms.text(graphics, Minecraft.getInstance().font,
                Component.literal(display(state.tradition()) + "  R" + state.rank()
                        + "  XP " + state.masteryExperience() + "/" + nextRank(state.rank())
                        + (state.unspentPoints() > 0 ? "  +" + state.unspentPoints() : "")),
                left + padding, top + headerY, 0xFFF3F5FF, scale);
        int energyTop = (int) Math.round(16 * scale);
        int energyBottom = (int) Math.round(22 * scale);
        graphics.fill(left + padding, top + energyTop, left + width - padding, top + energyBottom, 0xFF202632);
        int energyWidth = (width - 2 * padding) * state.energy() / 100;
        graphics.fill(left + padding, top + energyTop, left + padding + energyWidth, top + energyBottom, accent);
        int energyLabelY = (int) Math.round(14 * scale);
        int energyLabelWidth = (int) Math.round(
                Minecraft.getInstance().font.width(Integer.toString(state.energy())) * scale);
        HudRenderTransforms.text(graphics, Minecraft.getInstance().font,
                Component.literal(Integer.toString(state.energy())),
                left + width - padding - energyLabelWidth,
                top + energyLabelY, 0xFFF5F7FF, scale);
        int cellWidth = (width - 2 * padding) / 3;
        int cellGap = (int) Math.round(2 * scale);
        for (int slot = 0; slot < 3; slot++) {
            int cellLeft = left + padding + slot * cellWidth;
            int cellRight = cellLeft + cellWidth - cellGap;
            int cellTop = (int) Math.round(27 * scale);
            int cellBottom = (int) Math.round(52 * scale);
            boolean active = state.activeSlot() == slot && state.activeMode() != 0;
            graphics.fill(cellLeft, top + cellTop, cellRight, top + cellBottom,
                    active ? 0xAA48546C : 0xAA171C26);
            int cooldown = cooldown(state, slot);
            String ability = slot < state.abilities().size() ? state.abilities().get(slot) : "empty";
            int badgeOffset = (int) Math.round(3 * scale);
            int badgeY = (int) Math.round(30 * scale);
            drawAbilityBadge(graphics, cellLeft + badgeOffset, top + badgeY, ability, accent,
                    cooldown > 0, scale);
            String cast = active ? state.activeMode() == 2 ? "CHN" : "CHG" : "";
            String status = cooldown > 0 ? ((cooldown + 19) / 20) + "s" : cast;
            int textOffset = (int) Math.round(19 * scale);
            int labelY = (int) Math.round(29 * scale);
            HudRenderTransforms.text(graphics, Minecraft.getInstance().font,
                    Component.literal(KEYS[slot] + " " + compact(ability)),
                    cellLeft + textOffset, top + labelY,
                    cooldown > 0 ? 0xFF9BA3B2 : 0xFFF1F3FA, scale);
            if (!status.isBlank()) {
                int statusY = (int) Math.round(40 * scale);
                HudRenderTransforms.text(graphics, Minecraft.getInstance().font,
                        Component.literal(status), cellLeft + textOffset, top + statusY,
                        active ? accent : 0xFFC2C7D1, scale);
            }
            int progressTop = (int) Math.round(50 * scale);
            if (active) {
                int progress = Math.min(cellRight - cellLeft,
                        (cellRight - cellLeft) * state.activeTicks() / 100);
                graphics.fill(cellLeft, top + progressTop, cellLeft + progress, top + cellBottom, accent);
            } else {
                graphics.fill(cellLeft, top + progressTop, cellRight, top + cellBottom,
                        state.targetValid(slot) ? 0xFF49D17D : 0xFF9B3943);
            }
        }
        if (hasFailure) {
            int failureY = (int) Math.round(57 * scale);
            int unscaledWidth = (int) Math.floor((width - 2 * padding) / scale);
            HudRenderTransforms.text(graphics, Minecraft.getInstance().font,
                    Component.literal(trimToWidth(
                            friendlyFailure(state.failureReason()), unscaledWidth)),
                    left + padding, top + failureY, 0xFFFFA4A4, scale);
        }
    }

    private static void drawAbilityBadge(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            String ability,
            int accent,
            boolean coolingDown,
            double scale
    ) {
        int badgeSize = (int) Math.round(13 * scale);
        int badgePadding = Math.max(1, (int) Math.round(1 * scale));
        int color = coolingDown ? 0xFF59606D : abilityColor(ability, accent);
        graphics.fill(left, top, left + badgeSize, top + badgeSize, 0xFF080B10);
        graphics.fill(left + badgePadding, top + badgePadding, left + badgeSize - badgePadding, top + badgeSize - badgePadding, color);
        String glyph = compact(ability);
        glyph = glyph.length() > 2 ? glyph.substring(0, 2) : glyph;
        int glyphCenter = (int) Math.round(7 * scale);
        int glyphY = (int) Math.round(2 * scale);
        int glyphWidth = (int) Math.round(Minecraft.getInstance().font.width(glyph) * scale);
        int glyphLeft = left + glyphCenter - glyphWidth / 2;
        HudRenderTransforms.text(graphics, Minecraft.getInstance().font,
                Component.literal(glyph), glyphLeft, top + glyphY, 0xFFFFFFFF, scale);
    }

    private static int abilityColor(String ability, int accent) {
        if (ability.equals("empty")) return 0xFF363C48;
        if (ability.contains("heal") || ability.contains("weave") || ability.contains("valor")) {
            return 0xFF3FAF73;
        }
        if (ability.contains("lightning") || ability.contains("storm") || ability.contains("ichor")) {
            return 0xFF7754D9;
        }
        if (ability.contains("guard") || ability.contains("ward") || ability.contains("stasis")) {
            return 0xFF3B78C5;
        }
        if (ability.contains("choke") || ability.contains("crush") || ability.contains("hex")) {
            return 0xFFA93E55;
        }
        return accent;
    }

    private static int cooldown(galacticwars.clonewars.network.ForceHudPayload state, int slot) {
        return switch (slot) {
            case 0 -> state.cooldown1();
            case 1 -> state.cooldown2();
            default -> state.cooldown3();
        };
    }

    private static String compact(String value) {
        String[] words = value.replace("_", " ").split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) result.append(Character.toUpperCase(word.charAt(0)));
        }
        return result.length() > 4 ? result.substring(0, 4) : result.toString();
    }

    private static String display(String value) {
        return value.isBlank() ? "Force" : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String friendlyFailure(String value) {
        String source = value.startsWith("force_") ? value.substring(6) : value;
        String[] words = source.replace('_', ' ').split(" ");
        StringBuilder result = new StringBuilder("Cannot cast: ");
        for (int index = 0; index < words.length; index++) {
            if (index > 0) result.append(' ');
            result.append(words[index]);
        }
        return result.toString();
    }

    private static String trimToWidth(String value, int maximumWidth) {
        String result = value;
        while (result.length() > 3 && Minecraft.getInstance().font.width(result) > maximumWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result.equals(value) ? result : result.substring(0, Math.max(0, result.length() - 3)) + "...";
    }

    private static int nextRank(int rank) {
        int[] thresholds = {0, 10, 25, 45, 70, 100, 140, 190, 250, 320};
        return rank >= thresholds.length ? thresholds[thresholds.length - 1] : thresholds[Math.max(1, rank)];
    }
}
