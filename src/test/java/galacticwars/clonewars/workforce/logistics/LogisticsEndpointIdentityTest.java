package galacticwars.clonewars.workforce.logistics;

import java.util.UUID;

/** Dependency-light authority checks; physical ItemStack behavior runs in NeoForge GameTests. */
public final class LogisticsEndpointIdentityTest {
    public static void main(String[] args) {
        LogisticsEndpointIdentity source = new LogisticsEndpointIdentity("  storage/source  ");
        LogisticsEndpointIdentity destination = new LogisticsEndpointIdentity("worker/cargo");
        UUID actor = UUID.fromString("00000000-0000-0000-0000-00000000a701");
        LogisticsTransferAuthority authority = new LogisticsTransferAuthority(actor, source, destination);

        assertEquals("storage/source", source.value(), "identity is canonicalized");
        assertEquals(actor, authority.actorId(), "actor is retained");
        assertEquals(source, authority.expectedSource(), "source binding is retained");
        assertEquals(destination, authority.expectedDestination(), "destination binding is retained");
        assertThrows(() -> new LogisticsEndpointIdentity("   "), "blank identity rejected");
        assertThrows(() -> new LogisticsEndpointIdentity("x".repeat(257)), "oversized identity rejected");
        assertThrows(() -> new LogisticsTransferAuthority(null, source, destination), "missing actor rejected");
        assertThrows(() -> new LogisticsTransferAuthority(actor, null, destination), "missing source rejected");
        assertThrows(() -> new LogisticsTransferAuthority(actor, source, null), "missing destination rejected");
        System.out.println("LogisticsEndpointIdentityTest passed");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertThrows(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException | NullPointerException expected) {
            // Expected.
        }
    }
}
