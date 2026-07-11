package galacticwars.clonewars.army;

import java.util.UUID;

public final class ArmyTacticalPlannerTest {
    private static final ArmyPosition FALLBACK = new ArmyPosition(0, 64, 0);
    private static final ArmyPosition MOVE_TARGET = new ArmyPosition(12, 64, 4);
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");

    private ArmyTacticalPlannerTest() {
    }

    public static void main(String[] args) {
        executesOrderWhenVitalsAreReady();
        retreatsOnCriticalHealth();
        retreatsOnBrokenMorale();
        regroupsBeforeAttackOnLowMorale();
        holdsOnHungerExhaustion();
        regroupsWhenUpkeepIsOverdue();
        rejectsInvalidVitals();

        System.out.println("ArmyTacticalPlannerTest passed");
    }

    private static void executesOrderWhenVitalsAreReady() {
        ArmyBehaviorDecision behavior = ArmyBehaviorDecision.move(MOVE_TARGET, "move_command");

        ArmyTacticalDecision decision = ArmyTacticalPlanner.plan(behavior, new RecruitVitals(20, 24, 80, 80, 0), FALLBACK);

        assertEquals(ArmyTacticalIntent.EXECUTE_ORDER, decision.intent(), "ready intent");
        assertEquals(behavior, decision.behaviorDecision(), "ready behavior");
        assertEquals(null, decision.tacticalTarget(), "ready target");
        assertEquals("ready", decision.reasonCode(), "ready reason");
    }

    private static void retreatsOnCriticalHealth() {
        ArmyTacticalDecision decision = ArmyTacticalPlanner.plan(
                ArmyBehaviorDecision.attack(TARGET_ID, "attack_command"),
                new RecruitVitals(5, 24, 80, 80, 0),
                FALLBACK);

        assertEquals(ArmyTacticalIntent.RETREAT, decision.intent(), "critical health intent");
        assertEquals(FALLBACK, decision.tacticalTarget(), "critical health fallback");
        assertEquals("health_critical", decision.reasonCode(), "critical health reason");
    }

    private static void retreatsOnBrokenMorale() {
        ArmyTacticalDecision decision = ArmyTacticalPlanner.plan(
                ArmyBehaviorDecision.move(MOVE_TARGET, "move_command"),
                new RecruitVitals(20, 24, 15, 80, 0),
                FALLBACK);

        assertEquals(ArmyTacticalIntent.RETREAT, decision.intent(), "broken morale intent");
        assertEquals(FALLBACK, decision.tacticalTarget(), "broken morale fallback");
        assertEquals("morale_broken", decision.reasonCode(), "broken morale reason");
    }

    private static void regroupsBeforeAttackOnLowMorale() {
        ArmyTacticalDecision decision = ArmyTacticalPlanner.plan(
                ArmyBehaviorDecision.attack(TARGET_ID, "attack_command"),
                new RecruitVitals(20, 24, 35, 80, 0),
                FALLBACK);

        assertEquals(ArmyTacticalIntent.REGROUP, decision.intent(), "low morale intent");
        assertEquals(FALLBACK, decision.tacticalTarget(), "low morale fallback");
        assertEquals("morale_low", decision.reasonCode(), "low morale reason");
    }

    private static void holdsOnHungerExhaustion() {
        ArmyTacticalDecision decision = ArmyTacticalPlanner.plan(
                ArmyBehaviorDecision.move(MOVE_TARGET, "move_command"),
                new RecruitVitals(20, 24, 80, 10, 0),
                FALLBACK);

        assertEquals(ArmyTacticalIntent.HOLD_POSITION, decision.intent(), "hunger intent");
        assertEquals(MOVE_TARGET, decision.tacticalTarget(), "hunger hold target");
        assertEquals("hunger_exhausted", decision.reasonCode(), "hunger reason");
    }

    private static void regroupsWhenUpkeepIsOverdue() {
        ArmyTacticalDecision decision = ArmyTacticalPlanner.plan(
                ArmyBehaviorDecision.follow(MOVE_TARGET, "owner_out_of_range"),
                new RecruitVitals(20, 24, 80, 80, 24000),
                FALLBACK);

        assertEquals(ArmyTacticalIntent.REGROUP, decision.intent(), "upkeep intent");
        assertEquals(FALLBACK, decision.tacticalTarget(), "upkeep fallback");
        assertEquals("upkeep_overdue", decision.reasonCode(), "upkeep reason");
    }

    private static void rejectsInvalidVitals() {
        assertThrows(IllegalArgumentException.class, () -> new RecruitVitals(-1, 24, 80, 80, 0), "negative health");
        assertThrows(IllegalArgumentException.class, () -> new RecruitVitals(25, 24, 80, 80, 0), "health above max");
        assertThrows(IllegalArgumentException.class, () -> new RecruitVitals(1, 0, 80, 80, 0), "zero max health");
        assertThrows(IllegalArgumentException.class, () -> new RecruitVitals(1, 24, -1, 80, 0), "negative morale");
        assertThrows(IllegalArgumentException.class, () -> new RecruitVitals(1, 24, 101, 80, 0), "morale above range");
        assertThrows(IllegalArgumentException.class, () -> new RecruitVitals(1, 24, 80, -1, 0), "negative hunger");
        assertThrows(IllegalArgumentException.class, () -> new RecruitVitals(1, 24, 80, 101, 0), "hunger above range");
        assertThrows(IllegalArgumentException.class, () -> new RecruitVitals(1, 24, 80, 80, -1), "negative unpaid ticks");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static <T extends Throwable> void assertThrows(Class<T> expectedType, ThrowingRunnable runnable, String label) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                return;
            }
            throw new AssertionError(label + " threw " + throwable.getClass().getName() + " instead of "
                    + expectedType.getName(), throwable);
        }

        throw new AssertionError(label + " did not throw " + expectedType.getName());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
