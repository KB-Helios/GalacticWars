package galacticwars.clonewars.force;

public final class ForcePhysicsRulesTest {
    public static void main(String[] args) {
        assertEquals(32.0D, ForcePhysicsRules.boundedRange(80.0D), "range cap");
        assertEquals(0.0D, ForcePhysicsRules.boundedRange(Double.NaN), "invalid range");
        assertEquals(100, ForcePhysicsRules.boundedChannelTicks(400), "channel cap");
        assertEquals(2.5D, ForcePhysicsRules.impulseForMass(50.0D, 0.25D, 0.0D, false),
                "velocity cap");
        double light = ForcePhysicsRules.impulseForMass(2.0D, 1.0D, 0.0D, false);
        double heavy = ForcePhysicsRules.impulseForMass(2.0D, 8.0D, 0.0D, false);
        double resistant = ForcePhysicsRules.impulseForMass(2.0D, 1.0D, 0.75D, false);
        double boss = ForcePhysicsRules.impulseForMass(2.0D, 1.0D, 0.0D, true);
        assertTrue(light > heavy && light > resistant && light > boss,
                "mass, resistance, and boss anchoring reduce impulses");
        assertEquals(0.0D, ForcePhysicsRules.cappedCollisionDamage(0.45D, 2.0D),
                "minor motion causes no collision damage");
        assertEquals(12.0D, ForcePhysicsRules.cappedCollisionDamage(20.0D, 20.0D),
                "collision damage cap");
        assertEquals(16, ForcePhysicsRules.MAX_AOE_TARGETS, "AOE bound");
        assertEquals(64, ForcePhysicsRules.MAX_ACTIVE_CHANNELS, "channel bound");
        assertEquals(32, ForcePhysicsRules.MAX_LIFTED_BLOCKS, "proxy bound");
        System.out.println("ForcePhysicsRulesTest passed");
    }

    private static void assertEquals(double expected, double actual, String label) {
        if (Math.abs(expected - actual) > 0.0001D) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) throw new AssertionError(label);
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
