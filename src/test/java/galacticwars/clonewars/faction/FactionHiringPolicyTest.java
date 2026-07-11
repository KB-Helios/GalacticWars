package galacticwars.clonewars.faction;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import galacticwars.clonewars.army.HiringDecision;
import galacticwars.clonewars.army.HiringPolicy;

public final class FactionHiringPolicyTest {
    private FactionHiringPolicyTest() {
    }

    public static void main(String[] args) {
        storesFactionDefinitionValues();
        resolvesFactionRelations();
        acceptsEligibleHiringRequest();
        rejectsHiringRequestWithStableReasonCodes();

        System.out.println("FactionHiringPolicyTest passed");
    }

    private static void storesFactionDefinitionValues() {
        FactionDefinition republic = republic();

        assertEquals(FactionId.of("republic"), republic.id(), "republic id");
        assertEquals("Republic", republic.displayName(), "republic display name");
        assertEquals(25, republic.hireCost(), "republic hire cost");
        assertEquals(10, republic.minimumHiringAlignment(), "republic minimum hiring alignment");
        assertEquals(12, republic.maxOwnedRecruits(), "republic max owned recruits");
        assertTrue(republic.allies().contains(FactionId.of("mandalorian")), "republic ally mandalorian");
        assertTrue(republic.enemies().contains(FactionId.of("separatist")), "republic enemy separatist");
    }

    private static void resolvesFactionRelations() {
        FactionCatalog catalog = testCatalog();

        assertEquals(FactionRelation.SAME, catalog.relation(FactionId.of("republic"), FactionId.of("republic")),
                "same relation");
        assertEquals(FactionRelation.ALLY, catalog.relation(FactionId.of("republic"), FactionId.of("mandalorian")),
                "ally relation");
        assertEquals(FactionRelation.ENEMY, catalog.relation(FactionId.of("mandalorian"), FactionId.of("separatist")),
                "enemy relation");
        assertEquals(FactionRelation.NEUTRAL, catalog.relation(FactionId.of("separatist"), FactionId.of("unknown")),
                "neutral relation");
    }

    private static void acceptsEligibleHiringRequest() {
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        FactionDefinition republic = republic();
        FactionAlignment alignment = FactionAlignment.empty(playerId).withAddedScore(republic.id(), 11);

        HiringDecision decision = HiringPolicy.canHire(alignment, republic, 25, 11);

        assertTrue(decision.accepted(), "eligible hiring accepted");
        assertEquals("accepted", decision.reasonCode(), "accepted reason");
        assertEquals(25, decision.cost(), "accepted cost");
    }

    private static void rejectsHiringRequestWithStableReasonCodes() {
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000102");
        FactionDefinition republic = republic();
        FactionAlignment alignment = FactionAlignment.empty(playerId).withAddedScore(republic.id(), 9);

        assertEquals("unknown_player", HiringPolicy.canHire(null, republic, 25, 0).reasonCode(),
                "unknown player reason");
        assertEquals("alignment_too_low", HiringPolicy.canHire(alignment, republic, 25, 0).reasonCode(),
                "low alignment reason");
        assertEquals("coins_too_low", HiringPolicy.canHire(alignment.withAddedScore(republic.id(), 1), republic, 24, 0).reasonCode(),
                "low coins reason");
        assertEquals("recruit_limit_reached", HiringPolicy.canHire(alignment.withAddedScore(republic.id(), 1), republic, 25, 12).reasonCode(),
                "recruit limit reason");
    }

    private static FactionCatalog testCatalog() {
        FactionDefinition republic = republic();
        FactionDefinition mandalorian = new FactionDefinition(
                FactionId.of("mandalorian"),
                "Mandalorian",
                20,
                8,
                10,
                Set.of(FactionId.of("republic")),
                Set.of(FactionId.of("separatist")));
        FactionDefinition separatist = new FactionDefinition(
                FactionId.of("separatist"),
                "Separatist",
                30,
                15,
                16,
                Set.of(),
                Set.of(FactionId.of("republic"), FactionId.of("mandalorian")));

        return new FactionCatalog(Map.of(
                republic.id(), republic,
                mandalorian.id(), mandalorian,
                separatist.id(), separatist));
    }

    private static FactionDefinition republic() {
        return new FactionDefinition(
                FactionId.of("republic"),
                "Republic",
                25,
                10,
                12,
                Set.of(FactionId.of("mandalorian")),
                Set.of(FactionId.of("separatist")));
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label + " expected to be true");
        }
    }
}
