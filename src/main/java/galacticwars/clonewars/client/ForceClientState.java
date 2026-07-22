package galacticwars.clonewars.client;

import galacticwars.clonewars.network.ForceHudPayload;

public final class ForceClientState {
    private static final long CLIENT_TICK_NANOS = 50_000_000L;
    private static final long HUD_VISIBILITY_NANOS = 8_000_000_000L;
    private static ForceHudPayload snapshot = new ForceHudPayload(100, 0, 0, 0);
    private static long receivedAtNanos;
    private ForceClientState() {}
    public static void update(ForceHudPayload payload) {
        snapshot = payload;
        receivedAtNanos = System.nanoTime();
    }
    public static ForceHudPayload snapshot() {
        ForceHudPayload authoritative = snapshot;
        long elapsedNanos = Math.max(0L, System.nanoTime() - receivedAtNanos);
        int elapsedTicks = (int) Math.min(Integer.MAX_VALUE, elapsedNanos / CLIENT_TICK_NANOS);
        return new ForceHudPayload(authoritative.energy(),
                authoritative.rank(), authoritative.masteryExperience(),
                authoritative.unspentPoints(), authoritative.tradition(),
                authoritative.abilities(),
                remaining(authoritative.cooldown1(), elapsedTicks),
                remaining(authoritative.cooldown2(), elapsedTicks),
                remaining(authoritative.cooldown3(), elapsedTicks),
                authoritative.activeSlot(), authoritative.activeMode(),
                Math.min(100, authoritative.activeTicks() +
                        (authoritative.activeMode() == 0 ? 0 : elapsedTicks)),
                authoritative.targetValidityMask(),
                authoritative.failureReason());
    }
    public static boolean visible() {
        return receivedAtNanos != 0L && System.nanoTime() - receivedAtNanos < HUD_VISIBILITY_NANOS;
    }
    private static int remaining(int cooldown, int elapsedTicks) {
        return Math.max(0, cooldown - elapsedTicks);
    }
}
