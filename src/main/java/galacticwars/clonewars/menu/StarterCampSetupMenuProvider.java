package galacticwars.clonewars.menu;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class StarterCampSetupMenuProvider implements ExtendedMenuProvider {
    private final BlockPos commandCenterPos;

    public StarterCampSetupMenuProvider(BlockPos commandCenterPos) {
        this.commandCenterPos = commandCenterPos.immutable();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.galacticwars.starter_camp_setup");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new StarterCampSetupMenu(containerId, inventory, commandCenterPos);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(commandCenterPos);
    }
}
