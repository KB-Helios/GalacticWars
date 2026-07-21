package galacticwars.clonewars.army;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public record ArmySnapshotEquipment(
        ItemStack mainHand,
        ItemStack offHand,
        ItemStack head,
        ItemStack chest,
        ItemStack legs,
        ItemStack feet
) {
    public ArmySnapshotEquipment {
        mainHand = copy(mainHand);
        offHand = copy(offHand);
        head = copy(head);
        chest = copy(chest);
        legs = copy(legs);
        feet = copy(feet);
    }

    public static ArmySnapshotEquipment empty() {
        return new ArmySnapshotEquipment(
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY);
    }

    @Override
    public ItemStack mainHand() {
        return copy(mainHand);
    }

    @Override
    public ItemStack offHand() {
        return copy(offHand);
    }

    @Override
    public ItemStack head() {
        return copy(head);
    }

    @Override
    public ItemStack chest() {
        return copy(chest);
    }

    @Override
    public ItemStack legs() {
        return copy(legs);
    }

    @Override
    public ItemStack feet() {
        return copy(feet);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ArmySnapshotEquipment equipment)) {
            return false;
        }
        return ItemStack.matches(mainHand, equipment.mainHand)
                && ItemStack.matches(offHand, equipment.offHand)
                && ItemStack.matches(head, equipment.head)
                && ItemStack.matches(chest, equipment.chest)
                && ItemStack.matches(legs, equipment.legs)
                && ItemStack.matches(feet, equipment.feet);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashStackList(List.of(mainHand, offHand, head, chest, legs, feet));
    }

    private static ItemStack copy(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }
}
