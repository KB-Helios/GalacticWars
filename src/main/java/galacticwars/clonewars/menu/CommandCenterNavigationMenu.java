package galacticwars.clonewars.menu;

import galacticwars.clonewars.progression.LaunchContentCatalog;
import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.registry.ModMenuTypes;
import galacticwars.clonewars.world.PlanetTravelService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public final class CommandCenterNavigationMenu extends AbstractContainerMenu {
    public static final int MAX_PLANET_IDS = 32;
    private final List<String> planetIds;

    public CommandCenterNavigationMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, inventory, readPlanetIds(extraData));
    }

    public CommandCenterNavigationMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, LaunchContentCatalog.planets());
    }

    private CommandCenterNavigationMenu(int containerId, Inventory inventory, List<String> planetIds) {
        super(ModMenuTypes.COMMAND_CENTER_NAVIGATION.get(), containerId);
        this.planetIds = List.copyOf(planetIds);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !this.stillValid(player)
                || buttonId < 0
                || buttonId >= planetIds.size()) {
            return false;
        }
        String planetId = planetIds.get(buttonId);
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

    public List<String> planetIds() {
        return planetIds;
    }

    private static List<String> readPlanetIds(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_PLANET_IDS) {
            throw new IllegalArgumentException("invalid navigation payload size " + size);
        }
        ArrayList<String> ids = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            ids.add(buffer.readUtf(LaunchContentDefinitions.MAX_SERIALIZED_PLANET_ID_BYTES));
        }
        return List.copyOf(ids);
    }
}
