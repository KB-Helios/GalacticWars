package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.client.ClassClientState;
import galacticwars.clonewars.client.ClientGameplayCatalog;
import galacticwars.clonewars.menu.CommandCenterOperationsMenu;
import galacticwars.clonewars.network.ClassSelectPayload;
import galacticwars.clonewars.network.GalacticNetwork;
import galacticwars.clonewars.network.GameplayCatalogPayload;
import galacticwars.clonewars.progression.PlayerCampaignAttachmentRuntime;
import galacticwars.clonewars.progression.PlayerCampaignAttachmentState;
import galacticwars.clonewars.progression.ProgressionEventType;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Command Center class loadout screen backed by server-validated selection requests. */
public final class PlayerClassScreen extends Screen {
    private static final int PANEL_WIDTH = 390;
    private final Screen parent;
    private String displayedClassId = "";
    private int displayedEligibilityHash;
    private long displayedCatalogRevision;

    public PlayerClassScreen(Screen parent) {
        super(Component.translatable("screen.galacticwars.class.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        var player = Minecraft.getInstance().player;
        var attachment = player == null ? null : PlayerCampaignAttachmentRuntime.get(player);
        String factionId = attachment == null ? "" : attachment.campaign().factionId();
        displayedClassId = ClassClientState.snapshot().classId();
        displayedEligibilityHash = eligibilityHash(attachment);
        ClientGameplayCatalog.Snapshot catalog = ClientGameplayCatalog.snapshot();
        displayedCatalogRevision = catalog.revision();

        List<GameplayCatalogPayload.ClassEntry> classes = catalog.classes().stream()
                .filter(definition -> definition.factionId().equals(factionId))
                .sorted(Comparator.comparing(GameplayCatalogPayload.ClassEntry::displayName))
                .toList();
        int panelWidth = Math.min(PANEL_WIDTH, width - 24);
        int left = (width - panelWidth) / 2;
        int startY = Math.max(54, (height - classes.size() * 28) / 2);
        for (int index = 0; index < classes.size(); index++) {
            GameplayCatalogPayload.ClassEntry definition = classes.get(index);
            boolean available = attachment != null && available(definition, attachment);
            Component label = Component.literal(definition.displayName() + "  •  "
                    + abilitySummary(definition));
            Button button = Button.builder(label, pressed -> GalacticNetwork.CHANNEL.sendToServer(
                            new ClassSelectPayload(UUID.randomUUID(), definition.classId())))
                    .bounds(left, startY + index * 28, panelWidth, 24)
                    .build();
            button.active = available && !definition.classId().equals(displayedClassId);
            if (!available) {
                button.setTooltip(Tooltip.create(Component.translatable(
                        "screen.galacticwars.class.locked")));
            } else if (definition.classId().equals(displayedClassId)) {
                button.setTooltip(Tooltip.create(Component.translatable(
                        "screen.galacticwars.class.current")));
            }
            addRenderableWidget(button);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), pressed -> onClose())
                .bounds((width - 120) / 2, height - 34, 120, 20)
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.containerMenu instanceof CommandCenterOperationsMenu)) {
            minecraft.setScreenAndShow(null);
            return;
        }
        String current = ClassClientState.snapshot().classId();
        PlayerCampaignAttachmentState attachment = PlayerCampaignAttachmentRuntime.get(
                minecraft.player);
        if (!current.equals(displayedClassId)
                || eligibilityHash(attachment) != displayedEligibilityHash
                || ClientGameplayCatalog.snapshot().revision() != displayedCatalogRevision) {
            rebuildWidgets();
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        graphics.fill(0, 0, width, height, 0xE0080C12);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(font, title, (width - font.width(title)) / 2, 16, 0xFFFFD27A);
        Component guidance = Component.translatable("screen.galacticwars.class.guidance");
        graphics.text(font, guidance, (width - font.width(guidance)) / 2, 34, 0xFFC5D1DD);
        var player = Minecraft.getInstance().player;
        var attachment = player == null ? null : PlayerCampaignAttachmentRuntime.get(player);
        if (attachment == null
                || attachment.campaign().factionId().isBlank()
                || ClientGameplayCatalog.snapshot().serverGeneration() < 0L) {
            Component syncing = Component.translatable("screen.galacticwars.class.syncing");
            graphics.text(font, syncing, (width - font.width(syncing)) / 2, height / 2, 0xFFFFB86B);
        }
    }

    @Override
    public void onClose() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && minecraft.player.containerMenu instanceof CommandCenterOperationsMenu) {
            minecraft.setScreenAndShow(parent);
        } else {
            minecraft.setScreenAndShow(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean available(
            GameplayCatalogPayload.ClassEntry definition,
            PlayerCampaignAttachmentState attachment
    ) {
        if (!definition.forcePathSlot().isBlank()
                && !definition.forcePathSlot().equals(attachment.force().tradition())) {
            return false;
        }
        return definition.requirements().stream().allMatch(
                requirement -> requirementMet(requirement, attachment.campaign()));
    }

    private static int eligibilityHash(PlayerCampaignAttachmentState attachment) {
        if (attachment == null) {
            return 0;
        }
        return Objects.hash(
                attachment.campaign().factionId(),
                attachment.campaign().eventSubjects(),
                attachment.campaign().unlocks(),
                attachment.force().tradition());
    }

    private static boolean requirementMet(
            GameplayCatalogPayload.RequirementEntry requirement,
            PlayerCampaignAttachmentState.CampaignProjection campaign
    ) {
        if (!requirement.type().equals("quest") || requirement.amount() != 1) {
            return false;
        }
        Set<String> subjects = campaign.eventSubjects().getOrDefault(
                ProgressionEventType.QUEST_ADVANCED, Set.of());
        return subjects.stream().anyMatch(subject -> subject.substring(
                Math.max(0, subject.indexOf(':') + 1)).equals(requirement.subjectId()));
    }

    private static String abilitySummary(GameplayCatalogPayload.ClassEntry definition) {
        return definition.abilityDisplayNames().isEmpty()
                ? "No abilities"
                : String.join(" / ", definition.abilityDisplayNames());
    }
}
