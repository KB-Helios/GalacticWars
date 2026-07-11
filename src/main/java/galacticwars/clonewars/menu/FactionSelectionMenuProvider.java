package galacticwars.clonewars.menu;

import galacticwars.clonewars.data.GameplayDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class FactionSelectionMenuProvider implements MenuProvider {
    private final BlockPos commandCenterPos;

    public FactionSelectionMenuProvider(BlockPos commandCenterPos) {
        this.commandCenterPos = commandCenterPos.immutable();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.galacticwars.faction_selection");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new FactionSelectionMenu(containerId, inventory, commandCenterPos);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(commandCenterPos);
        var factionIds = GameplayDataManager.snapshot().selectableFactions().stream()
                .map(definition -> definition.id().toString()).toList();
        buffer.writeVarInt(factionIds.size());
        factionIds.forEach(id -> buffer.writeUtf(id, 128));
    }
}
