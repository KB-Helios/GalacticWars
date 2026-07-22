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
        Map<String, ForceTraditionDefinition> forceTraditions,
        Map<String, ForceNodeDefinition> forceNodes,
        Map<String, QuestDefinition> quests,
        Map<String, TradeDefinition> trades,
        Map<String, ConquestRegionDefinition> conquestRegions
) {
    public static final int MAX_SERIALIZED_PLANET_ID_BYTES = 128;
    public static final int MAX_SERIALIZED_TRADE_TEXT_BYTES = 128;
    public static final int MAX_TRADE_ITEM_COUNT = 64;
    public static final int MAX_TRADE_CREDIT_PRICE = 1_000_000;
    public static final int MAX_QUEST_OBJECTIVE_REQUIRED_COUNT = 1_000_000;

    public LaunchContentDefinitions {
        planets = immutable(planets, "planets");
        vehicles = immutable(vehicles, "vehicles");
        forceAbilities = immutable(forceAbilities, "forceAbilities");
        forceTraditions = immutable(forceTraditions, "forceTraditions");
        forceNodes = immutable(forceNodes, "forceNodes");
        quests = immutable(quests, "quests");
        trades = immutable(trades, "trades");
        conquestRegions = immutable(conquestRegions, "conquestRegions");
    }

    public static LaunchContentDefinitions empty() {
        return new LaunchContentDefinitions(
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    /** Compatibility constructor for dependency-light tests that do not exercise Force progression. */
    public LaunchContentDefinitions(
            Map<String, PlanetDefinition> planets,
            Map<String, VehicleDefinition> vehicles,
            Map<String, ForceAbilityDefinition> forceAbilities,
            Map<String, QuestDefinition> quests,
            Map<String, TradeDefinition> trades,
            Map<String, ConquestRegionDefinition> conquestRegions
    ) {
        this(planets, vehicles, forceAbilities, Map.of(), Map.of(), quests, trades, conquestRegions);
    }

    public List<String> planetIds() { return List.copyOf(planets.keySet()); }
    public List<String> vehicleIds() { return List.copyOf(vehicles.keySet()); }
    public Set<String> forceAbilityIds() { return Set.copyOf(forceAbilities.keySet()); }
    public Set<String> forceTraditionIds() { return Set.copyOf(forceTraditions.keySet()); }
    public Set<String> forceNodeIds() { return Set.copyOf(forceNodes.keySet()); }
    public List<String> questIds() { return List.copyOf(quests.keySet()); }
    public Set<String> questUnlocks(String id) { return quests.containsKey(id) ? quests.get(id).unlocks() : Set.of(); }
    public List<QuestObjectiveDefinition> questObjectives(String id) {
        return quests.containsKey(id) ? quests.get(id).objectives() : List.of();
    }
    public int questRewardCredits(String id) { return quests.containsKey(id) ? quests.get(id).rewardCredits() : 0; }
    public int questRewardMasteryExperience(String id) {
        return quests.containsKey(id) ? quests.get(id).rewardMasteryExperience() : 0;
    }
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
            String activeUnlock,
            String nodeId,
            String activation,
            String target,
            String executor,
            int sustainEnergy,
            int minChargeTicks,
            int maxChargeTicks,
            int requiredRank
    ) {
        public ForceAbilityDefinition {
            requireIds(id, path, requiredQuest, effect, activeUnlock,
                    activation, target, executor);
            nodeId = nodeId == null ? "" : nodeId;
            if (energy < 0 || cooldownTicks < 0 || !Double.isFinite(range)
                    || range < 0.0D || range > 32.0D || durationTicks < 0
                    || sustainEnergy < 0 || minChargeTicks < 0
                    || maxChargeTicks < minChargeTicks || maxChargeTicks > 100
                    || requiredRank < 1 || requiredRank > 10) {
                throw new IllegalArgumentException("Invalid Force ability " + id);
            }
            if (!Set.of("light", "dark", "jedi", "sith", "nightsister").contains(path)) {
                throw new IllegalArgumentException("Invalid Force tradition for " + id);
            }
            if (!Set.of("instant", "charged", "channeled").contains(activation)) {
                throw new IllegalArgumentException("Invalid Force activation for " + id);
            }
            if (!Set.of("self", "ray", "cone", "sphere", "projectile", "held_object")
                    .contains(target)) {
                throw new IllegalArgumentException("Invalid Force target mode for " + id);
            }
        }

        public ForceAbilityDefinition(
                String id, String path, int energy, int cooldownTicks,
                String requiredQuest, boolean enabled, String effect,
                double range, int durationTicks, String activeUnlock
        ) {
            this(id, path, energy, cooldownTicks, requiredQuest, enabled,
                    effect, range, durationTicks, activeUnlock, "",
                    "instant", range == 0.0D ? "self" : "ray", effect,
                    0, 0, 0, 1);
        }

        public ForceAbilityDefinition(
                String id, String path, int energy, int cooldownTicks,
                String requiredQuest, boolean enabled
        ) {
            this(id, path, energy, cooldownTicks, requiredQuest, enabled,
                    id, 16.0D, 20, requiredQuest);
        }
    }

    public record ForceTraditionDefinition(
            String id,
            String factionId,
            String initiationQuest,
            String displayName,
            List<String> coreNodes,
            List<String> branches,
            List<Integer> rankThresholds
    ) {
        public ForceTraditionDefinition {
            requireIds(id, factionId, initiationQuest);
            displayName = Objects.requireNonNull(displayName, "displayName for " + id).trim();
            coreNodes = List.copyOf(Objects.requireNonNull(coreNodes, "coreNodes for " + id));
            branches = List.copyOf(Objects.requireNonNull(branches, "branches for " + id));
            rankThresholds = List.copyOf(Objects.requireNonNull(
                    rankThresholds, "rankThresholds for " + id));
            if (displayName.isEmpty() || coreNodes.size() != 3 || branches.size() != 2
                    || rankThresholds.size() != 10 || rankThresholds.getFirst() != 0) {
                throw new IllegalArgumentException("Invalid Force tradition " + id);
            }
            int previous = -1;
            for (int threshold : rankThresholds) {
                if (threshold < 0 || threshold <= previous) {
                    if (threshold != 0 || previous != -1) {
                        throw new IllegalArgumentException("Force rank thresholds must increase for " + id);
                    }
                }
                previous = threshold;
            }
        }

        public int rankForExperience(int experience) {
            int rank = 1;
            for (int index = 1; index < rankThresholds.size(); index++) {
                if (experience < rankThresholds.get(index)) break;
                rank = index + 1;
            }
            return rank;
        }
    }

    public record ForceNodeDefinition(
            String id,
            String tradition,
            String branch,
            int tier,
            int pointCost,
            Set<String> prerequisites,
            String abilityId,
            boolean passive
    ) {
        public ForceNodeDefinition {
            requireIds(id, tradition, branch);
            prerequisites = Set.copyOf(Objects.requireNonNull(
                    prerequisites, "prerequisites for " + id));
            abilityId = abilityId == null ? "" : abilityId;
            if (tier < 0 || tier > 5 || pointCost < 0 || pointCost > 1
                    || (branch.equals("core") && pointCost != 0)
                    || (!branch.equals("core") && (tier < 1 || pointCost != 1))
                    || (!passive && abilityId.isBlank())) {
                throw new IllegalArgumentException("Invalid Force node " + id);
            }
        }
    }

    public record QuestObjectiveDefinition(
            String id,
            String eventType,
            Set<String> subjectIds,
            int requiredCount
    ) {
        public QuestObjectiveDefinition {
            requireIds(id, eventType);
            subjectIds = Set.copyOf(Objects.requireNonNull(subjectIds, "subjectIds for " + id));
            if (subjectIds.stream().anyMatch(String::isBlank)
                    || requiredCount < 1
                    || requiredCount > MAX_QUEST_OBJECTIVE_REQUIRED_COUNT) {
                throw new IllegalArgumentException("Invalid quest objective " + id);
            }
        }
    }

    public record QuestDefinition(
            String id,
            List<QuestObjectiveDefinition> objectives,
            int rewardCredits,
            Set<String> unlocks,
            int rewardMasteryExperience
    ) {
        public QuestDefinition {
            requireIds(id);
            Objects.requireNonNull(objectives, "objectives for quest " + id);
            Objects.requireNonNull(unlocks, "unlocks for quest " + id);
            objectives = List.copyOf(objectives);
            unlocks = Set.copyOf(unlocks);
            if (objectives.isEmpty() || rewardCredits < 0
                    || rewardMasteryExperience < 0 || rewardMasteryExperience > 320
                    || objectives.stream().map(QuestObjectiveDefinition::id).distinct().count()
                            != objectives.size()) {
                throw new IllegalArgumentException("Invalid quest " + id);
            }
        }

        public QuestDefinition(
                String id, List<QuestObjectiveDefinition> objectives,
                int rewardCredits, Set<String> unlocks
        ) {
            this(id, objectives, rewardCredits, unlocks, 0);
        }
    }

    public record TradeDefinition(
            String id, String factionId, int price, String itemId, int itemCount,
            String requiredUnlock, int stockTier, String regionalPrerequisite
    ) {
        public TradeDefinition {
            requireIds(id, factionId, itemId, requiredUnlock);
            regionalPrerequisite = regionalPrerequisite == null ? "" : regionalPrerequisite;
            requireUtf8Bound(id, MAX_SERIALIZED_TRADE_TEXT_BYTES, "trade id");
            requireUtf8Bound(itemId, MAX_SERIALIZED_TRADE_TEXT_BYTES, "trade item id");
            if (price <= 0 || price > MAX_TRADE_CREDIT_PRICE
                    || itemCount <= 0 || itemCount > MAX_TRADE_ITEM_COUNT
                    || stockTier < 1) {
                throw new IllegalArgumentException("Invalid trade " + id);
            }
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

    private static void requireUtf8Bound(String value, int maximumBytes, String label) {
        if (value.getBytes(StandardCharsets.UTF_8).length > maximumBytes) {
            throw new IllegalArgumentException(label + " exceeds " + maximumBytes + " UTF-8 bytes");
        }
    }
}
