package middleearth.lotr.warmod.client.gui;

import middleearth.lotr.warmod.entity.MiddleEarthRecruitEntity;
import middleearth.lotr.warmod.menu.RecruitCommandMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class RecruitCommandScreen extends Screen implements MenuAccess<RecruitCommandMenu> {
    private static final int BUTTON_WIDTH = 118;
    private static final int BUTTON_HEIGHT = 18;
    private static final int GAP = 2;
    private static final int COLUMN_GAP = 12;
    private static final int STATUS_COLOR = 0xE0E0E0;
    private static final int STATUS_MUTED_COLOR = 0x9CA3AF;
    private static final String[] PROFESSION_TRANSLATION_KEYS = {
            "screen.kingdomwarsmiddleearth.recruit.profession.farmer",
            "screen.kingdomwarsmiddleearth.recruit.profession.lumberjack",
            "screen.kingdomwarsmiddleearth.recruit.profession.fisherman",
            "screen.kingdomwarsmiddleearth.recruit.profession.animal_farmer",
            "screen.kingdomwarsmiddleearth.recruit.profession.miner",
            "screen.kingdomwarsmiddleearth.recruit.profession.builder",
            "screen.kingdomwarsmiddleearth.recruit.profession.cook",
            "screen.kingdomwarsmiddleearth.recruit.profession.merchant",
            "screen.kingdomwarsmiddleearth.recruit.profession.courier"
    };

    private final RecruitCommandMenu menu;

    public RecruitCommandScreen(RecruitCommandMenu menu, Inventory inventory, Component title) {
        super(title);
        this.menu = menu;
    }

    @Override
    protected void init() {
        super.init();
        Entity entity = (this.minecraft != null && this.minecraft.level != null)
                ? this.minecraft.level.getEntity(this.menu.recruitEntityId())
                : null;
        boolean ownedByPlayer = entity instanceof MiddleEarthRecruitEntity recruit
                && this.minecraft != null
                && this.minecraft.player != null
                && recruit.isOwnedBy(this.minecraft.player);
        boolean tame = entity instanceof MiddleEarthRecruitEntity recruit && recruit.isTame();

        int x = (this.width - BUTTON_WIDTH) / 2;
        int y = Math.max(8, (this.height - ((ownedByPlayer ? 13 : 1) * (BUTTON_HEIGHT + GAP))) / 2);
        if (!tame) {
            this.addButton(x, y, "screen.kingdomwarsmiddleearth.recruit.hire", RecruitCommandMenu.BUTTON_HIRE);
            return;
        }
        if (!ownedByPlayer) {
            this.addRenderableWidget(Button.builder(
                            Component.translatable("screen.kingdomwarsmiddleearth.recruit.locked"),
                            button -> this.onClose())
                    .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
            return;
        }

        int commandX = (this.width - (BUTTON_WIDTH * 2 + COLUMN_GAP)) / 2;
        int professionX = commandX + BUTTON_WIDTH + COLUMN_GAP;

        this.addButton(commandX, y, "screen.kingdomwarsmiddleearth.recruit.follow", RecruitCommandMenu.BUTTON_FOLLOW);
        this.addButton(commandX, y + (BUTTON_HEIGHT + GAP), "screen.kingdomwarsmiddleearth.recruit.hold", RecruitCommandMenu.BUTTON_HOLD);
        this.addButton(commandX, y + 2 * (BUTTON_HEIGHT + GAP), "screen.kingdomwarsmiddleearth.recruit.move", RecruitCommandMenu.BUTTON_MOVE);
        this.addButton(commandX, y + 3 * (BUTTON_HEIGHT + GAP), "screen.kingdomwarsmiddleearth.recruit.protect", RecruitCommandMenu.BUTTON_PROTECT);
        this.addButton(commandX, y + 4 * (BUTTON_HEIGHT + GAP), "screen.kingdomwarsmiddleearth.recruit.attack", RecruitCommandMenu.BUTTON_ATTACK);
        this.addButton(commandX, y + 5 * (BUTTON_HEIGHT + GAP), "screen.kingdomwarsmiddleearth.recruit.clear", RecruitCommandMenu.BUTTON_CLEAR);
        this.addButton(commandX, y + 6 * (BUTTON_HEIGHT + GAP), "screen.kingdomwarsmiddleearth.recruit.worksite.set", RecruitCommandMenu.BUTTON_SET_WORKSITE);
        this.addButton(commandX, y + 7 * (BUTTON_HEIGHT + GAP), "screen.kingdomwarsmiddleearth.recruit.worksite.return", RecruitCommandMenu.BUTTON_RETURN_WORKSITE);
        this.addButton(commandX, y + 8 * (BUTTON_HEIGHT + GAP), "screen.kingdomwarsmiddleearth.recruit.worksite.clear", RecruitCommandMenu.BUTTON_CLEAR_WORKSITE);
        this.addButton(commandX, y + 9 * (BUTTON_HEIGHT + GAP), "screen.kingdomwarsmiddleearth.recruit.storage.set", RecruitCommandMenu.BUTTON_SET_STORAGE);
        this.addButton(commandX, y + 10 * (BUTTON_HEIGHT + GAP), "screen.kingdomwarsmiddleearth.recruit.base.starter_keep", RecruitCommandMenu.BUTTON_BUILD_STARTER_KEEP);
        this.addButton(commandX, y + 11 * (BUTTON_HEIGHT + GAP), "screen.kingdomwarsmiddleearth.recruit.worksite.radius.decrease", RecruitCommandMenu.BUTTON_WORK_RADIUS_DECREASE);
        this.addButton(commandX, y + 12 * (BUTTON_HEIGHT + GAP), "screen.kingdomwarsmiddleearth.recruit.worksite.radius.increase", RecruitCommandMenu.BUTTON_WORK_RADIUS_INCREASE);

        int[] workerProfessionButtonIds = RecruitCommandMenu.workerProfessionButtonIds();
        for (int i = 0; i < workerProfessionButtonIds.length; i++) {
            this.addButton(
                    professionX,
                    y + i * (BUTTON_HEIGHT + GAP),
                    PROFESSION_TRANSLATION_KEYS[i],
                    workerProfessionButtonIds[i]);
        }
    }

    @Override
    public RecruitCommandMenu getMenu() {
        return this.menu;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (this.minecraft != null && this.minecraft.level != null) {
            Entity entity = this.minecraft.level.getEntity(this.menu.recruitEntityId());
            if (entity instanceof MiddleEarthRecruitEntity recruit) {
                this.drawRecruitStatusPanel(graphics, recruit);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
        super.onClose();
    }

    private void addButton(int x, int y, String translationKey, int buttonId) {
        this.addRenderableWidget(Button.builder(Component.translatable(translationKey), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
                    }
                    this.onClose();
                })
                .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void drawRecruitStatusPanel(GuiGraphicsExtractor graphics, MiddleEarthRecruitEntity recruit) {
        int x = Math.max(8, (this.width - (BUTTON_WIDTH * 2 + COLUMN_GAP)) / 2);
        int y = 8;
        graphics.text(this.font, Component.translatable("screen.kingdomwarsmiddleearth.recruit.status.title"), x, y, STATUS_COLOR);
        List<Component> statusLines = recruit.recruitStatusLines();
        int lineY = y + 12;
        for (Component line : statusLines) {
            graphics.text(this.font, line, x, lineY, STATUS_MUTED_COLOR);
            lineY += 10;
        }
    }
}
