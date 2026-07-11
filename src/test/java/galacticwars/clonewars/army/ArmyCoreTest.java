package galacticwars.clonewars.army;

import java.util.UUID;

import galacticwars.clonewars.faction.FactionAlignment;
import galacticwars.clonewars.faction.FactionId;

public final class ArmyCoreTest {
    private ArmyCoreTest() {
    }

    public static void main(String[] args) {
        appliesOwnerIssuedFollowCommandToRecruit();
        createsCommandPayloadsForAllArmyOrders();
        rejectsCommandFromNonOwner();
        tracksGroupMembershipAndAppliesMoveCommand();
        normalizesFactionIdsAndStoresAlignment();

        System.out.println("ArmyCoreTest passed");
    }

    private static void appliesOwnerIssuedFollowCommandToRecruit() {
        UUID ownerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID recruitId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        RecruitState recruit = RecruitState.createOwned(recruitId, ownerId, groupId);

        RecruitState updated = recruit.applyCommand(ArmyCommand.followOwner(ownerId, groupId));

        assertEquals(ArmyCommandType.FOLLOW_OWNER, updated.currentCommand().type(), "follow command type");
        assertEquals(ownerId, updated.ownerId(), "recruit owner id");
        assertEquals(groupId, updated.groupId(), "recruit group id");
    }

    private static void createsCommandPayloadsForAllArmyOrders() {
        UUID ownerId = UUID.fromString("00000000-0000-0000-0000-000000000041");
        UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000000042");
        UUID targetEntityId = UUID.fromString("00000000-0000-0000-0000-000000000043");
        ArmyPosition holdPosition = new ArmyPosition(7, 65, 9);

        assertEquals(ArmyCommandType.HOLD_POSITION, ArmyCommand.holdPosition(ownerId, groupId, holdPosition).type(),
                "hold command type");
        assertEquals(holdPosition, ArmyCommand.holdPosition(ownerId, groupId, holdPosition).targetPosition(),
                "hold command position");
        assertEquals(ArmyCommandType.PROTECT_OWNER, ArmyCommand.protectOwner(ownerId, groupId).type(),
                "protect command type");
        assertEquals(ArmyCommandType.ATTACK_TARGET, ArmyCommand.attackTarget(ownerId, groupId, targetEntityId).type(),
                "attack command type");
        assertEquals(targetEntityId, ArmyCommand.attackTarget(ownerId, groupId, targetEntityId).targetEntityId(),
                "attack command target");
        assertEquals(ArmyCommandType.CLEAR_TARGET, ArmyCommand.clearTarget(ownerId, groupId).type(),
                "clear command type");
    }

    private static void rejectsCommandFromNonOwner() {
        UUID ownerId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID otherPlayerId = UUID.fromString("00000000-0000-0000-0000-000000000012");
        UUID recruitId = UUID.fromString("00000000-0000-0000-0000-000000000013");
        UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000000014");
        RecruitState recruit = RecruitState.createOwned(recruitId, ownerId, groupId);

        assertThrows(SecurityException.class, () -> recruit.applyCommand(ArmyCommand.holdPosition(otherPlayerId, groupId,
                new ArmyPosition(10, 64, -3))), "non-owner command");
    }

    private static void tracksGroupMembershipAndAppliesMoveCommand() {
        UUID ownerId = UUID.fromString("00000000-0000-0000-0000-000000000021");
        UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000000022");
        UUID firstRecruit = UUID.fromString("00000000-0000-0000-0000-000000000023");
        UUID secondRecruit = UUID.fromString("00000000-0000-0000-0000-000000000024");
        ArmyPosition target = new ArmyPosition(32, 70, 48);

        ArmyGroupState group = ArmyGroupState.create(groupId, ownerId)
                .withRecruit(firstRecruit)
                .withRecruit(secondRecruit)
                .applyCommand(ArmyCommand.moveToPosition(ownerId, groupId, target));

        assertTrue(group.containsRecruit(firstRecruit), "first recruit membership");
        assertTrue(group.containsRecruit(secondRecruit), "second recruit membership");
        assertEquals(ArmyCommandType.MOVE_TO_POSITION, group.currentCommand().type(), "group move command type");
        assertEquals(target, group.currentCommand().targetPosition(), "group move target");

        ArmyGroupState removed = group.withoutRecruit(firstRecruit);
        assertTrue(!removed.containsRecruit(firstRecruit), "removed recruit membership");
        assertTrue(group.containsRecruit(firstRecruit), "original group remains immutable");
    }

    private static void normalizesFactionIdsAndStoresAlignment() {
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000031");
        FactionId republic = FactionId.of("republic");
        FactionAlignment alignment = FactionAlignment.empty(playerId)
                .withAddedScore(republic, 15)
                .withAddedScore(FactionId.of("galacticwars:republic"), -4);

        assertEquals("galacticwars:republic", republic.toString(), "normalized faction id");
        assertEquals(11, alignment.score(republic), "accumulated alignment score");
        assertEquals(0, alignment.score(FactionId.of("separatist")), "missing faction alignment score");
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

    private static <T extends Throwable> void assertThrows(Class<T> expectedType, ThrowingRunnable runnable, String label) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                return;
            }
            throw new AssertionError(label + " threw " + throwable.getClass().getName() + " instead of "
                    + expectedType.getName(), throwable);
        }

        throw new AssertionError(label + " did not throw " + expectedType.getName());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
