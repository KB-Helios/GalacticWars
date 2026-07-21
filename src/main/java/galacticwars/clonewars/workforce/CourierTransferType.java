package galacticwars.clonewars.workforce;

import java.util.Locale;

public enum CourierTransferType {
    TAKE,
    PUT,
    /** Legacy alias for {@link #PUT_FILL}; retained for existing saves. */
    FILL,
    /** Legacy alias for {@link #PUT_ALL}; retained for existing saves. */
    EMPTY,
    TAKE_ANY,
    PUT_ANY,
    TAKE_ALL,
    PUT_ALL,
    PUT_FILL,
    TAKE_FILL,
    WAIT;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static CourierTransferType byId(String id) {
        return valueOf(id.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Resolves the original Galactic Wars action names to the expanded route
     * vocabulary without changing their serialized identifiers.
     */
    public CourierTransferType canonical() {
        return switch (this) {
            case FILL -> PUT_FILL;
            case EMPTY -> PUT_ALL;
            default -> this;
        };
    }

    public boolean hasItemFilter() {
        return switch (canonical()) {
            case TAKE, PUT, TAKE_ANY, PUT_ANY, PUT_FILL, TAKE_FILL -> true;
            default -> false;
        };
    }

    public boolean usesQuantity() {
        return switch (canonical()) {
            case TAKE, PUT, PUT_FILL, TAKE_FILL, WAIT -> true;
            default -> false;
        };
    }

    public boolean takesFromWaypoint() {
        return switch (canonical()) {
            case TAKE, TAKE_ANY, TAKE_ALL, TAKE_FILL -> true;
            default -> false;
        };
    }

    public boolean putsIntoWaypoint() {
        return switch (canonical()) {
            case PUT, PUT_ANY, PUT_ALL, PUT_FILL -> true;
            default -> false;
        };
    }

    public boolean waitsAtWaypoint() {
        return canonical() == WAIT;
    }
}
