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

    /**
     * Preserves every surviving binding, then assigns reinforcements to the
     * nearest vacant tactical depth for their role. Slot indices remain the
     * durable identity even when the active geometry compresses to a column.
     */
    public static List<ArmyFormationSlotAssignment> reconcileRoleAware(
            Collection<UUID> memberIds,
            Map<UUID, ArmyFormationRole> roles,
            Collection<ArmyFormationSlotAssignment> existingAssignments
    ) {
        Objects.requireNonNull(memberIds, "memberIds");
        Objects.requireNonNull(roles, "roles");
        Objects.requireNonNull(existingAssignments, "existingAssignments");
        List<UUID> members = sortedDistinct(memberIds);
        int memberCount = members.size();
        Map<UUID, ArmyFormationSlotAssignment> existingByMember = new HashMap<>();
        for (ArmyFormationSlotAssignment assignment : existingAssignments) {
            if (assignment != null) {
                existingByMember.putIfAbsent(assignment.memberId(), assignment);
            }
        }
        ArrayList<ArmyFormationSlotAssignment> reconciled = new ArrayList<>(memberCount);
        Set<Integer> used = new HashSet<>();
        for (UUID member : members) {
            ArmyFormationSlotAssignment existing = existingByMember.get(member);
            if (existing != null && existing.slotIndex() < memberCount && used.add(existing.slotIndex())) {
                reconciled.add(existing);
            }
        }
        List<UUID> newcomers = members.stream()
                .filter(member -> reconciled.stream().noneMatch(binding -> binding.memberId().equals(member)))
                .sorted(Comparator.comparing((UUID member) -> roles.getOrDefault(
                                member, ArmyFormationRole.FRONTLINE).preferredDepth())
                        .thenComparing(UUID::compareTo))
                .toList();
        for (UUID newcomer : newcomers) {
            ArmyFormationRole role = roles.getOrDefault(newcomer, ArmyFormationRole.FRONTLINE);
            int slot = bestVacantSlot(memberCount, used, role.preferredDepth());
            used.add(slot);
            reconciled.add(new ArmyFormationSlotAssignment(newcomer, slot));
        }
        reconciled.sort(Comparator.comparingInt(ArmyFormationSlotAssignment::slotIndex)
                .thenComparing(ArmyFormationSlotAssignment::memberId));
        return List.copyOf(reconciled);
    }

    public static List<ArmyFormationSlotAssignment> assignRoleAware(
            Collection<UUID> memberIds,
            Map<UUID, ArmyFormationRole> roles
    ) {
        return reconcileRoleAware(memberIds, roles, List.of());
    }

    private static int bestVacantSlot(int memberCount, Set<Integer> used, double preferredDepth) {
        int best = -1;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int slot = 0; slot < memberCount; slot++) {
            if (used.contains(slot)) {
                continue;
            }
            double depth = memberCount <= 1 ? 0.0D : slot / (double) (memberCount - 1);
            double distance = Math.abs(depth - preferredDepth);
            if (distance < bestDistance) {
                best = slot;
                bestDistance = distance;
            }
        }
        if (best < 0) {
            throw new IllegalStateException("no vacant formation slot");
        }
        return best;
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
