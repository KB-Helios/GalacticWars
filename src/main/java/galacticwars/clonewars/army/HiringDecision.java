package galacticwars.clonewars.army;

import java.util.Objects;

public record HiringDecision(boolean accepted, String reasonCode, int cost) {
    public HiringDecision {
        reasonCode = requireNonBlank(reasonCode);
        if (cost < 0) {
            throw new IllegalArgumentException("cost cannot be negative");
        }
    }

    public static HiringDecision accepted(int cost) {
        return new HiringDecision(true, "accepted", cost);
    }

    public static HiringDecision rejected(String reasonCode) {
        return new HiringDecision(false, reasonCode, 0);
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
