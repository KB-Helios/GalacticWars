package middleearth.lotr.warmod.army;

import java.util.Objects;

public record VirtualArmyMovementDecision(ArmyLocation anchor, String pauseReason) {
    public VirtualArmyMovementDecision {
        Objects.requireNonNull(anchor, "anchor");
        pauseReason = pauseReason == null ? "" : pauseReason.trim();
    }

    public boolean paused() {
        return !pauseReason.isEmpty();
    }
}
