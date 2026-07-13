package galacticwars.clonewars.conquest;

import java.util.Objects;

public record ConquestControlState(
        String regionId,
        String dimensionId,
        int beaconX,
        int beaconY,
        int beaconZ,
        String controllingFaction,
        String controllingKingdom,
        String capturingPlayer,
        int progress,
        long revision
) {
    public ConquestControlState {
        Objects.requireNonNull(regionId, "regionId");
        Objects.requireNonNull(dimensionId, "dimensionId");
        controllingFaction = controllingFaction == null ? "" : controllingFaction;
        controllingKingdom = controllingKingdom == null ? "" : controllingKingdom;
        capturingPlayer = capturingPlayer == null ? "" : capturingPlayer;
        if (progress < 0 || revision < 0L) throw new IllegalArgumentException("Invalid conquest state");
    }

    public ConquestControlState withProgress(String playerId, int updatedProgress) {
        return new ConquestControlState(regionId, dimensionId, beaconX, beaconY, beaconZ,
                controllingFaction, controllingKingdom, playerId, updatedProgress, revision);
    }

    public ConquestControlState captured(String faction, String kingdom) {
        return new ConquestControlState(regionId, dimensionId, beaconX, beaconY, beaconZ,
                faction, kingdom, "", 0, revision + 1L);
    }
}
