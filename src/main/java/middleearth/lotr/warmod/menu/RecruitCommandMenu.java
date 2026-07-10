package middleearth.lotr.warmod.menu;

import middleearth.lotr.warmod.entity.MiddleEarthRecruitEntity;
import middleearth.lotr.warmod.registry.ModMenuTypes;
import middleearth.lotr.warmod.workforce.WorkerProfessionCatalog;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RecruitCommandMenu extends AbstractContainerMenu {
    public static final int BUTTON_HIRE = 0;
    public static final int BUTTON_FOLLOW = 1;
    public static final int BUTTON_HOLD = 2;
    public static final int BUTTON_MOVE = 3;
    public static final int BUTTON_PROTECT = 4;
    public static final int BUTTON_ATTACK = 5;
    public static final int BUTTON_CLEAR = 6;
    public static final int BUTTON_SET_WORKSITE = 7;
    public static final int BUTTON_RETURN_WORKSITE = 8;
    public static final int BUTTON_CLEAR_WORKSITE = 9;
    public static final int BUTTON_SET_STORAGE = 10;
    public static final int BUTTON_BUILD_STARTER_KEEP = 11;
    public static final int BUTTON_WORK_RADIUS_DECREASE = 12;
    public static final int BUTTON_WORK_RADIUS_INCREASE = 13;
    public static final int BUTTON_PROMOTE_COMMANDER = 14;
    public static final int BUTTON_TOGGLE_AUTO_RECRUITMENT = 15;
    public static final int BUTTON_START_RECRUITMENT = 16;
    public static final int BUTTON_NEXT_BLUEPRINT = 17;
    public static final int BUTTON_RETURN_TO_SOLDIER = 18;
    public static final int BUTTON_CANCEL_BUILD = 19;
    public static final int BUTTON_ASSIGN_FARMER = WorkerProfessionCatalog.FIRST_COMMAND_BUTTON_ID;
    public static final int BUTTON_ASSIGN_LUMBERJACK = WorkerProfessionCatalog.FIRST_COMMAND_BUTTON_ID + 1;
    public static final int BUTTON_ASSIGN_FISHERMAN = WorkerProfessionCatalog.FIRST_COMMAND_BUTTON_ID + 2;
    public static final int BUTTON_ASSIGN_ANIMAL_FARMER = WorkerProfessionCatalog.FIRST_COMMAND_BUTTON_ID + 3;
    public static final int BUTTON_ASSIGN_MINER = WorkerProfessionCatalog.FIRST_COMMAND_BUTTON_ID + 4;
    public static final int BUTTON_ASSIGN_BUILDER = WorkerProfessionCatalog.FIRST_COMMAND_BUTTON_ID + 5;
    public static final int BUTTON_ASSIGN_COOK = WorkerProfessionCatalog.FIRST_COMMAND_BUTTON_ID + 6;
    public static final int BUTTON_ASSIGN_MERCHANT = WorkerProfessionCatalog.FIRST_COMMAND_BUTTON_ID + 7;
    public static final int BUTTON_ASSIGN_COURIER = WorkerProfessionCatalog.FIRST_COMMAND_BUTTON_ID + 8;

    private final int recruitEntityId;
    private final Level level;

    public RecruitCommandMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readVarInt());
    }

    public RecruitCommandMenu(int containerId, Inventory inventory, int recruitEntityId) {
        super(ModMenuTypes.RECRUIT_COMMAND.get(), containerId);
        this.recruitEntityId = recruitEntityId;
        this.level = inventory.player.level();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        Entity entity = this.level.getEntity(this.recruitEntityId);
        if (isSupportedButton(buttonId)
                && this.stillValid(player)
                && player instanceof ServerPlayer serverPlayer
                && entity instanceof MiddleEarthRecruitEntity recruit) {
            return recruit.handleMenuButton(serverPlayer, buttonId);
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        Entity entity = this.level.getEntity(this.recruitEntityId);
        return player.level() == this.level
                && entity instanceof MiddleEarthRecruitEntity
                && entity.isAlive()
                && player.distanceToSqr(entity) <= 64.0;
    }

    public int recruitEntityId() {
        return recruitEntityId;
    }

    public static int[] workerProfessionButtonIds() {
        return WorkerProfessionCatalog.enabledProfessions().stream()
                .mapToInt(definition -> definition.commandButtonId())
                .toArray();
    }

    public static boolean isSupportedButton(int buttonId) {
        if (buttonId >= BUTTON_HIRE && buttonId <= BUTTON_CANCEL_BUILD) {
            return true;
        }
        return WorkerProfessionCatalog.definitionForButton(buttonId)
                .filter(definition -> WorkerProfessionCatalog.isEnabled(definition.profession()))
                .isPresent();
    }
}
