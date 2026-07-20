package galacticwars.clonewars.army;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A persisted member-to-slot binding.  The binding is independent of entity
 * iteration order so a loaded squad does not reshuffle simply because its
 * members were reconstructed in a different order.
 */
public record ArmyFormationSlotAssignment(UUID memberId, int slotIndex) {
    public ArmyFormationSlotAssignment {
        Objects.requireNonNull(memberId, "memberId");
        if (slotIndex < 0) {
            throw new IllegalArgumentException("slotIndex cannot be negative");
        }
    }

    /**
     * Produces a deterministic baseline for legacy groups that have never
     * stored slot bindings. UUID ordering is deliberately the only tiebreaker.
     */
    public static List<ArmyFormationSlotAssignment> assignDeterministically(Collection<UUID> memberIds) {
        Objects.requireNonNull(memberIds, "memberIds");
        List<UUID> sortedMembers = sortedDistinct(memberIds);
        ArrayList<ArmyFormationSlotAssignment> assignments = new ArrayList<>(sortedMembers.size());
        for (int index = 0; index < sortedMembers.size(); index++) {
            assignments.add(new ArmyFormationSlotAssignment(sortedMembers.get(index), index));
        }
        return List.copyOf(assignments);
    }

    /**
     * Keeps valid existing bindings when membership changes, then gives new
     * members the lowest available slot in UUID order. This is deterministic
     * and minimizes slot churn without leaving unusable gaps.
     */
    public static List<ArmyFormationSlotAssignment> reconcile(
            Collection<UUID> memberIds,
            Collection<ArmyFormationSlotAssignment> existingAssignments
    ) {
        Objects.requireNonNull(memberIds, "memberIds");
        Objects.requireNonNull(existingAssignments, "existingAssignments");

        List<UUID> sortedMembers = sortedDistinct(memberIds);
        int memberCount = sortedMembers.size();
        Map<UUID, ArmyFormationSlotAssignment> existingByMember = new HashMap<>();
        for (ArmyFormationSlotAssignment assignment : existingAssignments) {
            if (assignment != null) {
                existingByMember.putIfAbsent(assignment.memberId(), assignment);
            }
        }

        ArrayList<ArmyFormationSlotAssignment> reconciled = new ArrayList<>(memberCount);
        Set<Integer> usedSlots = new HashSet<>();
        for (UUID memberId : sortedMembers) {
            ArmyFormationSlotAssignment existing = existingByMember.get(memberId);
            if (existing != null && existing.slotIndex() < memberCount && usedSlots.add(existing.slotIndex())) {
                reconciled.add(existing);
            }
        }

        int nextSlot = 0;
        for (UUID memberId : sortedMembers) {
            boolean alreadyAssigned = reconciled.stream().anyMatch(assignment -> assignment.memberId().equals(memberId));
            if (alreadyAssigned) {
                continue;
            }
            while (usedSlots.contains(nextSlot)) {
                nextSlot++;
            }
            reconciled.add(new ArmyFormationSlotAssignment(memberId, nextSlot));
            usedSlots.add(nextSlot);
        }

        reconciled.sort(Comparator.comparingInt(ArmyFormationSlotAssignment::slotIndex)
                .thenComparing(ArmyFormationSlotAssignment::memberId));
        return List.copyOf(reconciled);
    }

    private static List<UUID> sortedDistinct(Collection<UUID> memberIds) {
        LinkedHashSet<UUID> uniqueMembers = new LinkedHashSet<>(memberIds);
        if (uniqueMembers.contains(null)) {
            throw new NullPointerException("memberIds cannot contain null");
        }
        ArrayList<UUID> sortedMembers = new ArrayList<>(uniqueMembers);
        sortedMembers.sort(UUID::compareTo);
        return List.copyOf(sortedMembers);
    }
}
