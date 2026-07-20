package galacticwars.clonewars.army;

/** Pure doctrine checks for the server-side ranged combat gate. */
public final class ArmyRangedFirePolicyTest {
    private ArmyRangedFirePolicyTest() {
    }

    public static void main(String[] args) {
        holdFireNeverPermitsShots();
        returnFireRequiresARecentAttack();
        freeFirePermitsAnyAuthorizedCombatTarget();
        focusFireRequiresTheExplicitCommandTarget();

        System.out.println("ArmyRangedFirePolicyTest passed");
    }

    private static void holdFireNeverPermitsShots() {
        assertFalse(ArmyRangedFirePolicy.HOLD_FIRE.allowsRangedFire(false, false),
                "hold fire ordinary target");
        assertFalse(ArmyRangedFirePolicy.HOLD_FIRE.allowsRangedFire(true, true),
                "hold fire return command target");
    }

    private static void returnFireRequiresARecentAttack() {
        assertFalse(ArmyRangedFirePolicy.RETURN_FIRE.allowsRangedFire(false, true),
                "return fire command target alone");
        assertTrue(ArmyRangedFirePolicy.RETURN_FIRE.allowsRangedFire(true, false),
                "return fire retaliatory target");
    }

    private static void freeFirePermitsAnyAuthorizedCombatTarget() {
        assertTrue(ArmyRangedFirePolicy.FREE_FIRE.allowsRangedFire(false, false),
                "free fire ordinary target");
    }

    private static void focusFireRequiresTheExplicitCommandTarget() {
        assertFalse(ArmyRangedFirePolicy.FOCUS_COMMAND_TARGET.allowsRangedFire(true, false),
                "focus fire return-fire distraction");
        assertTrue(ArmyRangedFirePolicy.FOCUS_COMMAND_TARGET.allowsRangedFire(false, true),
                "focus fire command target");
    }

    private static void assertFalse(boolean value, String label) {
        if (value) {
            throw new AssertionError(label + " expected false");
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label + " expected true");
        }
    }
}
