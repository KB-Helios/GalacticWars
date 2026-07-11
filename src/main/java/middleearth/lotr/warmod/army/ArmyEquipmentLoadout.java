package middleearth.lotr.warmod.army;

import java.util.Locale;

public record ArmyEquipmentLoadout(
        String mainHandItemId,
        String headItemId,
        String chestItemId,
        String legsItemId,
        String feetItemId
) {
    public static ArmyEquipmentLoadout empty() {
        return new ArmyEquipmentLoadout("", "", "", "", "");
    }

    public ArmyEquipmentLoadout {
        mainHandItemId = normalize(mainHandItemId);
        headItemId = normalize(headItemId);
        chestItemId = normalize(chestItemId);
        legsItemId = normalize(legsItemId);
        feetItemId = normalize(feetItemId);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
