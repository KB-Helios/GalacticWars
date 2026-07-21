package galacticwars.clonewars.workforce;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public record WorkAreaConfiguration(
        WorkAreaBounds bounds,
        boolean kingdomAccess,
        int priority,
        boolean overlayVisible,
        List<String> itemFilters,
        List<CourierWaypoint> courierRoute,
        CourierRouteMode courierRouteMode,
        long courierRouteRevision
) {
    public WorkAreaConfiguration {
        Objects.requireNonNull(bounds, "bounds");
        if (priority < 0 || priority > 100) {
            throw new IllegalArgumentException("work area priority must be between 0 and 100");
        }
        LinkedHashSet<String> filters = new LinkedHashSet<>();
        for (String filter : Objects.requireNonNull(itemFilters, "itemFilters")) {
            String normalized = filter.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isBlank()) filters.add(normalized);
        }
        itemFilters = List.copyOf(filters);
        List<CourierWaypoint> normalizedRoute = List.copyOf(
                Objects.requireNonNull(courierRoute, "courierRoute"));
        courierRoute = normalizedRoute.size() <= CourierRoutePlan.MAX_WAYPOINTS
                ? normalizedRoute
                : List.copyOf(normalizedRoute.subList(0, CourierRoutePlan.MAX_WAYPOINTS));
        Objects.requireNonNull(courierRouteMode, "courierRouteMode");
        if (courierRouteRevision < 0L) {
            throw new IllegalArgumentException("courier route revision cannot be negative");
        }
    }

    public WorkAreaConfiguration(
            WorkAreaBounds bounds,
            boolean kingdomAccess,
            int priority,
            boolean overlayVisible,
            List<String> itemFilters,
            List<CourierWaypoint> courierRoute
    ) {
        this(bounds, kingdomAccess, priority, overlayVisible, itemFilters, courierRoute,
                CourierRouteMode.LOOP, 0L);
    }

    public static WorkAreaConfiguration defaults(int radius) {
        return new WorkAreaConfiguration(WorkAreaBounds.radius(radius), true, 50, false, List.of(), List.of());
    }

    public Optional<CourierRoutePlan> courierRoutePlan() {
        return courierRoute.size() < 2
                ? Optional.empty()
                : Optional.of(new CourierRoutePlan(courierRoute, courierRouteMode, courierRouteRevision));
    }

    public WorkAreaConfiguration withCourierRoute(List<CourierWaypoint> route, CourierRouteMode mode) {
        return new WorkAreaConfiguration(bounds, kingdomAccess, priority, overlayVisible, itemFilters,
                route, mode, Math.addExact(courierRouteRevision, 1L));
    }
}
