package galacticwars.clonewars.army;

import galacticwars.clonewars.faction.FactionBalanceService;

/**
 * Compatibility facade for the pre-SmartBrainLib squad cadence calculations.
 *
 * <p>Live grouped recruits are now executed by the SmartBrain sensors and
 * behaviours. This type deliberately retains no entity state and must not
 * navigate, select targets, or perform attacks.</p>
 */
@Deprecated(forRemoval = false)
public final class ArmyRecruitRuntimeController {
    private ArmyRecruitRuntimeController() {
    }

    static int effectiveCoordinationPercent(
            int coordinationPercent,
            int withoutCommandNodePercent,
            boolean commandNodeActive
    ) {
        int boundedCoordination = Math.max(1,
                Math.min(FactionBalanceService.MAX_PERCENT, coordinationPercent));
        if (commandNodeActive) {
            return boundedCoordination;
        }
        int combined = FactionBalanceService.applyPercentCeil(
                boundedCoordination, withoutCommandNodePercent);
        return Math.max(1, Math.min(FactionBalanceService.MAX_PERCENT, combined));
    }

    static int coordinatedAttackCooldownTicks(
            int baseCooldownTicks,
            int effectiveCoordinationPercent,
            boolean commanderNearby
    ) {
        if (baseCooldownTicks <= 0) {
            throw new IllegalArgumentException("baseCooldownTicks must be positive");
        }
        if (!commanderNearby) {
            return baseCooldownTicks;
        }
        int boundedPercent = Math.max(1,
                Math.min(FactionBalanceService.MAX_PERCENT, effectiveCoordinationPercent));
        long numerator = (long) baseCooldownTicks * 100L;
        long adjusted = (numerator + boundedPercent - 1L) / boundedPercent;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, adjusted));
    }
}
