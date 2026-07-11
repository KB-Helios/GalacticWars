package galacticwars.clonewars.kingdom;

public record ClaimedChunk(int x, int z) {
    public boolean adjacentTo(ClaimedChunk other) {
        return Math.abs(x - other.x) + Math.abs(z - other.z) == 1;
    }
}
