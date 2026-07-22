package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.client.ForceClientState;
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
        int width = Math.min(222, Math.max(174, graphics.guiWidth() / 3));
        boolean hasFailure = !state.failureReason().isBlank();
        int height = hasFailure ? 69 : 57;
        int left = 12;
        int top = Math.max(4, graphics.guiHeight() - height - 12);
        int accent = switch (state.tradition()) {
            case "sith" -> 0xFFE34848;
            case "nightsister" -> 0xFF56D98C;
            default -> 0xFF7F8FFF;
        };
        graphics.fill(left, top, left + width, top + height, 0xDD080C12);
        graphics.fill(left, top, left + width, top + 1, accent);
        graphics.text(Minecraft.getInstance().font,
                Component.literal(display(state.tradition()) + "  R" + state.rank()
                        + "  XP " + state.masteryExperience() + "/" + nextRank(state.rank())
                        + (state.unspentPoints() > 0 ? "  +" + state.unspentPoints() : "")),
                left + 5, top + 5, 0xFFF3F5FF);
        graphics.fill(left + 5, top + 16, left + width - 5, top + 22, 0xFF202632);
        int energyWidth = (width - 10) * state.energy() / 100;
        graphics.fill(left + 5, top + 16, left + 5 + energyWidth, top + 22, accent);
        graphics.text(Minecraft.getInstance().font,
                Component.literal(Integer.toString(state.energy())),
                left + width - 5 - Minecraft.getInstance().font.width(Integer.toString(state.energy())),
                top + 14, 0xFFF5F7FF);
        for (int slot = 0; slot < 3; slot++) {
            int cellLeft = left + 5 + slot * ((width - 10) / 3);
            int cellRight = left + 5 + (slot + 1) * ((width - 10) / 3) - 2;
            boolean active = state.activeSlot() == slot && state.activeMode() != 0;
            graphics.fill(cellLeft, top + 27, cellRight, top + 52,
                    active ? 0xAA48546C : 0xAA171C26);
            int cooldown = cooldown(state, slot);
            String ability = slot < state.abilities().size() ? state.abilities().get(slot) : "empty";
            drawAbilityBadge(graphics, cellLeft + 3, top + 30, ability, accent,
                    cooldown > 0);
            String cast = active ? state.activeMode() == 2 ? "CHN" : "CHG" : "";
            String status = cooldown > 0 ? ((cooldown + 19) / 20) + "s" : cast;
            graphics.text(Minecraft.getInstance().font,
                    Component.literal(KEYS[slot] + " " + compact(ability)),
                    cellLeft + 19, top + 29,
                    cooldown > 0 ? 0xFF9BA3B2 : 0xFFF1F3FA);
            if (!status.isBlank()) {
                graphics.text(Minecraft.getInstance().font, Component.literal(status),
                        cellLeft + 19, top + 40, active ? accent : 0xFFC2C7D1);
            }
            if (active) {
                int progress = Math.min(cellRight - cellLeft,
                        (cellRight - cellLeft) * state.activeTicks() / 100);
                graphics.fill(cellLeft, top + 50, cellLeft + progress, top + 52, accent);
            } else {
                graphics.fill(cellLeft, top + 50, cellRight, top + 52,
                        state.targetValid(slot) ? 0xFF49D17D : 0xFF9B3943);
            }
        }
        if (hasFailure) {
            graphics.text(Minecraft.getInstance().font,
                    Component.literal(trimToWidth(friendlyFailure(state.failureReason()), width - 10)),
                    left + 5, top + 57, 0xFFFFA4A4);
        }
    }

    private static void drawAbilityBadge(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            String ability,
            int accent,
            boolean coolingDown
    ) {
        int color = coolingDown ? 0xFF59606D : abilityColor(ability, accent);
        graphics.fill(left, top, left + 13, top + 13, 0xFF080B10);
        graphics.fill(left + 1, top + 1, left + 12, top + 12, color);
        String glyph = compact(ability);
        glyph = glyph.length() > 2 ? glyph.substring(0, 2) : glyph;
        int glyphLeft = left + 7 - Minecraft.getInstance().font.width(glyph) / 2;
        graphics.text(Minecraft.getInstance().font, Component.literal(glyph),
                glyphLeft, top + 2, 0xFFFFFFFF);
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
