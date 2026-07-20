package galacticwars.clonewars.army;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ArmyGroupOrderPlanner {
    private ArmyGroupOrderPlanner() {
    }

    public static List<ArmyGroupOrderAssignment> plan(ArmyGroupState group, ArmyFormation formation, int spacing) {
        return plan(group, formation, spacing, null, 0.0F, List.of());
    }

    public static List<ArmyGroupOrderAssignment> plan(
            ArmyGroupState group,
            ArmyFormation formation,
            int spacing,
            ArmyPosition ownerAnchor
    ) {
        return plan(group, formation, spacing, ownerAnchor, 0.0F, List.of());
    }

    /**
     * Uses persisted slot bindings and formation yaw when they are available.
     * The existing overloads remain world-axis and UUID-deterministic.
     */
    public static List<ArmyGroupOrderAssignment> plan(
            ArmyGroupState group,
            ArmyFormation formation,
            int spacing,
            ArmyPosition ownerAnchor,
            float yawDegrees,
            Collection<ArmyFormationSlotAssignment> slotAssignments
    ) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(formation, "formation");
        Objects.requireNonNull(slotAssignments, "slotAssignments");

        ArmyCommand groupCommand = group.currentCommand();
        return switch (groupCommand.type()) {
            case MOVE_TO_POSITION, PATROL_ROUTE -> planFormationCommand(
                    group, formation, spacing, true, yawDegrees, slotAssignments, "move_group_order");
            case HOLD_POSITION -> planFormationCommand(
                    group, formation, spacing, false, yawDegrees, slotAssignments, "hold_group_order");
            case FOLLOW_OWNER -> ownerAnchor == null
                    ? planDirectCommand(group, slotAssignments, "follow_group_order")
                    : planOwnerFormationCommand(group, formation, spacing, ownerAnchor, yawDegrees,
                            slotAssignments, "follow_group_order");
            case PROTECT_OWNER -> ownerAnchor == null
                    ? planDirectCommand(group, slotAssignments, "protect_group_order")
                    : planOwnerFormationCommand(group, formation, spacing, ownerAnchor, yawDegrees,
                            slotAssignments, "protect_group_order");
            case PROTECT_ENTITY -> planDirectCommand(group, slotAssignments, "protect_entity_group_order");
            case ATTACK_TARGET -> planDirectCommand(group, slotAssignments, "attack_group_order");
            case CLEAR_TARGET -> planDirectCommand(group, slotAssignments, "clear_group_order");
            case RETURN_TO_RALLY -> planDirectCommand(group, slotAssignments, "return_to_rally_group_order");
        };
    }

    /** Plans a persisted group using its optional durable doctrine and slot bindings. */
    public static List<ArmyGroupOrderAssignment> plan(ArmyGroupRecord group, ArmyPosition ownerAnchor) {
        Objects.requireNonNull(group, "group");
        ArmyGroupTactics tactics = group.effectiveTactics();
        if (group.order().type() == ArmyCommandType.RETURN_TO_RALLY && group.rallyPoint().isPresent()) {
            ArmyCommand returnCommand = ArmyCommand.moveToPosition(group.ownerId(), group.id(),
                    group.rallyPoint().orElseThrow().blockPosition());
            ArmyGroupState returnState = group.plannerState().applyCommand(returnCommand);
            return planFormationCommand(returnState, group.order().formation(), group.order().spacing(), true,
                    tactics.effectiveFormationYawDegrees(), group.effectiveFormationSlotAssignments(), "return_to_rally");
        }
        return plan(group.plannerState(), group.order().formation(), group.order().spacing(), ownerAnchor,
                tactics.effectiveFormationYawDegrees(), group.effectiveFormationSlotAssignments());
    }

    /**
     * Resolves a durable respawn position without depending on snapshot or
     * entity iteration order. The commander anchors the formation; every
     * soldier keeps its persisted slot through virtual unload/reload cycles.
     */
    public static ArmyPosition formationPositionForMember(
            ArmyGroupRecord group,
            UUID recruitId,
            ArmyPosition anchor
    ) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(recruitId, "recruitId");
        Objects.requireNonNull(anchor, "anchor");
        if (group.commanderId().filter(recruitId::equals).isPresent()) {
            return anchor;
        }
        if (!group.memberIds().contains(recruitId)) {
            throw new IllegalArgumentException("recruit is not a member of this army group");
        }
        ArmyFormationSlotAssignment assignment = group.effectiveFormationSlotAssignments().stream()
                .filter(candidate -> candidate.memberId().equals(recruitId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("member is missing a formation slot"));
        FormationSlot slot = FormationPlanner.planSlots(
                group.order().formation(), group.memberIds().size(), group.order().spacing())
                .get(assignment.slotIndex());
        return FormationPlanner.positionFor(anchor, slot, group.effectiveTactics().effectiveFormationYawDegrees());
    }

    private static List<ArmyGroupOrderAssignment> planFormationCommand(
            ArmyGroupState group,
            ArmyFormation formation,
            int spacing,
            boolean moveCommand,
            float yawDegrees,
            Collection<ArmyFormationSlotAssignment> persistedAssignments,
            String reasonCode
    ) {
        ArmyCommand groupCommand = group.currentCommand();
        ArmyPosition anchor = Objects.requireNonNull(groupCommand.targetPosition(), "group command targetPosition");
        List<ArmyFormationSlotAssignment> assignments = stableAssignments(group.recruitIds(), persistedAssignments);
        List<FormationSlot> slots = FormationPlanner.planSlots(formation, assignments.size(), spacing);
        ArrayList<ArmyGroupOrderAssignment> planned = new ArrayList<>(assignments.size());

        for (ArmyFormationSlotAssignment slotAssignment : assignments) {
            FormationSlot slot = slots.get(slotAssignment.slotIndex());
            ArmyPosition assignedPosition = FormationPlanner.positionFor(anchor, slot, yawDegrees);
            ArmyCommand recruitCommand = moveCommand
                    ? ArmyCommand.moveToPosition(group.ownerId(), group.groupId(), assignedPosition)
                    : ArmyCommand.holdPosition(group.ownerId(), group.groupId(), assignedPosition);
            planned.add(new ArmyGroupOrderAssignment(
                    slotAssignment.memberId(), recruitCommand, assignedPosition, slot, reasonCode));
        }

        return List.copyOf(planned);
    }

    private static List<ArmyGroupOrderAssignment> planOwnerFormationCommand(
            ArmyGroupState group,
            ArmyFormation formation,
            int spacing,
            ArmyPosition ownerAnchor,
            float yawDegrees,
            Collection<ArmyFormationSlotAssignment> persistedAssignments,
            String reasonCode
    ) {
        List<ArmyFormationSlotAssignment> assignments = stableAssignments(group.recruitIds(), persistedAssignments);
        List<FormationSlot> slots = FormationPlanner.planSlots(formation, assignments.size(), spacing);
        ArrayList<ArmyGroupOrderAssignment> planned = new ArrayList<>(assignments.size());
        for (ArmyFormationSlotAssignment slotAssignment : assignments) {
            FormationSlot slot = slots.get(slotAssignment.slotIndex());
            ArmyPosition assignedPosition = FormationPlanner.positionFor(ownerAnchor, slot, yawDegrees);
            planned.add(new ArmyGroupOrderAssignment(
                    slotAssignment.memberId(), copyForRecruit(group), assignedPosition, slot, reasonCode));
        }
        return List.copyOf(planned);
    }

    private static List<ArmyGroupOrderAssignment> planDirectCommand(
            ArmyGroupState group,
            Collection<ArmyFormationSlotAssignment> persistedAssignments,
            String reasonCode
    ) {
        List<ArmyFormationSlotAssignment> assignments = stableAssignments(group.recruitIds(), persistedAssignments);
        ArrayList<ArmyGroupOrderAssignment> planned = new ArrayList<>(assignments.size());
        for (ArmyFormationSlotAssignment assignment : assignments) {
            planned.add(new ArmyGroupOrderAssignment(
                    assignment.memberId(), copyForRecruit(group), null, null, reasonCode));
        }
        return List.copyOf(planned);
    }

    private static List<ArmyFormationSlotAssignment> stableAssignments(
            Collection<UUID> recruitIds,
            Collection<ArmyFormationSlotAssignment> persistedAssignments
    ) {
        return ArmyFormationSlotAssignment.reconcile(recruitIds, persistedAssignments);
    }

    private static ArmyCommand copyForRecruit(ArmyGroupState group) {
        ArmyCommand command = group.currentCommand();
        return switch (command.type()) {
            case FOLLOW_OWNER -> ArmyCommand.followOwner(group.ownerId(), group.groupId());
            case PROTECT_OWNER -> ArmyCommand.protectOwner(group.ownerId(), group.groupId());
            case PROTECT_ENTITY -> ArmyCommand.protectEntity(group.ownerId(), group.groupId(), command.targetEntityId());
            case ATTACK_TARGET -> ArmyCommand.attackTarget(group.ownerId(), group.groupId(), command.targetEntityId());
            case CLEAR_TARGET -> ArmyCommand.clearTarget(group.ownerId(), group.groupId());
            case RETURN_TO_RALLY -> ArmyCommand.returnToRally(group.ownerId(), group.groupId());
            case MOVE_TO_POSITION -> ArmyCommand.moveToPosition(group.ownerId(), group.groupId(), command.targetPosition());
            case HOLD_POSITION -> ArmyCommand.holdPosition(group.ownerId(), group.groupId(), command.targetPosition());
            case PATROL_ROUTE -> ArmyCommand.patrolRoute(
                    group.ownerId(), group.groupId(), command.targetPosition());
        };
    }
}
