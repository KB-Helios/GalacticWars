package galacticwars.clonewars.recruitment;

import java.util.Set;
import java.util.UUID;

import galacticwars.clonewars.army.ArmyFormation;
import galacticwars.clonewars.army.ArmyUnitDefinition;
import galacticwars.clonewars.army.ArmyUnitId;
import galacticwars.clonewars.army.ArmyUnitRole;
import galacticwars.clonewars.army.HiringDecision;
import galacticwars.clonewars.army.RecruitState;
import galacticwars.clonewars.faction.FactionDefinition;
import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.workforce.WorkerProfession;

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
                republicSoldier(),
                republic(),
                HiringDecision.accepted(25),
                3);

        assertEquals(RecruitmentScreenMode.HIRE_OFFER, state.mode(), "mode");
        assertEquals("Hire Clone Trooper", state.title(), "title");
        assertEquals("Republic", state.factionName(), "faction name");
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

        RecruitmentScreenState state = RecruitmentScreenState.ownedCommandPanel(recruitState, republicSoldier());

        assertEquals(RecruitmentScreenMode.COMMAND_PANEL, state.mode(), "mode");
        assertEquals("Clone Trooper", state.title(), "title");
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
                republicSoldier(),
                republic(),
                WorkerProfession.BUILDER,
                25,
                3);

        RecruitmentScreenState state = RecruitmentScreenState.contractOffer(offer, 4);

        assertEquals(RecruitmentScreenMode.HIRE_OFFER, state.mode(), "mode");
        assertEquals("Hire Clone Trooper as Builder", state.title(), "title");
        assertEquals("Republic", state.factionName(), "faction");
        assertEquals(25, state.hireCost(), "hire cost");
        assertEquals(3, state.dailyUpkeep(), "daily upkeep");
        assertEquals("builder", state.workerProfessionId(), "worker profession id");
        assertTrue(state.statusLines().contains("Upkeep: 3 credits/day"), "upkeep status");
        assertTrue(state.statusLines().contains("Worker: Builder"), "worker status");
    }

    private static void recruitOwnedBySomeoneElseIsLocked() {
        UUID ownerId = UUID.fromString("00000000-0000-0000-0000-000000000302");

        RecruitmentScreenState state = RecruitmentScreenState.lockedByOtherOwner(republicSoldier(), ownerId);

        assertEquals(RecruitmentScreenMode.LOCKED, state.mode(), "mode");
        assertEquals("Clone Trooper", state.title(), "title");
        assertEquals("owned_by_other_player", state.reasonCode(), "reason code");
        assertEquals(RecruitmentAction.NONE, state.primaryAction(), "primary action");
        assertTrue(state.commandActions().isEmpty(), "locked recruit has no command actions");
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
