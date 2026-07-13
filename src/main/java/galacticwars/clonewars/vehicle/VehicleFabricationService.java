package galacticwars.clonewars.vehicle;

import galacticwars.clonewars.economy.CreditTransactionService;
import galacticwars.clonewars.progression.LaunchContentCatalog;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.registry.ModItems;
import galacticwars.clonewars.settlement.CommandCenterBlockEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

public final class VehicleFabricationService {
    private VehicleFabricationService() {
    }

    public static String fabricate(
            ServerPlayer player, CommandCenterBlockEntity hall, String vehicleId
    ) {
        if (!(player.level() instanceof ServerLevel level)) return "server_only";
        var definition = LaunchContentCatalog.data().vehicles().get(vehicleId);
        var state = ProgressionSavedData.get(level).state(player.getUUID());
        if (definition == null) return "unknown_vehicle";
        for (String requirement : definition.deploymentRequirements()) {
            if (!requirementSatisfied(state, requirement)) {
                return requirement.equals("supply_depot")
                        ? "supply_depot_required" : "vehicle_quest_locked";
            }
        }
        Map<Item, Integer> inputs = resolveInputs(definition.fabricationInputs());
        if (inputs.isEmpty()) return "invalid_recipe";
        if (CreditTransactionService.containerBalance(hall) < definition.fabricationCredits()) {
            return "insufficient_credits";
        }
        for (Map.Entry<Item, Integer> input : inputs.entrySet()) {
            if (count(hall, input.getKey()) < input.getValue()) return "missing_materials";
        }
        CreditTransactionService.withdrawContainer(hall, definition.fabricationCredits());
        inputs.forEach((item, count) -> consume(hall, item, count));
        ItemStack kit = new ItemStack(kit(vehicleId).get());
        player.getInventory().add(kit);
        if (!kit.isEmpty()) player.spawnAtLocation(level, kit);
        return "accepted";
    }

    private static Map<Item, Integer> resolveInputs(Map<String, Integer> configured) {
        LinkedHashMap<Item, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : configured.entrySet()) {
            Identifier id;
            try {
                id = Identifier.parse(entry.getKey());
            } catch (RuntimeException invalid) {
                return Map.of();
            }
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item == null || !BuiltInRegistries.ITEM.getKey(item).equals(id)) return Map.of();
            result.put(item, entry.getValue());
        }
        return Map.copyOf(result);
    }

    private static boolean requirementSatisfied(
            galacticwars.clonewars.progression.ProgressionState state, String requirement
    ) {
        return requirement.equals("supply_depot")
                ? state.hasSubjectPath(ProgressionEventType.BUILDING_COMPLETED, "supply_depot")
                : state.unlocks().contains(requirement)
                || state.hasSubject(ProgressionEventType.QUEST_ADVANCED, requirement);
    }

    private static DeferredItem<? extends Item> kit(String id) {
        return switch (id) {
            case "barc_speeder" -> ModItems.BARC_SPEEDER_DEPLOYMENT_KIT;
            case "at_rt" -> ModItems.AT_RT_DEPLOYMENT_KIT;
            case "stap" -> ModItems.STAP_DEPLOYMENT_KIT;
            case "aat" -> ModItems.AAT_DEPLOYMENT_KIT;
            case "laat_gunship" -> ModItems.LAAT_GUNSHIP_DEPLOYMENT_KIT;
            default -> throw new IllegalArgumentException("unknown vehicle " + id);
        };
    }

    private static int count(CommandCenterBlockEntity hall, Item item) {
        int total = 0;
        for (int slot = 0; slot < hall.getContainerSize(); slot++) {
            if (hall.getItem(slot).is(item)) total += hall.getItem(slot).getCount();
        }
        return total;
    }

    private static void consume(CommandCenterBlockEntity hall, Item item, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < hall.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = hall.getItem(slot);
            if (!stack.is(item)) continue;
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
        }
        hall.setChanged();
    }
}
