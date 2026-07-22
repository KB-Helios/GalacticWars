package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.network.ForceProgressionActionPayload;
import galacticwars.clonewars.network.ForceProgressionPayload;
import galacticwars.clonewars.network.GalacticNetwork;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Two-branch shrine screen backed exclusively by bounded server snapshots. */
public final class ForceProgressionScreen extends Screen {
    private ForceProgressionPayload snapshot;

    private ForceProgressionScreen(ForceProgressionPayload snapshot) {
        super(Component.translatable("screen.galacticwars.force.title"));
        this.snapshot = snapshot;
    }

    public static void open(ForceProgressionPayload snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof ForceProgressionScreen screen
                && screen.snapshot.shrinePos().equals(snapshot.shrinePos())) {
            screen.snapshot = snapshot;
            screen.rebuildWidgets();
        } else {
            minecraft.setScreenAndShow(new ForceProgressionScreen(snapshot));
        }
    }

    @Override
    protected void init() {
        super.init();
        int panelWidth = Math.min(560, width - 20);
        int left = (width - panelWidth) / 2;
        List<String> branches = snapshot.nodes().stream()
                .map(ForceProgressionPayload.NodeEntry::branch)
                .filter(branch -> !branch.equals("core"))
                .distinct().limit(2).toList();
        for (int branchIndex = 0; branchIndex < branches.size(); branchIndex++) {
            String branch = branches.get(branchIndex);
            int columnLeft = left + branchIndex * (panelWidth / 2);
            List<ForceProgressionPayload.NodeEntry> nodes = snapshot.nodes().stream()
                    .filter(node -> node.branch().equals(branch))
                    .sorted(Comparator.comparingInt(ForceProgressionPayload.NodeEntry::tier))
                    .toList();
            for (int index = 0; index < nodes.size(); index++) {
                addNodeRow(nodes.get(index), columnLeft, 76 + index * 29, panelWidth / 2 - 8);
            }
        }
        Button respec = Button.builder(
                        Component.translatable("screen.galacticwars.force.respec", snapshot.respecCost()),
                        pressed -> send(ForceProgressionActionPayload.RESPEC, "", -1))
                .bounds((width - 180) / 2, height - 34, 180, 20).build();
        respec.active = snapshot.learnedNodes().size() > 3;
        addRenderableWidget(respec);
    }

    private void addNodeRow(
            ForceProgressionPayload.NodeEntry node, int left, int top, int width
    ) {
        boolean learned = snapshot.learnedNodes().contains(node.id());
        boolean prerequisites = snapshot.learnedNodes().containsAll(node.prerequisites());
        boolean rankReady = snapshot.rank() >= node.tier() + 1;
        Button nodeButton = Button.builder(
                        Component.literal((learned ? "✓ " : "○ ") + display(node.id())
                                + (node.passive() ? " [P]" : "")),
                        pressed -> send(ForceProgressionActionPayload.LEARN, node.id(), -1))
                .bounds(left + 4, top, width - (node.abilityId().isBlank() ? 8 : 82), 22)
                .build();
        nodeButton.active = !learned && prerequisites && rankReady
                && snapshot.unspentPoints() >= node.pointCost();
        if (!nodeButton.active && !learned) {
            nodeButton.setTooltip(Tooltip.create(Component.literal(
                    !rankReady ? "Requires rank " + (node.tier() + 1)
                            : !prerequisites ? "Requires preceding nodes"
                            : "Requires a skill point")));
        }
        addRenderableWidget(nodeButton);
        if (!node.abilityId().isBlank()) {
            for (int slot = 0; slot < 3; slot++) {
                int selectedSlot = slot;
                Button equip = Button.builder(Component.literal(switch (slot) {
                    case 0 -> "Z";
                    case 1 -> "X";
                    default -> "C";
                }), pressed -> send(ForceProgressionActionPayload.EQUIP,
                        node.abilityId(), selectedSlot))
                        .bounds(left + width - 76 + slot * 24, top, 22, 22).build();
                equip.active = learned && !equippedAt(node.abilityId(), slot);
                if (equippedAt(node.abilityId(), slot)) {
                    equip.setTooltip(Tooltip.create(Component.literal("Equipped")));
                }
                addRenderableWidget(equip);
            }
        }
    }

    private void send(int action, String subject, int slot) {
        GalacticNetwork.CHANNEL.sendToServer(new ForceProgressionActionPayload(
                UUID.randomUUID(), snapshot.shrinePos(), action, subject, slot));
    }

    private boolean equippedAt(String ability, int slot) {
        return slot < snapshot.equippedAbilities().size()
                && snapshot.equippedAbilities().get(slot).equals(ability);
    }

    @Override
    public void tick() {
        super.tick();
        var player = Minecraft.getInstance().player;
        if (player == null || player.distanceToSqr(
                snapshot.shrinePos().getX() + 0.5D,
                snapshot.shrinePos().getY() + 0.5D,
                snapshot.shrinePos().getZ() + 0.5D) > 100.0D) {
            onClose();
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        graphics.fill(0, 0, width, height, 0xE0080C12);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int accent = snapshot.tradition().equals("sith") ? 0xFFE34848
                : snapshot.tradition().equals("nightsister") ? 0xFF56D98C : 0xFF8DA1FF;
        graphics.text(font, Component.literal(display(snapshot.tradition()) + " Training"),
                (width - font.width(display(snapshot.tradition()) + " Training")) / 2,
                14, accent);
        Component status = Component.literal("Rank " + snapshot.rank() + "  •  Mastery "
                + snapshot.masteryExperience() + "  •  " + snapshot.unspentPoints()
                + " points available");
        graphics.text(font, status, (width - font.width(status)) / 2, 31, 0xFFE7EBF5);
        String core = snapshot.nodes().stream().filter(node -> node.branch().equals("core"))
                .map(node -> display(node.id())).reduce((first, next) -> first + "  •  " + next)
                .orElse("");
        graphics.text(font, Component.literal("Fundamentals: " + core),
                (width - font.width("Fundamentals: " + core)) / 2, 48, 0xFFB8C1D1);
        List<String> branches = snapshot.nodes().stream().map(ForceProgressionPayload.NodeEntry::branch)
                .filter(branch -> !branch.equals("core")).distinct().limit(2).toList();
        int panelWidth = Math.min(560, width - 20);
        int left = (width - panelWidth) / 2;
        for (int index = 0; index < branches.size(); index++) {
            String label = display(branches.get(index));
            int center = left + panelWidth / 4 + index * panelWidth / 2;
            graphics.text(font, Component.literal(label), center - font.width(label) / 2,
                    63, accent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String display(String id) {
        String[] words = id.replace('_', ' ').split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
