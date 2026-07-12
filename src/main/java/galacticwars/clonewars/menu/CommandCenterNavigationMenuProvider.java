package galacticwars.clonewars.menu;

import galacticwars.clonewars.progression.LaunchContentCatalog;
import net.minecraft.network.RegistryFriendlyByteBuf;
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

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        var planets = menu instanceof CommandCenterNavigationMenu navigation
                ? navigation.planetIds()
                : LaunchContentCatalog.planets();
        if (planets.size() > CommandCenterNavigationMenu.MAX_PLANET_IDS) {
            throw new IllegalStateException("planet list exceeds navigation payload cap: " + planets.size());
        }
        buffer.writeVarInt(planets.size());
        planets.forEach(id -> buffer.writeUtf(id, 128));
    }
}
