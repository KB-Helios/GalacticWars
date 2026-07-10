package middleearth.lotr.warmod.recruitment;

import java.util.Set;
import java.util.UUID;

import middleearth.lotr.warmod.army.ArmyFormation;
import middleearth.lotr.warmod.army.ArmyUnitDefinition;
import middleearth.lotr.warmod.army.ArmyUnitId;
import middleearth.lotr.warmod.army.ArmyUnitRole;
import middleearth.lotr.warmod.faction.FactionAlignment;
import middleearth.lotr.warmod.faction.FactionDefinition;
import middleearth.lotr.warmod.faction.FactionId;
import middleearth.lotr.warmod.workforce.WorkerProfession;

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
                gondorSoldier(),
                gondor(),
                FactionAlignment.empty(playerId()).withAddedScore(FactionId.of("gondor"), 25),
                new RecruitmentCapacity(2, 4, 1, 2),
                100,
                WorkerProfession.LUMBERJACK);

        assertTrue(offer.accepted(), "offer accepted");
        assertEquals("accepted", offer.reasonCode(), "reason code");
        assertEquals(25, offer.hireCost(), "hire cost");
        assertEquals(3, offer.dailyUpkeep(), "daily upkeep");
        assertEquals(WorkerProfession.LUMBERJACK, offer.workerProfession(), "profession");
        assertEquals("Hire Gondor Soldier as Lumberjack", offer.summaryTitle(), "summary");
    }

    private static void contractRejectsWhenHousingIsFull() {
        RecruitmentContractOffer offer = RecruitmentContractPolicy.evaluate(
                gondorSoldier(),
                gondor(),
                FactionAlignment.empty(playerId()).withAddedScore(FactionId.of("gondor"), 25),
                new RecruitmentCapacity(4, 4, 1, 2),
                100,
                WorkerProfession.FARMER);

        assertFalse(offer.accepted(), "offer rejected");
        assertEquals("housing_full", offer.reasonCode(), "reason code");
    }

    private static void contractRejectsWhenWorksiteIsMissingForWorker() {
        RecruitmentContractOffer offer = RecruitmentContractPolicy.evaluate(
                gondorSoldier(),
                gondor(),
                FactionAlignment.empty(playerId()).withAddedScore(FactionId.of("gondor"), 25),
                new RecruitmentCapacity(1, 4, 0, 2),
                100,
                WorkerProfession.MINER);

        assertFalse(offer.accepted(), "offer rejected");
        assertEquals("worksite_required", offer.reasonCode(), "reason code");
    }

    private static ArmyUnitDefinition gondorSoldier() {
        return new ArmyUnitDefinition(
                ArmyUnitId.of("gondor_soldier"),
                "Gondor Soldier",
                FactionId.of("gondor"),
                ArmyUnitRole.INFANTRY,
                25,
                20,
                5,
                ArmyFormation.LINE);
    }

    private static FactionDefinition gondor() {
        return new FactionDefinition(
                FactionId.of("gondor"),
                "Gondor",
                25,
                10,
                12,
                Set.of(FactionId.of("rohan")),
                Set.of(FactionId.of("mordor")));
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
