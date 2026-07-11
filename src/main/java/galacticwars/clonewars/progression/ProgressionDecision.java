package galacticwars.clonewars.progression;

import java.util.Objects;

public record ProgressionDecision(boolean accepted, boolean changed, String reason, ProgressionState state) {
    public ProgressionDecision {
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(state, "state");
    }

    public static ProgressionDecision accepted(ProgressionState before, ProgressionState after) {
        return new ProgressionDecision(true, before != after, before == after ? "duplicate_event" : "accepted", after);
    }

    public static ProgressionDecision rejected(String reason, ProgressionState state) {
        return new ProgressionDecision(false, false, reason, state);
    }
}
