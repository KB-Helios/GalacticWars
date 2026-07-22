package galacticwars.clonewars.client;

import galacticwars.clonewars.network.ServerPolicyPayload;

/** Read-only server-policy projection; no client preference mutates these values. */
public final class ServerPolicyClientState {
    private static final ServerPolicyPayload DEFAULT_POLICY =
            new ServerPolicyPayload(false, true, false, true, true, true);
    private static ServerPolicyPayload snapshot = DEFAULT_POLICY;

    private ServerPolicyClientState() {
    }

    public static void update(ServerPolicyPayload payload) {
        snapshot = payload == null ? DEFAULT_POLICY : payload;
    }

    public static ServerPolicyPayload snapshot() {
        return snapshot;
    }

    public static void clear() {
        snapshot = DEFAULT_POLICY;
    }
}
