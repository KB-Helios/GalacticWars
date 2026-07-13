package galacticwars.clonewars.client;

import galacticwars.clonewars.network.ForceHudPayload;

public final class ForceClientState {
    private static ForceHudPayload snapshot = new ForceHudPayload(100, 0, 0, 0);
    private static long receivedAt;
    private ForceClientState() {}
    public static void update(ForceHudPayload payload) {
        snapshot = payload;
        receivedAt = System.currentTimeMillis();
    }
    public static ForceHudPayload snapshot() { return snapshot; }
    public static boolean visible() { return System.currentTimeMillis() - receivedAt < 8000L; }
}
