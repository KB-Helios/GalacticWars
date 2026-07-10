package middleearth.lotr.warmod.settlement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import middleearth.lotr.warmod.workforce.ResourceInventory;

public record KingdomBaseBlueprint(
        String id,
        String displayName,
        List<BaseBlockPlacement> placements,
        int housingReward,
        int storageSlotReward,
        String worksiteType,
        int worksiteCapacity,
        int commanderSlotReward
) {
    public static final String STARTER_KEEP_ID = "starter_keep";

    public KingdomBaseBlueprint {
        id = requireNonBlank(id, "id");
        displayName = requireNonBlank(displayName, "displayName");
        placements = List.copyOf(Objects.requireNonNull(placements, "placements"));
        if (placements.isEmpty()) {
            throw new IllegalArgumentException("placements cannot be empty");
        }
        if (housingReward < 0 || storageSlotReward < 0 || worksiteCapacity < 0 || commanderSlotReward < 0) {
            throw new IllegalArgumentException("blueprint rewards cannot be negative");
        }
        worksiteType = worksiteType == null ? "" : worksiteType.trim();
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

    public static Optional<KingdomBaseBlueprint> byId(String id) {
        return all().stream().filter(blueprint -> blueprint.id().equals(id)).findFirst();
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
}
