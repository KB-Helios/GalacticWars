package middleearth.lotr.warmod.army;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ArmyGroupOrderPlanner {
    private ArmyGroupOrderPlanner() {
    }

    public static List<ArmyGroupOrderAssignment> plan(ArmyGroupState group, ArmyFormation formation, int spacing) {
        return plan(group, formation, spacing, null);
    }

    public static List<ArmyGroupOrderAssignment> plan(
            ArmyGroupState group,
            ArmyFormation formation,
            int spacing,
            ArmyPosition ownerAnchor
    ) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(formation, "formation");

        ArmyCommand groupCommand = group.currentCommand();
        return switch (groupCommand.type()) {
            case MOVE_TO_POSITION -> planFormationCommand(group, formation, spacing, true);
            case HOLD_POSITION -> planFormationCommand(group, formation, spacing, false);
            case FOLLOW_OWNER -> ownerAnchor == null
                    ? planDirectCommand(group, "follow_group_order")
                    : planOwnerFormationCommand(group, formation, spacing, ownerAnchor, "follow_group_order");
            case PROTECT_OWNER -> ownerAnchor == null
                    ? planDirectCommand(group, "protect_group_order")
                    : planOwnerFormationCommand(group, formation, spacing, ownerAnchor, "protect_group_order");
            case ATTACK_TARGET -> planDirectCommand(group, "attack_group_order");
            case CLEAR_TARGET -> planDirectCommand(group, "clear_group_order");
        };
    }

    private static List<ArmyGroupOrderAssignment> planFormationCommand(
            ArmyGroupState group,
            ArmyFormation formation,
            int spacing,
            boolean moveCommand
    ) {
        ArmyCommand groupCommand = group.currentCommand();
        ArmyPosition anchor = Objects.requireNonNull(groupCommand.targetPosition(), "group command targetPosition");
        List<UUID> recruitIds = List.copyOf(group.recruitIds());
        List<FormationSlot> slots = FormationPlanner.planSlots(formation, recruitIds.size(), spacing);
        ArrayList<ArmyGroupOrderAssignment> assignments = new ArrayList<>(recruitIds.size());

        for (int index = 0; index < recruitIds.size(); index++) {
            FormationSlot slot = slots.get(index);
            ArmyPosition assignedPosition = new ArmyPosition(
                    anchor.x() + slot.sideOffset(),
                    anchor.y(),
                    anchor.z() + slot.forwardOffset());
            ArmyCommand recruitCommand = moveCommand
                    ? ArmyCommand.moveToPosition(group.ownerId(), group.groupId(), assignedPosition)
                    : ArmyCommand.holdPosition(group.ownerId(), group.groupId(), assignedPosition);
            assignments.add(new ArmyGroupOrderAssignment(
                    recruitIds.get(index),
                    recruitCommand,
                    assignedPosition,
                    slot,
                    moveCommand ? "move_group_order" : "hold_group_order"));
        }

        return List.copyOf(assignments);
    }

    private static List<ArmyGroupOrderAssignment> planOwnerFormationCommand(
            ArmyGroupState group,
            ArmyFormation formation,
            int spacing,
            ArmyPosition ownerAnchor,
            String reasonCode
    ) {
        List<UUID> recruitIds = List.copyOf(group.recruitIds());
        List<FormationSlot> slots = FormationPlanner.planSlots(formation, recruitIds.size(), spacing);
        ArrayList<ArmyGroupOrderAssignment> assignments = new ArrayList<>(recruitIds.size());
        for (int index = 0; index < recruitIds.size(); index++) {
            FormationSlot slot = slots.get(index);
            ArmyPosition assignedPosition = new ArmyPosition(
                    ownerAnchor.x() + slot.sideOffset(),
                    ownerAnchor.y(),
                    ownerAnchor.z() + slot.forwardOffset());
            assignments.add(new ArmyGroupOrderAssignment(
                    recruitIds.get(index),
                    copyForRecruit(group),
                    assignedPosition,
                    slot,
                    reasonCode));
        }
        return List.copyOf(assignments);
    }

    private static List<ArmyGroupOrderAssignment> planDirectCommand(ArmyGroupState group, String reasonCode) {
        ArrayList<ArmyGroupOrderAssignment> assignments = new ArrayList<>(group.recruitIds().size());
        for (UUID recruitId : group.recruitIds()) {
            assignments.add(new ArmyGroupOrderAssignment(recruitId, copyForRecruit(group), null, null, reasonCode));
        }
        return List.copyOf(assignments);
    }

    private static ArmyCommand copyForRecruit(ArmyGroupState group) {
        ArmyCommand command = group.currentCommand();
        return switch (command.type()) {
            case FOLLOW_OWNER -> ArmyCommand.followOwner(group.ownerId(), group.groupId());
            case PROTECT_OWNER -> ArmyCommand.protectOwner(group.ownerId(), group.groupId());
            case ATTACK_TARGET -> ArmyCommand.attackTarget(group.ownerId(), group.groupId(), command.targetEntityId());
            case CLEAR_TARGET -> ArmyCommand.clearTarget(group.ownerId(), group.groupId());
            case MOVE_TO_POSITION -> ArmyCommand.moveToPosition(group.ownerId(), group.groupId(), command.targetPosition());
            case HOLD_POSITION -> ArmyCommand.holdPosition(group.ownerId(), group.groupId(), command.targetPosition());
        };
    }
}
