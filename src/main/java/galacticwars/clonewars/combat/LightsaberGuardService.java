package galacticwars.clonewars.combat;

import galacticwars.clonewars.progression.ForceRuntimeState;
import galacticwars.clonewars.progression.ForceSavedData;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative right-click guard timing, cone validation, and Force spend. */
public final class LightsaberGuardService {
    public static final int PERFECT_DEFLECT_TICKS = 6;
    private static final int NORMAL_DEFLECT_COST = 5;
    private static final int PERFECT_DEFLECT_COST = 2;
    private static final int MAX_TRACKED_GUARDS = 128;
    private static final Map<UUID, Long> GUARD_STARTS = new LinkedHashMap<>();

    private LightsaberGuardService() {
    }

    public static synchronized boolean begin(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        ForceRuntimeState state = ForceSavedData.get(level).state(player.getUUID());
        if (!state.initiated() || !state.learned("saber_guard")) return false;
        GUARD_STARTS.remove(player.getUUID());
        GUARD_STARTS.put(player.getUUID(), level.getGameTime());
        while (GUARD_STARTS.size() > MAX_TRACKED_GUARDS) {
            GUARD_STARTS.remove(GUARD_STARTS.keySet().iterator().next());
        }
        return true;
    }

    public static synchronized void end(ServerPlayer player) {
        GUARD_STARTS.remove(player.getUUID());
    }

    public static synchronized void clear(UUID playerId) {
        GUARD_STARTS.remove(playerId);
    }

    public static synchronized boolean tryDeflect(
            BlasterBoltEntity bolt, ServerPlayer player, long gameTime
    ) {
        Long start = GUARD_STARTS.get(player.getUUID());
        if (start == null || !player.isUsingItem()
                || !LightsaberDeflectionService.isLightsaber(player.getUseItem())
                || !insideViewCone(bolt, player)) {
            return false;
        }
        ServerLevel level = player.level();
        ForceSavedData data = ForceSavedData.get(level);
        ForceRuntimeState previous = data.state(player.getUUID());
        if (!previous.learned("saber_guard")) return false;
        boolean perfectWindow = gameTime - start <= PERFECT_DEFLECT_TICKS;
        boolean preciseRedirect = perfectWindow && previous.learned("precise_deflection");
        int cost = perfectWindow ? PERFECT_DEFLECT_COST : NORMAL_DEFLECT_COST;
        if (previous.energy() < cost) return false;
        ForceRuntimeState spent = previous.spendSustain(cost);
        if (!data.compareAndSet(player.getUUID(), previous, spent)) return false;
        boolean deflected = preciseRedirect
                ? bolt.deflectTowardOwner(player)
                : bolt.deflectAlongLook(player);
        if (!deflected) data.compareAndSet(player.getUUID(), spent, previous);
        return deflected;
    }

    private static boolean insideViewCone(BlasterBoltEntity bolt, ServerPlayer player) {
        Vec3 incomingSourceDirection = bolt.getDeltaMovement().scale(-1.0D);
        return incomingSourceDirection.lengthSqr() > 0.01D
                && incomingSourceDirection.normalize().dot(player.getLookAngle()) >= 0.35D;
    }
}
