package middleearth.lotr.warmod.army;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ArmyGroupRecord(
        UUID id,
        UUID ownerId,
        UUID kingdomId,
        Optional<UUID> commanderId,
        List<UUID> memberIds,
        ArmyGroupOrder order,
        ArmyGroupSimulation simulation,
        List<ArmyMemberSnapshot> snapshots
) {
    public ArmyGroupRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(kingdomId, "kingdomId");
        commanderId = commanderId == null ? Optional.empty() : commanderId;
        memberIds = List.copyOf(new LinkedHashSet<>(Objects.requireNonNull(memberIds, "memberIds")));
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(simulation, "simulation");
        snapshots = List.copyOf(Objects.requireNonNull(snapshots, "snapshots"));
    }

    public static ArmyGroupRecord create(
            UUID ownerId,
            UUID kingdomId,
            UUID commanderId,
            List<UUID> memberIds,
            ArmyFormation formation,
            ArmyLocation anchor,
            long gameTime
    ) {
        return new ArmyGroupRecord(
                UUID.randomUUID(), ownerId, kingdomId, Optional.of(commanderId), memberIds,
                ArmyGroupOrder.follow(formation),
                new ArmyGroupSimulation(ArmyGroupLifecycleState.LIVE, anchor, gameTime, 0L, 0L, ""),
                List.of());
    }

    public ArmyGroupState plannerState() {
        return new ArmyGroupState(id, ownerId, new LinkedHashSet<>(memberIds), order.toCommand(ownerId, id));
    }

    public boolean contains(UUID recruitId) {
        return commanderId.filter(recruitId::equals).isPresent() || memberIds.contains(recruitId);
    }

    public ArmyGroupRecord withOrder(ArmyGroupOrder order) {
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, memberIds, order,
                new ArmyGroupSimulation(
                        simulation.lifecycleState(), simulation.anchor(), simulation.lastSimulationGameTime(),
                        simulation.revision() + 1, simulation.snapshotGeneration(), ""),
                snapshots);
    }

    public ArmyGroupRecord withCommander(UUID commanderId) {
        List<UUID> remainingMembers = memberIds.stream().filter(id -> !id.equals(commanderId)).toList();
        return new ArmyGroupRecord(id, ownerId, kingdomId, Optional.of(commanderId), remainingMembers, order,
                new ArmyGroupSimulation(
                        ArmyGroupLifecycleState.LIVE, simulation.anchor(), simulation.lastSimulationGameTime(),
                        simulation.revision() + 1, simulation.snapshotGeneration(), ""), snapshots);
    }

    public ArmyGroupRecord orphan(ArmyLocation anchor) {
        ArmyGroupOrder hold = new ArmyGroupOrder(
                ArmyCommandType.HOLD_POSITION, Optional.of(anchor), Optional.empty(), order.formation(), order.spacing());
        return new ArmyGroupRecord(id, ownerId, kingdomId, Optional.empty(), memberIds, hold,
                new ArmyGroupSimulation(
                        ArmyGroupLifecycleState.ORPHANED, anchor, simulation.lastSimulationGameTime(),
                        simulation.revision() + 1, simulation.snapshotGeneration(), "commander_missing"), snapshots);
    }

    public ArmyGroupRecord withMembers(List<UUID> members) {
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, members, order,
                new ArmyGroupSimulation(
                        simulation.lifecycleState(), simulation.anchor(), simulation.lastSimulationGameTime(),
                        simulation.revision() + 1, simulation.snapshotGeneration(), simulation.blockedReason()), snapshots);
    }

    public ArmyGroupRecord withSimulation(ArmyGroupSimulation simulation, List<ArmyMemberSnapshot> snapshots) {
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, memberIds, order, simulation, snapshots);
    }

    public ArmyGroupRecord withSnapshot(ArmyMemberSnapshot snapshot) {
        java.util.ArrayList<ArmyMemberSnapshot> updated = new java.util.ArrayList<>(snapshots);
        for (int i = 0; i < updated.size(); i++) {
            if (updated.get(i).recruitId().equals(snapshot.recruitId())) {
                if (updated.get(i).equals(snapshot)) {
                    return this;
                }
                updated.set(i, snapshot);
                return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, memberIds, order,
                        new ArmyGroupSimulation(
                                simulation.lifecycleState(), simulation.anchor(), simulation.lastSimulationGameTime(),
                                simulation.revision() + 1, simulation.snapshotGeneration(), simulation.blockedReason()),
                        updated);
            }
        }
        updated.add(snapshot);
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, memberIds, order,
                new ArmyGroupSimulation(
                        simulation.lifecycleState(), simulation.anchor(), simulation.lastSimulationGameTime(),
                        simulation.revision() + 1, simulation.snapshotGeneration(), simulation.blockedReason()),
                updated);
    }
}
