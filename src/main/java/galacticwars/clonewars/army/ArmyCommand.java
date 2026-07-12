package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.UUID;

public record ArmyCommand(
        ArmyCommandType type,
        UUID issuedBy,
        UUID groupId,
        ArmyPosition targetPosition,
        UUID targetEntityId
) {
    public ArmyCommand {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(issuedBy, "issuedBy");
        Objects.requireNonNull(groupId, "groupId");
    }

    public static ArmyCommand followOwner(UUID issuedBy, UUID groupId) {
        return new ArmyCommand(ArmyCommandType.FOLLOW_OWNER, issuedBy, groupId, null, null);
    }

    public static ArmyCommand holdPosition(UUID issuedBy, UUID groupId, ArmyPosition position) {
        return new ArmyCommand(ArmyCommandType.HOLD_POSITION, issuedBy, groupId, Objects.requireNonNull(position, "position"), null);
    }

    public static ArmyCommand moveToPosition(UUID issuedBy, UUID groupId, ArmyPosition position) {
        return new ArmyCommand(ArmyCommandType.MOVE_TO_POSITION, issuedBy, groupId, Objects.requireNonNull(position, "position"), null);
    }

    public static ArmyCommand protectOwner(UUID issuedBy, UUID groupId) {
        return new ArmyCommand(ArmyCommandType.PROTECT_OWNER, issuedBy, groupId, null, null);
    }

    public static ArmyCommand attackTarget(UUID issuedBy, UUID groupId, UUID targetEntityId) {
        return new ArmyCommand(ArmyCommandType.ATTACK_TARGET, issuedBy, groupId, null,
                Objects.requireNonNull(targetEntityId, "targetEntityId"));
    }

    public static ArmyCommand clearTarget(UUID issuedBy, UUID groupId) {
        return new ArmyCommand(ArmyCommandType.CLEAR_TARGET, issuedBy, groupId, null, null);
    }

    public static ArmyCommand patrolRoute(UUID issuedBy, UUID groupId, ArmyPosition waypoint) {
        return new ArmyCommand(
                ArmyCommandType.PATROL_ROUTE,
                issuedBy,
                groupId,
                Objects.requireNonNull(waypoint, "waypoint"),
                null);
    }
}
