package galacticwars.clonewars.army;

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
        List<ArmyMemberSnapshot> snapshots,
        String name,
        Optional<ArmyLocation> rallyPoint,
        List<ArmyLocation> patrolRoute,
        Optional<UUID> defendedClaimId,
        int supplyUnits
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
        name = Objects.requireNonNull(name, "name").trim();
        if (name.isEmpty() || name.length() > 48) {
            throw new IllegalArgumentException("name must contain 1-48 characters");
        }
        rallyPoint = rallyPoint == null ? Optional.empty() : rallyPoint;
        patrolRoute = List.copyOf(Objects.requireNonNull(patrolRoute, "patrolRoute"));
        if (patrolRoute.size() == 1 || patrolRoute.size() > 32) {
            throw new IllegalArgumentException("patrolRoute must be empty or contain 2-32 waypoints");
        }
        defendedClaimId = defendedClaimId == null ? Optional.empty() : defendedClaimId;
        if (supplyUnits < 0) {
            throw new IllegalArgumentException("supplyUnits cannot be negative");
        }
    }

    public ArmyGroupRecord(
            UUID id,
            UUID ownerId,
            UUID kingdomId,
            Optional<UUID> commanderId,
            List<UUID> memberIds,
            ArmyGroupOrder order,
            ArmyGroupSimulation simulation,
            List<ArmyMemberSnapshot> snapshots
    ) {
        this(id, ownerId, kingdomId, commanderId, memberIds, order, simulation, snapshots,
                "Squad " + id.toString().substring(0, 4), Optional.empty(), List.of(), Optional.empty(), 0);
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
        UUID id = UUID.randomUUID();
        return new ArmyGroupRecord(
                id, ownerId, kingdomId, Optional.of(commanderId), memberIds,
                ArmyGroupOrder.follow(formation),
                new ArmyGroupSimulation(ArmyGroupLifecycleState.LIVE, anchor, gameTime, 0L, 0L, ""),
                List.of(), "Squad " + id.toString().substring(0, 4), Optional.of(anchor), List.of(),
                Optional.empty(), 0);
    }

    public ArmyGroupState plannerState() {
        return new ArmyGroupState(id, ownerId, new LinkedHashSet<>(memberIds), order.toCommand(ownerId, id));
    }

    public boolean contains(UUID recruitId) {
        return commanderId.filter(recruitId::equals).isPresent() || memberIds.contains(recruitId);
    }

    private ArmyGroupRecord copy(
            Optional<UUID> commander,
            List<UUID> members,
            ArmyGroupOrder nextOrder,
            ArmyGroupSimulation nextSimulation,
            List<ArmyMemberSnapshot> nextSnapshots
    ) {
        return new ArmyGroupRecord(id, ownerId, kingdomId, commander, members, nextOrder, nextSimulation,
                nextSnapshots, name, rallyPoint, patrolRoute, defendedClaimId, supplyUnits);
    }

    public ArmyGroupRecord withOrder(ArmyGroupOrder order) {
        return copy(commanderId, memberIds, order,
                new ArmyGroupSimulation(
                        simulation.lifecycleState(), simulation.anchor(), simulation.lastSimulationGameTime(),
                        simulation.revision() + 1, simulation.snapshotGeneration(), ""), snapshots);
    }

    public ArmyGroupRecord withCommander(UUID commanderId) {
        List<UUID> remainingMembers = memberIds.stream().filter(id -> !id.equals(commanderId)).toList();
        return copy(Optional.of(commanderId), remainingMembers, order,
                new ArmyGroupSimulation(
                        ArmyGroupLifecycleState.LIVE, simulation.anchor(), simulation.lastSimulationGameTime(),
                        simulation.revision() + 1, simulation.snapshotGeneration(), ""), snapshots);
    }

    public ArmyGroupRecord orphan(ArmyLocation anchor) {
        ArmyGroupOrder hold = new ArmyGroupOrder(
                ArmyCommandType.HOLD_POSITION, Optional.of(anchor), Optional.empty(), order.formation(), order.spacing());
        return copy(Optional.empty(), memberIds, hold,
                new ArmyGroupSimulation(
                        ArmyGroupLifecycleState.ORPHANED, anchor, simulation.lastSimulationGameTime(),
                        simulation.revision() + 1, simulation.snapshotGeneration(), "commander_missing"), snapshots);
    }

    public ArmyGroupRecord withMembers(List<UUID> members) {
        return copy(commanderId, members, order,
                new ArmyGroupSimulation(
                        simulation.lifecycleState(), simulation.anchor(), simulation.lastSimulationGameTime(),
                        simulation.revision() + 1, simulation.snapshotGeneration(), simulation.blockedReason()), snapshots);
    }

    public ArmyGroupRecord withSimulation(ArmyGroupSimulation simulation, List<ArmyMemberSnapshot> snapshots) {
        return copy(commanderId, memberIds, order, simulation, snapshots);
    }

    public ArmyGroupRecord withSnapshot(ArmyMemberSnapshot snapshot) {
        java.util.ArrayList<ArmyMemberSnapshot> updated = new java.util.ArrayList<>(snapshots);
        for (int i = 0; i < updated.size(); i++) {
            if (updated.get(i).recruitId().equals(snapshot.recruitId())) {
                if (updated.get(i).equals(snapshot)) {
                    return this;
                }
                updated.set(i, snapshot);
                return copy(commanderId, memberIds, order,
                        new ArmyGroupSimulation(
                                simulation.lifecycleState(), simulation.anchor(), simulation.lastSimulationGameTime(),
                                simulation.revision() + 1, simulation.snapshotGeneration(), simulation.blockedReason()),
                        updated);
            }
        }
        updated.add(snapshot);
        return copy(commanderId, memberIds, order,
                new ArmyGroupSimulation(
                        simulation.lifecycleState(), simulation.anchor(), simulation.lastSimulationGameTime(),
                        simulation.revision() + 1, simulation.snapshotGeneration(), simulation.blockedReason()),
                updated);
    }

    public ArmyGroupRecord withName(String name) {
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, memberIds, order, simulation, snapshots,
                name, rallyPoint, patrolRoute, defendedClaimId, supplyUnits);
    }

    public ArmyGroupRecord withRallyPoint(ArmyLocation rallyPoint) {
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, memberIds, order, simulation, snapshots,
                name, Optional.of(rallyPoint), patrolRoute, defendedClaimId, supplyUnits);
    }

    public ArmyGroupRecord withPatrolRoute(List<ArmyLocation> patrolRoute) {
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, memberIds, order, simulation, snapshots,
                name, rallyPoint, patrolRoute, defendedClaimId, supplyUnits);
    }

    public ArmyGroupRecord defendingClaim(UUID claimId) {
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, memberIds, order, simulation, snapshots,
                name, rallyPoint, patrolRoute, Optional.of(claimId), supplyUnits);
    }

    public ArmyGroupRecord withSupplyUnits(int supplyUnits) {
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, memberIds, order, simulation, snapshots,
                name, rallyPoint, patrolRoute, defendedClaimId, supplyUnits);
    }
}
