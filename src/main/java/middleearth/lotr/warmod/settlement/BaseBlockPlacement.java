package middleearth.lotr.warmod.settlement;

import java.util.Objects;

public record BaseBlockPlacement(
        int x,
        int y,
        int z,
        String blockId,
        String itemId
) {
    public BaseBlockPlacement {
        blockId = requireId(blockId, "blockId");
        itemId = requireId(itemId, "itemId");
    }

    private static String requireId(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return normalized;
    }
}
