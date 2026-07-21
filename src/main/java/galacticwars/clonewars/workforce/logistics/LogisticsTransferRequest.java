package galacticwars.clonewars.workforce.logistics;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/** Exact item-and-component identity, requested quantity, and fulfillment semantics. */
public record LogisticsTransferRequest(
        ItemStack itemTemplate,
        int quantity,
        Fulfillment fulfillment
) {
    public LogisticsTransferRequest {
        Objects.requireNonNull(itemTemplate, "itemTemplate");
        Objects.requireNonNull(fulfillment, "fulfillment");
        if (itemTemplate.isEmpty()) {
            throw new IllegalArgumentException("item template cannot be empty");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        itemTemplate = itemTemplate.copyWithCount(1);
    }

    @Override
    public ItemStack itemTemplate() {
        return itemTemplate.copy();
    }

    public enum Fulfillment {
        REQUIRE_EXACT,
        ALLOW_PARTIAL
    }
}
