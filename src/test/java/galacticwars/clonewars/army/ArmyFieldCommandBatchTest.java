package galacticwars.clonewars.army;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Exercises the pure all-or-nothing preflight used before SavedData mutation. */
public final class ArmyFieldCommandBatchTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-00000000a001");
    private static final UUID KINGDOM = UUID.fromString("00000000-0000-0000-0000-00000000a002");
    private static final UUID COMMANDER_ONE = UUID.fromString("00000000-0000-0000-0000-00000000a003");
    private static final UUID COMMANDER_TWO = UUID.fromString("00000000-0000-0000-0000-00000000a004");

    private ArmyFieldCommandBatchTest() {
    }

    public static void main(String[] args) {
        preparesEveryReplacementAtTheObservedRevision();
        rejectsAnInvalidMemberWithoutReturningPartialUpdates();
        rejectsDuplicateGroupsAndBadRevisionTransitions();
        replayIdsAreScopedAndBoundedPerPlayer();

        System.out.println("ArmyFieldCommandBatchTest passed");
    }

    private static void preparesEveryReplacementAtTheObservedRevision() {
        ArmyGroupRecord first = group(COMMANDER_ONE, 4L);
        ArmyGroupRecord second = group(COMMANDER_TWO, 9L);

        ArmyFieldCommandBatch batch = ArmyFieldCommandBatch.prepare(
                List.of(first, second), group -> group.withOrder(new ArmyGroupOrder(
                        ArmyCommandType.HOLD_POSITION,
                        Optional.of(group.simulation().anchor()),
                        Optional.empty(),
                        group.order().formation(),
                        group.order().spacing())))
                .orElseThrow(() -> new AssertionError("valid multi-squad update was rejected"));

        assertEquals(2, batch.replacements().size(), "replacement count");
        assertEquals(4L, batch.expectedRevisions().get(first.id()), "first expected revision");
        assertEquals(9L, batch.expectedRevisions().get(second.id()), "second expected revision");
        assertEquals(5L, batch.replacements().getFirst().simulation().revision(), "first next revision");
        assertEquals(10L, batch.replacements().get(1).simulation().revision(), "second next revision");
    }

    private static void rejectsAnInvalidMemberWithoutReturningPartialUpdates() {
        ArmyGroupRecord first = group(COMMANDER_ONE, 2L);
        ArmyGroupRecord second = group(COMMANDER_TWO, 2L);

        Optional<ArmyFieldCommandBatch> batch = ArmyFieldCommandBatch.prepare(
                List.of(first, second), group -> group.id().equals(second.id())
                        ? group
                        : group.withOrder(new ArmyGroupOrder(
                                ArmyCommandType.HOLD_POSITION,
                                Optional.of(group.simulation().anchor()), Optional.empty(),
                                group.order().formation(), group.order().spacing())));

        assertTrue(batch.isEmpty(), "a single stale replacement rejects the entire batch");
    }

    private static void rejectsDuplicateGroupsAndBadRevisionTransitions() {
        ArmyGroupRecord first = group(COMMANDER_ONE, 1L);
        assertTrue(ArmyFieldCommandBatch.prepare(List.of(first, first), group -> group.withOrder(new ArmyGroupOrder(
                ArmyCommandType.HOLD_POSITION, Optional.of(group.simulation().anchor()), Optional.empty(),
                group.order().formation(), group.order().spacing()))).isEmpty(),
                "duplicate group selection");
        assertTrue(ArmyFieldCommandBatch.prepare(List.of(first), group -> new ArmyGroupRecord(
                group.id(), group.ownerId(), group.kingdomId(), group.commanderId(), group.memberIds(), group.order(),
                group.simulation(), group.snapshots(), group.name(), group.rallyPoint(), group.patrolRoute(),
                group.defendedClaimId(), group.supplyUnits(), group.formationSlotAssignments(), group.patrolPlan(),
                group.tactics())).isEmpty(), "unchanged revision");
    }

    private static void replayIdsAreScopedAndBoundedPerPlayer() {
        ArmyFieldCommandReplayGuard guard = new ArmyFieldCommandReplayGuard(2, 2);
        UUID firstPlayer = UUID.fromString("00000000-0000-0000-0000-00000000b001");
        UUID secondPlayer = UUID.fromString("00000000-0000-0000-0000-00000000b002");
        UUID replay = UUID.fromString("00000000-0000-0000-0000-00000000c001");

        assertTrue(guard.claim(firstPlayer, replay), "first replay accepted");
        assertFalse(guard.claim(firstPlayer, replay), "same player replay rejected");
        assertTrue(guard.claim(secondPlayer, replay), "same token is independent for another player");
        assertTrue(guard.claim(firstPlayer, UUID.fromString("00000000-0000-0000-0000-00000000c002")),
                "second replay accepted");
        assertTrue(guard.claim(firstPlayer, UUID.fromString("00000000-0000-0000-0000-00000000c003")),
                "third replay accepted while oldest is evicted");
        assertTrue(guard.claim(firstPlayer, replay), "bounded history evicts oldest replay");
        guard.clear(firstPlayer);
        assertTrue(guard.claim(firstPlayer, UUID.fromString("00000000-0000-0000-0000-00000000c003")),
                "player cleanup releases replay history");
    }

    private static ArmyGroupRecord group(UUID commander, long revision) {
        UUID member = UUID.nameUUIDFromBytes(("member:" + commander).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ArmyLocation anchor = new ArmyLocation("minecraft:overworld", 12.0D, 64.0D, -4.0D);
        ArmyGroupRecord created = ArmyGroupRecord.create(
                OWNER, KINGDOM, commander, List.of(member), ArmyFormation.LINE, anchor, 0L);
        ArmyGroupSimulation simulation = new ArmyGroupSimulation(
                created.simulation().lifecycleState(), anchor, 0L, revision, 0L, "");
        return new ArmyGroupRecord(
                created.id(), OWNER, KINGDOM, Optional.of(commander), List.of(member), created.order(), simulation,
                List.of(), created.name(), Optional.of(anchor), List.of(), Optional.empty(), 0,
                created.formationSlotAssignments(), Optional.empty(), Optional.empty());
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertFalse(boolean condition, String label) {
        if (condition) {
            throw new AssertionError(label + " expected false");
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label + " expected true");
        }
    }
}
