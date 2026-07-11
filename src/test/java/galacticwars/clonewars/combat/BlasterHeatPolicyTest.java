package galacticwars.clonewars.combat;

public final class BlasterHeatPolicyTest {
    private BlasterHeatPolicyTest() {
    }

    public static void main(String[] args) {
        BlasterHeatPolicy.BlasterHeatState state = BlasterHeatPolicy.BlasterHeatState.ready();
        for (int shot = 0; shot < BlasterHeatPolicy.SHOTS_BEFORE_OVERHEAT; shot++) {
            assertTrue(BlasterHeatPolicy.canFire(state), "shot " + shot + " should be available");
            state = BlasterHeatPolicy.afterShot(state);
            while (state.shotCooldownTicks() > 0) {
                state = BlasterHeatPolicy.tick(state);
            }
        }
        assertTrue(state.overheatTicks() > 0, "sixth shot should overheat");
        assertFalse(BlasterHeatPolicy.canFire(state), "overheated weapon must not fire");
        while (state.overheatTicks() > 0) {
            state = BlasterHeatPolicy.tick(state);
        }
        assertTrue(BlasterHeatPolicy.canFire(state), "cooled weapon should fire again");
        assertTrue(state.shotsRemaining() == BlasterHeatPolicy.SHOTS_BEFORE_OVERHEAT,
                "cooling should restore the heat budget");
        System.out.println("BlasterHeatPolicyTest passed");
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private static void assertFalse(boolean condition, String label) {
        assertTrue(!condition, label);
    }
}
