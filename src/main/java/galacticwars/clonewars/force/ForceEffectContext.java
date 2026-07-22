package galacticwars.clonewars.force;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public record ForceEffectContext(
        ServerLevel level,
        LivingEntity caster,
        LaunchContentDefinitions.ForceAbilityDefinition ability,
        List<Entity> targets,
        int activeTicks
) {
    public ForceEffectContext {
        targets = List.copyOf(targets.stream().limit(ForcePhysicsRules.MAX_AOE_TARGETS).toList());
        activeTicks = ForcePhysicsRules.boundedChannelTicks(activeTicks);
    }
}
