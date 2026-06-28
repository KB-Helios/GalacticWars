package middleearth.lotr.warmod.faction;

import java.util.Objects;

public record FactionAlignmentChange(
        FactionId factionId,
        int beforeScore,
        int delta,
        int afterScore,
        String reasonCode
) {
    public FactionAlignmentChange {
        Objects.requireNonNull(factionId, "factionId");
        reasonCode = requireNonBlank(reasonCode);
        if (delta == 0) {
            throw new IllegalArgumentException("delta cannot be zero");
        }
        if (beforeScore + delta != afterScore) {
            throw new IllegalArgumentException("afterScore must equal beforeScore plus delta");
        }
    }

    private static String requireNonBlank(String value) {
        Objects.requireNonNull(value, "reasonCode");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("reasonCode cannot be blank");
        }
        return trimmed;
    }
}
