package galacticwars.clonewars.menu;

import galacticwars.clonewars.progression.LaunchContentCatalog;
import galacticwars.clonewars.registry.ModMenuTypes;
import galacticwars.clonewars.world.PlanetTravelService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class CommandCenterNavigationMenu extends AbstractContainerMenu {
    public CommandCenterNavigationMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, inventory);
    }

    public CommandCenterNavigationMenu(int containerId, Inventory inventory) {
        super(ModMenuTypes.COMMAND_CENTER_NAVIGATION.get(), containerId);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !this.stillValid(player)
                || buttonId < 0
                || buttonId >= LaunchContentCatalog.PLANETS.size()) {
            return false;
        }
        String planetId = LaunchContentCatalog.PLANETS.get(buttonId);
        PlanetTravelService.TravelResult result = PlanetTravelService.travel(
                serverPlayer, planetId);
        if (result.accepted()) {
            player.sendSystemMessage(Component.translatable(
                    result.squadTransferred()
                            ? "message.galacticwars.travel.success_with_squad"
                            : "message.galacticwars.travel.success",
                    Component.translatable("planet.galacticwars." + result.planetId())));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.galacticwars.travel." + result.reason()));
        }
        return result.accepted();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive()
                && player instanceof ServerPlayer serverPlayer
                && PlanetTravelService.hasActiveCommandCenter(serverPlayer);
    }
}
