package galacticwars.clonewars.settlement;

import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public record BaseBlockPlacement(
        int x,
        int y,
        int z,
        String blockId,
        String itemId,
        Map<String, String> properties
) {
    public BaseBlockPlacement {
        blockId = requireId(blockId, "blockId");
        itemId = requireId(itemId, "itemId");
        TreeMap<String, String> normalized = new TreeMap<>();
        Objects.requireNonNull(properties, "properties").forEach((key, value) -> {
            String normalizedKey = requireNonBlank(key, "property name");
            String normalizedValue = requireNonBlank(value, "property value");
            normalized.put(normalizedKey, normalizedValue);
        });
        properties = Map.copyOf(normalized);
    }

    public BaseBlockPlacement(int x, int y, int z, String blockId, String itemId) {
        this(x, y, z, blockId, itemId, Map.of());
    }

    public BlockState blockState() {
        var block = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(blockId))
                .orElseThrow(() -> new IllegalStateException("unknown blueprint block " + blockId));
        BlockState state = block.defaultBlockState();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            Property<?> property = block.getStateDefinition().getProperty(entry.getKey());
            if (property == null) {
                throw new IllegalStateException("unknown property " + entry.getKey() + " for " + blockId);
            }
            state = setProperty(state, property, entry.getValue());
        }
        return state;
    }

    public Map<String, String> rotatedProperties(int rotationSteps) {
        int steps = Math.floorMod(rotationSteps, 4);
        if (steps == 0 || properties.isEmpty()) {
            return properties;
        }
        TreeMap<String, String> result = new TreeMap<>(properties);
        String facing = result.get("facing");
        if (facing != null && List.of("north", "east", "south", "west").contains(facing)) {
            List<String> directions = List.of("north", "east", "south", "west");
            result.put("facing", directions.get((directions.indexOf(facing) + steps) % 4));
        }
        String axis = result.get("axis");
        if (axis != null && steps % 2 == 1 && (axis.equals("x") || axis.equals("z"))) {
            result.put("axis", axis.equals("x") ? "z" : "x");
        }
        String rotation = result.get("rotation");
        if (rotation != null) {
            try {
                result.put("rotation", Integer.toString((Integer.parseInt(rotation) + steps * 4) & 15));
            } catch (NumberFormatException ignored) {
                // Validation in blockState() reports malformed registered properties at runtime.
            }
        }
        return Map.copyOf(result);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState setProperty(BlockState state, Property property, String value) {
        java.util.Optional parsedValue = property.getValue(value);
        if (parsedValue.isEmpty()) {
            throw new IllegalStateException("invalid value " + value + " for property " + property.getName());
        }
        Comparable parsed = (Comparable) parsedValue.get();
        return state.setValue(property, parsed);
    }

    private static String requireId(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return normalized;
    }

    private static String requireNonBlank(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return normalized;
    }
}
