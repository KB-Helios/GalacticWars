package galacticwars.clonewars.menu;

import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.kingdom.BuildProject;
import galacticwars.clonewars.kingdom.KingdomRecord;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.registry.ModMenuTypes;
import galacticwars.clonewars.settlement.CommandCenterBlockEntity;
import galacticwars.clonewars.settlement.KingdomBaseBlueprint;
import galacticwars.clonewars.settlement.StarterCampDeployment;
import galacticwars.clonewars.settlement.StarterCampDeploymentPhase;
import galacticwars.clonewars.settlement.StarterCampDeploymentService;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative orientation, validation, deployment, retry, and pack-up surface. */
public final class StarterCampSetupMenu extends AbstractContainerMenu {
    public static final int SET_ROTATION_FIRST = 0;
    public static final int CONFIRM_DEPLOYMENT = 4;
    public static final int RETRY_DEPLOYMENT = 5;
    public static final int PACK_UP = 6;
    public static final int REASSIGN_BUILDER = 7;
    private static final int DATA_COUNT = 6;

    private final BlockPos commandCenterPos;
    private final ContainerData data;
    private final Inventory inventory;
    private int selectedRotation;

    public StarterCampSetupMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }

    public StarterCampSetupMenu(int containerId, Inventory inventory, BlockPos commandCenterPos) {
        this(containerId, inventory, commandCenterPos, new SimpleContainerData(DATA_COUNT));
    }

    private StarterCampSetupMenu(
            int containerId,
            Inventory inventory,
            BlockPos commandCenterPos,
            ContainerData data
    ) {
        super(ModMenuTypes.STARTER_CAMP_SETUP.get(), containerId);
        this.commandCenterPos = Objects.requireNonNull(commandCenterPos, "commandCenterPos").immutable();
        this.data = Objects.requireNonNull(data, "data");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        checkContainerDataCount(data, DATA_COUNT);
        this.addDataSlots(data);
        this.selectedRotation = 0;
        refreshData();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer) || !stillValid(player)
                || !(serverPlayer.level().getBlockEntity(commandCenterPos)
                        instanceof CommandCenterBlockEntity hall)) {
            return false;
        }
        if (buttonId >= SET_ROTATION_FIRST && buttonId < SET_ROTATION_FIRST + 4) {
            selectedRotation = buttonId - SET_ROTATION_FIRST;
            StarterCampDeploymentService.Result preview = StarterCampDeploymentService.preview(
                    serverPlayer.level(), serverPlayer, hall, selectedRotation);
            serverPlayer.sendSystemMessage(message(preview.reason()));
            refreshData();
            return preview.accepted();
        }
        if (buttonId == CONFIRM_DEPLOYMENT || buttonId == RETRY_DEPLOYMENT) {
            StarterCampDeploymentService.Result result = StarterCampDeploymentService.deploy(
                    serverPlayer.level(), serverPlayer, hall, selectedRotation);
            serverPlayer.sendSystemMessage(message(result.reason()));
            refreshData();
            return result.accepted();
        }
        if (buttonId == PACK_UP) {
            boolean packed = StarterCampDeploymentService.packUp(serverPlayer.level(), player.getUUID());
            serverPlayer.sendSystemMessage(message(packed ? "packed_up" : "pack_up_unavailable"));
            refreshData();
            return packed;
        }
        if (buttonId == REASSIGN_BUILDER) {
            StarterCampDeploymentService.Result result = StarterCampDeploymentService.reassign(
                    serverPlayer.level(), serverPlayer, hall);
            serverPlayer.sendSystemMessage(message(result.reason()));
            refreshData();
            return result.accepted();
        }
        return false;
    }

    @Override
    public void broadcastChanges() {
        refreshData();
        super.broadcastChanges();
    }

    private void refreshData() {
        if (!(inventory.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        KingdomSavedData savedData = KingdomSavedData.get(serverPlayer.level());
        KingdomRecord kingdom = savedData.kingdomForOwner(serverPlayer.getUUID()).orElse(null);
        StarterCampDeployment deployment = kingdom == null
                ? null : savedData.starterCampDeployment(kingdom.id()).orElse(null);
        if (deployment != null && deployment.phase().ordinal() >= 2) {
            selectedRotation = deployment.rotationSteps();
        }
        data.set(0, selectedRotation);
        data.set(1, deployment == null ? 0 : deployment.phase().ordinal() + 1);
        BuildProject project = deployment == null || kingdom == null
                ? null
                : deployment.projectId().flatMap(projectId -> kingdom.settlement().buildProjects().stream()
                        .filter(candidate -> candidate.id().equals(projectId)).findFirst()).orElse(null);
        data.set(2, project == null ? 0 : project.completedPlacements().size());
        data.set(3, galacticwars.clonewars.data.GameplayDataManager.snapshot()
                .blueprint(KingdomBaseBlueprint.STARTER_CAMP_ID)
                .map(blueprint -> blueprint.placements().size()).orElse(0));
        if (serverPlayer.level().getBlockEntity(commandCenterPos) instanceof CommandCenterBlockEntity hall) {
            int supplies = 0;
            for (int slot = 0; slot < hall.getContainerSize(); slot++) {
                supplies += hall.getItem(slot).getCount();
            }
            data.set(4, supplies);
        } else {
            data.set(4, 0);
        }
        int builderStatus = deployment == null ? 0
                : deployment.phase() == StarterCampDeploymentPhase.COMPLETE ? 3
                : deployment.phase() == StarterCampDeploymentPhase.BLOCKED ? 2
                : deployment.builderId().map(serverPlayer.level()::getEntity)
                        .filter(GalacticRecruitEntity.class::isInstance).isPresent() ? 1 : 0;
        data.set(5, builderStatus);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return player.isAlive();
        }
        return player.isAlive()
                && serverPlayer.distanceToSqr(
                        commandCenterPos.getX() + 0.5D,
                        commandCenterPos.getY() + 0.5D,
                        commandCenterPos.getZ() + 0.5D) <= 64.0D
                && serverPlayer.level().getBlockEntity(commandCenterPos)
                        instanceof CommandCenterBlockEntity hall
                && hall.isOwner(serverPlayer);
    }

    public int rotationSteps() {
        return data.get(0);
    }

    public int phaseCode() {
        return data.get(1);
    }

    public int completedPlacements() {
        return data.get(2);
    }

    public int totalPlacements() {
        return data.get(3);
    }

    public int storedSupplyItems() {
        return data.get(4);
    }

    public int builderStatus() {
        return data.get(5);
    }

    public static Component message(String reason) {
        return Component.translatable("message.galacticwars.starter_camp." + reason);
    }
}
