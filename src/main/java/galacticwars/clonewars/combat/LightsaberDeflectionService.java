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
        if (defender instanceof ServerPlayer player && !player.isCrouching()) {
            return false;
        }
        if (!bolt.canDeflectTowardOwner(defender)) {
            return false;
        }
        if (defender instanceof GalacticRecruitEntity recruit) {
            var unitClass = recruit.unitClassDefinition().orElse(null);
            if (unitClass == null || !unitClass.id().toString().equals("galacticwars:jedi_guardian")) {
                return false;
            }
            var activation = recruit.activateClassAbility(
                    "galacticwars:saber_guard", gameTime, false, 0.0D, false);
            if (!activation.accepted()) {
                return false;
            }
        }
        return bolt.deflectTowardOwner(defender);
    }

    public static boolean holdsLightsaber(LivingEntity defender) {
        return isLightsaber(defender.getMainHandItem()) || isLightsaber(defender.getOffhandItem());
    }

    private static boolean isLightsaber(ItemStack stack) {
        return !stack.isEmpty() && LIGHTSABERS.contains(stack.getItem());
    }
}
