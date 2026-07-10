package middleearth.lotr.warmod.workforce;

import java.util.Map;
import java.util.Objects;

public final class WorkerResourcePlanner {
    private static final Map<WorkerProfession, String> PRIMARY_RESOURCES = Map.of(
            WorkerProfession.FARMER, "minecraft:wheat",
            WorkerProfession.LUMBERJACK, "minecraft:oak_log",
            WorkerProfession.FISHERMAN, "minecraft:cod",
            WorkerProfession.ANIMAL_FARMER, "minecraft:leather",
            WorkerProfession.MINER, "minecraft:cobblestone",
            WorkerProfession.BUILDER, "kingdomwarsmiddleearth:middle_earth_stone",
            WorkerProfession.COOK, "minecraft:bread",
            WorkerProfession.MERCHANT, "minecraft:emerald",
            WorkerProfession.COURIER, "minecraft:chest");

    private WorkerResourcePlanner() {
    }

    public static WorkerResourceDecision plan(
            WorkerProfession profession,
            WorkerWorksite worksite,
            ResourceInventory carried,
            ResourceInventory storage,
            int carriedLimit
    ) {
        Objects.requireNonNull(profession, "profession");
        Objects.requireNonNull(carried, "carried");
        Objects.requireNonNull(storage, "storage");

        WorkerTaskDecision task = WorkerTaskPlanner.plan(profession, worksite);
        if (task.taskType() == WorkerTaskType.IDLE || task.taskType() == WorkerTaskType.REASSIGN_WORKSITE) {
            return WorkerResourceDecision.idle(task.reasonCode());
        }

        int effectiveLimit = Math.max(1, carriedLimit);
        if (carried.totalCount() >= effectiveLimit) {
            return carried.firstResource()
                    .map(entry -> new WorkerResourceDecision(
                            WorkerResourceAction.DEPOSIT_TO_STORAGE,
                            entry.getKey(),
                            entry.getValue(),
                            "inventory_full"))
                    .orElseGet(() -> WorkerResourceDecision.idle("nothing_to_deposit"));
        }

        String itemId = PRIMARY_RESOURCES.get(profession);
        if (profession == WorkerProfession.COURIER && storage.count(itemId) > 0) {
            return new WorkerResourceDecision(WorkerResourceAction.DELIVER_TO_WORKSITE, itemId, 1, "ready_to_deliver");
        }
        if (profession == WorkerProfession.BUILDER && storage.count(itemId) > 0) {
            return new WorkerResourceDecision(WorkerResourceAction.BUILD_FROM_STORAGE, itemId, 1, "ready_to_build");
        }
        return new WorkerResourceDecision(WorkerResourceAction.GATHER_RESOURCE, itemId, 1, "ready_to_gather");
    }
}
