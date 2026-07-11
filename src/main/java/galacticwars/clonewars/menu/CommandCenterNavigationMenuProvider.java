package galacticwars.clonewars.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class CommandCenterNavigationMenuProvider implements MenuProvider {
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.galacticwars.navigation_console");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CommandCenterNavigationMenu(containerId, inventory);
    }
}
