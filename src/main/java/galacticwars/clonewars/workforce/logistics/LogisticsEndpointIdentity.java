package galacticwars.clonewars.workforce.logistics;

import java.util.Objects;

/** Stable, caller-supplied identity for a physical logistics endpoint. */
public record LogisticsEndpointIdentity(String value) {
    public LogisticsEndpointIdentity {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty() || value.length() > 256) {
            throw new IllegalArgumentException("endpoint identity must contain 1-256 characters");
        }
    }
}
