package galacticwars.clonewars.menu;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Opens the live recruit loadout and sends only its bounded entity id to the client. */
public final class RecruitLoadoutMenuProvider implements ExtendedMenuProvider {
    private final GalacticRecruitEntity recruit;

    public RecruitLoadoutMenuProvider(GalacticRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.galacticwars.recruit_loadout");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory inventory,
            Player player
    ) {
        return new RecruitLoadoutMenu(containerId, inventory, this.recruit);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.recruit.getId());
    }
}
