package middleearth.lotr.warmod.settlement;

import java.util.Objects;
import java.util.Optional;

import middleearth.lotr.warmod.workforce.WorkerProfession;

public final class KingdomSettlementPlanner {
    private static final int COURIER_PRESSURE_THRESHOLD = 32;

    private KingdomSettlementPlanner() {
    }

    public static KingdomWorkOrder planNextWorkOrder(
            KingdomSettlementState state,
            KingdomBaseBlueprint blueprint
    ) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(blueprint, "blueprint");

        if (!state.starterBaseRequested()) {
            return KingdomWorkOrder.idle("no_active_build_project");
        }
        KingdomBaseBuildDecision buildDecision = KingdomBaseBuildPlanner.planNext(
                blueprint,
                state.stockpile(),
                state.completedStarterBaseBlocks());
        if (buildDecision.action() == KingdomBaseBuildAction.COMPLETE) {
            return KingdomWorkOrder.idle("base_project_complete");
        }
        if (buildDecision.action() == KingdomBaseBuildAction.PLACE_BLOCK) {
            if (state.workerCount(WorkerProfession.BUILDER) <= 0) {
                return KingdomWorkOrder.recruit(WorkerProfession.BUILDER, "builder_required");
            }
            return new KingdomWorkOrder(
                    KingdomWorkOrderType.BUILD_BLOCK,
                    WorkerProfession.BUILDER,
                    buildDecision.itemId(),
                    buildDecision.quantity(),
                    "ready_to_place_base_block");
        }

        WorkerProfession profession = professionForResource(buildDecision.itemId());
        if (state.workerCount(profession) <= 0) {
            return KingdomWorkOrder.recruit(profession, "resource_worker_required");
        }
        return new KingdomWorkOrder(
                KingdomWorkOrderType.GATHER_RESOURCE,
                profession,
                buildDecision.itemId(),
                buildDecision.quantity(),
                "missing_build_supply");
    }

    public static Optional<WorkerProfession> recommendNextProfession(
            KingdomSettlementState state,
            KingdomBaseBlueprint blueprint
    ) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(blueprint, "blueprint");
        if (!state.hasHousingSpace()) {
            return Optional.empty();
        }
        if (!state.starterBaseRequested()) {
            return Optional.empty();
        }
        if (state.completedStarterBaseBlocks() >= blueprint.placements().size()) {
            return Optional.empty();
        }
        if (state.workerCount(WorkerProfession.BUILDER) <= 0) {
            return Optional.of(WorkerProfession.BUILDER);
        }
        if (state.stockpile().totalCount() >= COURIER_PRESSURE_THRESHOLD
                && state.workerCount(WorkerProfession.COURIER) <= 0) {
            return Optional.of(WorkerProfession.COURIER);
        }

        KingdomBaseBuildDecision buildDecision = KingdomBaseBuildPlanner.planNext(
                blueprint,
                state.stockpile(),
                state.completedStarterBaseBlocks());
        if (buildDecision.action() == KingdomBaseBuildAction.GATHER_SUPPLIES) {
            WorkerProfession profession = professionForResource(buildDecision.itemId());
            if (state.workerCount(profession) <= 0) {
                return Optional.of(profession);
            }
        }
        return Optional.empty();
    }

    private static WorkerProfession professionForResource(String itemId) {
        return switch (itemId) {
            case "kingdomwarsmiddleearth:middle_earth_stone", "kingdomwarsmiddleearth:mithril_ore",
                    "minecraft:cobblestone", "minecraft:stone" -> WorkerProfession.MINER;
            case "kingdomwarsmiddleearth:mallorn_log", "minecraft:oak_log", "minecraft:oak_planks",
                    "minecraft:planks" -> WorkerProfession.LUMBERJACK;
            case "minecraft:wheat", "minecraft:seeds" -> WorkerProfession.FARMER;
            case "minecraft:bread" -> WorkerProfession.COOK;
            default -> WorkerProfession.COURIER;
        };
    }
}
