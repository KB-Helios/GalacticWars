package galacticwars.clonewars.settlement;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import galacticwars.clonewars.workforce.ResourceInventory;
import galacticwars.clonewars.workforce.WorkerProfession;

public record KingdomSettlementState(
        ResourceInventory stockpile,
        Map<WorkerProfession, Integer> workersByProfession,
        int population,
        int housingCapacity,
        int completedStarterBaseBlocks,
        boolean starterBaseRequested
) {
    public KingdomSettlementState {
        stockpile = Objects.requireNonNull(stockpile, "stockpile");
        Objects.requireNonNull(workersByProfession, "workersByProfession");
        EnumMap<WorkerProfession, Integer> normalized = new EnumMap<>(WorkerProfession.class);
        for (Map.Entry<WorkerProfession, Integer> entry : workersByProfession.entrySet()) {
            WorkerProfession profession = Objects.requireNonNull(entry.getKey(), "profession");
            int count = Objects.requireNonNull(entry.getValue(), "count");
            if (count > 0) {
                normalized.merge(profession, count, Integer::sum);
            }
        }
        workersByProfession = Collections.unmodifiableMap(normalized);
        if (population < 0) {
            throw new IllegalArgumentException("population cannot be negative");
        }
        if (housingCapacity < 0) {
            throw new IllegalArgumentException("housingCapacity cannot be negative");
        }
        if (completedStarterBaseBlocks < 0) {
            throw new IllegalArgumentException("completedStarterBaseBlocks cannot be negative");
        }
    }

    public static KingdomSettlementState empty() {
        return new KingdomSettlementState(ResourceInventory.empty(), Map.of(), 0, 8, 0, true);
    }

    public KingdomSettlementState withStockpile(ResourceInventory stockpile) {
        return new KingdomSettlementState(
                stockpile,
                workersByProfession,
                population,
                housingCapacity,
                completedStarterBaseBlocks,
                starterBaseRequested);
    }

    public KingdomSettlementState withWorker(WorkerProfession profession, int count) {
        Objects.requireNonNull(profession, "profession");
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        EnumMap<WorkerProfession, Integer> updated = new EnumMap<>(WorkerProfession.class);
        updated.putAll(workersByProfession);
        updated.merge(profession, count, Integer::sum);
        return new KingdomSettlementState(
                stockpile,
                updated,
                population + count,
                housingCapacity,
                completedStarterBaseBlocks,
                starterBaseRequested);
    }

    public int workerCount(WorkerProfession profession) {
        return workersByProfession.getOrDefault(profession, 0);
    }

    public boolean hasHousingSpace() {
        return population < housingCapacity;
    }
}
