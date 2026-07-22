package galacticwars.clonewars.force;

import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Bounded tactical Force evaluation for datapack-equipped recruits. */
public final class NpcForceRuntimeService {
    public static final int EVALUATION_INTERVAL_TICKS = 20;
    private static final int ENERGY_REGEN_PER_EVALUATION = 4;

    private NpcForceRuntimeService() {
    }

    public static void tick(ServerLevel level, GalacticRecruitEntity recruit) {
        recruit.regenerateNpcForceEnergy(ENERGY_REGEN_PER_EVALUATION);
        LivingEntity combatTarget = recruit.getTarget();
        if (combatTarget == null || !combatTarget.isAlive()) return;

        String entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(recruit.getType()).toString();
        List<String> loadout = GameplayDataManager.snapshot().unitForEntityType(entityTypeId)
                .map(definition -> definition.forceLoadout())
                .orElse(List.of());
        if (loadout.isEmpty()) return;

        recruit.getLookControl().setLookAt(combatTarget, 30.0F, 30.0F);
        int firstSlot = Math.floorMod(recruit.npcForceLoadoutCursor(), loadout.size());
        long gameTime = level.getGameTime();
        for (int offset = 0; offset < loadout.size(); offset++) {
            int slot = Math.floorMod(firstSlot + offset, loadout.size());
            LaunchContentDefinitions.ForceAbilityDefinition ability =
                    GameplayDataManager.snapshot().launchContent().forceAbilities().get(loadout.get(slot));
            if (ability == null || !ability.enabled()
                    || recruit.npcForceEnergy() < ability.energy()
                    || recruit.npcForceCooldownEnd(slot) > gameTime
                    || combatTarget.distanceTo(recruit) > ForcePhysicsRules.boundedRange(ability.range())) {
                continue;
            }
            List<Entity> targets = ForceTargetingService.targets(
                    level, recruit, ability.target(), ability.range());
            if (targets.isEmpty() && !ability.target().equals("self")) continue;
            int activeTicks = switch (ability.activation()) {
                case "charged" -> Math.max(ability.minChargeTicks(), ability.maxChargeTicks() / 2);
                case "channeled" -> Math.min(20, ability.maxChargeTicks());
                default -> 0;
            };
            ForceEffectReport report = ForceEffectExecutorRegistry.execute(
                    new ForceEffectContext(level, recruit, ability, targets, activeTicks));
            if (!report.succeeded()) continue;
            if (!recruit.commitNpcForceCast(
                    slot, ability.energy(), gameTime + ability.cooldownTicks(), loadout.size())) {
                return;
            }
            emitVisuals(level, recruit, ability.path(), report.affectedEntities().size());
            return; // At most one tactical cast per evaluation.
        }
    }

    private static void emitVisuals(
            ServerLevel level, GalacticRecruitEntity recruit, String tradition, int affected
    ) {
        level.sendParticles(
                tradition.equals("nightsister") ? ParticleTypes.WITCH
                        : tradition.equals("sith") ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD,
                recruit.getX(), recruit.getEyeY(), recruit.getZ(), Math.max(4, affected * 2),
                0.3D, 0.3D, 0.3D, 0.03D);
        level.playSound(null, recruit.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.HOSTILE, 0.65F, tradition.equals("sith") ? 0.7F : 1.05F);
    }
}
