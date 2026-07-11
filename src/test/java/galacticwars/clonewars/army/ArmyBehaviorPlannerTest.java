package galacticwars.clonewars.army;

import java.util.UUID;

public final class ArmyBehaviorPlannerTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID RECRUIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000203");
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000204");
    private static final UUID THREAT_ID = UUID.fromString("00000000-0000-0000-0000-000000000205");

    private ArmyBehaviorPlannerTest() {
    }

    public static void main(String[] args) {
        followsOwnerOnlyWhenOutsideRange();
        movesAndHoldsCommandTargets();
        attacksOnlyWhenCommandTargetIsAvailable();
        protectsOwnerByAttackingVisibleThreat();
        clearsTargetToIdle();

        System.out.println("ArmyBehaviorPlannerTest passed");
    }

    private static void followsOwnerOnlyWhenOutsideRange() {
        RecruitState recruit = RecruitState.createOwned(RECRUIT_ID, OWNER_ID, GROUP_ID)
                .applyCommand(ArmyCommand.followOwner(OWNER_ID, GROUP_ID));

        ArmyBehaviorDecision farDecision = ArmyBehaviorPlanner.plan(recruit,
                context(new ArmyPosition(0, 64, 0), new ArmyPosition(10, 64, 0), null, false, 4));
        ArmyBehaviorDecision closeDecision = ArmyBehaviorPlanner.plan(recruit,
                context(new ArmyPosition(8, 64, 0), new ArmyPosition(10, 64, 0), null, false, 4));

        assertEquals(ArmyBehaviorIntent.FOLLOW_OWNER, farDecision.intent(), "far follow intent");
        assertEquals(new ArmyPosition(10, 64, 0), farDecision.moveTarget(), "far follow target");
        assertEquals("owner_out_of_range", farDecision.reasonCode(), "far follow reason");
        assertEquals(ArmyBehaviorIntent.IDLE, closeDecision.intent(), "close follow intent");
        assertEquals("within_follow_range", closeDecision.reasonCode(), "close follow reason");
    }

    private static void movesAndHoldsCommandTargets() {
        ArmyPosition moveTarget = new ArmyPosition(20, 65, 30);
        ArmyPosition holdTarget = new ArmyPosition(-5, 63, 9);
        RecruitState recruit = RecruitState.createOwned(RECRUIT_ID, OWNER_ID, GROUP_ID);

        ArmyBehaviorDecision moveDecision = ArmyBehaviorPlanner.plan(
                recruit.applyCommand(ArmyCommand.moveToPosition(OWNER_ID, GROUP_ID, moveTarget)),
                context(new ArmyPosition(0, 64, 0), new ArmyPosition(0, 64, 0), null, false, 4));
        ArmyBehaviorDecision holdDecision = ArmyBehaviorPlanner.plan(
                recruit.applyCommand(ArmyCommand.holdPosition(OWNER_ID, GROUP_ID, holdTarget)),
                context(new ArmyPosition(0, 64, 0), new ArmyPosition(0, 64, 0), null, false, 4));

        assertEquals(ArmyBehaviorIntent.MOVE_TO_POSITION, moveDecision.intent(), "move intent");
        assertEquals(moveTarget, moveDecision.moveTarget(), "move target");
        assertEquals("move_command", moveDecision.reasonCode(), "move reason");
        assertEquals(ArmyBehaviorIntent.HOLD_POSITION, holdDecision.intent(), "hold intent");
        assertEquals(holdTarget, holdDecision.moveTarget(), "hold target");
        assertEquals("hold_command", holdDecision.reasonCode(), "hold reason");
    }

    private static void attacksOnlyWhenCommandTargetIsAvailable() {
        RecruitState recruit = RecruitState.createOwned(RECRUIT_ID, OWNER_ID, GROUP_ID)
                .applyCommand(ArmyCommand.attackTarget(OWNER_ID, GROUP_ID, TARGET_ID));

        ArmyBehaviorDecision attackDecision = ArmyBehaviorPlanner.plan(recruit,
                context(new ArmyPosition(0, 64, 0), new ArmyPosition(0, 64, 0), null, true, 4));
        ArmyBehaviorDecision unavailableDecision = ArmyBehaviorPlanner.plan(recruit,
                context(new ArmyPosition(0, 64, 0), new ArmyPosition(0, 64, 0), null, false, 4));

        assertEquals(ArmyBehaviorIntent.ATTACK_TARGET, attackDecision.intent(), "attack intent");
        assertEquals(TARGET_ID, attackDecision.attackTargetId(), "attack target");
        assertEquals("attack_command", attackDecision.reasonCode(), "attack reason");
        assertEquals(ArmyBehaviorIntent.IDLE, unavailableDecision.intent(), "unavailable attack intent");
        assertEquals("target_unavailable", unavailableDecision.reasonCode(), "unavailable attack reason");
    }

    private static void protectsOwnerByAttackingVisibleThreat() {
        RecruitState recruit = RecruitState.createOwned(RECRUIT_ID, OWNER_ID, GROUP_ID)
                .applyCommand(ArmyCommand.protectOwner(OWNER_ID, GROUP_ID));

        ArmyBehaviorDecision threatDecision = ArmyBehaviorPlanner.plan(recruit,
                context(new ArmyPosition(0, 64, 0), new ArmyPosition(3, 64, 0), THREAT_ID, false, 4));
        ArmyBehaviorDecision noThreatDecision = ArmyBehaviorPlanner.plan(recruit,
                context(new ArmyPosition(0, 64, 0), new ArmyPosition(8, 64, 0), null, false, 4));

        assertEquals(ArmyBehaviorIntent.ATTACK_TARGET, threatDecision.intent(), "protect threat intent");
        assertEquals(THREAT_ID, threatDecision.attackTargetId(), "protect threat target");
        assertEquals("owner_threat_visible", threatDecision.reasonCode(), "protect threat reason");
        assertEquals(ArmyBehaviorIntent.FOLLOW_OWNER, noThreatDecision.intent(), "protect no threat intent");
        assertEquals("protect_follow_owner", noThreatDecision.reasonCode(), "protect no threat reason");
    }

    private static void clearsTargetToIdle() {
        RecruitState recruit = RecruitState.createOwned(RECRUIT_ID, OWNER_ID, GROUP_ID)
                .applyCommand(ArmyCommand.clearTarget(OWNER_ID, GROUP_ID));

        ArmyBehaviorDecision decision = ArmyBehaviorPlanner.plan(recruit,
                context(new ArmyPosition(0, 64, 0), new ArmyPosition(8, 64, 0), THREAT_ID, true, 4));

        assertEquals(ArmyBehaviorIntent.IDLE, decision.intent(), "clear intent");
        assertEquals("target_cleared", decision.reasonCode(), "clear reason");
    }

    private static ArmyBehaviorContext context(
            ArmyPosition snightsisterPosition,
            ArmyPosition ownerPosition,
            UUID visibleThreatToOwner,
            boolean commandTargetAlive,
            int followRange
    ) {
        return ArmyBehaviorContext.of(snightsisterPosition, ownerPosition, visibleThreatToOwner, commandTargetAlive, followRange);
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
