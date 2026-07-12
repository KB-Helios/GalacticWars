package galacticwars.clonewars.army;

import java.util.concurrent.atomic.AtomicInteger;

public final class AtomicTravelReservationTest {
    private AtomicTravelReservationTest() {
    }

    public static void main(String[] args) {
        duplicateReserveAndCommitAreIdempotent();
        failedActionsRemainRetryable();
        terminalTransitionsCannotReverse();
        System.out.println("AtomicTravelReservationTest passed");
    }

    private static void duplicateReserveAndCommitAreIdempotent() {
        AtomicTravelReservation reservation = new AtomicTravelReservation();
        AtomicInteger reserves = new AtomicInteger();
        AtomicInteger commits = new AtomicInteger();

        assertTrue(reservation.reserve(() -> {
            reserves.incrementAndGet();
            return true;
        }), "initial reserve");
        assertTrue(reservation.reserve(() -> {
            reserves.incrementAndGet();
            return false;
        }), "duplicate reserve");
        assertEquals(1, reserves.get(), "reserve action count");
        assertTrue(reservation.commit(commits::incrementAndGet), "initial commit");
        assertTrue(reservation.commit(commits::incrementAndGet), "duplicate commit");
        assertEquals(1, commits.get(), "commit action count");
    }

    private static void failedActionsRemainRetryable() {
        AtomicTravelReservation reservation = new AtomicTravelReservation();
        AtomicInteger reserves = new AtomicInteger();
        assertFalse(reservation.reserve(() -> reserves.incrementAndGet() > 1), "failed reserve");
        assertTrue(reservation.reserve(() -> reserves.incrementAndGet() > 1), "reserve retry");

        AtomicInteger rollbacks = new AtomicInteger();
        assertFalse(reservation.rollback(() -> rollbacks.incrementAndGet() > 1), "failed rollback");
        assertTrue(reservation.rollback(() -> rollbacks.incrementAndGet() > 1), "rollback retry");
        assertTrue(reservation.rollback(() -> false), "duplicate rollback");
        assertEquals(2, rollbacks.get(), "rollback action count");
    }

    private static void terminalTransitionsCannotReverse() {
        AtomicTravelReservation committed = new AtomicTravelReservation();
        assertFalse(committed.commit(() -> {}), "commit before reserve");
        assertFalse(committed.rollback(() -> true), "rollback before reserve");
        assertTrue(committed.reserve(() -> true), "reserve for commit");
        assertTrue(committed.commit(() -> {}), "commit");
        assertFalse(committed.rollback(() -> true), "rollback after commit");
        assertFalse(committed.reserve(() -> true), "reserve after commit");

        AtomicTravelReservation rolledBack = new AtomicTravelReservation();
        assertTrue(rolledBack.reserve(() -> true), "reserve for rollback");
        assertTrue(rolledBack.rollback(() -> true), "rollback");
        assertFalse(rolledBack.commit(() -> {}), "commit after rollback");
        assertFalse(rolledBack.reserve(() -> true), "reserve after rollback");
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label + " expected true");
        }
    }

    private static void assertFalse(boolean value, String label) {
        if (value) {
            throw new AssertionError(label + " expected false");
        }
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }
}
