package middleearth.lotr.warmod.army;

import middleearth.lotr.warmod.faction.FactionAlignment;
import middleearth.lotr.warmod.faction.FactionId;

public final class ArmyCommandPolicy {
    private ArmyCommandPolicy() {
    }

    public static ArmyCommandValidation canIssue(
            ArmyCommand command,
            ArmyGroupState group,
            FactionAlignment alignment,
            FactionId unitFaction,
            int minimumAlignment
    ) {
        if (command == null) {
            return ArmyCommandValidation.rejected("missing_command");
        }
        if (group == null) {
            return ArmyCommandValidation.rejected("missing_group");
        }
        if (unitFaction == null) {
            return ArmyCommandValidation.rejected("unknown_faction");
        }
        if (!group.ownerId().equals(command.issuedBy())) {
            return ArmyCommandValidation.rejected("not_owner");
        }
        if (!group.groupId().equals(command.groupId())) {
            return ArmyCommandValidation.rejected("group_mismatch");
        }
        if (alignment == null || !alignment.playerId().equals(command.issuedBy())) {
            return ArmyCommandValidation.rejected("unknown_player");
        }
        if (group.recruitIds().isEmpty()) {
            return ArmyCommandValidation.rejected("empty_group");
        }
        if (alignment.score(unitFaction) < minimumAlignment) {
            return ArmyCommandValidation.rejected("alignment_too_low");
        }
        if (!hasValidPayload(command)) {
            return ArmyCommandValidation.rejected("invalid_payload");
        }
        return ArmyCommandValidation.acceptedResult();
    }

    private static boolean hasValidPayload(ArmyCommand command) {
        return switch (command.type()) {
            case FOLLOW_OWNER, PROTECT_OWNER, CLEAR_TARGET ->
                    command.targetPosition() == null && command.targetEntityId() == null;
            case HOLD_POSITION, MOVE_TO_POSITION ->
                    command.targetPosition() != null && command.targetEntityId() == null;
            case ATTACK_TARGET ->
                    command.targetPosition() == null && command.targetEntityId() != null;
        };
    }
}
