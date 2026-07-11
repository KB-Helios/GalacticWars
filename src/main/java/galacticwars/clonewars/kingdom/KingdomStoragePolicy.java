package galacticwars.clonewars.kingdom;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/** Resolves the Hall and rewarded external storage that gameplay is allowed to consume. */
public final class KingdomStoragePolicy {
    public static final int HALL_STORAGE_SLOTS = 54;

    private KingdomStoragePolicy() {
    }

    public static List<StorageEndpoint> registeredEndpoints(SettlementRecord settlement) {
        Objects.requireNonNull(settlement, "settlement");
        ArrayList<StorageEndpoint> registered = new ArrayList<>();
        registered.add(new StorageEndpoint(
                settlement.dimensionId(), settlement.hallX(), settlement.hallY(), settlement.hallZ(),
                HALL_STORAGE_SLOTS));

        int remainingRewardedSlots = settlement.rewards().externalStorageSlots();
        Set<EndpointKey> seen = new LinkedHashSet<>();
        for (WorksiteRecord worksite : settlement.worksites()) {
            for (StorageEndpoint endpoint : worksite.storageEndpoints()) {
                if (remainingRewardedSlots == 0) {
                    return List.copyOf(registered);
                }
                EndpointKey key = EndpointKey.of(endpoint);
                if (!seen.add(key)) {
                    continue;
                }
                int grantedSlots = Math.min(endpoint.slots(), remainingRewardedSlots);
                registered.add(new StorageEndpoint(
                        endpoint.dimensionId(), endpoint.x(), endpoint.y(), endpoint.z(), grantedSlots));
                remainingRewardedSlots -= grantedSlots;
            }
        }
        return List.copyOf(registered);
    }

    public static boolean isRegistered(
            SettlementRecord settlement,
            String dimensionId,
            int x,
            int y,
            int z
    ) {
        return registeredEndpoints(settlement).stream().anyMatch(endpoint ->
                endpoint.dimensionId().equals(dimensionId)
                        && endpoint.x() == x
                        && endpoint.y() == y
                        && endpoint.z() == z);
    }

    public static int accessibleSlots(
            SettlementRecord settlement,
            Predicate<StorageEndpoint> loadedContainerAvailable
    ) {
        Objects.requireNonNull(loadedContainerAvailable, "loadedContainerAvailable");
        return registeredEndpoints(settlement).stream()
                .filter(loadedContainerAvailable)
                .mapToInt(StorageEndpoint::slots)
                .sum();
    }

    private record EndpointKey(String dimensionId, int x, int y, int z) {
        static EndpointKey of(StorageEndpoint endpoint) {
            return new EndpointKey(endpoint.dimensionId(), endpoint.x(), endpoint.y(), endpoint.z());
        }
    }
}
