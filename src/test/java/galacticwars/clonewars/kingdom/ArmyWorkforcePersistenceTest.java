package galacticwars.clonewars.kingdom;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import galacticwars.clonewars.army.ArmyFormation;
import galacticwars.clonewars.army.ArmyCommand;
import galacticwars.clonewars.army.ArmyCommandPolicy;
import galacticwars.clonewars.army.ArmyGroupLifecycleState;
import galacticwars.clonewars.army.ArmyGroupRecord;
import galacticwars.clonewars.army.ArmyLocation;
import galacticwars.clonewars.army.ArmyMemberSnapshot;
import galacticwars.clonewars.army.ArmySnapshotEquipment;
import galacticwars.clonewars.recruitment.RecruitDuty;
import galacticwars.clonewars.workforce.WorkerProfession;
import galacticwars.clonewars.workforce.CourierTransferAction;
import galacticwars.clonewars.workforce.CourierTransferType;
import galacticwars.clonewars.workforce.CourierRouteMode;
import galacticwars.clonewars.workforce.CourierRoutePlan;
import galacticwars.clonewars.workforce.CourierWaypoint;
import galacticwars.clonewars.workforce.WorkAreaBounds;
import galacticwars.clonewars.workforce.WorkAreaConfiguration;
import galacticwars.clonewars.workforce.WorkforceCodecs;

public final class ArmyWorkforcePersistenceTest {
    private ArmyWorkforcePersistenceTest() {
    }

    public static void main(String[] args) {
        armyMembershipOrderAndOrphaningPersistInDomainRecords();
        commanderOnlySquadsAcceptCommands();
        membershipChangesPruneStaleVirtualSnapshots();
        namedSquadLogisticsAndPatrolMetadataPersist();
        settlementsSupportRewardBoundedMultipleCommanders();
        worksiteCapacityAndAssignmentsAreAuthoritative();
        projectSlotsAndAssignmentReleaseAreAtomic();
        frontierWorksitesMigrateAndPersistConfiguration();
        workAreasPersistBoundsFiltersPriorityAndCourierRoutes();
        oversizedLegacyCourierRoutesMigrateToV8Bounds();
        workOrdersUseGuardedRevisionedTransitions();
        System.out.println("ArmyWorkforcePersistenceTest passed");
    }

    private static void armyMembershipOrderAndOrphaningPersistInDomainRecords() {
        UUID owner = UUID.randomUUID();
        UUID kingdom = UUID.randomUUID();
        UUID commander = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ArmyLocation anchor = new ArmyLocation("minecraft:overworld", 4, 64, 8);
        ArmyGroupRecord group = ArmyGroupRecord.create(
                owner, kingdom, commander, List.of(first, second, first), ArmyFormation.WEDGE, anchor, 100);
        assertEquals(List.of(first, second), group.memberIds(), "ordered unique membership");
        ArmyGroupRecord orphaned = group.orphan(anchor);
        assertEquals(ArmyGroupLifecycleState.ORPHANED, orphaned.simulation().lifecycleState(), "orphan state");
        assertEquals(group.memberIds(), orphaned.memberIds(), "orphan membership retention");
        assertEquals(ArmyFormation.WEDGE, orphaned.order().formation(), "formation retention");
    }

    private static void commanderOnlySquadsAcceptCommands() {
        UUID owner = UUID.randomUUID();
        UUID commander = UUID.randomUUID();
        ArmyLocation anchor = new ArmyLocation("minecraft:overworld", 4, 64, 8);
        ArmyGroupRecord group = ArmyGroupRecord.create(
                owner, UUID.randomUUID(), commander, List.of(), ArmyFormation.LINE, anchor, 100L);

        assertTrue(ArmyCommandPolicy.canIssue(
                ArmyCommand.holdPosition(owner, group.id(), anchor.blockPosition()),
                group.commandValidationState()).accepted(),
                "commander-only squad command validation");
        assertTrue(group.plannerState().recruitIds().isEmpty(),
                "commander remains outside member formation slots");
    }

