package galacticwars.clonewars.settlement;

public record BlueprintTerrainConstraints(int maxSlope, int minY, int maxY) {
    public static final BlueprintTerrainConstraints DEFAULT = new BlueprintTerrainConstraints(4, -64, 320);

    public BlueprintTerrainConstraints {
        if (maxSlope < 0 || maxSlope > 32 || minY > maxY) {
            throw new IllegalArgumentException("invalid blueprint terrain constraints");
        }
    }
}
