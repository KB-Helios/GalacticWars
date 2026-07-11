package middleearth.lotr.warmod.faction;

import middleearth.lotr.warmod.data.GameplayDataManager;
import middleearth.lotr.warmod.settlement.KingdomHallBlockEntity;
import middleearth.lotr.warmod.kingdom.KingdomSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class FactionTokenItem extends Item {
    private final FactionId factionId;

    public FactionTokenItem(FactionId factionId, Properties properties) {
        super(properties);
        this.factionId = factionId;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)
                || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof KingdomHallBlockEntity hall)
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
                    "message.kingdomwarsmiddleearth.faction_token.hall_required"));
            return InteractionResult.FAIL;
        }
        FactionDefinition faction = GameplayDataManager.snapshot().factions().definition(factionId).orElse(null);
        if (faction == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.kingdomwarsmiddleearth.faction_token.data_missing"));
            return InteractionResult.FAIL;
        }

        FactionAlignmentUpdateResult result = FactionAlignmentSavedData.get(level).applyPledge(
                player.getUUID(), faction, GameplayDataManager.snapshot().factions());
        if (!player.hasInfiniteMaterials()) {
            context.getItemInHand().shrink(1);
        }
        player.sendSystemMessage(Component.translatable(
                "message.kingdomwarsmiddleearth.faction_token.applied",
                Component.literal(faction.displayName()),
                result.alignment().score(faction.id())));
        return InteractionResult.SUCCESS;
    }
}
