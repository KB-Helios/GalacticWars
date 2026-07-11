package galacticwars.clonewars.kingdom;

import galacticwars.clonewars.progression.ProgressionEvent;
import galacticwars.clonewars.progression.ProgressionEventType;
import java.util.Objects;
import java.util.UUID;

public record KingdomGameplayAction(
        KingdomActionId id,
        UUID playerId,
        ProgressionEventType progressionType,
        String subjectId,
        int amount
) {
    public KingdomGameplayAction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(progressionType, "progressionType");
        Objects.requireNonNull(subjectId, "subjectId");
    }

    public ProgressionEvent progressionEvent() {
        return new ProgressionEvent(id.progressionEventId(), playerId, progressionType, subjectId, amount);
    }
}
