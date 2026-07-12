package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Guards the side-effecting phases of cross-dimension squad travel.
 *
 * <p>Successful phases are idempotent; failed reserve/rollback actions remain retryable.
 */
public final class AtomicTravelReservation {
    private State state = State.READY;

    public synchronized boolean reserve(BooleanSupplier reservation) {
        Objects.requireNonNull(reservation, "reservation");
        if (state == State.RESERVED) {
            return true;
        }
        if (state != State.READY) {
            return false;
        }
        if (!reservation.getAsBoolean()) {
            return false;
        }
        state = State.RESERVED;
        return true;
    }

    public synchronized boolean commit(Runnable commitAction) {
        Objects.requireNonNull(commitAction, "commitAction");
        if (state == State.COMMITTED) {
            return true;
        }
        if (state != State.RESERVED) {
            return false;
        }
        commitAction.run();
        state = State.COMMITTED;
        return true;
    }

    public synchronized boolean rollback(BooleanSupplier restoration) {
        Objects.requireNonNull(restoration, "restoration");
        if (state == State.ROLLED_BACK) {
            return true;
        }
        if (state != State.RESERVED) {
            return false;
        }
        if (!restoration.getAsBoolean()) {
            return false;
        }
        state = State.ROLLED_BACK;
        return true;
    }

    public synchronized boolean reserved() {
        return state == State.RESERVED;
    }

    private enum State {
        READY,
        RESERVED,
        COMMITTED,
        ROLLED_BACK
    }
}
