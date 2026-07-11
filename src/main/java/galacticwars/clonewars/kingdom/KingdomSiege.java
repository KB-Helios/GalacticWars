package galacticwars.clonewars.kingdom;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record KingdomSiege(
        UUID id,
        UUID claimId,
        UUID attackerKingdomId,
        UUID defenderKingdomId,
        SiegeState state,
        int captureProgress,
        int captureGoal,
        long lastProgressGameTime,
        List<UUID> attackingParticipants,
        List<UUID> defendingParticipants
) {
    public KingdomSiege {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(claimId, "claimId");
        Objects.requireNonNull(attackerKingdomId, "attackerKingdomId");
        Objects.requireNonNull(defenderKingdomId, "defenderKingdomId");
        Objects.requireNonNull(state, "state");
        if (attackerKingdomId.equals(defenderKingdomId)) {
            throw new IllegalArgumentException("siege participants must be different kingdoms");
        }
        if (captureGoal <= 0 || captureProgress < 0 || captureProgress > captureGoal) {
            throw new IllegalArgumentException("invalid siege progress");
        }
        lastProgressGameTime = Math.max(0L, lastProgressGameTime);
        attackingParticipants = List.copyOf(new LinkedHashSet<>(attackingParticipants));
        defendingParticipants = List.copyOf(new LinkedHashSet<>(defendingParticipants));
    }

    public static KingdomSiege start(
            UUID claimId,
            UUID attackerKingdomId,
            UUID defenderKingdomId,
            int captureGoal,
            long gameTime
    ) {
        return new KingdomSiege(UUID.randomUUID(), claimId, attackerKingdomId, defenderKingdomId,
                SiegeState.ACTIVE, 0, captureGoal, gameTime, List.of(), List.of());
    }

    public KingdomSiege progress(
            int attackerStrength,
            int defenderStrength,
            long gameTime,
            List<UUID> attackers,
            List<UUID> defenders
    ) {
        if (state != SiegeState.ACTIVE || attackerStrength <= 0) {
            return this;
        }
        int delta = Math.max(0, attackerStrength - defenderStrength);
        if (delta == 0) {
            return new KingdomSiege(id, claimId, attackerKingdomId, defenderKingdomId, state,
                    captureProgress, captureGoal, gameTime, attackers, defenders);
        }
        int updated = Math.min(captureGoal, Math.addExact(captureProgress, delta));
        SiegeState updatedState = updated == captureGoal ? SiegeState.CAPTURED : SiegeState.ACTIVE;
        return new KingdomSiege(id, claimId, attackerKingdomId, defenderKingdomId, updatedState,
                updated, captureGoal, gameTime, attackers, defenders);
    }

    public KingdomSiege cancel(SiegeState terminalState) {
        if (!terminalState.terminal()) {
            throw new IllegalArgumentException("siege cancellation requires a terminal state");
        }
        return new KingdomSiege(id, claimId, attackerKingdomId, defenderKingdomId, terminalState,
                captureProgress, captureGoal, lastProgressGameTime,
                attackingParticipants, defendingParticipants);
    }
}
