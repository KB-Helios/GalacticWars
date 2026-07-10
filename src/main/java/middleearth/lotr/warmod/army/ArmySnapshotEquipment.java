package middleearth.lotr.warmod.army;

public record ArmySnapshotEquipment(
        String mainHand,
        String head,
        String chest,
        String legs,
        String feet
) {
    public ArmySnapshotEquipment {
        mainHand = normalize(mainHand);
        head = normalize(head);
        chest = normalize(chest);
        legs = normalize(legs);
        feet = normalize(feet);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
