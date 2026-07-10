package middleearth.lotr.warmod.settlement;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Pattern;
import middleearth.lotr.warmod.kingdom.BuildProject;
import middleearth.lotr.warmod.kingdom.StorageEndpoint;

import middleearth.lotr.warmod.workforce.ResourceInventory;

public record KingdomBaseBlueprint(
        String id,
        String displayName,
        BlueprintAnchor anchor,
        List<Integer> allowedRotations,
        List<BaseBlockPlacement> placements,
        int housingReward,
        int storageSlotReward,
        String worksiteType,
        int worksiteCapacity,
        int commanderSlotReward
) {
    public static final String DEFAULT_NAMESPACE = "kingdomwarsmiddleearth";
    public static final String STARTER_KEEP_ID = DEFAULT_NAMESPACE + ":starter_keep";
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");
    private static final List<Integer> ALL_ROTATIONS = List.of(0, 90, 180, 270);

    public KingdomBaseBlueprint {
        id = canonicalId(id);
        displayName = requireNonBlank(displayName, "displayName");
        anchor = Objects.requireNonNull(anchor, "anchor");
        allowedRotations = normalizeAllowedRotations(allowedRotations);
        placements = List.copyOf(Objects.requireNonNull(placements, "placements"));
        if (placements.isEmpty()) {
            throw new IllegalArgumentException("placements cannot be empty");
        }
        HashSet<String> occupiedPositions = new HashSet<>();
        for (BaseBlockPlacement placement : placements) {
            Objects.requireNonNull(placement, "placement");
            String position = placement.x() + ":" + placement.y() + ":" + placement.z();
            if (!occupiedPositions.add(position)) {
                throw new IllegalArgumentException("duplicate blueprint placement at " + position);
            }
        }
        if (housingReward < 0 || storageSlotReward < 0 || worksiteCapacity < 0 || commanderSlotReward < 0) {
            throw new IllegalArgumentException("blueprint rewards cannot be negative");
        }
        worksiteType = worksiteType == null ? "" : worksiteType.trim().toLowerCase(Locale.ROOT);
        if (worksiteType.isBlank() != (worksiteCapacity == 0)) {
            throw new IllegalArgumentException("worksite type and capacity must be defined together");
        }
        if (storageSlotReward > 0) {
            long containerCount = placements.stream()
                    .filter(placement -> placement.blockId().equals("minecraft:chest"))
                    .count();
            if (containerCount == 0 || storageSlotReward < containerCount) {
                throw new IllegalArgumentException("storage reward requires enough chest placements");
            }
        }
    }

    public KingdomBaseBlueprint(
            String id,
            String displayName,
            List<BaseBlockPlacement> placements,
            int housingReward,
            int storageSlotReward,
            String worksiteType,
            int worksiteCapacity,
            int commanderSlotReward
    ) {
        this(id, displayName, BlueprintAnchor.ORIGIN, ALL_ROTATIONS, placements,
                housingReward, storageSlotReward, worksiteType, worksiteCapacity, commanderSlotReward);
    }

    private static final List<KingdomBaseBlueprint> ALL_BLUEPRINTS = List.of(
            starterKeep(),
            house(),
            storehouse(),
            farmPlot(),
            lumberCamp(),
            mineSite());

    public static KingdomBaseBlueprint starterKeep() {
        ArrayList<BaseBlockPlacement> placements = new ArrayList<>();
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                placements.add(new BaseBlockPlacement(
                        x,
                        0,
                        z,
                        "kingdomwarsmiddleearth:middle_earth_stone",
                        "kingdomwarsmiddleearth:middle_earth_stone"));
            }
        }
        placements.add(new BaseBlockPlacement(0, 1, 0, "kingdomwarsmiddleearth:mallorn_log", "kingdomwarsmiddleearth:mallorn_log"));
        placements.add(new BaseBlockPlacement(4, 1, 0, "kingdomwarsmiddleearth:mallorn_log", "kingdomwarsmiddleearth:mallorn_log"));
        placements.add(new BaseBlockPlacement(0, 1, 4, "kingdomwarsmiddleearth:mallorn_log", "kingdomwarsmiddleearth:mallorn_log"));
        placements.add(new BaseBlockPlacement(4, 1, 4, "kingdomwarsmiddleearth:mallorn_log", "kingdomwarsmiddleearth:mallorn_log"));
        placements.add(new BaseBlockPlacement(2, 1, 0, "minecraft:oak_planks", "minecraft:oak_planks"));
        placements.add(new BaseBlockPlacement(2, 1, 4, "minecraft:oak_planks", "minecraft:oak_planks"));
        placements.add(new BaseBlockPlacement(0, 1, 2, "minecraft:oak_planks", "minecraft:oak_planks"));
        placements.add(new BaseBlockPlacement(4, 1, 2, "minecraft:oak_planks", "minecraft:oak_planks"));
        return new KingdomBaseBlueprint(STARTER_KEEP_ID, "Starter Keep", placements, 2, 0, "", 0, 1);
    }

    public static KingdomBaseBlueprint house() {
        ArrayList<BaseBlockPlacement> placements = floor(3, 3, "minecraft:oak_planks", "minecraft:oak_planks");
        addCornerPosts(placements, 2, 2, 2);
        placements.add(new BaseBlockPlacement(1, 1, 2, "minecraft:oak_planks", "minecraft:oak_planks"));
        placements.add(new BaseBlockPlacement(1, 2, 2, "minecraft:oak_planks", "minecraft:oak_planks"));
        return new KingdomBaseBlueprint("house", "Starter House", placements, 4, 0, "", 0, 0);
    }

    public static KingdomBaseBlueprint storehouse() {
        ArrayList<BaseBlockPlacement> placements = floor(4, 3, "kingdomwarsmiddleearth:middle_earth_stone",
                "kingdomwarsmiddleearth:middle_earth_stone");
        addCornerPosts(placements, 3, 2, 2);
        placements.add(new BaseBlockPlacement(1, 1, 2, "minecraft:oak_planks", "minecraft:oak_planks"));
        placements.add(new BaseBlockPlacement(2, 1, 2, "minecraft:oak_planks", "minecraft:oak_planks"));
        placements.add(new BaseBlockPlacement(1, 1, 1, "minecraft:chest", "minecraft:chest"));
        placements.add(new BaseBlockPlacement(2, 1, 1, "minecraft:chest", "minecraft:chest"));
        return new KingdomBaseBlueprint("storehouse", "Storehouse", placements, 0, 54, "courier", 2, 0);
    }

    public static KingdomBaseBlueprint farmPlot() {
        ArrayList<BaseBlockPlacement> placements = floor(5, 5, "minecraft:dirt", "minecraft:dirt");
        for (int x = 0; x < 5; x++) {
            placements.add(new BaseBlockPlacement(x, 1, 0, "minecraft:oak_log", "minecraft:oak_log"));
            placements.add(new BaseBlockPlacement(x, 1, 4, "minecraft:oak_log", "minecraft:oak_log"));
        }
        return new KingdomBaseBlueprint("farm_plot", "Farm Plot", placements, 0, 0, "farmer", 2, 0);
    }

    public static KingdomBaseBlueprint lumberCamp() {
        ArrayList<BaseBlockPlacement> placements = floor(3, 3, "minecraft:oak_planks", "minecraft:oak_planks");
        placements.add(new BaseBlockPlacement(0, 1, 0, "minecraft:oak_log", "minecraft:oak_log"));
        placements.add(new BaseBlockPlacement(2, 1, 0, "minecraft:oak_log", "minecraft:oak_log"));
        placements.add(new BaseBlockPlacement(1, 1, 2, "minecraft:oak_log", "minecraft:oak_log"));
        return new KingdomBaseBlueprint("lumber_camp", "Lumber Camp", placements, 0, 0, "lumberjack", 2, 0);
    }

    public static KingdomBaseBlueprint mineSite() {
        ArrayList<BaseBlockPlacement> placements = floor(3, 2, "kingdomwarsmiddleearth:middle_earth_stone",
                "kingdomwarsmiddleearth:middle_earth_stone");
        placements.add(new BaseBlockPlacement(0, 1, 1, "kingdomwarsmiddleearth:middle_earth_stone",
                "kingdomwarsmiddleearth:middle_earth_stone"));
        placements.add(new BaseBlockPlacement(2, 1, 1, "kingdomwarsmiddleearth:middle_earth_stone",
                "kingdomwarsmiddleearth:middle_earth_stone"));
        placements.add(new BaseBlockPlacement(1, 2, 1, "kingdomwarsmiddleearth:middle_earth_stone",
                "kingdomwarsmiddleearth:middle_earth_stone"));
        return new KingdomBaseBlueprint("mine_site", "Mine Site", placements, 0, 0, "miner", 2, 0);
    }

    public static List<KingdomBaseBlueprint> all() {
        return ALL_BLUEPRINTS;
    }

    public static List<KingdomBaseBlueprint> builtIns() {
        return ALL_BLUEPRINTS;
    }

    public static Optional<KingdomBaseBlueprint> byId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String normalizedId;
        try {
            normalizedId = canonicalId(id);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        return all().stream().filter(blueprint -> blueprint.id().equals(normalizedId)).findFirst();
    }

    public String definitionHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, "kingdom-blueprint-v2");
            update(digest, id);
            update(digest, anchor.x());
            update(digest, anchor.y());
            update(digest, anchor.z());
            update(digest, allowedRotations.size());
            for (int rotation : allowedRotations) {
                update(digest, rotation);
            }
            update(digest, placements.size());
            for (BaseBlockPlacement placement : placements) {
                update(digest, placement.x());
                update(digest, placement.y());
                update(digest, placement.z());
                update(digest, placement.blockId());
                update(digest, placement.itemId());
            }
            update(digest, housingReward);
            update(digest, storageSlotReward);
            update(digest, worksiteType);
            update(digest, worksiteCapacity);
            update(digest, commanderSlotReward);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public BaseBlockPlacement rotatedPlacement(int index, int rotationSteps) {
        requireRotationSteps(rotationSteps);
        BaseBlockPlacement placement = placements.get(index);
        int relativeX = placement.x() - anchor.x();
        int relativeY = placement.y() - anchor.y();
        int relativeZ = placement.z() - anchor.z();
        return switch (rotationSteps) {
            case 1 -> new BaseBlockPlacement(-relativeZ, relativeY, relativeX,
                    placement.blockId(), placement.itemId());
            case 2 -> new BaseBlockPlacement(-relativeX, relativeY, -relativeZ,
                    placement.blockId(), placement.itemId());
            case 3 -> new BaseBlockPlacement(relativeZ, relativeY, -relativeX,
                    placement.blockId(), placement.itemId());
            default -> new BaseBlockPlacement(relativeX, relativeY, relativeZ,
                    placement.blockId(), placement.itemId());
        };
    }

    public boolean supportsRotationSteps(int rotationSteps) {
        return rotationSteps >= 0 && rotationSteps < 4 && allowedRotations.contains(rotationSteps * 90);
    }

    public void requireRotationSteps(int rotationSteps) {
        if (!supportsRotationSteps(rotationSteps)) {
            throw new IllegalArgumentException("rotation " + rotationSteps * 90 + " is not allowed for " + id);
        }
    }

    public List<StorageEndpoint> storageEndpoints(BuildProject project) {
        if (storageSlotReward <= 0) {
            return List.of();
        }
        List<BaseBlockPlacement> containers = java.util.stream.IntStream.range(0, placements.size())
                .mapToObj(index -> rotatedPlacement(index, project.rotationSteps()))
                .filter(placement -> placement.blockId().equals("minecraft:chest"))
                .toList();
        if (containers.isEmpty()) {
            return List.of();
        }
        int slotsPerEndpoint = storageSlotReward / containers.size();
        int remainder = storageSlotReward % containers.size();
        ArrayList<StorageEndpoint> endpoints = new ArrayList<>(containers.size());
        for (int index = 0; index < containers.size(); index++) {
            BaseBlockPlacement placement = containers.get(index);
            endpoints.add(new StorageEndpoint(
                    project.dimensionId(),
                    project.originX() + placement.x(),
                    project.originY() + placement.y(),
                    project.originZ() + placement.z(),
                    slotsPerEndpoint + (index < remainder ? 1 : 0)));
        }
        return List.copyOf(endpoints);
    }

    private static ArrayList<BaseBlockPlacement> floor(int width, int depth, String blockId, String itemId) {
        ArrayList<BaseBlockPlacement> placements = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                placements.add(new BaseBlockPlacement(x, 0, z, blockId, itemId));
            }
        }
        return placements;
    }

    private static void addCornerPosts(ArrayList<BaseBlockPlacement> placements, int maxX, int maxZ, int height) {
        for (int y = 1; y <= height; y++) {
            placements.add(new BaseBlockPlacement(0, y, 0, "minecraft:oak_log", "minecraft:oak_log"));
            placements.add(new BaseBlockPlacement(maxX, y, 0, "minecraft:oak_log", "minecraft:oak_log"));
            placements.add(new BaseBlockPlacement(0, y, maxZ, "minecraft:oak_log", "minecraft:oak_log"));
            placements.add(new BaseBlockPlacement(maxX, y, maxZ, "minecraft:oak_log", "minecraft:oak_log"));
        }
    }

    public ResourceInventory requiredResources() {
        ResourceInventory inventory = ResourceInventory.empty();
        for (BaseBlockPlacement placement : placements) {
            inventory = inventory.withAdded(placement.itemId(), 1);
        }
        return inventory;
    }

    private static String requireNonBlank(String value, String label) {
        Objects.requireNonNull(value, label);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return trimmed;
    }

    public static String canonicalId(String value) {
        String normalized = requireNonBlank(value, "id").toLowerCase(Locale.ROOT);
        if (normalized.indexOf(':') < 0) {
            normalized = DEFAULT_NAMESPACE + ":" + normalized;
        }
        int separator = normalized.indexOf(':');
        if (separator < 1 || separator != normalized.lastIndexOf(':')
                || !NAMESPACE.matcher(normalized.substring(0, separator)).matches()
                || !PATH.matcher(normalized.substring(separator + 1)).matches()) {
            throw new IllegalArgumentException("invalid namespaced blueprint id " + value);
        }
        return normalized;
    }

    public static String path(String id) {
        String canonical = canonicalId(id);
        return canonical.substring(canonical.indexOf(':') + 1);
    }

    private static List<Integer> normalizeAllowedRotations(List<Integer> rotations) {
        Objects.requireNonNull(rotations, "allowedRotations");
        TreeSet<Integer> normalized = new TreeSet<>();
        for (Integer rotation : rotations) {
            if (rotation == null || rotation < 0 || rotation > 270 || rotation % 90 != 0) {
                throw new IllegalArgumentException("allowed rotations must be 0, 90, 180, or 270 degrees");
            }
            normalized.add(rotation);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("at least one rotation must be allowed");
        }
        return List.copyOf(normalized);
    }

    private static void update(MessageDigest digest, int value) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        update(digest, bytes.length);
        digest.update(bytes);
    }
}