    private static void membershipChangesPruneStaleVirtualSnapshots() {
        UUID owner = UUID.randomUUID();
        UUID kingdom = UUID.randomUUID();
        UUID commander = UUID.randomUUID();
        UUID retained = UUID.randomUUID();
        UUID removed = UUID.randomUUID();
        ArmyLocation anchor = new ArmyLocation("minecraft:overworld", 4, 64, 8);
        ArmyGroupRecord group = ArmyGroupRecord.create(
                        owner, kingdom, commander, List.of(retained, removed),
                        ArmyFormation.LINE, anchor, 100L)
                .withSnapshot(snapshot(commander, owner, kingdom, RecruitDuty.COMMANDER))
                .withSnapshot(snapshot(retained, owner, kingdom, RecruitDuty.SOLDIER))
                .withSnapshot(snapshot(removed, owner, kingdom, RecruitDuty.SOLDIER));

        ArmyGroupRecord updated = group.withMembers(List.of(retained));
        assertEquals(List.of(commander, retained),
                updated.snapshots().stream().map(ArmyMemberSnapshot::recruitId).toList(),
                "membership snapshot pruning");
    }

    private static ArmyMemberSnapshot snapshot(
            UUID recruitId,
            UUID ownerId,
            UUID kingdomId,
            RecruitDuty duty
    ) {
        return new ArmyMemberSnapshot(
                recruitId,
                "galacticwars:clone_trooper",
                "galacticwars:clone_trooper",
                ownerId,
                kingdomId,
                duty,
                20.0F,
                100,
                100,
                0,
                1L,
                ArmySnapshotEquipment.empty(),
                List.of(),
                "");
    }

