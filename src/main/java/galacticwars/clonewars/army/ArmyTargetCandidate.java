package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.faction.FactionRelation;

public record ArmyTargetCandidate(
        UUID entityId,
        FactionId factionId,
        ArmyPosition position,
        boolean attackingOwner,
        boolean attackingRecruit,
        int threat,
        Optional<FactionRelation> relationOverride
) {
    public ArmyTargetCandidate {
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(factionId, "factionId");
        Objects.requireNonNull(position, "position");
        if (threat < 0 || threat > 100) {
            throw new IllegalArgumentException("threat must be between 0 and 100");
        }
        relationOverride = relationOverride == null ? Optional.empty() : relationOverride;
    }

    public ArmyTargetCandidate(
            UUID entityId,
            FactionId factionId,
            ArmyPosition position,
            boolean attackingOwner,
            boolean attackingRecruit,
            int threat
    ) {
        this(entityId, factionId, position, attackingOwner, attackingRecruit, threat, Optional.empty());
    }
}
