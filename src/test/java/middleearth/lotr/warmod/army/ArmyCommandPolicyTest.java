package middleearth.lotr.warmod.army;

import java.util.UUID;

import middleearth.lotr.warmod.faction.FactionAlignment;
import middleearth.lotr.warmod.faction.FactionId;

public final class ArmyCommandPolicyTest {
    private ArmyCommandPolicyTest() {
    }

    public static void main(String[] args) {
        acceptsOwnerCommandWithValidContext();
        rejectsInvalidOwnershipAndGroupContext();
        rejectsLowAlignment();
        rejectsMalformedCommandPayloads();
        rejectsMissingContextWithStableReasons();

        System.out.println("ArmyCommandPolicyTest passed");
    }

    private static void acceptsOwnerCommandWithValidContext() {
        ArmyCommandValidation validation = ArmyCommandPolicy.canIssue(
                ArmyCommand.moveToPosition(ownerId(), groupId(), new ArmyPosition(12, 64, -8)),
                populatedGroup(),
                alignment(12),
                FactionId.of("gondor"),
                10);

        assertTrue(validation.accepted(), "valid command accepted");
        assertEquals("accepted", validation.reasonCode(), "valid command reason");
    }

    private static void rejectsInvalidOwnershipAndGroupContext() {
        assertRejected("not_owner", ArmyCommandPolicy.canIssue(
                ArmyCommand.followOwner(otherPlayerId(), groupId()),
                populatedGroup(),
                alignment(12),
                FactionId.of("gondor"),
                10), "non-owner command");

        assertRejected("group_mismatch", ArmyCommandPolicy.canIssue(
                ArmyCommand.followOwner(ownerId(), otherGroupId()),
                populatedGroup(),
                alignment(12),
                FactionId.of("gondor"),
                10), "wrong group command");

        assertRejected("empty_group", ArmyCommandPolicy.canIssue(
                ArmyCommand.followOwner(ownerId(), groupId()),
                ArmyGroupState.create(groupId(), ownerId()),
                alignment(12),
                FactionId.of("gondor"),
                10), "empty group command");
    }

    private static void rejectsLowAlignment() {
        assertRejected("alignment_too_low", ArmyCommandPolicy.canIssue(
                ArmyCommand.protectOwner(ownerId(), groupId()),
                populatedGroup(),
                alignment(9),
                FactionId.of("gondor"),
                10), "low alignment command");
    }

    private static void rejectsMalformedCommandPayloads() {
        assertRejected("invalid_payload", ArmyCommandPolicy.canIssue(
                new ArmyCommand(ArmyCommandType.MOVE_TO_POSITION, ownerId(), groupId(), null, null),
                populatedGroup(),
                alignment(12),
                FactionId.of("gondor"),
                10), "move command without position");

        assertRejected("invalid_payload", ArmyCommandPolicy.canIssue(
                new ArmyCommand(ArmyCommandType.ATTACK_TARGET, ownerId(), groupId(), null, null),
                populatedGroup(),
                alignment(12),
                FactionId.of("gondor"),
                10), "attack command without target");

        assertRejected("invalid_payload", ArmyCommandPolicy.canIssue(
                new ArmyCommand(ArmyCommandType.CLEAR_TARGET, ownerId(), groupId(), new ArmyPosition(1, 2, 3), null),
                populatedGroup(),
                alignment(12),
                FactionId.of("gondor"),
                10), "clear command with position");
    }

    private static void rejectsMissingContextWithStableReasons() {
        assertRejected("missing_command", ArmyCommandPolicy.canIssue(
                null,
                populatedGroup(),
                alignment(12),
                FactionId.of("gondor"),
                10), "missing command");

        assertRejected("missing_group", ArmyCommandPolicy.canIssue(
                ArmyCommand.followOwner(ownerId(), groupId()),
                null,
                alignment(12),
                FactionId.of("gondor"),
                10), "missing group");

        assertRejected("unknown_player", ArmyCommandPolicy.canIssue(
                ArmyCommand.followOwner(ownerId(), groupId()),
                populatedGroup(),
                null,
                FactionId.of("gondor"),
                10), "missing alignment");

        assertRejected("unknown_player", ArmyCommandPolicy.canIssue(
                ArmyCommand.followOwner(ownerId(), groupId()),
                populatedGroup(),
                alignment(otherPlayerId(), 12),
                FactionId.of("gondor"),
                10), "mismatched alignment player");

        assertRejected("unknown_player", ArmyCommandPolicy.canIssue(
                ArmyCommand.followOwner(ownerId(), groupId()),
                populatedGroup(),
                alignment(otherPlayerId(), 12),
                FactionId.of("gondor"),
                10), "mismatched alignment player");

        assertRejected("unknown_faction", ArmyCommandPolicy.canIssue(
                ArmyCommand.followOwner(ownerId(), groupId()),
                populatedGroup(),
                alignment(12),
                null,
                10), "missing faction");
    }

    private static ArmyGroupState populatedGroup() {
        return ArmyGroupState.create(groupId(), ownerId()).withRecruit(recruitId());
    }

    private static FactionAlignment alignment(int score) {
        return alignment(ownerId(), score);
    }

    private static FactionAlignment alignment(int score) {
        return alignment(ownerId(), score);
    }

    private static FactionAlignment alignment(UUID playerId, int score) {
        return FactionAlignment.empty(playerId).withAddedScore(FactionId.of("gondor"), score);
    }

    private static UUID ownerId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000301");
    }

    private static UUID otherPlayerId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000302");
    }

    private static UUID groupId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000303");
    }

    private static UUID otherGroupId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000304");
    }

    private static UUID recruitId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000305");
    }

    private static void assertRejected(String expectedReason, ArmyCommandValidation validation, String label) {
        assertTrue(!validation.accepted(), label + " rejected");
        assertEquals(expectedReason, validation.reasonCode(), label + " reason");
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
}
