package galacticwars.clonewars.network;

import galacticwars.clonewars.army.FieldCommandAction;
import galacticwars.clonewars.army.FieldCommandResult;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class FieldCommandPayloadValidationTest {
    private FieldCommandPayloadValidationTest() {
    }

    public static void main(String[] args) {
        UUID replayId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        FieldCommandRequestPayload request = new FieldCommandRequestPayload(
                replayId, FieldCommandAction.FOLLOW, List.of(groupId));
        if (!request.groupIds().equals(List.of(groupId)) || request.action() != FieldCommandAction.FOLLOW) {
            throw new AssertionError("valid field command request did not retain its selection");
        }
        FieldCommandRequestPayload patrolEdit = new FieldCommandRequestPayload(
                replayId, FieldCommandAction.SET_PATROL_WAYPOINT_WAIT, List.of(groupId),
                "Landing Pad Sweep", 3, 140);
        if (!patrolEdit.patrolRouteName().equals("Landing Pad Sweep")
                || patrolEdit.patrolWaypointIndex() != 3
                || patrolEdit.patrolWaypointWaitTicks() != 140) {
            throw new AssertionError("bounded patrol edit fields did not retain their values");
        }
        expectFailure(() -> new FieldCommandRequestPayload(
                        UUID.randomUUID(), FieldCommandAction.FOLLOW, List.of()),
                "command without a squad");
        expectFailure(() -> new FieldCommandRequestPayload(
                        UUID.randomUUID(), FieldCommandAction.REFRESH, List.of(groupId)),
                "refresh with a squad");
        expectFailure(() -> new FieldCommandRequestPayload(
                        UUID.randomUUID(), FieldCommandAction.FOLLOW,
                        Collections.nCopies(FieldCommandRequestPayload.MAX_GROUPS + 1, UUID.randomUUID())),
                "oversized squad selection");
        expectFailure(() -> new FieldCommandRequestPayload(
                        UUID.randomUUID(), FieldCommandAction.FOLLOW, List.of(groupId, groupId)),
                "duplicate squad selection");
        expectFailure(() -> new FieldCommandRequestPayload(
                        UUID.randomUUID(), FieldCommandAction.RENAME_PATROL_ROUTE, List.of(groupId),
                        "x".repeat(33), 0, 0),
                "oversized patrol name");
        expectFailure(() -> new FieldCommandRequestPayload(
                        UUID.randomUUID(), FieldCommandAction.SET_PATROL_WAYPOINT_WAIT, List.of(groupId),
                        "Route", FieldCommandRequestPayload.MAX_PATROL_WAYPOINTS, 0),
                "out-of-range patrol waypoint index");
        expectFailure(() -> new FieldCommandRequestPayload(
                        UUID.randomUUID(), FieldCommandAction.SET_PATROL_WAYPOINT_WAIT, List.of(groupId),
                        "Route", 0, 12_001),
                "out-of-range patrol wait");

        FieldCommandStatePayload.Squad squad = new FieldCommandStatePayload.Squad(
                groupId, "Alpha Squad", 3, "FOLLOW_OWNER", "LINE", "LIVE");
        FieldCommandStatePayload state = new FieldCommandStatePayload(
                replayId, FieldCommandResult.ACCEPTED, List.of(squad), true, false);
        if (!state.markedBlockAvailable() || state.markedEntityAvailable()
                || !state.squads().getFirst().name().equals("Alpha Squad")) {
            throw new AssertionError("valid field command state did not retain its projection");
        }
        expectFailure(() -> new FieldCommandStatePayload(
                        UUID.randomUUID(), FieldCommandResult.ACCEPTED, List.of(), true, true),
                "ambiguous marker state");
        expectFailure(() -> new FieldCommandStatePayload.Squad(
                        UUID.randomUUID(), "x".repeat(FieldCommandStatePayload.MAX_TEXT_BYTES + 1),
                        1, "FOLLOW_OWNER", "LINE", "LIVE"),
                "oversized squad label");
        expectFailure(() -> new FieldCommandStatePayload.Squad.Patrol("Route", 1),
                "single-waypoint patrol projection");

        System.out.println("FieldCommandPayloadValidationTest passed");
    }

    private static void expectFailure(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected validation failure.
        }
    }
}
