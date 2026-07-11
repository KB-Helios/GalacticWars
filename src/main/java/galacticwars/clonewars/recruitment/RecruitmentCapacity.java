package galacticwars.clonewars.recruitment;

public record RecruitmentCapacity(
        int occupiedHousing,
        int totalHousing,
        int availableWorksites,
        int maxWorkerContracts
) {
    public RecruitmentCapacity {
        requireNonNegative(occupiedHousing, "occupiedHousing");
        requireNonNegative(totalHousing, "totalHousing");
        requireNonNegative(availableWorksites, "availableWorksites");
        requireNonNegative(maxWorkerContracts, "maxWorkerContracts");
        if (occupiedHousing > totalHousing) {
            throw new IllegalArgumentException("occupiedHousing cannot exceed totalHousing");
        }
    }

    public boolean hasHousingSpace() {
        return occupiedHousing < totalHousing;
    }

    public boolean hasWorksiteSpace() {
        return availableWorksites > 0 && maxWorkerContracts > 0;
    }

    private static void requireNonNegative(int value, String label) {
        if (value < 0) {
            throw new IllegalArgumentException(label + " cannot be negative");
        }
    }
}
