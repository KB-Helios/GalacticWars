package middleearth.lotr.warmod.settlement;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.BiConsumer;
import middleearth.lotr.warmod.kingdom.KingdomRecord;
import middleearth.lotr.warmod.kingdom.KingdomSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
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
        if (!(placer instanceof Player player)
                || !(level instanceof ServerLevel serverLevel)
                || !(level.getBlockEntity(pos) instanceof KingdomHallBlockEntity hall)) {
            return;
        }
        Optional<KingdomRecord> activated = KingdomSavedData.get(serverLevel).activateHall(
                player.getUUID(), hall.factionId(), serverLevel.dimension().identifier().toString(), pos);
        if (activated.isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "message.kingdomwarsmiddleearth.kingdom_hall.duplicate"));
            serverLevel.destroyBlock(pos, true, player);
            return;
        }
        hall.claim(player);
        hall.setFaction(activated.orElseThrow().factionId());
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
        Optional<KingdomRecord> activated = data.activateHall(
                player.getUUID(), hall.factionId(), serverLevel.dimension().identifier().toString(), pos);
        if (activated.isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "message.kingdomwarsmiddleearth.kingdom_hall.duplicate"));
            return InteractionResult.FAIL;
        }
        if (!hall.factionId().equals(activated.orElseThrow().factionId())) {
            hall.setFaction(activated.orElseThrow().factionId());
        }
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

        KingdomRecord kingdom = activated.orElseThrow();
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

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof KingdomHallBlockEntity hall
                && (hall.ownerId() == null || hall.isOwner(player))) {
            hall.prepareForOwnerRemoval(serverLevel);
            dropHallContents(serverLevel, pos, hall);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            ItemStack tool
    ) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override
    protected void onExplosionHit(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> dropConsumer
    ) {
        // Hall removal is owner-authorized so treasury and campaign reservations remain conserved.
    }

    private static void dropHallContents(ServerLevel level, BlockPos pos, KingdomHallBlockEntity hall) {
        for (int slot = 0; slot < hall.getContainerSize(); slot++) {
            ItemStack stored = hall.removeItemNoUpdate(slot);
            if (!stored.isEmpty()) {
                popResource(level, pos, stored);
            }
        }
        hall.clearContent();
    }
}
