package middleearth.lotr.warmod.army;

import java.util.Locale;
import java.util.Objects;

public record ArmyLocation(String dimensionId, double x, double y, double z) {
    public ArmyLocation {
        Objects.requireNonNull(dimensionId, "dimensionId");
        dimensionId = dimensionId.trim().toLowerCase(Locale.ROOT);
        if (dimensionId.isEmpty()) {
            throw new IllegalArgumentException("dimensionId cannot be blank");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Army location coordinates must be finite");
        }
    }

    public ArmyPosition blockPosition() {
        return new ArmyPosition((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }
}
