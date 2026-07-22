package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.menu.StarterCampSetupMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact guided onboarding screen backed entirely by bounded menu actions. */
public final class StarterCampSetupScreen extends Screen implements MenuAccess<StarterCampSetupMenu> {
    private static final int PANEL = 0xD018202B;
    private static final int BORDER = 0xFF46566A;
    private static final int TEXT = 0xFFE7EEF5;
    private static final int MUTED = 0xFFAAB7C4;
    private static final int ACCENT = 0xFFFFE082;
    private final StarterCampSetupMenu menu;
    private int selectedRotation;
    private int lastPhaseCode;
    private Component status = Component.translatable("screen.galacticwars.starter_camp.choose_orientation");

    public StarterCampSetupScreen(StarterCampSetupMenu menu, Inventory inventory, Component title) {
        super(title);
        this.menu = menu;
        this.selectedRotation = menu.rotationSteps();
        this.lastPhaseCode = menu.phaseCode();
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new StringWidget(
                0, 14, width, 16, title.copy().withColor(0xFFE082), font));
        addRenderableWidget(new StringWidget(
                0, 32, width, 14,
                Component.translatable("screen.galacticwars.starter_camp.subtitle").withColor(0xD7E7F5), font));

        int panelWidth = Math.min(430, Math.max(260, width - 24));
        int left = (width - panelWidth) / 2;
        int rotationWidth = Math.max(48, (panelWidth - 12) / 4);
        for (int rotation = 0; rotation < 4; rotation++) {
            int option = rotation;
            Button button = Button.builder(rotationLabel(rotation), ignored -> selectRotation(option))
                    .bounds(left + 3 + rotation * rotationWidth, 62, rotationWidth - 3, 20)
                    .build();
            button.active = rotation != selectedRotation && menu.phaseCode() < 3;
            button.setTooltip(Tooltip.create(Component.translatable(
                    "screen.galacticwars.starter_camp.rotation_tooltip", rotation * 90)));
            addRenderableWidget(button);
        }

        int actionsY = Math.min(height - 48, 151);
        int actionWidth = Math.max(52, (panelWidth - 13) / 4);
        Button confirm = Button.builder(
                        Component.translatable("screen.galacticwars.starter_camp.confirm"),
                        ignored -> submit(StarterCampSetupMenu.CONFIRM_DEPLOYMENT, "submitted"))
                .bounds(left + 3, actionsY, actionWidth, 20).build();
        confirm.active = menu.phaseCode() == 0 || menu.phaseCode() == 1 || menu.phaseCode() == 2
                || menu.phaseCode() == 5;
        confirm.setTooltip(Tooltip.create(Component.translatable(
                "screen.galacticwars.starter_camp.confirm_tooltip")));
        addRenderableWidget(confirm);

        Button retry = Button.builder(
                        Component.translatable("screen.galacticwars.starter_camp.retry"),
                        ignored -> submit(StarterCampSetupMenu.RETRY_DEPLOYMENT, "retrying"))
                .bounds(left + 5 + actionWidth, actionsY, actionWidth, 20).build();
        retry.active = menu.phaseCode() == 2 || menu.phaseCode() == 5;
        retry.setTooltip(Tooltip.create(Component.translatable(
                "screen.galacticwars.starter_camp.retry_tooltip")));
        addRenderableWidget(retry);

        Button reassign = Button.builder(
                        Component.translatable("screen.galacticwars.starter_camp.reassign"),
                        ignored -> submit(StarterCampSetupMenu.REASSIGN_BUILDER, "reassigning"))
                .bounds(left + 7 + actionWidth * 2, actionsY, actionWidth, 20).build();
        reassign.active = menu.phaseCode() == 2 || menu.phaseCode() == 3;
        reassign.setTooltip(Tooltip.create(Component.translatable(
                "screen.galacticwars.starter_camp.reassign_tooltip")));
        addRenderableWidget(reassign);

        Button pack = Button.builder(
                        Component.translatable("screen.galacticwars.starter_camp.pack_up"),
                        ignored -> submit(StarterCampSetupMenu.PACK_UP, "packing"))
                .bounds(left + 9 + actionWidth * 3, actionsY, actionWidth, 20).build();
        pack.active = menu.phaseCode() == 2 || menu.phaseCode() == 3;
        pack.setTooltip(Tooltip.create(Component.translatable(
                "screen.galacticwars.starter_camp.pack_up_tooltip")));
        addRenderableWidget(pack);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), ignored -> onClose())
                .bounds(width / 2 - 50, Math.min(height - 24, actionsY + 28), 100, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        int serverRotation = menu.rotationSteps();
        int currentPhase = menu.phaseCode();
        if ((serverRotation != selectedRotation && currentPhase >= 3) || currentPhase != lastPhaseCode) {
            selectedRotation = serverRotation;
            lastPhaseCode = currentPhase;
            rebuildWidgets();
        }
    }

    private void selectRotation(int rotation) {
        selectedRotation = rotation;
        submit(StarterCampSetupMenu.SET_ROTATION_FIRST + rotation, "previewing");
        rebuildWidgets();
    }

    private void submit(int buttonId, String statusKey) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
            status = Component.translatable("screen.galacticwars.starter_camp." + statusKey);
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        int panelWidth = Math.min(430, Math.max(260, width - 24));
        int left = (width - panelWidth) / 2;
        graphics.fill(left, 54, left + panelWidth, 145, PANEL);
        graphics.fill(left, 54, left + panelWidth, 55, BORDER);
        graphics.fill(left, 144, left + panelWidth, 145, BORDER);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Component orientation = Component.translatable(
                "screen.galacticwars.starter_camp.orientation", rotationLabel(selectedRotation));
        graphics.text(font, orientation, left + 8, 90, ACCENT);
        graphics.text(font, Component.translatable(
                        "screen.galacticwars.starter_camp.phase." + phaseKey(menu.phaseCode())),
                left + 8, 104, TEXT);
        graphics.text(font, Component.translatable(
                        "screen.galacticwars.starter_camp.progress",
                        menu.completedPlacements(), menu.totalPlacements()),
                left + 8, 116, TEXT);
        graphics.text(font, Component.translatable(
                        "screen.galacticwars.starter_camp.builder." + builderKey(menu.builderStatus()),
                        menu.storedSupplyItems()),
                left + 8, 128, MUTED);
        Component clipped = font.width(status) <= panelWidth - 16
                ? status
                : Component.literal(font.plainSubstrByWidth(status.getString(), panelWidth - 28) + "...");
        graphics.text(font, clipped, left + panelWidth - font.width(clipped) - 8, 90, MUTED);
    }

    private static Component rotationLabel(int rotation) {
        return Component.translatable("screen.galacticwars.starter_camp.rotation." + switch (rotation) {
            case 1 -> "east";
            case 2 -> "south";
            case 3 -> "west";
            default -> "north";
        });
    }

    private static String phaseKey(int phaseCode) {
        return switch (phaseCode) {
            case 1 -> "awaiting";
            case 2 -> "blocked";
            case 3 -> "building";
            case 4 -> "complete";
            case 5 -> "packed";
            default -> "not_started";
        };
    }

    private static String builderKey(int statusCode) {
        return switch (statusCode) {
            case 1 -> "working";
            case 2 -> "blocked";
            case 3 -> "complete";
            default -> "waiting";
        };
    }

    @Override
    public StarterCampSetupMenu getMenu() {
        return menu;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.closeContainer();
        }
        super.onClose();
    }
}
