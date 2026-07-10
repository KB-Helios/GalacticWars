package middleearth.lotr.warmod.workforce;

import java.util.Objects;

public record WorkerLogisticsRoute(
        WorkerWorksite storageSite,
        WorkerWorksite destinationSite
) {
    public WorkerLogisticsRoute {
        Objects.requireNonNull(storageSite, "storageSite");
        Objects.requireNonNull(destinationSite, "destinationSite");
        if (storageSite.areaType() != WorkAreaType.STORAGE) {
            throw new IllegalArgumentException("storageSite must use STORAGE area type");
        }
    }
}
