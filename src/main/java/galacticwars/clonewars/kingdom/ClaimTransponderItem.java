package galacticwars.clonewars.kingdom;

import galacticwars.clonewars.progression.ProgressionSavedData;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;

/** Physical claim expansion and enemy-outpost siege initiator. */
public final class ClaimTransponderItem extends Item {
    public ClaimTransponderItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null) {
            return context.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        UUID actorId = context.getPlayer().getUUID();
        KingdomSavedData data = KingdomSavedData.get(level);
        KingdomRecord own = data.kingdomForPlayer(actorId).orElse(null);
        if (own == null) return InteractionResult.FAIL;
        String dimension = level.dimension().identifier().toString();
        net.minecraft.core.BlockPos target = context.getClickedPos().relative(context.getClickedFace());
        ChunkPos chunk = new ChunkPos(target.getX() >> 4, target.getZ() >> 4);
        if (context.getPlayer().isShiftKeyDown()) {
            KingdomClaim enemy = data.claimAt(dimension, chunk)
                    .filter(claim -> !claim.kingdomId().equals(own.id()) && !claim.capital()).orElse(null);
            if (enemy == null || !eligibleSquadPresent(level, own, context.getClickedPos())) {
                return InteractionResult.FAIL;
            }
            KingdomRecord defender = data.kingdom(enemy.kingdomId()).orElse(null);
            boolean defenderOnline = defender != null && level.getServer().getPlayerList().getPlayers().stream()
                    .anyMatch(player -> defender.member(player.getUUID()).isPresent());
            boolean started = data.startSiege(actorId, enemy.id(), defenderOnline,
                    true, 1200, level.getGameTime()).isPresent();
            if (started) context.getPlayer().sendSystemMessage(Component.translatable(
                    "message.galacticwars.siege.started"));
            return started ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        KingdomClaim claim = own.claims().stream()
                .filter(candidate -> candidate.dimensionId().equals(dimension)
                        && candidate.canExpandTo(new ClaimedChunk(chunk.x(), chunk.z())))
                .findFirst().orElse(null);
        boolean expanded = claim != null && data.expandClaim(actorId, claim.id(), dimension, chunk);
        if (expanded) context.getPlayer().sendSystemMessage(Component.translatable(
                "message.galacticwars.claim.expanded", chunk.x(), chunk.z()));
        return expanded ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    private static boolean eligibleSquadPresent(ServerLevel level, KingdomRecord kingdom, net.minecraft.core.BlockPos pos) {
        var nearby = level.getEntitiesOfClass(galacticwars.clonewars.entity.GalacticRecruitEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(48.0D));
        return nearby.stream().anyMatch(recruit -> kingdom.npc(recruit.getUUID())
                .filter(entry -> entry.serviceBranch() == galacticwars.clonewars.recruitment.NpcServiceBranch.MILITARY)
                .isPresent());
    }
}
