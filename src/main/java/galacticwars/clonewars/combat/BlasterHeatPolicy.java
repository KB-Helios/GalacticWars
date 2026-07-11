package galacticwars.clonewars.combat;

public final class BlasterHeatPolicy {
    public static final int SHOTS_BEFORE_OVERHEAT = 6;
    public static final int OVERHEAT_TICKS = 40;
    public static final int SHOT_COOLDOWN_TICKS = 12;

    private BlasterHeatPolicy() {
    }

    public static BlasterHeatState tick(BlasterHeatState state) {
        return new BlasterHeatState(
                state.shotsRemaining(),
                Math.max(0, state.shotCooldownTicks() - 1),
                Math.max(0, state.overheatTicks() - 1));
    }

    public static boolean canFire(BlasterHeatState state) {
        return state.shotCooldownTicks() == 0 && state.overheatTicks() == 0;
    }

    public static BlasterHeatState afterShot(BlasterHeatState state) {
        if (!canFire(state)) {
            return state;
        }
        int remaining = state.shotsRemaining() - 1;
        if (remaining <= 0) {
            return new BlasterHeatState(SHOTS_BEFORE_OVERHEAT, SHOT_COOLDOWN_TICKS, OVERHEAT_TICKS);
        }
        return new BlasterHeatState(remaining, SHOT_COOLDOWN_TICKS, 0);
    }

    public record BlasterHeatState(int shotsRemaining, int shotCooldownTicks, int overheatTicks) {
        public BlasterHeatState {
            if (shotsRemaining < 1 || shotsRemaining > SHOTS_BEFORE_OVERHEAT
                    || shotCooldownTicks < 0 || overheatTicks < 0) {
                throw new IllegalArgumentException("Invalid blaster heat state");
            }
        }

        public static BlasterHeatState ready() {
            return new BlasterHeatState(SHOTS_BEFORE_OVERHEAT, 0, 0);
        }

        public boolean isReady() {
            return this.equals(ready());
        }

        public float heatFraction() {
            if (overheatTicks > 0) {
                return 1.0F;
            }
            return (SHOTS_BEFORE_OVERHEAT - shotsRemaining) / (float) SHOTS_BEFORE_OVERHEAT;
        }
    }
}
