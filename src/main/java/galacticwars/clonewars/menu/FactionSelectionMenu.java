package galacticwars.clonewars.menu;

import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.faction.FactionDefinition;
import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.faction.FactionPledgeService;
import galacticwars.clonewars.registry.ModMenuTypes;
import galacticwars.clonewars.settlement.CommandCenterBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class FactionSelectionMenu extends AbstractContainerMenu {
    private final BlockPos commandCenterPos;
    private final List<String> factionIds;

    public FactionSelectionMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos(), readFactionIds(extraData));
    }

    public FactionSelectionMenu(int containerId, Inventory inventory, BlockPos commandCenterPos) {
        this(containerId, inventory, commandCenterPos,
                GameplayDataManager.snapshot().selectableFactions().stream()
                        .map(definition -> definition.id().toString()).toList());
    }

    private FactionSelectionMenu(
            int containerId,
            Inventory inventory,
            BlockPos commandCenterPos,
            List<String> factionIds
    ) {
        super(ModMenuTypes.FACTION_SELECTION.get(), containerId);
        this.commandCenterPos = commandCenterPos.immutable();
        this.factionIds = List.copyOf(factionIds);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || buttonId < 0
                || buttonId >= factionIds.size()
                || !this.stillValid(player)) {
            return false;
        }
        ServerLevel level = serverPlayer.level();
        if (!(level.getBlockEntity(this.commandCenterPos) instanceof CommandCenterBlockEntity commandCenter)
                || !commandCenter.isOwner(serverPlayer)) {
            return false;
        }

        String factionId = factionIds.get(buttonId);
        FactionDefinition faction = GameplayDataManager.snapshot().factions()
                .definition(FactionId.of(factionId)).orElse(null);
        if (faction == null) {
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.galacticwars.faction_selection.data_missing"));
            return false;
        }

        FactionPledgeService.Result result = FactionPledgeService.pledge(
                serverPlayer, commandCenter, factionId);
        if (!result.accepted()) {
            serverPlayer.sendSystemMessage(pledgeFailure(result.reason(), factionId));
            return false;
        }

        serverPlayer.sendSystemMessage(Component.translatable(
                "message.galacticwars.faction_selection.confirmed",
                Component.translatable(factionTranslation(factionId))));
        serverPlayer.sendSystemMessage(Component.translatable(
                "message.galacticwars.starter_camp.open_command_center"));
        serverPlayer.closeContainer();
        return true;
    }

    private static Component pledgeFailure(String reason, String factionId) {
        if (reason.equals("chip_required")) {
            return Component.translatable(
                    "message.galacticwars.faction_selection.chip_required",
                    Component.translatable("item.galacticwars." + path(factionId) + "_identity_chip"));
        }
        return Component.translatable("message.galacticwars.faction_selection.rejected." + reason);
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
                && serverPlayer.level().getBlockEntity(commandCenterPos) instanceof CommandCenterBlockEntity commandCenter
                && commandCenter.isOwner(serverPlayer);
    }

    public static String factionTranslation(String factionId) {
        return "faction.galacticwars." + path(factionId);
    }

    public List<String> factionIds() {
        return factionIds;
    }

    private static List<String> readFactionIds(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > 64) {
            throw new IllegalArgumentException("invalid faction selection payload size " + size);
        }
        java.util.ArrayList<String> ids = new java.util.ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            ids.add(buffer.readUtf(128));
        }
        return List.copyOf(ids);
    }

    private static String path(String id) {
        int separator = id.indexOf(':');
        return separator < 0 ? id : id.substring(separator + 1);
    }
}
