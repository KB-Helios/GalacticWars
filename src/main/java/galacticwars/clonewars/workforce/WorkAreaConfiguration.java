package galacticwars.clonewars.workforce;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record WorkAreaConfiguration(
        WorkAreaBounds bounds,
        boolean kingdomAccess,
        int priority,
        boolean overlayVisible,
        List<String> itemFilters,
        List<CourierWaypoint> courierRoute
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
        courierRoute = List.copyOf(Objects.requireNonNull(courierRoute, "courierRoute"));
    }

    public static WorkAreaConfiguration defaults(int radius) {
        return new WorkAreaConfiguration(WorkAreaBounds.radius(radius), true, 50, false, List.of(), List.of());
    }
}
