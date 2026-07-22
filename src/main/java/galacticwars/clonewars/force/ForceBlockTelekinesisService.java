package galacticwars.clonewars.force;

import galacticwars.clonewars.Config;
import galacticwars.clonewars.force.ForceBlockRestorationSavedData.LiftedBlockRecord;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.registry.ModBlockTags;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Protected, non-dropping, reversible block proxy runtime. */
public final class ForceBlockTelekinesisService {
    private static final int MAX_HELD_TICKS = 100;
    private static final int RELEASE_FLIGHT_TICKS = 20;

    private ForceBlockTelekinesisService() {
    }

    public static boolean hasGrip(ServerPlayer caster) {
        return ForceBlockRestorationSavedData.get((ServerLevel) caster.level())
                .forCaster(caster.getUUID()) != null;
    }

    public static boolean hasMovableTarget(ServerPlayer caster, double requestedRange) {
        if (!Config.ALLOW_FORCE_BLOCK_PHYSICS.getAsBoolean()) return false;
        if (hasGrip(caster)) return true;
        ServerLevel level = (ServerLevel) caster.level();
        double range = ForcePhysicsRules.boundedRange(requestedRange);
        Vec3 eye = caster.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(
                eye, eye.add(caster.getLookAngle().scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (hit.getType() != HitResult.Type.BLOCK) return false;
        BlockPos source = hit.getBlockPos();
        return rejection(level, caster, source, level.getBlockState(source)).isBlank();
    }

    public static ForceEffectReport grip(ServerPlayer caster, double requestedRange) {
        if (!Config.ALLOW_FORCE_BLOCK_PHYSICS.getAsBoolean()) {
            return ForceEffectReport.failed("force_block_physics_disabled");
        }
        ServerLevel level = (ServerLevel) caster.level();
        ForceBlockRestorationSavedData ledger = ForceBlockRestorationSavedData.get(level);
        if (ledger.forCaster(caster.getUUID()) != null) {
            return ForceEffectReport.failed("force_object_already_gripped");
        }
        double range = ForcePhysicsRules.boundedRange(requestedRange);
        Vec3 eye = caster.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(
                eye, eye.add(caster.getLookAngle().scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return ForceEffectReport.failed("force_block_target_required");
        }
        BlockPos source = hit.getBlockPos();
        BlockState state = level.getBlockState(source);
        String rejection = rejection(level, caster, source, state);
        if (!rejection.isBlank()) return ForceEffectReport.failed(rejection);

        Vec3 center = Vec3.atCenterOf(source).add(0.0D, -0.5D, 0.0D);
        UUID pendingProxyId = UUID.randomUUID();
        LiftedBlockRecord entry = new LiftedBlockRecord(
                pendingProxyId, caster.getUUID(), level.dimension().identifier().toString(),
                source.immutable(), state, level.getGameTime() + MAX_HELD_TICKS,
                0L, center.x, center.y, center.z, 0.0D, 0.0D, 0.0D);
        if (!ledger.add(entry)) return ForceEffectReport.failed("force_block_proxy_limit");
        FallingBlockEntity proxy = FallingBlockEntity.fall(level, source, state);
        if (proxy == null) {
            level.setBlock(source, state, 3);
            ledger.remove(pendingProxyId);
            return ForceEffectReport.failed("force_block_lift_failed");
        }
        proxy.disableDrop();
        proxy.setNoGravity(true);
        proxy.noPhysics = true;
        proxy.setDeltaMovement(Vec3.ZERO);
        proxy.setPos(center.x, center.y, center.z);
        entry = entry.withProxyId(proxy.getUUID());
        if (!ledger.rekey(pendingProxyId, entry)) {
            proxy.discard();
            level.setBlock(source, state, 3);
            ledger.remove(pendingProxyId);
            return ForceEffectReport.failed("force_block_lift_failed");
        }
        return new ForceEffectReport(true, "accepted", List.of(proxy.getUUID()),
                0.1D, 0.0D, false, List.of("block_lift"));
    }

    public static ForceEffectReport release(ServerPlayer caster, int activeTicks) {
        ServerLevel level = (ServerLevel) caster.level();
        ForceBlockRestorationSavedData ledger = ForceBlockRestorationSavedData.get(level);
        LiftedBlockRecord entry = ledger.forCaster(caster.getUUID());
        if (entry == null) return ForceEffectReport.failed("force_object_not_gripped");
        double charge = Math.max(0.35D,
                Math.min(1.0D, ForcePhysicsRules.boundedChannelTicks(activeTicks) / 60.0D));
        Vec3 velocity = caster.getLookAngle().normalize().scale(ForcePhysicsRules.MAX_VELOCITY * charge)
                .add(0.0D, 0.12D, 0.0D);
        ledger.replace(entry.released(level.getGameTime(), velocity.x, velocity.y, velocity.z));
        return new ForceEffectReport(true, "accepted", List.of(entry.proxyId()),
                velocity.length(), 0.0D, false, List.of("block_throw"));
    }

    public static void cancel(ServerPlayer caster) {
        restoreCaster(caster.level().getServer(), caster.getUUID());
    }

    public static void tick(MinecraftServer server) {
        ForceBlockRestorationSavedData ledger = ForceBlockRestorationSavedData.get(server.overworld());
        for (LiftedBlockRecord entry : ledger.entries()) {
            ServerLevel level = level(server, entry.dimensionId());
            if (level == null) continue;
            Entity entity = level.getEntity(entry.proxyId());
            if (!(entity instanceof FallingBlockEntity proxy)) {
                restore(level, ledger, entry, null);
                continue;
            }
            long gameTime = level.getGameTime();
            proxy.disableDrop();
            proxy.setNoGravity(true);
            proxy.noPhysics = true;
            proxy.time = 1;
            if (entry.releasedAt() > 0L) {
                Vec3 current = new Vec3(entry.x(), entry.y(), entry.z());
                Vec3 velocity = new Vec3(entry.dx(), entry.dy(), entry.dz());
                Vec3 next = current.add(velocity);
                BlockHitResult collision = level.clip(new ClipContext(
                        current, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, proxy));
                if (collision.getType() == HitResult.Type.BLOCK
                        || gameTime - entry.releasedAt() >= RELEASE_FLIGHT_TICKS) {
                    restore(level, ledger, entry, proxy);
                    continue;
                }
                proxy.setPos(next.x, next.y, next.z);
                ledger.replace(new LiftedBlockRecord(
                        entry.proxyId(), entry.casterId(), entry.dimensionId(), entry.sourcePos(),
                        entry.sourceState(), entry.expiresAt(), entry.releasedAt(),
                        next.x, next.y, next.z, velocity.x * 0.96D,
                        velocity.y - 0.04D, velocity.z * 0.96D));
                continue;
            }
            ServerPlayer caster = server.getPlayerList().getPlayer(entry.casterId());
            if (caster == null || !caster.isAlive()
                    || caster.level() != level || gameTime >= entry.expiresAt()
                    || caster.containerMenu != caster.inventoryMenu) {
                restore(level, ledger, entry, proxy);
                continue;
            }
            Vec3 held = caster.getEyePosition().add(caster.getLookAngle().scale(2.0D))
                    .add(0.0D, -0.5D, 0.0D);
            proxy.setPos(held.x, held.y, held.z);
            ledger.replace(entry.moved(held.x, held.y, held.z));
        }
    }

    private static String rejection(
            ServerLevel level, ServerPlayer caster, BlockPos pos, BlockState state
    ) {
        if (state.isAir() || !state.is(ModBlockTags.FORCE_MOVABLE)
                || state.is(ModBlockTags.FORCE_IMMOVABLE)) return "force_block_immovable";
        if (!state.getFluidState().isEmpty()) return "force_block_fluid";
        if (level.getBlockEntity(pos) != null) return "force_block_entity";
        if (state.getDestroySpeed(level, pos) < 0.0F
                || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.END_GATEWAY)) return "force_block_immovable";
        KingdomSavedData kingdoms = KingdomSavedData.get(level);
        var claim = kingdoms.claimAt(level.dimension().identifier().toString(),
                new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4));
        if (claim.isEmpty()) return "";
        return kingdoms.kingdomForOwner(caster.getUUID())
                .filter(kingdom -> kingdom.id().equals(claim.orElseThrow().kingdomId()))
                .isPresent() ? "" : "force_block_foreign_claim";
    }

    private static void restoreCaster(MinecraftServer server, UUID casterId) {
        ForceBlockRestorationSavedData ledger = ForceBlockRestorationSavedData.get(server.overworld());
        LiftedBlockRecord entry = ledger.forCaster(casterId);
        if (entry == null) return;
        ServerLevel level = level(server, entry.dimensionId());
        if (level != null) restore(level, ledger, entry, level.getEntity(entry.proxyId()));
    }

    private static void restore(
            ServerLevel level, ForceBlockRestorationSavedData ledger,
            LiftedBlockRecord entry, Entity proxy
    ) {
        level.setBlock(entry.sourcePos(), entry.sourceState(), 3);
        if (proxy != null) proxy.discard();
        ledger.remove(entry.proxyId());
    }

    private static ServerLevel level(MinecraftServer server, String dimensionId) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().identifier().toString().equals(dimensionId)) return level;
        }
        return null;
    }
}
