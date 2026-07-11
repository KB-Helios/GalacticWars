package galacticwars.clonewars.workforce;

import java.util.Objects;

public record WorkerWorksite(
        WorkAreaType areaType,
        int x,
        int y,
        int z,
        int radius
) {
    public WorkerWorksite {
        Objects.requireNonNull(areaType, "areaType");
        if (radius <= 0) {
            throw new IllegalArgumentException("radius must be positive");
        }
    }
}
