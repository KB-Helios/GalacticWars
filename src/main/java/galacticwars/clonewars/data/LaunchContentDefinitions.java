package galacticwars.clonewars.data;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record LaunchContentDefinitions(
        Map<String, PlanetDefinition> planets,
        Map<String, VehicleDefinition> vehicles,
        Map<String, ForceAbilityDefinition> forceAbilities,
        Map<String, QuestDefinition> quests,
        Map<String, TradeDefinition> trades,
        Map<String, ConquestRegionDefinition> conquestRegions
) {
    public static final int MAX_SERIALIZED_PLANET_ID_BYTES = 128;

    public LaunchContentDefinitions {
        planets = immutable(planets, "planets");
        vehicles = immutable(vehicles, "vehicles");
        forceAbilities = immutable(forceAbilities, "forceAbilities");
        quests = immutable(quests, "quests");
        trades = immutable(trades, "trades");
        conquestRegions = immutable(conquestRegions, "conquestRegions");
    }

    public static LaunchContentDefinitions empty() {
        return new LaunchContentDefinitions(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    public List<String> planetIds() { return List.copyOf(planets.keySet()); }
    public List<String> vehicleIds() { return List.copyOf(vehicles.keySet()); }
    public Set<String> forceAbilityIds() { return Set.copyOf(forceAbilities.keySet()); }
    public List<String> questIds() { return List.copyOf(quests.keySet()); }
    public Set<String> questUnlocks(String id) { return quests.containsKey(id) ? quests.get(id).unlocks() : Set.of(); }
    public List<String> questObjectives(String id) { return quests.containsKey(id) ? quests.get(id).objectives() : List.of(); }
    public int questRewardCredits(String id) { return quests.containsKey(id) ? quests.get(id).rewardCredits() : 0; }
    public int regionRewardCredits(String id) { return conquestRegions.containsKey(id) ? conquestRegions.get(id).rewardCredits() : 0; }
    public Optional<TradeDefinition> trade(String id) { return Optional.ofNullable(trades.get(id)); }

    private static <V> Map<String, V> immutable(Map<String, V> source, String label) {
        Objects.requireNonNull(source, label);
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    public record PlanetDefinition(String id, String dimensionId, String arrival, String theme, String factionId) {
        public PlanetDefinition {
            requireIds(id, dimensionId, arrival, theme, factionId);
            if (id.getBytes(StandardCharsets.UTF_8).length > MAX_SERIALIZED_PLANET_ID_BYTES) {
                throw new IllegalArgumentException("Planet id exceeds navigation packet limit: " + id);
            }
        }
    }

    public record VehicleDefinition(
            String id,
            String movement,
            int seats,
            int maxHealth,
            int fuelCapacity,
            String requiredUnlock,
            double maxSpeed,
            double strafeMultiplier,
            double verticalSpeed,
            int fuelRateTicks,
            String weaponEffect,
            List<String> seatRoles,
            int fabricationCredits,
            Map<String, Integer> fabricationInputs,
            Set<String> deploymentRequirements
    ) {
        public VehicleDefinition {
            requireIds(id, movement, requiredUnlock, weaponEffect);
            seatRoles = List.copyOf(Objects.requireNonNull(seatRoles, "seatRoles for " + id));
            fabricationInputs = Map.copyOf(Objects.requireNonNull(
                    fabricationInputs, "fabricationInputs for " + id));
            deploymentRequirements = Set.copyOf(Objects.requireNonNull(
                    deploymentRequirements, "deploymentRequirements for " + id));
            if (seats < 1 || maxHealth < 1 || fuelCapacity < 1
                    || !Double.isFinite(maxSpeed) || maxSpeed <= 0.0D
                    || !Double.isFinite(strafeMultiplier) || strafeMultiplier < 0.0D
                    || !Double.isFinite(verticalSpeed) || verticalSpeed < 0.0D
                    || fuelRateTicks < 1 || fabricationCredits < 1
                    || seatRoles.size() != seats || !seatRoles.getFirst().equals("driver")
                    || fabricationInputs.isEmpty()
                    || fabricationInputs.entrySet().stream().anyMatch(entry -> entry.getKey().isBlank()
                            || entry.getValue() == null || entry.getValue() < 1)
                    || deploymentRequirements.stream().anyMatch(String::isBlank)) {
                throw new IllegalArgumentException("Invalid vehicle " + id);
            }
        }

        public VehicleDefinition(
                String id, String movement, int seats, int maxHealth,
                int fuelCapacity, String requiredUnlock
        ) {
            this(id, movement, seats, maxHealth, fuelCapacity, requiredUnlock,
                    movement.equals("walker") ? 0.18D : movement.equals("flight") ? 0.48D : 0.38D,
                    0.65D, movement.equals("flight") ? 0.28D : 0.0D, 5,
                    "blaster", java.util.stream.IntStream.range(0, seats)
                            .mapToObj(index -> index == 0 ? "driver" : "passenger").toList(),
                    1, Map.of("minecraft:iron_ingot", 1), Set.of("vehicle_crafting"));
        }
    }

    public record ForceAbilityDefinition(
            String id,
            String path,
            int energy,
            int cooldownTicks,
            String requiredQuest,
            boolean enabled,
            String effect,
            double range,
            int durationTicks,
            String activeUnlock
    ) {
        public ForceAbilityDefinition {
            requireIds(id, path, requiredQuest, effect, activeUnlock);
            if (energy < 0 || cooldownTicks < 0 || !Double.isFinite(range)
                    || range < 0.0D || durationTicks < 0) {
                throw new IllegalArgumentException("Invalid Force ability " + id);
            }
            if (!path.equals("light") && !path.equals("dark")) {
                throw new IllegalArgumentException("Invalid Force path for " + id);
            }
        }

        public ForceAbilityDefinition(
                String id, String path, int energy, int cooldownTicks,
                String requiredQuest, boolean enabled
        ) {
            this(id, path, energy, cooldownTicks, requiredQuest, enabled,
                    id, 16.0D, 20, requiredQuest);
        }
    }

    public record QuestDefinition(String id, List<String> objectives, int rewardCredits, Set<String> unlocks) {
        public QuestDefinition {
            requireIds(id);
            Objects.requireNonNull(objectives, "objectives for quest " + id);
            Objects.requireNonNull(unlocks, "unlocks for quest " + id);
            objectives = List.copyOf(objectives);
            unlocks = Set.copyOf(unlocks);
            if (objectives.isEmpty() || rewardCredits < 0) throw new IllegalArgumentException("Invalid quest " + id);
        }
    }

    public record TradeDefinition(
            String id, String factionId, int price, String itemId, int itemCount,
            String requiredUnlock, int stockTier, String regionalPrerequisite
    ) {
        public TradeDefinition {
            requireIds(id, factionId, itemId, requiredUnlock);
            regionalPrerequisite = regionalPrerequisite == null ? "" : regionalPrerequisite;
            if (price <= 0 || itemCount <= 0 || stockTier < 1) throw new IllegalArgumentException("Invalid trade " + id);
        }

        public TradeDefinition(
                String id, String factionId, int price, String itemId, int itemCount, String requiredUnlock
        ) {
            this(id, factionId, price, itemId, itemCount, requiredUnlock, 1, "");
        }
    }

    public record ConquestRegionDefinition(
            String id,
            String planetId,
            int protectedRadius,
            int captureTicks,
            int rewardCredits,
            int landmarkX,
            int landmarkZ,
            int captureRadius,
            String defenderFaction
    ) {
        public ConquestRegionDefinition {
            requireIds(id, planetId, defenderFaction);
            if (protectedRadius < 1 || captureTicks < 1 || rewardCredits < 0 || captureRadius < 4) {
                throw new IllegalArgumentException("Invalid region " + id);
            }
        }

        public ConquestRegionDefinition(
                String id, String planetId, int protectedRadius, int captureTicks, int rewardCredits
        ) {
            this(id, planetId, protectedRadius, captureTicks, rewardCredits,
                    0, 0, Math.max(8, protectedRadius / 2), "neutral");
        }
    }

    private static void requireIds(String... values) {
        for (String value : values) if (value == null || value.isBlank()) throw new IllegalArgumentException("Launch identifiers cannot be blank");
    }
}
