package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.UUID;

import galacticwars.clonewars.recruitment.RecruitDuty;

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
        String customName
) {
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
        equipment = equipment == null ? new ArmySnapshotEquipment("", "", "", "", "") : equipment;
        customName = customName == null ? "" : customName.trim();
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
}
