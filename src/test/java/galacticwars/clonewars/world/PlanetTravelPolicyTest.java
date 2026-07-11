package galacticwars.clonewars.world;

public final class PlanetTravelPolicyTest {
    private PlanetTravelPolicyTest() {
    }

    public static void main(String[] args) {
        assertRejected("unknown_planet", "alderaan", true, true, true, true, true, true, false);
        assertRejected("not_owner", "tatooine", false, true, true, true, true, true, false);
        assertRejected("not_owner", "tatooine", true, false, true, true, true, true, false);
        assertRejected("faction_required", "tatooine", true, true, false, true, true, true, false);
        assertRejected("planet_travel_locked", "tatooine", true, true, true, false, true, true, false);
        assertRejected("upkeep_unpaid", "tatooine", true, true, true, true, false, true, false);
        assertRejected("destination_unavailable", "tatooine", true, true, true, true, true, false, false);
        assertRejected("already_there", "tatooine", true, true, true, true, true, true, true);
        assertTrue(PlanetTravelPolicy.authorize(
                "tatooine", true, true, true, true, true, true, false).accepted(),
                "fully authorized travel should pass");
        System.out.println("PlanetTravelPolicyTest passed");
    }

    private static void assertRejected(
            String reason,
            String planetId,
            boolean owner,
            boolean authoritative,
            boolean pledged,
            boolean unlocked,
            boolean upkeep,
            boolean loaded,
            boolean alreadyThere
    ) {
        PlanetTravelPolicy.TravelAuthorization result = PlanetTravelPolicy.authorize(
                planetId, owner, authoritative, pledged, unlocked, upkeep, loaded, alreadyThere);
        assertTrue(!result.accepted() && result.reason().equals(reason), reason);
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
