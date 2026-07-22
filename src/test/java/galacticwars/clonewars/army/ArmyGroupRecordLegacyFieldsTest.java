package galacticwars.clonewars.army;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ArmyGroupRecordLegacyFieldsTest {
    private static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000021");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000022");
    private static final UUID KINGDOM_ID = UUID.fromString("00000000-0000-0000-0000-000000000023");
    private static final UUID COMMANDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000024");
    private static final UUID FIRST_MEMBER = UUID.fromString("00000000-0000-0000-0000-000000000025");
    private static final UUID SECOND_MEMBER = UUID.fromString("00000000-0000-0000-0000-000000000026");
    private static final UUID THIRD_MEMBER = UUID.fromString("00000000-0000-0000-0000-000000000027");
    private static final UUID NEW_MEMBER = UUID.fromString("00000000-0000-0000-0000-000000000028");
    private static final ArmyLocation RALLY = new ArmyLocation("minecraft:overworld", 4, 64, 8);
    private static final ArmyLocation PATROL_STOP = new ArmyLocation("minecraft:overworld", 24, 64, 8);

    private ArmyGroupRecordLegacyFieldsTest() {
    }

    public static void main(String[] args) {
        legacyFieldsRemainAbsentWhileEffectiveViewsPreserveCurrentBehavior();
        membershipChangesMaterializeOnlyReconciledFormationSlots();
        tacticsOnlyUpdatesPreserveActiveMarches();
        replacingPatrolRoutesResetsActiveMarches();
        directConstructionRejectsIncompletePatrolRoutes();

        System.out.println("ArmyGroupRecordLegacyFieldsTest passed");
    }

    private static void legacyFieldsRemainAbsentWhileEffectiveViewsPreserveCurrentBehavior() {
        ArmyGroupRecord legacy = legacyGroup();

        assertTrue(legacy.formationSlotAssignments().isEmpty(), "legacy slots remain absent");
        assertTrue(legacy.patrolPlan().isEmpty(), "legacy patrol plan remains absent");
        assertTrue(legacy.tactics().isEmpty(), "legacy tactics remain absent");
        assertEquals(List.of(
                new ArmyFormationSlotAssignment(FIRST_MEMBER, 0),
                new ArmyFormationSlotAssignment(SECOND_MEMBER, 1),
                new ArmyFormationSlotAssignment(THIRD_MEMBER, 2)),
                legacy.effectiveFormationSlotAssignments(), "legacy deterministic slots");

        ArmyPatrolPlan derivedPatrol = legacy.effectivePatrolPlan().orElseThrow();
        assertEquals(List.of(RALLY, PATROL_STOP), derivedPatrol.locations(), "legacy patrol locations");
        assertEquals(ArmyPatrolMode.LOOP, derivedPatrol.mode(), "legacy patrol mode");
        assertEquals(ArmyPatrolState.start(), derivedPatrol.state(), "legacy patrol state");
        assertEquals(ArmyGroupTactics.DEFAULT, legacy.effectiveTactics(), "legacy tactics default");

        assertTrue(legacy.formationSlotAssignments().isEmpty(), "effective slots do not migrate save data");
        assertTrue(legacy.patrolPlan().isEmpty(), "effective patrol does not migrate save data");
        assertTrue(legacy.tactics().isEmpty(), "effective tactics do not migrate save data");
    }

    private static void membershipChangesMaterializeOnlyReconciledFormationSlots() {
        ArmyGroupRecord updated = legacyGroup().withMembers(List.of(THIRD_MEMBER, NEW_MEMBER, SECOND_MEMBER));

        assertEquals(List.of(
                new ArmyFormationSlotAssignment(NEW_MEMBER, 0),
                new ArmyFormationSlotAssignment(SECOND_MEMBER, 1),
                new ArmyFormationSlotAssignment(THIRD_MEMBER, 2)),
                updated.formationSlotAssignments().orElseThrow(), "reconciled persisted slots");
        assertTrue(updated.patrolPlan().isEmpty(), "membership change does not force patrol migration");
        assertTrue(updated.tactics().isEmpty(), "membership change does not force tactics migration");
        assertEquals(1L, updated.simulation().revision(), "membership revision");
    }

    private static void tacticsOnlyUpdatesPreserveActiveMarches() {
        ArmyMarchState compressed = new ArmyMarchState(
                ArmyMarchPhase.COMPRESSED, ArmyFormation.COLUMN, 68, 90.0F, 120L);
        ArmyGroupRecord marching = legacyGroup().withSimulation(new ArmyGroupSimulation(
                ArmyGroupLifecycleState.LIVE, PATROL_STOP, 120L, 4L, 0L, "", compressed), List.of());

        ArmyGroupRecord updated = marching.withTactics(
                ArmyGroupTactics.DEFAULT.withFormationYaw(45.0F));

        assertEquals(compressed, updated.simulation().marchState(),
                "tactics update preserves active march state");
        assertEquals(PATROL_STOP, updated.simulation().anchor(),
                "tactics update preserves active march anchor");
        assertEquals(5L, updated.simulation().revision(),
                "tactics update advances optimistic-lock revision");
    }

    private static void replacingPatrolRoutesResetsActiveMarches() {
        ArmyPatrolPlan original = ArmyPatrolPlan.fromLegacyRoute(List.of(RALLY, PATROL_STOP)).orElseThrow();
        ArmyGroupOrder patrolOrder = new ArmyGroupOrder(
                ArmyCommandType.PATROL_ROUTE, Optional.of(RALLY), Optional.empty(), ArmyFormation.LINE, 2);
        ArmyMarchState marchingState = new ArmyMarchState(
                ArmyMarchPhase.MARCHING, ArmyFormation.LINE, 82, 0.0F, 140L);
        ArmyGroupRecord marching = legacyGroup()
                .withPatrolPlanAndOrder(original, patrolOrder)
                .withSimulation(new ArmyGroupSimulation(
                        ArmyGroupLifecycleState.LIVE, PATROL_STOP, 140L, 7L, 0L, "", marchingState), List.of());
        ArmyLocation replacementStop = new ArmyLocation("minecraft:overworld", 48, 64, 8);
        ArmyPatrolPlan replacement = ArmyPatrolPlan.fromLegacyRoute(
                List.of(RALLY, replacementStop)).orElseThrow();

        ArmyGroupRecord updated = marching.withPatrolPlanAndOrder(replacement, patrolOrder);

        assertEquals(ArmyMarchPhase.FORMING, updated.simulation().marchState().phase(),
                "new patrol route reforms from live members");
        assertEquals(PATROL_STOP, updated.simulation().anchor(),
                "new patrol retains the last authoritative anchor until the live coordinator advances");
        assertEquals(8L, updated.simulation().revision(), "new patrol revision");

        ArmyGroupRecord paused = marching.withPatrolProgressAndOrder(original.pause(), patrolOrder);
        assertEquals(marchingState, paused.simulation().marchState(),
                "patrol controls do not reset an unchanged route");
    }

    private static void directConstructionRejectsIncompletePatrolRoutes() {
        assertThrows(() -> new ArmyGroupRecord(
                GROUP_ID,
                OWNER_ID,
                KINGDOM_ID,
                Optional.of(COMMANDER_ID),
                List.of(FIRST_MEMBER),
                ArmyGroupOrder.follow(ArmyFormation.LINE),
                new ArmyGroupSimulation(ArmyGroupLifecycleState.LIVE, RALLY, 100L, 0L, 0L, ""),
                List.of(),
                "Invalid Legacy Squad",
                Optional.of(RALLY),
                List.of(RALLY),
                Optional.empty(),
                0), "single-waypoint direct patrol route");
    }

    private static ArmyGroupRecord legacyGroup() {
        return new ArmyGroupRecord(
                GROUP_ID,
                OWNER_ID,
                KINGDOM_ID,
                Optional.of(COMMANDER_ID),
                List.of(THIRD_MEMBER, FIRST_MEMBER, SECOND_MEMBER),
                ArmyGroupOrder.follow(ArmyFormation.LINE),
                new ArmyGroupSimulation(ArmyGroupLifecycleState.LIVE, RALLY, 100L, 0L, 0L, ""),
                List.of(),
                "Legacy 501st",
                Optional.of(RALLY),
                List.of(RALLY, PATROL_STOP),
                Optional.empty(),
                12);
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label + " expected to be true");
        }
    }

    private static void assertThrows(Runnable action, String label) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(label + " did not throw IllegalArgumentException");
    }
}
