package galacticwars.clonewars.kingdom;

public record SettlementRewards(int externalStorageSlots, int commanderSlots) {
    public SettlementRewards {
        if (externalStorageSlots < 0 || commanderSlots < 0) {
            throw new IllegalArgumentException("settlement rewards cannot be negative");
        }
    }

    public static SettlementRewards none() {
        return new SettlementRewards(0, 0);
    }

    public SettlementRewards add(int storageSlots, int newCommanderSlots) {
        return new SettlementRewards(
                Math.addExact(externalStorageSlots, Math.max(0, storageSlots)),
                Math.addExact(commanderSlots, Math.max(0, newCommanderSlots)));
    }
}
