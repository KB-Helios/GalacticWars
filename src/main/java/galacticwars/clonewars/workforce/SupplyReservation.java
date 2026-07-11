package galacticwars.clonewars.workforce;

import galacticwars.clonewars.kingdom.StorageEndpoint;
import java.util.Objects;
import java.util.UUID;

public record SupplyReservation(
        UUID id,
        UUID demandId,
        UUID workerId,
        StorageEndpoint endpoint,
        int quantity,
        long expiresAtGameTime,
        State state
) {
    public SupplyReservation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(demandId, "demandId");
        Objects.requireNonNull(workerId, "workerId");
        Objects.requireNonNull(endpoint, "endpoint");
        Objects.requireNonNull(state, "state");
        if (quantity <= 0 || expiresAtGameTime < 0) {
            throw new IllegalArgumentException("invalid supply reservation");
        }
    }

    public boolean active(long gameTime) {
        return state == State.ACTIVE && gameTime < expiresAtGameTime;
    }

    public SupplyReservation complete() {
        return state == State.ACTIVE
                ? new SupplyReservation(id, demandId, workerId, endpoint, quantity,
                        expiresAtGameTime, State.COMPLETED)
                : this;
    }

    public SupplyReservation release() {
        return state == State.ACTIVE
                ? new SupplyReservation(id, demandId, workerId, endpoint, quantity,
                        expiresAtGameTime, State.RELEASED)
                : this;
    }

    public enum State {
        ACTIVE,
        COMPLETED,
        RELEASED
    }
}
