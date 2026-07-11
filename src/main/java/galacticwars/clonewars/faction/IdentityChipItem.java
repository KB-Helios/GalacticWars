package galacticwars.clonewars.faction;

import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.settlement.CommandCenterBlockEntity;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.kingdom.KingdomActionId;
import galacticwars.clonewars.kingdom.KingdomGameplayAction;
import galacticwars.clonewars.kingdom.KingdomGameplayResult;
import galacticwars.clonewars.kingdom.KingdomGameplayRuntimeService;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

import java.util.UUID;

public final class IdentityChipItem extends Item {
    private final FactionId factionId;

    public IdentityChipItem(FactionId factionId, Properties properties) {
        super(properties);
        this.factionId = factionId;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)
                || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof CommandCenterBlockEntity hall)
                || !hall.isOwner(player)
                || !KingdomSavedData.get(level).isHallActive(player.getUUID())
                || KingdomSavedData.get(level).kingdomForOwner(player.getUUID())
                        .map(kingdom -> kingdom.settlement())
                        .filter(settlement -> settlement.dimensionId().equals(level.dimension().identifier().toString()))
                        .filter(settlement -> settlement.hallX() == context.getClickedPos().getX()
                                && settlement.hallY() == context.getClickedPos().getY()
                                && settlement.hallZ() == context.getClickedPos().getZ())
                        .isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "message.galacticwars.identity_chip.hall_required"));
            return InteractionResult.FAIL;
        }
        FactionDefinition faction = GameplayDataManager.snapshot().factions().definition(factionId).orElse(null);
        if (faction == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.galacticwars.identity_chip.data_missing"));
            return InteractionResult.FAIL;
        }
        String commandCenterFaction = KingdomSavedData.get(level).kingdomForOwner(player.getUUID())
                .map(kingdom -> kingdom.factionId())
                .orElse("");
        if (!commandCenterFaction.equals(faction.id().toString())) {
            player.sendSystemMessage(Component.translatable(
                    "message.galacticwars.identity_chip.faction_mismatch",
                    Component.literal(commandCenterFaction),
                    Component.literal(faction.id().toString())));
            return InteractionResult.FAIL;
        }

        KingdomGameplayResult progression = KingdomGameplayRuntimeService.applyProgression(
                ProgressionSavedData.get(level), new KingdomGameplayAction(
                        KingdomActionId.of("faction_pledge", player.getUUID(), faction.id()),
                        player.getUUID(), ProgressionEventType.FACTION_PLEDGED,
                        faction.id().toString(), 1));
        if (!progression.accepted()) {
            player.sendSystemMessage(Component.literal("Faction pledge rejected: " + progression.reason()));
            return InteractionResult.FAIL;
        }

        FactionAlignmentUpdateResult result = FactionAlignmentSavedData.get(level).applyPledge(
                player.getUUID(), faction, GameplayDataManager.snapshot().factions());
        if (!player.hasInfiniteMaterials()) {
            context.getItemInHand().shrink(1);
        }
        player.sendSystemMessage(Component.translatable(
                "message.galacticwars.identity_chip.applied",
                Component.literal(faction.displayName()),
                result.alignment().score(faction.id())));
        return InteractionResult.SUCCESS;
    }
}
