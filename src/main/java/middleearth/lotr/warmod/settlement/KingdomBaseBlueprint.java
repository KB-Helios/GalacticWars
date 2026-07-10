package middleearth.lotr.warmod.settlement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import middleearth.lotr.warmod.workforce.ResourceInventory;

public record KingdomBaseBlueprint(
        String id,
        String displayName,
        List<BaseBlockPlacement> placements
) {
    public KingdomBaseBlueprint {
        id = requireNonBlank(id, "id");
        displayName = requireNonBlank(displayName, "displayName");
        placements = List.copyOf(Objects.requireNonNull(placements, "placements"));
        if (placements.isEmpty()) {
            throw new IllegalArgumentException("placements cannot be empty");
        }
    }

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
        return new KingdomBaseBlueprint("starter_keep", "Starter Keep", placements);
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
