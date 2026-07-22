package galacticwars.clonewars.army;

import java.util.List;
import java.util.UUID;

public final class ArmyMarchCoordinatorTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-00000000a101");
    private static final UUID KINGDOM = UUID.fromString("00000000-0000-0000-0000-00000000a102");
    private static final UUID COMMANDER = UUID.fromString("00000000-0000-0000-0000-00000000a103");
    private static final List<UUID> MEMBERS = List.of(
            UUID.fromString("00000000-0000-0000-0000-00000000a104"),
            UUID.fromString("00000000-0000-0000-0000-00000000a105"),
            UUID.fromString("00000000-0000-0000-0000-00000000a106"));
    private static final ArmyLocation START = new ArmyLocation("minecraft:overworld", 0, 64, 0);
    private static final ArmyLocation DESTINATION = new ArmyLocation("minecraft:overworld", 20, 64, 0);

    private ArmyMarchCoordinatorTest() {
    }

    public static void main(String[] args) {
        advancesTheGroupAnchorInPreferredFormation();
        compressesAtAChokepointWithoutChangingPreferredFormation();
        engagementStopsTheMovingAnchor();
        lowReadinessReducesButDoesNotReverseMovement();
        brieflyHoldsTheAnchorForStragglers();
        System.out.println("ArmyMarchCoordinatorTest passed");
    }

    private static void advancesTheGroupAnchorInPreferredFormation() {
        ArmyGroupRecord group = group(ArmyFormation.WEDGE);
        ArmyMarchCoordinator.Decision decision = ArmyMarchCoordinator.advance(
                group, START, DESTINATION, cohesiveMembers(), true, false, 0.25D, 1.0D, 20L);
        assertEquals(ArmyMarchPhase.MARCHING, decision.marchState().phase(), "open march phase");
        assertEquals(ArmyFormation.WEDGE, decision.marchState().activeFormation(), "preferred formation active");
        assertTrue(decision.anchor().x() > START.x() && decision.anchor().x() < DESTINATION.x(),
                "moving anchor advances by a bounded step");
    }

    private static void compressesAtAChokepointWithoutChangingPreferredFormation() {
        ArmyGroupRecord group = group(ArmyFormation.HOLLOW_SQUARE);
        ArmyMarchCoordinator.Decision decision = ArmyMarchCoordinator.advance(
                group, START, DESTINATION, cohesiveMembers(), false, false, 0.25D, 1.0D, 20L);
        assertEquals(ArmyMarchPhase.COMPRESSED, decision.marchState().phase(), "chokepoint phase");
        assertEquals(ArmyFormation.COLUMN, decision.marchState().activeFormation(), "compressed geometry");
        assertEquals(ArmyFormation.HOLLOW_SQUARE, group.order().formation(), "preferred formation persisted");
    }

    private static void engagementStopsTheMovingAnchor() {
        ArmyMarchCoordinator.Decision decision = ArmyMarchCoordinator.advance(
                group(ArmyFormation.LINE), START, DESTINATION, cohesiveMembers(),
                true, true, 0.25D, 1.0D, 20L);
        assertEquals(ArmyMarchPhase.ENGAGED, decision.marchState().phase(), "engaged phase");
        assertEquals(START, decision.anchor(), "engagement uses live anchor");
    }

    private static void lowReadinessReducesButDoesNotReverseMovement() {
        ArmyGroupRecord group = group(ArmyFormation.COLUMN);
        ArmyMarchCoordinator.Decision ready = ArmyMarchCoordinator.advance(
                group, START, DESTINATION, cohesiveMembers(), true, false, 0.25D, 1.0D, 20L);
        ArmyMarchCoordinator.Decision depleted = ArmyMarchCoordinator.advance(
                group, START, DESTINATION, cohesiveMembers(), true, false, 0.25D, 0.5D, 20L);
        assertTrue(depleted.anchor().x() > 0.0D && depleted.anchor().x() < ready.anchor().x(),
                "supply and morale readiness reduces march speed");
    }

    private static void brieflyHoldsTheAnchorForStragglers() {
        ArmyGroupRecord group = group(ArmyFormation.LINE);
        ArmyMarchCoordinator.Decision first = ArmyMarchCoordinator.advance(
                group, START, DESTINATION, cohesiveMembers(), true, false, 0.25D, 1.0D, 20L);
        ArmyGroupRecord marching = group.withSimulation(
                group.simulation().withMarch(first.anchor(), first.marchState(), 20L), group.snapshots());
        ArmyMarchCoordinator.Decision stragglerHold = ArmyMarchCoordinator.advance(
                marching, first.anchor(), DESTINATION,
                List.of(
                        first.anchor().blockPosition(),
                        new ArmyPosition(first.anchor().blockPosition().x(), 64, 1),
                        new ArmyPosition(-100, 64, -100)),
                true, false, 0.25D, 1.0D, 40L);
        assertEquals(ArmyMarchPhase.REFORMING, stragglerHold.marchState().phase(),
                "straggler recovery phase");
        assertEquals(first.anchor(), stragglerHold.anchor(), "leader anchor briefly held");
    }

    private static ArmyGroupRecord group(ArmyFormation formation) {
        return ArmyGroupRecord.create(OWNER, KINGDOM, COMMANDER, MEMBERS, formation, START, 0L)
                .withOrder(new ArmyGroupOrder(
                        ArmyCommandType.MOVE_TO_POSITION,
                        java.util.Optional.of(DESTINATION),
                        java.util.Optional.empty(), formation, 2));
    }

    private static List<ArmyPosition> cohesiveMembers() {
        return List.of(new ArmyPosition(0, 64, 1), new ArmyPosition(1, 64, 1), new ArmyPosition(-1, 64, 1));
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label);
        }
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
