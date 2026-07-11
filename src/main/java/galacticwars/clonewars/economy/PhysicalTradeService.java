package galacticwars.clonewars.economy;

import galacticwars.clonewars.progression.LaunchContentCatalog;
import galacticwars.clonewars.progression.ProgressionDecision;
import galacticwars.clonewars.progression.ProgressionEvent;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.progression.ProgressionState;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class PhysicalTradeService {
    private PhysicalTradeService() {
    }

    public static TradeResult purchase(ServerPlayer player, UUID eventId, String tradeId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(eventId, "eventId");
        if (tradeId == null) {
            return TradeResult.rejected("unknown_trade");
        }
        LaunchContentCatalog.TradeDefinition trade = LaunchContentCatalog.TRADES.get(tradeId);
        if (trade == null) {
            return TradeResult.rejected("unknown_trade");
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return TradeResult.rejected("server_only");
        }
        ProgressionSavedData progression = ProgressionSavedData.get(level);
        ProgressionState state = progression.state(player.getUUID());
        if (state.processed(eventId)) {
            return TradeResult.duplicate();
        }
        if (!state.factionId().equals("galacticwars:" + trade.factionId())) {
            return TradeResult.rejected("hostile_merchant");
        }
        if (!state.unlocks().contains(trade.requiredUnlock())) {
            return TradeResult.rejected("trade_locked");
        }
        Identifier resultId;
        try {
            resultId = Identifier.parse(trade.itemId());
        } catch (IllegalArgumentException exception) {
            return TradeResult.rejected("unknown_trade_item");
        }
        Item resultItem = BuiltInRegistries.ITEM.getValue(resultId);
        if (resultItem == null || !trade.itemId().equals(BuiltInRegistries.ITEM.getKey(resultItem).toString())) {
            return TradeResult.rejected("unknown_trade_item");
        }
        if (!CreditTransactionService.withdrawPlayer(player, trade.price())) {
            return TradeResult.rejected("insufficient_credits");
        }
        ProgressionDecision completion = progression.apply(new ProgressionEvent(
                eventId, player.getUUID(), ProgressionEventType.TRADE_COMPLETED, tradeId, 1));
        if (!completion.accepted() || !completion.changed()) {
            CreditTransactionService.refundPlayer(player, trade.price());
            return completion.accepted() ? TradeResult.duplicate() : TradeResult.rejected(completion.reason());
        }
        ItemStack result = new ItemStack(resultItem, trade.itemCount());
        player.getInventory().add(result);
        if (!result.isEmpty()) {
            player.spawnAtLocation(level, result);
        }
        return new TradeResult(true, true, "accepted", trade.itemId(), trade.itemCount(), trade.price());
    }

    public record TradeResult(
            boolean accepted,
            boolean changed,
            String reason,
            String itemId,
            int itemCount,
            int creditsCharged
    ) {
        public TradeResult {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(itemId, "itemId");
        }

        static TradeResult rejected(String reason) {
            return new TradeResult(false, false, reason, "", 0, 0);
        }

        static TradeResult duplicate() {
            return new TradeResult(true, false, "duplicate_event", "", 0, 0);
        }
    }
}
