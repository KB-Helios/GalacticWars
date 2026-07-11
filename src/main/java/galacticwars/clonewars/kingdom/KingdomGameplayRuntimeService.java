package galacticwars.clonewars.kingdom;

import galacticwars.clonewars.progression.ProgressionDecision;
import galacticwars.clonewars.progression.ProgressionSavedData;
import java.util.Objects;

/** Minecraft runtime adapter that commits evaluated kingdom gameplay actions to SavedData. */
public final class KingdomGameplayRuntimeService {
    private KingdomGameplayRuntimeService() {
    }

    public static KingdomGameplayResult applyProgression(
            ProgressionSavedData progression,
            KingdomGameplayAction action
    ) {
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(action, "action");
        KingdomGameplayResult evaluated = KingdomGameplayTransactionService.evaluate(
                progression.state(action.playerId()), action);
        if (!evaluated.accepted() || !evaluated.changed()) {
            return evaluated;
        }
        ProgressionDecision committed = progression.apply(action.progressionEvent());
        return new KingdomGameplayResult(
                committed.accepted(), committed.changed(),
                committed.accepted() ? (committed.changed() ? "accepted" : "duplicate_action") : committed.reason(),
                committed.state());
    }
}
