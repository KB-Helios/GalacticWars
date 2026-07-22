package galacticwars.clonewars.force;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Applies bounded collision damage from server-observed post-Force velocity. */
public final class ForceCollisionDamageService {
    private static final int MAX_TRACKED_IMPULSES = 256;
    private static final Map<UUID, TrackedImpulse> TRACKED = new LinkedHashMap<>();
    private static final Map<UUID, Long> LAST_DAMAGE = new LinkedHashMap<>();

    private ForceCollisionDamageService() {
    }

    public static synchronized void track(
            ServerLevel level, LivingEntity caster, Entity target, double mass
    ) {
        TRACKED.put(target.getUUID(), new TrackedImpulse(
                level.dimension().identifier().toString(), caster.getUUID(), mass,
                Math.min(ForcePhysicsRules.MAX_VELOCITY, target.getDeltaMovement().length()),
                level.getGameTime() + 40L));
        trim(TRACKED, MAX_TRACKED_IMPULSES);
    }

    public static synchronized void tick(MinecraftServer server) {
        long gameTime = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, TrackedImpulse>> iterator = TRACKED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TrackedImpulse> entry = iterator.next();
            TrackedImpulse tracked = entry.getValue();
            ServerLevel level = null;
            for (ServerLevel candidate : server.getAllLevels()) {
                if (candidate.dimension().identifier().toString().equals(tracked.dimensionId())) {
                    level = candidate;
                    break;
                }
            }
            Entity target = level == null ? null : level.getEntity(entry.getKey());
            if (target == null || !target.isAlive() || gameTime > tracked.expiresAt()) {
                iterator.remove();
                continue;
            }
            tracked.observe(target.getDeltaMovement().length());
            if (!target.horizontalCollision && !target.verticalCollision) continue;
            double damage = ForcePhysicsRules.cappedCollisionDamage(
                    tracked.observedVelocity(), tracked.mass());
            long lastHit = LAST_DAMAGE.getOrDefault(target.getUUID(), Long.MIN_VALUE / 2L);
            if (damage > 0.0D && gameTime - lastHit >= ForcePhysicsRules.COLLISION_IMMUNITY_TICKS
                    && target instanceof LivingEntity living) {
                living.hurtServer(level, level.damageSources().flyIntoWall(), (float) damage);
                LAST_DAMAGE.put(target.getUUID(), gameTime);
                trim(LAST_DAMAGE, MAX_TRACKED_IMPULSES);
            }
            iterator.remove();
        }
    }

    private static <K, V> void trim(Map<K, V> map, int maximum) {
        while (map.size() > maximum) map.remove(map.keySet().iterator().next());
    }

    private static final class TrackedImpulse {
        private final String dimensionId;
        private final UUID casterId;
        private final double mass;
        private double observedVelocity;
        private final long expiresAt;

        private TrackedImpulse(
                String dimensionId, UUID casterId, double mass,
                double observedVelocity, long expiresAt
        ) {
            this.dimensionId = dimensionId;
            this.casterId = casterId;
            this.mass = mass;
            this.observedVelocity = observedVelocity;
            this.expiresAt = expiresAt;
        }

        String dimensionId() { return dimensionId; }
        @SuppressWarnings("unused") UUID casterId() { return casterId; }
        double mass() { return mass; }
        double observedVelocity() { return observedVelocity; }
        long expiresAt() { return expiresAt; }
        void observe(double velocity) {
            if (Double.isFinite(velocity)) observedVelocity = Math.max(observedVelocity, velocity);
        }
    }
}
