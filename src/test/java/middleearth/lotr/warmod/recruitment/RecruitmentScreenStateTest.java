package middleearth.lotr.warmod.recruitment;

import java.util.Set;
import java.util.UUID;

import middleearth.lotr.warmod.army.ArmyFormation;
import middleearth.lotr.warmod.army.ArmyUnitDefinition;
import middleearth.lotr.warmod.army.ArmyUnitId;
import middleearth.lotr.warmod.army.ArmyUnitRole;
import middleearth.lotr.warmod.army.HiringDecision;
import middleearth.lotr.warmod.army.RecruitState;
import middleearth.lotr.warmod.faction.FactionDefinition;
import middleearth.lotr.warmod.faction.FactionId;
import middleearth.lotr.warmod.workforce.WorkerProfession;

public final class RecruitmentScreenStateTest {
    private RecruitmentScreenStateTest() {
    }

    public static void main(String[] args) {
        wildRecruitShowsAcceptedHireOffer();
        wildRecruitShowsContractDetails();
        ownedRecruitShowsCommandActions();
        recruitOwnedBySomeoneElseIsLocked();

        System.out.println("RecruitmentScreenStateTest passed");
    }

    private static void wildRecruitShowsAcceptedHireOffer() {
        RecruitmentScreenState state = RecruitmentScreenState.wildOffer(
                gondorSoldier(),
                gondor(),
                HiringDecision.accepted(25),
                3);

        assertEquals(RecruitmentScreenMode.HIRE_OFFER, state.mode(), "mode");
        assertEquals("Hire Gondor Soldier", state.title(), "title");
        assertEquals("Gondor", state.factionName(), "faction name");
        assertEquals(25, state.hireCost(), "hire cost");
        assertEquals(3, state.ownedRecruitCount(), "owned recruit count");
        assertEquals("accepted", state.reasonCode(), "reason code");
        assertEquals(RecruitmentAction.ACCEPT_HIRE, state.primaryAction(), "primary action");
        assertTrue(state.commandActions().isEmpty(), "wild recruit has no command actions");
    }

    private static void ownedRecruitShowsCommandActions() {
        UUID recruitId = UUID.fromString("00000000-0000-0000-0000-000000000201");
        UUID ownerId = UUID.fromString("00000000-0000-0000-0000-000000000202");
        UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000000203");
        RecruitState recruitState = RecruitState.createOwned(recruitId, ownerId, groupId);

        RecruitmentScreenState state = RecruitmentScreenState.ownedCommandPanel(recruitState, gondorSoldier());

        assertEquals(RecruitmentScreenMode.COMMAND_PANEL, state.mode(), "mode");
        assertEquals("Gondor Soldier", state.title(), "title");
        assertEquals(RecruitmentAction.NONE, state.primaryAction(), "primary action");
        assertEquals(6, state.commandActions().size(), "command action count");
        assertTrue(state.commandActions().contains(RecruitmentAction.FOLLOW_OWNER), "follow action");
        assertTrue(state.commandActions().contains(RecruitmentAction.HOLD_POSITION), "hold action");
        assertTrue(state.commandActions().contains(RecruitmentAction.MOVE_TO_POSITION), "move action");
        assertTrue(state.commandActions().contains(RecruitmentAction.PROTECT_OWNER), "protect action");
        assertTrue(state.commandActions().contains(RecruitmentAction.ATTACK_TARGET), "attack action");
        assertTrue(state.commandActions().contains(RecruitmentAction.CLEAR_TARGET), "clear action");
    }

    private static void wildRecruitShowsContractDetails() {
        RecruitmentContractOffer offer = RecruitmentContractOffer.accepted(
                gondorSoldier(),
                gondor(),
                WorkerProfession.BUILDER,
                25,
                3);

        RecruitmentScreenState state = RecruitmentScreenState.contractOffer(offer, 4);

        assertEquals(RecruitmentScreenMode.HIRE_OFFER, state.mode(), "mode");
        assertEquals("Hire Gondor Soldier as Builder", state.title(), "title");
        assertEquals("Gondor", state.factionName(), "faction");
        assertEquals(25, state.hireCost(), "hire cost");
        assertEquals(3, state.dailyUpkeep(), "daily upkeep");
        assertEquals("builder", state.workerProfessionId(), "worker profession id");
        assertTrue(state.statusLines().contains("Upkeep: 3 emeralds/day"), "upkeep status");
        assertTrue(state.statusLines().contains("Worker: Builder"), "worker status");
    }

    private static void recruitOwnedBySomeoneElseIsLocked() {
        UUID ownerId = UUID.fromString("00000000-0000-0000-0000-000000000302");

        RecruitmentScreenState state = RecruitmentScreenState.lockedByOtherOwner(gondorSoldier(), ownerId);

        assertEquals(RecruitmentScreenMode.LOCKED, state.mode(), "mode");
        assertEquals("Gondor Soldier", state.title(), "title");
        assertEquals("owned_by_other_player", state.reasonCode(), "reason code");
        assertEquals(RecruitmentAction.NONE, state.primaryAction(), "primary action");
        assertTrue(state.commandActions().isEmpty(), "locked recruit has no command actions");
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