    private static void worksiteCapacityAndAssignmentsAreAuthoritative() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        WorksiteRecord worksite = new WorksiteRecord(
                UUID.randomUUID(), "farmer", "minecraft:overworld", 0, 64, 0, 8, 2,
                List.of(WorkerProfession.FARMER), Optional.empty(), List.of(), List.of());
        WorksiteRecord full = worksite.assign(first).assign(second);
        assertTrue(!full.hasCapacity(), "two-slot capacity");
        try {
            full.assign(third);
            throw new AssertionError("full worksite accepted third assignment");
        } catch (IllegalStateException expected) {
            // Expected.
        }
        assertTrue(full.release(first).hasCapacity(), "released capacity");
    }

    private static void workOrdersUseGuardedRevisionedTransitions() {
        UUID recruit = UUID.randomUUID();
        WorkOrder queued = new WorkOrder(
                UUID.randomUUID(), WorkOrderType.BUILD, Optional.empty(), WorkOrderState.QUEUED,
                Optional.empty(), Optional.of(UUID.randomUUID()), "minecraft:overworld", 0, 64, 0,
                "minecraft:stone", 3, 0, "", 0);
        WorkOrder claimed = queued.claim(recruit);
        WorkOrder progressed = claimed.progress(2);
        WorkOrder completed = progressed.progress(1);
        assertEquals(1, claimed.revision(), "claim revision");
        assertEquals(WorkOrderState.IN_PROGRESS, progressed.state(), "progress state");
        assertEquals(WorkOrderState.COMPLETED, completed.state(), "completion state");
        assertTrue(completed.release() == completed, "terminal order cannot be released");
    }

    private static void projectSlotsAndAssignmentReleaseAreAtomic() {
        UUID settlementId = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID builder = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        WorksiteRecord fullFrontier = new WorksiteRecord(
                UUID.randomUUID(), "frontier", "minecraft:overworld", 0, 64, 0, 16, 2,
                List.of(WorkerProfession.FARMER, WorkerProfession.BUILDER), Optional.empty(),
                List.of(first, second), List.of());
        WorksiteRecord projectSlot = new WorksiteRecord(
                UUID.randomUUID(), "builder", "minecraft:overworld", 8, 64, 8, 16, 1,
                List.of(WorkerProfession.BUILDER), Optional.of(projectId), List.of(), List.of());
        SettlementRecord settlement = new SettlementRecord(
                settlementId, "minecraft:overworld", 0, 64, 0, 48, 4,
                List.of(first, second, builder), Optional.empty(), CommanderPolicy.defaults(),
                List.of(fullFrontier, projectSlot), List.of(), List.of(), List.of(),
                SettlementRewards.none(), 0);

        SettlementRecord reserved = settlement.reserveWorksite(
                builder, WorkerProfession.BUILDER, Optional.of(projectId));
        WorksiteRecord assigned = reserved.assignedWorksite(builder).orElseThrow();
        assertEquals(projectSlot.id(), assigned.id(), "project-specific builder slot");

        WorkOrder queued = new WorkOrder(
                UUID.randomUUID(), WorkOrderType.BUILD, Optional.empty(), WorkOrderState.QUEUED,
                Optional.of(projectSlot.id()), Optional.of(projectId), "minecraft:overworld",
                8, 64, 8, "minecraft:stone", 2, 0, "", 0);
        WorkOrder claimed = queued.claim(builder);
        SettlementRecord withOrder = reserved.withWorkOrder(queued, true).withWorkOrder(claimed, false);
        SettlementRecord released = withOrder.releaseWorkerAssignments(builder);
        assertTrue(released.assignedWorksite(builder).isEmpty(), "profession exit frees capacity");
        assertEquals(WorkOrderState.QUEUED,
                released.workOrder(queued.id()).orElseThrow().state(), "order released without deletion");
    }

    private static void frontierWorksitesMigrateAndPersistConfiguration() {
        UUID worker = UUID.randomUUID();
        WorksiteRecord legacySpecialized = new WorksiteRecord(
                UUID.randomUUID(), "farmer", "minecraft:overworld", 10, 64, 10, 8, 1);
        SettlementRecord migrated = new SettlementRecord(
                UUID.randomUUID(), "minecraft:overworld", 0, 64, 0, 48, 4,
                List.of(worker), Optional.empty(), CommanderPolicy.defaults(),
                List.of(legacySpecialized), List.of(), List.of(), List.of(),
                SettlementRewards.none(), 0);
        assertTrue(migrated.worksites().stream().anyMatch(site -> site.type().equals("frontier")),
                "legacy settlement receives frontier worksite");
        SettlementRecord assigned = migrated.reserveWorksite(worker, WorkerProfession.BUILDER);
        SettlementRecord configured = assigned.configureAssignedFrontierWorksite(
                worker, "minecraft:overworld", 5, 65, 6, 12);
        WorksiteRecord frontier = configured.assignedWorksite(worker).orElseThrow();
        assertEquals(5, frontier.x(), "frontier x");
        assertEquals(12, frontier.radius(), "frontier radius");
        assertEquals(25, frontier.configuration().bounds().width(), "frontier bounds follow radius");
    }

    private static void namedSquadLogisticsAndPatrolMetadataPersist() {
        ArmyLocation anchor = new ArmyLocation("minecraft:overworld", 4, 64, 8);
        ArmyLocation second = new ArmyLocation("minecraft:overworld", 24, 64, 8);
        UUID claimId = UUID.randomUUID();
        ArmyGroupRecord configured = ArmyGroupRecord.create(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), List.of(),
                        ArmyFormation.LINE, anchor, 100L)
                .withName("501st Vanguard")
                .withRallyPoint(anchor)
                .withPatrolRoute(List.of(anchor, second))
                .defendingClaim(claimId)
                .withSupplyUnits(64);
        assertEquals("501st Vanguard", configured.name(), "squad name");
        assertEquals(List.of(anchor, second), configured.patrolRoute(), "patrol route");
        assertEquals(claimId, configured.defendedClaimId().orElseThrow(), "defended claim");
        assertEquals(64, configured.supplyUnits(), "military supply");
    }

    private static void settlementsSupportRewardBoundedMultipleCommanders() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        SettlementRecord settlement = new SettlementRecord(
                UUID.randomUUID(), "minecraft:overworld", 0, 64, 0, 48, 4,
                List.of(first, second), Optional.empty(), CommanderPolicy.defaults(), List.of(),
                List.of(), List.of(), List.of(), new SettlementRewards(0, 2), 0)
                .withCommander(first)
                .withCommander(second);
        assertEquals(List.of(first, second), settlement.commanderIds(), "multiple commanders");
        assertTrue(!settlement.hasCommanderSlot(), "commander reward capacity");
    }

    private static void workAreasPersistBoundsFiltersPriorityAndCourierRoutes() {
        CourierWaypoint source = new CourierWaypoint(
                "minecraft:overworld", 1, 64, 2,
                List.of(new CourierTransferAction(CourierTransferType.TAKE, "galacticwars:energy_cell", 16)));
        CourierWaypoint destination = new CourierWaypoint(
                "minecraft:overworld", 20, 64, 2,
                List.of(new CourierTransferAction(CourierTransferType.FILL, "galacticwars:energy_cell", 32)));
        WorkAreaConfiguration configuration = new WorkAreaConfiguration(
                new WorkAreaBounds(12, 6, 8), true, 80, true,
                List.of("galacticwars:energy_cell", "galacticwars:energy_cell"),
                List.of(source, destination), CourierRouteMode.PING_PONG, 4L);
        WorkAreaConfiguration restored = KingdomCodecs.WORK_AREA_CONFIGURATION.parse(
                JsonOps.INSTANCE,
                KingdomCodecs.WORK_AREA_CONFIGURATION.encodeStart(JsonOps.INSTANCE, configuration)
                        .getOrThrow()).getOrThrow();
        WorksiteRecord configured = new WorksiteRecord(
                UUID.randomUUID(), "courier", "minecraft:overworld", 0, 64, 0, 8, 2)
                .configured(restored);
        assertEquals(new WorkAreaBounds(12, 6, 8), configured.configuration().bounds(), "work area bounds");
        assertEquals(1, configured.configuration().itemFilters().size(), "normalized filters");
        assertEquals(80, configured.configuration().priority(), "work priority");
        assertEquals(2, configured.configuration().courierRoute().size(), "courier route");
        assertEquals(CourierRouteMode.PING_PONG,
                configured.configuration().courierRouteMode(), "courier route mode");
        assertEquals(4L, configured.configuration().courierRouteRevision(), "courier route revision");
    }

    private static void oversizedLegacyCourierRoutesMigrateToV8Bounds() {
        JsonObject legacyConfiguration = new JsonObject();
        JsonObject bounds = new JsonObject();
        bounds.addProperty("width", 12);
        bounds.addProperty("height", 6);
        bounds.addProperty("depth", 8);
        legacyConfiguration.add("bounds", bounds);

        JsonArray route = new JsonArray();
        for (int waypointIndex = 0; waypointIndex < CourierRoutePlan.MAX_WAYPOINTS + 3; waypointIndex++) {
            JsonObject waypoint = new JsonObject();
            waypoint.addProperty("dimension", "minecraft:overworld");
            waypoint.addProperty("x", waypointIndex);
            waypoint.addProperty("y", 64);
            waypoint.addProperty("z", 2);
            JsonArray actions = new JsonArray();
            for (int actionIndex = 0;
                    actionIndex < WorkforceCodecs.MAX_ACTIONS_PER_WAYPOINT + 3;
                    actionIndex++) {
                JsonObject action = new JsonObject();
                action.addProperty("type", "take");
                action.addProperty("item", "galacticwars:energy_cell");
                action.addProperty("quantity", actionIndex + 1);
                actions.add(action);
            }
            waypoint.add("actions", actions);
            route.add(waypoint);
        }
        legacyConfiguration.add("courier_route", route);

        WorkAreaConfiguration migrated = KingdomCodecs.WORK_AREA_CONFIGURATION
                .parse(JsonOps.INSTANCE, legacyConfiguration)
                .getOrThrow();
        assertEquals(CourierRoutePlan.MAX_WAYPOINTS, migrated.courierRoute().size(),
                "legacy route truncation");
        assertEquals(0, migrated.courierRoute().getFirst().x(), "legacy route keeps first waypoint");
        assertEquals(CourierRoutePlan.MAX_WAYPOINTS - 1,
                migrated.courierRoute().getLast().x(), "legacy route keeps deterministic prefix");
        assertTrue(migrated.courierRoute().stream().allMatch(waypoint ->
                        waypoint.actions().size() == WorkforceCodecs.MAX_ACTIONS_PER_WAYPOINT),
                "legacy actions truncate on every retained waypoint");
        assertEquals(WorkforceCodecs.MAX_ACTIONS_PER_WAYPOINT,
                migrated.courierRoute().getFirst().actions().getLast().quantity(),
                "legacy actions keep deterministic prefix");

        JsonElement encoded = KingdomCodecs.WORK_AREA_CONFIGURATION
                .encodeStart(JsonOps.INSTANCE, migrated)
                .getOrThrow();
        JsonArray encodedRoute = encoded.getAsJsonObject().getAsJsonArray("courier_route");
        assertEquals(CourierRoutePlan.MAX_WAYPOINTS, encodedRoute.size(), "v8 encoded route bound");
        assertTrue(encodedRoute.asList().stream().allMatch(waypoint ->
                        waypoint.getAsJsonObject().getAsJsonArray("actions").size()
                                <= WorkforceCodecs.MAX_ACTIONS_PER_WAYPOINT),
                "v8 encoded action bound");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + " expected " + expected + " but was " + actual);
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
