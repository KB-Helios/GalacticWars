package galacticwars.clonewars.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class CommandCenterOperationsMenuProvider implements MenuProvider {
    private final BlockPos hallPos;

    public CommandCenterOperationsMenuProvider(BlockPos hallPos) {
        this.hallPos = hallPos.immutable();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.galacticwars.command_center.operations");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CommandCenterOperationsMenu(containerId, inventory, hallPos);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(hallPos);
    }
}
