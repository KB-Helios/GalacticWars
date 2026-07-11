package galacticwars.clonewars.workforce;

import java.util.Map;
import java.util.Objects;

public final class WorkerLogisticsPlanner {
    private WorkerLogisticsPlanner() {
    }

    public static WorkerLogisticsDecision plan(
            WorkerLogisticsRoute route,
            WorkerSupplyRequest request,
            ResourceInventory carried,
            ResourceInventory storage,
            int carriedLimit
    ) {
        Objects.requireNonNull(carried, "carried");
        Objects.requireNonNull(storage, "storage");
        if (route == null) {
            return WorkerLogisticsDecision.idle(request == null ? "" : request.itemId(), null, "missing_route");
        }
        if (request == null) {
            return WorkerLogisticsDecision.idle("", route, "no_supply_request");
        }

        int carryingRequested = carried.count(request.itemId());
        if (carryingRequested > 0) {
            return new WorkerLogisticsDecision(
                    WorkerResourceAction.DELIVER_TO_WORKSITE,
                    request.itemId(),
                    Math.min(carryingRequested, request.quantity()),
                    route,
                    "carrying_requested_item");
        }

        int effectiveLimit = Math.max(1, carriedLimit);
        if (carried.totalCount() >= effectiveLimit) {
            return carried.firstResource()
                    .map(entry -> new WorkerLogisticsDecision(
                            WorkerResourceAction.DEPOSIT_TO_STORAGE,
                            entry.getKey(),
                            entry.getValue(),
                            route,
                            "inventory_full_unrequested"))
                    .orElseGet(() -> WorkerLogisticsDecision.idle(request.itemId(), route, "nothing_to_deposit"));
        }

        int available = storage.count(request.itemId());
        if (available <= 0) {
            return WorkerLogisticsDecision.idle(request.itemId(), route, "storage_missing_requested_item");
        }

        int freeSpace = Math.max(1, effectiveLimit - carried.totalCount());
        int quantity = Math.min(Math.min(request.quantity(), available), freeSpace);
        return new WorkerLogisticsDecision(
                WorkerResourceAction.WITHDRAW_FROM_STORAGE,
                request.itemId(),
                quantity,
                route,
                "storage_can_fill_request");
    }

    public static WorkerLogisticsDecision planAnyAvailableSupply(
            WorkerLogisticsRoute route,
            ResourceInventory carried,
            ResourceInventory storage,
            int carriedLimit
    ) {
        Objects.requireNonNull(carried, "carried");
        Objects.requireNonNull(storage, "storage");

        return carried.firstResource()
                .map(entry -> plan(route, new WorkerSupplyRequest(entry.getKey(), entry.getValue()), carried, storage, carriedLimit))
                .or(() -> storage.firstResource()
                        .map(entry -> plan(route, supplyRequestForStoredResource(entry), carried, storage, carriedLimit)))
                .orElseGet(() -> WorkerLogisticsDecision.idle("", route, "storage_empty"));
    }

    private static WorkerSupplyRequest supplyRequestForStoredResource(Map.Entry<String, Integer> entry) {
        return new WorkerSupplyRequest(entry.getKey(), entry.getValue());
    }
}
