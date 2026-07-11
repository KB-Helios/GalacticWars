package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.UUID;

public record RecruitState(
        UUID recruitId,
        UUID ownerId,
        UUID groupId,
        ArmyCommand currentCommand
) {
    public RecruitState {
        Objects.requireNonNull(recruitId, "recruitId");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(currentCommand, "currentCommand");
    }

    public static RecruitState createOwned(UUID recruitId, UUID ownerId, UUID groupId) {
        return new RecruitState(recruitId, ownerId, groupId, ArmyCommand.followOwner(ownerId, groupId));
    }

    public RecruitState applyCommand(ArmyCommand command) {
        validateCommand(command);
        return new RecruitState(recruitId, ownerId, groupId, command);
    }

    private void validateCommand(ArmyCommand command) {
        Objects.requireNonNull(command, "command");
        if (!ownerId.equals(command.issuedBy())) {
            throw new SecurityException("Army command rejected: issuing player does not own recruit " + recruitId);
        }
        if (!groupId.equals(command.groupId())) {
            throw new IllegalArgumentException("Army command rejected: command group does not match recruit group " + groupId);
        }
    }
}
