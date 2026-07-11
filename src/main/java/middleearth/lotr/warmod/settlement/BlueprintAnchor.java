package middleearth.lotr.warmod.settlement;

public record BlueprintAnchor(int x, int y, int z) {
    public static final BlueprintAnchor ORIGIN = new BlueprintAnchor(0, 0, 0);
}
