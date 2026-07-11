package galacticwars.clonewars.kingdom;

import galacticwars.clonewars.progression.ProgressionState;
import java.util.Objects;

public record KingdomGameplayResult(
        boolean accepted,
        boolean changed,
        String reason,
        ProgressionState progressionState
) {
    public KingdomGameplayResult {
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(progressionState, "progressionState");
    }
}
