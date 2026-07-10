package middleearth.lotr.warmod.settlement;

import com.mojang.serialization.MapCodec;
import middleearth.lotr.warmod.kingdom.KingdomRecord;
import middleearth.lotr.warmod.kingdom.KingdomSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public final class KingdomHallBlock extends BaseEntityBlock {
    public static final MapCodec<KingdomHallBlock> CODEC = simpleCodec(KingdomHallBlock::new);

    public KingdomHallBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KingdomHallBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof KingdomHallBlockEntity hall) {
            hall.claim(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!(level instanceof ServerLevel serverLevel)
                || !(level.getBlockEntity(pos) instanceof KingdomHallBlockEntity hall)) {
            return InteractionResult.SUCCESS;
        }
        if (!hall.claim(player)) {
            player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.kingdom_hall.not_owner"));
            return InteractionResult.FAIL;
        }

        KingdomSavedData data = KingdomSavedData.get(serverLevel);
        if (player.isShiftKeyDown()) {
            String previousFaction = hall.factionId();
            String nextFaction = hall.cycleFaction();
            if (!data.changeFaction(player.getUUID(), nextFaction)) {
                hall.setFaction(previousFaction);
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.kingdom_hall.faction_locked"));
                return InteractionResult.FAIL;
            }
            player.sendSystemMessage(Component.translatable(
                    "message.kingdomwarsmiddleearth.kingdom_hall.faction",
                    Component.literal(nextFaction)));
            return InteractionResult.SUCCESS;
        }

        KingdomRecord kingdom = data.foundKingdom(
                player.getUUID(),
                hall.factionId(),
                serverLevel.dimension().identifier().toString(),
                pos);
        hall.settlePendingCampaignRefunds(serverLevel);
        player.openMenu(hall);
        player.sendSystemMessage(Component.translatable(
                "message.kingdomwarsmiddleearth.kingdom_hall.overview",
                Component.literal(kingdom.factionId()),
                kingdom.settlement().recruitIds().size(),
                kingdom.settlement().housingCapacity(),
                hall.treasuryEmeralds(),
                kingdom.settlement().commanderId().isPresent()
                        ? Component.translatable("message.kingdomwarsmiddleearth.kingdom_hall.commander.assigned")
                        : Component.translatable("message.kingdomwarsmiddleearth.kingdom_hall.commander.unassigned")));
        return InteractionResult.SUCCESS;
    }
}
