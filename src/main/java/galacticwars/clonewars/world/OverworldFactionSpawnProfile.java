package galacticwars.clonewars.world;

import galacticwars.clonewars.recruitment.NpcServiceBranch;
import java.util.Map;
import java.util.Objects;

public record OverworldFactionSpawnProfile(
        String factionId,
        Map<String, NpcServiceBranch> branchesByEntityType,
        int outpostRadius,
        int minimumOutpostSpacing,
        int militaryCapacity,
        int civilianCapacity
) {
    public OverworldFactionSpawnProfile {
        factionId = required(factionId, "factionId");
        branchesByEntityType = Map.copyOf(Objects.requireNonNull(branchesByEntityType, "branchesByEntityType"));
        if (branchesByEntityType.isEmpty() || outpostRadius < 16 || minimumOutpostSpacing < outpostRadius
                || militaryCapacity < 1 || civilianCapacity < 0) {
            throw new IllegalArgumentException("invalid overworld faction spawn profile");
        }
    }

    public NpcServiceBranch branchFor(String entityTypeId) {
        return branchesByEntityType.get(entityTypeId);
    }

    public boolean supports(String entityTypeId) {
        return branchesByEntityType.containsKey(entityTypeId);
    }

    private static String required(String value, String label) {
        value = Objects.requireNonNull(value, label).trim().toLowerCase(java.util.Locale.ROOT);
        if (value.isEmpty()) throw new IllegalArgumentException(label + " cannot be blank");
        return value;
    }
}
