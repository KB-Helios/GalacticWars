package galacticwars.clonewars.faction;

import java.util.List;
import java.util.Objects;

public record FactionAlignmentUpdateResult(FactionAlignment alignment, List<FactionAlignmentChange> changes) {
    public FactionAlignmentUpdateResult {
        Objects.requireNonNull(alignment, "alignment");
        Objects.requireNonNull(changes, "changes");
        changes = List.copyOf(changes);
    }
}
