package galacticwars.clonewars.world;

import galacticwars.clonewars.progression.LaunchContentCatalog;

public final class PlanetTravelPolicy {
    private PlanetTravelPolicy() {
    }

    public static TravelAuthorization authorize(
            String planetId,
            boolean owner,
            boolean authoritativeCommandCenter,
            boolean factionPledged,
            boolean travelUnlocked,
            boolean upkeepPaid,
            boolean destinationLoaded,
            boolean alreadyThere
    ) {
        if (!LaunchContentCatalog.PLANETS.contains(planetId)) {
            return TravelAuthorization.rejected("unknown_planet");
        }
        if (!owner || !authoritativeCommandCenter) {
            return TravelAuthorization.rejected("not_owner");
        }
        if (!factionPledged) {
            return TravelAuthorization.rejected("faction_required");
        }
        if (!travelUnlocked) {
            return TravelAuthorization.rejected("planet_travel_locked");
        }
        if (!upkeepPaid) {
            return TravelAuthorization.rejected("upkeep_unpaid");
        }
        if (!destinationLoaded) {
            return TravelAuthorization.rejected("destination_unavailable");
        }
        if (alreadyThere) {
            return TravelAuthorization.rejected("already_there");
        }
        return TravelAuthorization.allow();
    }

    public record TravelAuthorization(boolean accepted, String reason) {
        private static TravelAuthorization allow() {
            return new TravelAuthorization(true, "accepted");
        }

        private static TravelAuthorization rejected(String reason) {
            return new TravelAuthorization(false, reason);
        }
    }
}
