package galacticwars.clonewars.kingdom;

import galacticwars.clonewars.progression.ProgressionDecision;
import galacticwars.clonewars.progression.GalacticProgressionCoordinator;
import galacticwars.clonewars.progression.ProgressionState;
import java.util.Objects;

/** Authoritative entry point for idempotent kingdom progression transactions. */
public final class KingdomGameplayTransactionService {
    private KingdomGameplayTransactionService() {
    }

    public static KingdomGameplayResult evaluate(ProgressionState progression, KingdomGameplayAction action) {
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(action, "action");
        ProgressionDecision decision = GalacticProgressionCoordinator.apply(
                progression, action.progressionEvent());
        String reason = decision.accepted()
                ? (decision.changed() ? "accepted" : "duplicate_action")
                : decision.reason();
        return new KingdomGameplayResult(
                decision.accepted(), decision.changed(), reason, decision.state());
    }
}
