package galacticwars.clonewars.kingdom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import galacticwars.clonewars.army.ArmyFormation;
import galacticwars.clonewars.army.ArmyGroupLifecycleState;
import galacticwars.clonewars.army.ArmyGroupRecord;
import galacticwars.clonewars.army.ArmyLocation;
import galacticwars.clonewars.workforce.WorkerProfession;
import galacticwars.clonewars.workforce.CourierTransferAction;
import galacticwars.clonewars.workforce.CourierTransferType;
import galacticwars.clonewars.workforce.CourierWaypoint;
import galacticwars.clonewars.workforce.WorkAreaBounds;
import galacticwars.clonewars.workforce.WorkAreaConfiguration;

public final class ArmyWorkforcePersistenceTest {
    private ArmyWorkforcePersistenceTest() {
    }

    public static void main(String[] args) {
        armyMembershipOrderAndOrphaningPersistInDomainRecords();
        namedSquadLogisticsAndPatrolMetadataPersist();
        settlementsSupportRewardBoundedMultipleCommanders();
        worksiteCapacityAndAssignmentsAreAuthoritative();
        projectSlotsAndAssignmentReleaseAreAtomic();
        frontierWorksitesMigrateAndPersistConfiguration();
        workAreasPersistBoundsFiltersPriorityAndCourierRoutes();
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
                List.of(source, destination));
        WorksiteRecord configured = new WorksiteRecord(
                UUID.randomUUID(), "courier", "minecraft:overworld", 0, 64, 0, 8, 2)
                .configured(configuration);
        assertEquals(new WorkAreaBounds(12, 6, 8), configured.configuration().bounds(), "work area bounds");
        assertEquals(1, configured.configuration().itemFilters().size(), "normalized filters");
        assertEquals(80, configured.configuration().priority(), "work priority");
        assertEquals(2, configured.configuration().courierRoute().size(), "courier route");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + " expected " + expected + " but was " + actual);
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
