package galacticwars.clonewars.recruitment;

import java.util.Set;
import java.util.UUID;

import galacticwars.clonewars.army.ArmyFormation;
import galacticwars.clonewars.army.ArmyUnitDefinition;
import galacticwars.clonewars.army.ArmyUnitId;
import galacticwars.clonewars.army.ArmyUnitRole;
import galacticwars.clonewars.faction.FactionAlignment;
import galacticwars.clonewars.faction.FactionDefinition;
import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.workforce.WorkerProfession;

public final class RecruitmentContractTest {
    private RecruitmentContractTest() {
    }

    public static void main(String[] args) {
        acceptedContractIncludesProfessionUpkeepAndCapacity();
        contractRejectsWhenHousingIsFull();
        contractRejectsWhenWorksiteIsMissingForWorker();

        System.out.println("RecruitmentContractTest passed");
    }

    private static void acceptedContractIncludesProfessionUpkeepAndCapacity() {
        RecruitmentContractOffer offer = RecruitmentContractPolicy.evaluate(
                republicSoldier(),
                republic(),
                FactionAlignment.empty(playerId()).withAddedScore(FactionId.of("republic"), 25),
                new RecruitmentCapacity(2, 4, 1, 2),
                100,
                WorkerProfession.LUMBERJACK);

        assertTrue(offer.accepted(), "offer accepted");
        assertEquals("accepted", offer.reasonCode(), "reason code");
        assertEquals(25, offer.hireCost(), "hire cost");
        assertEquals(3, offer.dailyUpkeep(), "daily upkeep");
        assertEquals(WorkerProfession.LUMBERJACK, offer.workerProfession(), "profession");
        assertEquals("Hire Clone Trooper as Lumberjack", offer.summaryTitle(), "summary");
    }

    private static void contractRejectsWhenHousingIsFull() {
        RecruitmentContractOffer offer = RecruitmentContractPolicy.evaluate(
                republicSoldier(),
                republic(),
                FactionAlignment.empty(playerId()).withAddedScore(FactionId.of("republic"), 25),
                new RecruitmentCapacity(4, 4, 1, 2),
                100,
                WorkerProfession.FARMER);

        assertFalse(offer.accepted(), "offer rejected");
        assertEquals("housing_full", offer.reasonCode(), "reason code");
    }

    private static void contractRejectsWhenWorksiteIsMissingForWorker() {
        RecruitmentContractOffer offer = RecruitmentContractPolicy.evaluate(
                republicSoldier(),
                republic(),
                FactionAlignment.empty(playerId()).withAddedScore(FactionId.of("republic"), 25),
                new RecruitmentCapacity(1, 4, 0, 2),
                100,
                WorkerProfession.MINER);

        assertFalse(offer.accepted(), "offer rejected");
        assertEquals("worksite_required", offer.reasonCode(), "reason code");
    }

    private static ArmyUnitDefinition republicSoldier() {
        return new ArmyUnitDefinition(
                ArmyUnitId.of("clone_trooper"),
                "Clone Trooper",
                FactionId.of("republic"),
                ArmyUnitRole.INFANTRY,
                25,
                20,
                5,
                ArmyFormation.LINE);
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

    private static UUID playerId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000501");
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

    private static void assertFalse(boolean condition, String label) {
        if (condition) {
            throw new AssertionError(label + " expected to be false");
        }
    }
}
