package middleearth.lotr.warmod.menu;

import middleearth.lotr.warmod.entity.MiddleEarthRecruitEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class RecruitCommandMenuProvider implements MenuProvider {
    private final MiddleEarthRecruitEntity recruit;

    public RecruitCommandMenuProvider(MiddleEarthRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.kingdomwarsmiddleearth.recruit_command");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RecruitCommandMenu(containerId, playerInventory, this.recruit.getId());
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(this.recruit.getId());
    }
}
