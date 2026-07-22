package galacticwars.clonewars.client;

import galacticwars.clonewars.network.ObjectiveMarkerPayload;

/** Latest owner-only campaign marker received from the server. */
public final class ObjectiveMarkerClientState {
    private static ObjectiveMarkerPayload snapshot = ObjectiveMarkerPayload.inactive();

    private ObjectiveMarkerClientState() {
    }

    public static void update(ObjectiveMarkerPayload payload) {
        snapshot = payload == null ? ObjectiveMarkerPayload.inactive() : payload;
    }

    public static ObjectiveMarkerPayload snapshot() {
        return snapshot;
    }

    public static void clear() {
        snapshot = ObjectiveMarkerPayload.inactive();
    }
}
