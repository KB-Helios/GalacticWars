package galacticwars.clonewars.workforce;

import java.util.Locale;

public enum CourierTransferType {
    TAKE,
    PUT,
    FILL,
    EMPTY;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static CourierTransferType byId(String id) {
        return valueOf(id.trim().toUpperCase(Locale.ROOT));
    }
}
