package galacticwars.clonewars.combat;

import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.registry.ModItems;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class LightsaberDeflectionService {
    private static final Set<Item> LIGHTSABERS = Set.of(
            ModItems.BLUE_LIGHTSABER.get(),
            ModItems.GREEN_LIGHTSABER.get(),
            ModItems.RED_LIGHTSABER.get(),
            ModItems.PURPLE_LIGHTSABER.get(),
            ModItems.YELLOW_LIGHTSABER.get(),
            ModItems.WHITE_LIGHTSABER.get());

    private LightsaberDeflectionService() {
    }

    public static boolean tryDeflect(BlasterBoltEntity bolt, LivingEntity defender, long gameTime) {
        if (!holdsLightsaber(defender)) {
            return false;
        }
        if (!bolt.canDeflectTowardOwner(defender)) {
            return false;
        }
        if (defender instanceof ServerPlayer player) {
            return LightsaberGuardService.tryDeflect(bolt, player, gameTime);
        }
        if (defender instanceof GalacticRecruitEntity recruit) {
            var incomingSourceDirection = bolt.getDeltaMovement().scale(-1.0D);
            if (incomingSourceDirection.lengthSqr() < 0.01D
                    || incomingSourceDirection.normalize().dot(recruit.getLookAngle()) < 0.35D) {
                return false;
            }
            var definition = galacticwars.clonewars.data.GameplayDataManager.snapshot()
                    .unitForEntityType(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                            .getKey(recruit.getType()).toString())
                    .orElse(null);
            int slot = definition == null ? -1 : definition.forceLoadout().indexOf("saber_guard");
            return slot >= 0
                    && recruit.npcForceEnergy() >= 5
                    && recruit.npcForceCooldownEnd(slot) <= gameTime
                    && recruit.commitNpcForceCast(slot, 5, gameTime + 20, definition.forceLoadout().size())
                    && bolt.deflectTowardOwner(defender);
        }
        return false;
    }

    public static boolean holdsLightsaber(LivingEntity defender) {
        return isLightsaber(defender.getMainHandItem()) || isLightsaber(defender.getOffhandItem());
    }

    static boolean isLightsaber(ItemStack stack) {
        return !stack.isEmpty() && LIGHTSABERS.contains(stack.getItem());
    }
}
