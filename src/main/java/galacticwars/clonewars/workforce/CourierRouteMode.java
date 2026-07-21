package galacticwars.clonewars.workforce;

import java.util.Locale;

/** Determines how a courier selects the waypoint after reaching a route edge. */
public enum CourierRouteMode {
    LOOP,
    PING_PONG;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static CourierRouteMode byId(String id) {
        return valueOf(id.trim().toUpperCase(Locale.ROOT));
    }
}
