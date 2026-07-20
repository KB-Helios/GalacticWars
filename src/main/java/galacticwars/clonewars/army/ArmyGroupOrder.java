package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ArmyGroupOrder(
        ArmyCommandType type,
        Optional<ArmyLocation> targetPosition,
        Optional<UUID> targetEntityId,
        ArmyFormation formation,
        int spacing
) {
    public ArmyGroupOrder {
        Objects.requireNonNull(type, "type");
        targetPosition = targetPosition == null ? Optional.empty() : targetPosition;
        targetEntityId = targetEntityId == null ? Optional.empty() : targetEntityId;
        Objects.requireNonNull(formation, "formation");
        if (spacing < 1 || spacing > 8) {
            throw new IllegalArgumentException("spacing must be between 1 and 8");
        }
        boolean positionRequired = type == ArmyCommandType.MOVE_TO_POSITION
                || type == ArmyCommandType.HOLD_POSITION
                || type == ArmyCommandType.PATROL_ROUTE;
        boolean entityRequired = type == ArmyCommandType.ATTACK_TARGET || type == ArmyCommandType.PROTECT_ENTITY;
        boolean positionForbidden = !positionRequired && type != ArmyCommandType.ATTACK_TARGET;
        if ((positionRequired && targetPosition.isEmpty())
                || (positionForbidden && targetPosition.isPresent())
                || entityRequired != targetEntityId.isPresent()) {
            throw new IllegalArgumentException("Invalid persisted payload for " + type);
        }
    }

    public static ArmyGroupOrder follow(ArmyFormation formation) {
        return new ArmyGroupOrder(ArmyCommandType.FOLLOW_OWNER, Optional.empty(), Optional.empty(), formation, 2);
    }

    public ArmyCommand toCommand(UUID ownerId, UUID groupId) {
        return switch (type) {
            case FOLLOW_OWNER -> ArmyCommand.followOwner(ownerId, groupId);
            case HOLD_POSITION -> ArmyCommand.holdPosition(ownerId, groupId, targetPosition.orElseThrow().blockPosition());
            case MOVE_TO_POSITION -> ArmyCommand.moveToPosition(ownerId, groupId, targetPosition.orElseThrow().blockPosition());
            case PROTECT_OWNER -> ArmyCommand.protectOwner(ownerId, groupId);
            case PROTECT_ENTITY -> ArmyCommand.protectEntity(ownerId, groupId, targetEntityId.orElseThrow());
            case ATTACK_TARGET -> ArmyCommand.attackTarget(ownerId, groupId, targetEntityId.orElseThrow());
            case CLEAR_TARGET -> ArmyCommand.clearTarget(ownerId, groupId);
            case RETURN_TO_RALLY -> ArmyCommand.returnToRally(ownerId, groupId);
            case PATROL_ROUTE -> ArmyCommand.patrolRoute(
                    ownerId, groupId, targetPosition.orElseThrow().blockPosition());
        };
    }

    public ArmyGroupOrder withFormation(ArmyFormation formation) {
        return new ArmyGroupOrder(type, targetPosition, targetEntityId, formation, spacing);
    }
}
