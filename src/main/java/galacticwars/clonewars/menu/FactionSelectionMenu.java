package galacticwars.clonewars.menu;

import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.faction.FactionAlignmentSavedData;
import galacticwars.clonewars.faction.FactionDefinition;
import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.kingdom.KingdomRecord;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.kingdom.KingdomActionId;
import galacticwars.clonewars.kingdom.KingdomGameplayAction;
import galacticwars.clonewars.kingdom.KingdomGameplayResult;
import galacticwars.clonewars.kingdom.KingdomGameplayRuntimeService;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.LaunchContentCatalog;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.progression.ProgressionState;
import galacticwars.clonewars.registry.ModMenuTypes;
import galacticwars.clonewars.settlement.CommandCenterBlockEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class FactionSelectionMenu extends AbstractContainerMenu {
    @Deprecated
    public static final List<String> FACTION_IDS = LaunchContentCatalog.FACTIONS;

    private final BlockPos commandCenterPos;
    private final List<String> factionIds;

    public FactionSelectionMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
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

        KingdomSavedData kingdoms = KingdomSavedData.get(level);
        Optional<KingdomRecord> existing = kingdoms.kingdomForOwner(serverPlayer.getUUID());
        ProgressionSavedData progression = ProgressionSavedData.get(level);
        ProgressionState progressionState = progression.state(serverPlayer.getUUID());
        String kingdomFaction = existing.map(KingdomRecord::factionId).orElse("");
        String progressionFaction = progressionState.factionId();
        if (!kingdomFaction.isEmpty() && !progressionFaction.isEmpty()
                && !kingdomFaction.equals(progressionFaction)) {
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.galacticwars.faction_selection.data_conflict"));
            return false;
        }
        String lockedFaction = kingdomFaction.isEmpty() ? progressionFaction : kingdomFaction;
        if (!lockedFaction.isEmpty() && !lockedFaction.equals(factionId)) {
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.galacticwars.faction_selection.locked",
                    Component.translatable(factionTranslation(lockedFaction))));
            return false;
        }

        if (existing.isPresent() && kingdoms.isHallActive(serverPlayer.getUUID())) {
            var settlement = existing.orElseThrow().settlement();
            boolean sameHall = settlement.dimensionId().equals(level.dimension().identifier().toString())
                    && settlement.hallX() == commandCenterPos.getX()
                    && settlement.hallY() == commandCenterPos.getY()
                    && settlement.hallZ() == commandCenterPos.getZ();
            if (!sameHall) {
                serverPlayer.sendSystemMessage(Component.translatable(
                        "message.galacticwars.command_center.duplicate"));
                return false;
            }
        }

        if (progressionFaction.isEmpty()) {
            KingdomGameplayResult pledge = KingdomGameplayRuntimeService.applyProgression(
                    progression, new KingdomGameplayAction(
                            KingdomActionId.of("faction_pledge", serverPlayer.getUUID(), factionId),
                            serverPlayer.getUUID(), ProgressionEventType.FACTION_PLEDGED, factionId, 1));
            if (!pledge.accepted()) {
                serverPlayer.sendSystemMessage(Component.literal(
                        "Faction selection rejected: " + pledge.reason()));
                return false;
            }
        }

        Optional<KingdomRecord> kingdom = kingdoms.activateHall(
                serverPlayer.getUUID(), factionId, level.dimension().identifier().toString(), commandCenterPos);
        if (kingdom.isEmpty()) {
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.galacticwars.command_center.duplicate"));
            return false;
        }
        commandCenter.setFaction(kingdom.orElseThrow().factionId());

        if (progressionFaction.isEmpty()) {
            FactionAlignmentSavedData.get(level).applyPledge(
                    serverPlayer.getUUID(), faction, GameplayDataManager.snapshot().factions());
        }

        serverPlayer.sendSystemMessage(Component.translatable(
                "message.galacticwars.faction_selection.confirmed",
                Component.translatable(factionTranslation(factionId))));
        serverPlayer.closeContainer();
        return true;
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
        int separator = factionId.indexOf(':');
        String path = separator < 0 ? factionId : factionId.substring(separator + 1);
        return "faction.galacticwars." + path;
    }

    public List<String> factionIds() {
        return factionIds;
    }

    private static List<String> readFactionIds(RegistryFriendlyByteBuf buffer) {
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
}
