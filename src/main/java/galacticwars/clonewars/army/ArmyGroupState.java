package galacticwars.clonewars.army;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record ArmyGroupState(
        UUID groupId,
        UUID ownerId,
        Set<UUID> recruitIds,
        ArmyCommand currentCommand
) {
    public ArmyGroupState {
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(recruitIds, "recruitIds");
        Objects.requireNonNull(currentCommand, "currentCommand");
        recruitIds = Collections.unmodifiableSet(new LinkedHashSet<>(recruitIds));
    }

    public static ArmyGroupState create(UUID groupId, UUID ownerId) {
        return new ArmyGroupState(groupId, ownerId, Set.of(), ArmyCommand.followOwner(ownerId, groupId));
    }

    public ArmyGroupState withRecruit(UUID recruitId) {
        Objects.requireNonNull(recruitId, "recruitId");
        LinkedHashSet<UUID> updatedRecruitIds = new LinkedHashSet<>(recruitIds);
        updatedRecruitIds.add(recruitId);
        return new ArmyGroupState(groupId, ownerId, updatedRecruitIds, currentCommand);
    }

    public ArmyGroupState withoutRecruit(UUID recruitId) {
        Objects.requireNonNull(recruitId, "recruitId");
        LinkedHashSet<UUID> updatedRecruitIds = new LinkedHashSet<>(recruitIds);
        updatedRecruitIds.remove(recruitId);
        return new ArmyGroupState(groupId, ownerId, updatedRecruitIds, currentCommand);
    }

    public boolean containsRecruit(UUID recruitId) {
        return recruitIds.contains(recruitId);
    }

    public ArmyGroupState applyCommand(ArmyCommand command) {
        validateCommand(command);
        return new ArmyGroupState(groupId, ownerId, recruitIds, command);
    }

    private void validateCommand(ArmyCommand command) {
        Objects.requireNonNull(command, "command");
        if (!ownerId.equals(command.issuedBy())) {
            throw new SecurityException("Army group command rejected: issuing player does not own group " + groupId);
        }
        if (!groupId.equals(command.groupId())) {
            throw new IllegalArgumentException("Army group command rejected: command group does not match " + groupId);
        }
    }
}
