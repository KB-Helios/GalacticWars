package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.menu.CommandCenterOperationsMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.UUID;
import galacticwars.clonewars.network.MenuActionPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class CommandCenterOperationsScreen extends Screen
        implements MenuAccess<CommandCenterOperationsMenu> {
    private static final String[] TABS = {"overview", "campaign", "squads", "kingdom", "diplomacy", "storage"};
    private final CommandCenterOperationsMenu menu;
    private int selectedTab;

    public CommandCenterOperationsScreen(
            CommandCenterOperationsMenu menu, Inventory inventory, Component title
    ) {
        super(title);
        this.menu = menu;
    }

    @Override
    protected void init() {
        int tabWidth = 82;
        int left = (width - tabWidth * TABS.length) / 2;
        for (int index = 0; index < TABS.length; index++) {
            int tab = index;
            addRenderableWidget(Button.builder(Component.translatable(
                            "screen.galacticwars.operations.tab." + TABS[index]), button -> {
                        selectedTab = tab;
                        rebuildWidgets();
                    }).bounds(left + index * tabWidth, 18, tabWidth - 2, 20).build());
        }
        int x = (width - 220) / 2;
        int y = 58;
        switch (selectedTab) {
            case 0 -> addAction(x, y, "screen.galacticwars.operations.claim_rewards",
                    CommandCenterOperationsMenu.CLAIM_REWARDS);
            case 1 -> {
                String[] vehicles = {"barc_speeder", "at_rt", "stap", "aat", "laat_gunship"};
                for (int i = 0; i < vehicles.length; i++) addAction(x, y + i * 22,
                        "screen.galacticwars.operations.fabricate." + vehicles[i],
                        CommandCenterOperationsMenu.FABRICATE_FIRST + i);
            }
            case 2 -> {
                addAction(x, y, "screen.galacticwars.operations.create_squad", CommandCenterOperationsMenu.CREATE_SQUAD);
                addAction(x, y + 22, "screen.galacticwars.operations.split", CommandCenterOperationsMenu.SPLIT_SQUAD);
                addAction(x, y + 44, "screen.galacticwars.operations.merge", CommandCenterOperationsMenu.MERGE_SQUADS);
                addAction(x, y + 66, "screen.galacticwars.operations.configure", CommandCenterOperationsMenu.CONFIGURE_SQUAD);
                addAction(x, y + 88, "screen.galacticwars.operations.formation", CommandCenterOperationsMenu.CYCLE_FORMATION);
                addAction(x, y + 110, "screen.galacticwars.operations.patrol", CommandCenterOperationsMenu.PATROL_SQUAD);
                addAction(x, y + 132, "screen.galacticwars.operations.follow", CommandCenterOperationsMenu.FOLLOW_SQUAD);
                addAction(x, y + 154, "screen.galacticwars.operations.supply", CommandCenterOperationsMenu.SUPPLY_SQUAD);
            }
            case 3 -> {
                addAction(x, y, "screen.galacticwars.operations.outpost", CommandCenterOperationsMenu.REGISTER_OUTPOST);
                addAction(x, y + 22, "screen.galacticwars.operations.invite", CommandCenterOperationsMenu.INVITE_NEAREST);
                addAction(x, y + 44, "screen.galacticwars.operations.accept_invite", CommandCenterOperationsMenu.ACCEPT_INVITE);
                addAction(x, y + 66, "screen.galacticwars.operations.reject_invite", CommandCenterOperationsMenu.REJECT_INVITE);
                addAction(x, y + 88, "screen.galacticwars.operations.role", CommandCenterOperationsMenu.CYCLE_MEMBER_ROLE);
                addAction(x, y + 110, "screen.galacticwars.operations.remove", CommandCenterOperationsMenu.REMOVE_MEMBER);
            }
            case 4 -> {
                addAction(x, y, "screen.galacticwars.operations.alliance", CommandCenterOperationsMenu.PROPOSE_ALLIANCE);
                addAction(x, y + 22, "screen.galacticwars.operations.accept_diplomacy", CommandCenterOperationsMenu.ACCEPT_DIPLOMACY);
                addAction(x, y + 44, "screen.galacticwars.operations.reject_diplomacy", CommandCenterOperationsMenu.REJECT_DIPLOMACY);
                addAction(x, y + 66, "screen.galacticwars.operations.hostility", CommandCenterOperationsMenu.DECLARE_HOSTILITY);
                addAction(x, y + 88, "screen.galacticwars.operations.embargo", CommandCenterOperationsMenu.TOGGLE_EMBARGO);
            }
            case 5 -> addAction(x, y, "screen.galacticwars.operations.open_storage", CommandCenterOperationsMenu.STORAGE);
            default -> { }
        }
    }

    private void addAction(int x, int y, String key, int id) {
        addRenderableWidget(Button.builder(Component.translatable(key), button -> {
            ClientPacketDistributor.sendToServer(
                    new MenuActionPayload(UUID.randomUUID(), menu.containerId, id));
        }).bounds(x, y, 220, 20).build());
    }

    @Override public CommandCenterOperationsMenu getMenu() { return menu; }
    @Override public boolean isPauseScreen() { return false; }
}
