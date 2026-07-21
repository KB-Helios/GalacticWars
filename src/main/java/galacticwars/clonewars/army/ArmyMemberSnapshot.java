package galacticwars.clonewars.army;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import galacticwars.clonewars.recruitment.RecruitDuty;
import net.minecraft.world.item.ItemStack;

public record ArmyMemberSnapshot(
        UUID recruitId,
        String entityTypeId,
        String unitId,
        UUID ownerId,
        UUID kingdomId,
        RecruitDuty duty,
        float health,
        int morale,
        int hunger,
        int unpaidTicks,
        long generation,
        ArmySnapshotEquipment equipment,
        List<ItemStack> cargo,
        String customName
) {
    public static final int CARGO_SLOT_COUNT = 9;

    public ArmyMemberSnapshot {
        Objects.requireNonNull(recruitId, "recruitId");
        entityTypeId = requireText(entityTypeId, "entityTypeId");
        unitId = requireText(unitId, "unitId");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(kingdomId, "kingdomId");
        Objects.requireNonNull(duty, "duty");
        if (!Float.isFinite(health) || health <= 0.0F) {
            throw new IllegalArgumentException("health must be positive and finite");
        }
        morale = clamp(morale);
        hunger = clamp(hunger);
        if (unpaidTicks < 0 || generation < 0L) {
            throw new IllegalArgumentException("unpaidTicks and generation cannot be negative");
        }
        equipment = equipment == null ? ArmySnapshotEquipment.empty() : equipment;
        cargo = copyCargo(cargo);
        customName = customName == null ? "" : customName.trim();
    }

    @Override
    public List<ItemStack> cargo() {
        return copyCargo(cargo);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ArmyMemberSnapshot snapshot)) {
            return false;
        }
        return Float.compare(health, snapshot.health) == 0
                && morale == snapshot.morale
                && hunger == snapshot.hunger
                && unpaidTicks == snapshot.unpaidTicks
                && generation == snapshot.generation
                && recruitId.equals(snapshot.recruitId)
                && entityTypeId.equals(snapshot.entityTypeId)
                && unitId.equals(snapshot.unitId)
                && ownerId.equals(snapshot.ownerId)
                && kingdomId.equals(snapshot.kingdomId)
                && duty == snapshot.duty
                && equipment.equals(snapshot.equipment)
                && ItemStack.listMatches(cargo, snapshot.cargo)
                && customName.equals(snapshot.customName);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(
                recruitId,
                entityTypeId,
                unitId,
                ownerId,
                kingdomId,
                duty,
                health,
                morale,
                hunger,
                unpaidTicks,
                generation,
                equipment,
                customName);
        return 31 * result + ItemStack.hashStackList(cargo);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return normalized;
    }

    private static List<ItemStack> copyCargo(List<ItemStack> stacks) {
        if (stacks != null && stacks.size() > CARGO_SLOT_COUNT) {
            throw new IllegalArgumentException("cargo cannot exceed " + CARGO_SLOT_COUNT + " slots");
        }
        ArrayList<ItemStack> copy = new ArrayList<>(CARGO_SLOT_COUNT);
        if (stacks != null) {
            for (ItemStack stack : stacks) {
                copy.add(stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            }
        }
        while (copy.size() < CARGO_SLOT_COUNT) {
            copy.add(ItemStack.EMPTY);
        }
        return List.copyOf(copy);
    }
}
