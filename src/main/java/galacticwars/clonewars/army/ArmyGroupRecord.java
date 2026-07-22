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
        int supplyUnits,
        Optional<List<ArmyFormationSlotAssignment>> formationSlotAssignments,
        Optional<ArmyPatrolPlan> patrolPlan,
        Optional<ArmyGroupTactics> tactics
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
        if (!patrolRoute.isEmpty()) {
            String patrolDimension = patrolRoute.getFirst().dimensionId();
            if (patrolRoute.stream().anyMatch(waypoint -> !waypoint.dimensionId().equals(patrolDimension))) {
                throw new IllegalArgumentException("patrolRoute waypoints must share one dimension");
            }
        }
        defendedClaimId = defendedClaimId == null ? Optional.empty() : defendedClaimId;
        if (supplyUnits < 0) {
            throw new IllegalArgumentException("supplyUnits cannot be negative");
        }
        formationSlotAssignments = normalizeFormationSlots(memberIds, formationSlotAssignments);
        patrolPlan = normalizePatrolPlan(patrolPlan);
        tactics = tactics == null ? Optional.empty() : tactics;
        if (patrolPlan.isPresent()) {
            List<ArmyLocation> plannedLocations = patrolPlan.orElseThrow().locations();
            if (patrolRoute.isEmpty()) {
                patrolRoute = plannedLocations;
            } else if (!patrolRoute.equals(plannedLocations)) {
                throw new IllegalArgumentException("patrolPlan waypoints must match patrolRoute");
            }
        }
    }

    /**
     * Compatibility constructor retained for existing codecs and save data.
     * It assigns deterministic UUID slots and derives the legacy loop patrol
     * plan (zero waits, active, default speed/enemy policy) when a route exists.
     */
    public ArmyGroupRecord(
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
        this(id, ownerId, kingdomId, commanderId, memberIds, order, simulation, snapshots,
                name, rallyPoint, patrolRoute, defendedClaimId, supplyUnits,
                Optional.empty(), Optional.empty(), Optional.empty());
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
                new ArmyGroupSimulation(
                        ArmyGroupLifecycleState.LIVE, anchor, gameTime, 0L, 0L, "",
                        ArmyMarchState.halted(formation)),
                List.of(), "Squad " + id.toString().substring(0, 4), Optional.of(anchor), List.of(),
                Optional.empty(), 0, Optional.of(ArmyFormationSlotAssignment.assignDeterministically(memberIds)),
                Optional.empty(), Optional.empty());
    }

    public ArmyGroupState plannerState() {
        return new ArmyGroupState(id, ownerId, new LinkedHashSet<>(memberIds), order.toCommand(ownerId, id));
    }

    /**
     * Includes the commander when validating whether a persisted order can be
     * issued. The formation planner intentionally excludes the commander, but
     * a newly promoted commander is still a commandable one-unit squad.
     */
    public ArmyGroupState commandValidationState() {
        LinkedHashSet<UUID> participants = new LinkedHashSet<>(memberIds);
        commanderId.ifPresent(participants::add);
        return new ArmyGroupState(id, ownerId, participants, order.toCommand(ownerId, id));
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
                nextSnapshots, name, rallyPoint, patrolRoute, defendedClaimId, supplyUnits,
                formationSlotAssignments, patrolPlan, tactics);
    }

    public ArmyGroupRecord withOrder(ArmyGroupOrder order) {
        return copy(commanderId, memberIds, order, simulation.resetMarch(order.formation()), snapshots);
    }

    public ArmyGroupRecord withCommander(UUID commanderId) {
        List<UUID> remainingMembers = memberIds.stream().filter(id -> !id.equals(commanderId)).toList();
        return copy(Optional.of(commanderId), remainingMembers, order,
                new ArmyGroupSimulation(
                        ArmyGroupLifecycleState.LIVE, simulation.anchor(), simulation.lastSimulationGameTime(),
                        simulation.revision() + 1, simulation.snapshotGeneration(), "", simulation.marchState()), snapshots);
    }

    public ArmyGroupRecord orphan(ArmyLocation anchor) {
        ArmyGroupOrder hold = new ArmyGroupOrder(
                ArmyCommandType.HOLD_POSITION, Optional.of(anchor), Optional.empty(), order.formation(), order.spacing());
        return copy(Optional.empty(), memberIds, hold,
                new ArmyGroupSimulation(
                        ArmyGroupLifecycleState.ORPHANED, anchor, simulation.lastSimulationGameTime(),
                        simulation.revision() + 1, simulation.snapshotGeneration(), "commander_missing",
                        ArmyMarchState.halted(order.formation())), snapshots);
    }

    public ArmyGroupRecord withMembers(List<UUID> members) {
        List<UUID> normalizedMembers = List.copyOf(new LinkedHashSet<>(members));
        LinkedHashSet<UUID> retainedIds = new LinkedHashSet<>(normalizedMembers);
        commanderId.ifPresent(retainedIds::add);
        List<ArmyMemberSnapshot> retainedSnapshots = snapshots.stream()
                .filter(snapshot -> retainedIds.contains(snapshot.recruitId()))
                .toList();
        boolean membershipChanged = !new LinkedHashSet<>(memberIds).equals(new LinkedHashSet<>(normalizedMembers));
        Optional<List<ArmyFormationSlotAssignment>> nextAssignments = !membershipChanged
                ? formationSlotAssignments
                : Optional.of(ArmyFormationSlotAssignment.reconcile(
                        normalizedMembers, effectiveFormationSlotAssignments()));
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, normalizedMembers, order,
                new ArmyGroupSimulation(
                        simulation.lifecycleState(), simulation.anchor(), simulation.lastSimulationGameTime(),
                        simulation.revision() + 1, simulation.snapshotGeneration(), simulation.blockedReason(),
                        simulation.marchState()),
                retainedSnapshots, name, rallyPoint, patrolRoute, defendedClaimId, supplyUnits,
                nextAssignments, patrolPlan, tactics);
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
                        simulation.touch(simulation.blockedReason()),
                        updated);
            }
        }
        updated.add(snapshot);
        return copy(commanderId, memberIds, order,
                simulation.touch(simulation.blockedReason()),
                updated);
    }

    public ArmyGroupRecord withName(String name) {
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, memberIds, order, simulation, snapshots,
                name, rallyPoint, patrolRoute, defendedClaimId, supplyUnits,
                formationSlotAssignments, patrolPlan, tactics);
    }

    public ArmyGroupRecord withRallyPoint(ArmyLocation rallyPoint) {
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, memberIds, order, simulation, snapshots,
                name, Optional.of(rallyPoint), patrolRoute, defendedClaimId, supplyUnits,
                formationSlotAssignments, patrolPlan, tactics);
    }

    public ArmyGroupRecord withPatrolRoute(List<ArmyLocation> patrolRoute) {
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, memberIds, order, simulation, snapshots,
                name, rallyPoint, patrolRoute, defendedClaimId, supplyUnits, formationSlotAssignments,
                Optional.empty(), tactics);
    }

    public ArmyGroupRecord withPatrolPlan(ArmyPatrolPlan patrolPlan) {
        return withPatrolProgressAndOrder(patrolPlan, order);
    }

    /**
     * Updates patrol configuration and its active order in one revision. This
     * keeps a field-command batch compare-and-swap atomic instead of creating
     * an intermediate route state that another tick can observe.
     */
    public ArmyGroupRecord withPatrolPlanAndOrder(ArmyPatrolPlan patrolPlan, ArmyGroupOrder nextOrder) {
        return updatePatrolPlanAndOrder(patrolPlan, nextOrder, true);
    }

    /** Advances or edits an existing patrol without interrupting its current formation march. */
    public ArmyGroupRecord withPatrolProgressAndOrder(ArmyPatrolPlan patrolPlan, ArmyGroupOrder nextOrder) {
        return updatePatrolPlanAndOrder(patrolPlan, nextOrder, false);
    }

    private ArmyGroupRecord updatePatrolPlanAndOrder(
            ArmyPatrolPlan patrolPlan,
            ArmyGroupOrder nextOrder,
            boolean resetMarch
    ) {
        Objects.requireNonNull(patrolPlan, "patrolPlan");
        Objects.requireNonNull(nextOrder, "nextOrder");
        if (this.patrolPlan.filter(patrolPlan::equals).isPresent() && order.equals(nextOrder)) {
            return this;
        }
        ArmyGroupSimulation nextSimulation = resetMarch
                ? simulation.resetMarch(nextOrder.formation())
                : simulation.touch(simulation.blockedReason());
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, memberIds, nextOrder, nextSimulation, snapshots,
                name, rallyPoint, patrolPlan.locations(), defendedClaimId, supplyUnits, formationSlotAssignments,
                Optional.of(patrolPlan), tactics);
    }

    public ArmyGroupRecord pausePatrol() {
        return effectivePatrolPlan().map(plan -> withPatrolPlan(plan.pause())).orElse(this);
    }

    public ArmyGroupRecord resumePatrol() {
        return effectivePatrolPlan().map(plan -> withPatrolPlan(plan.resume())).orElse(this);
    }

    public ArmyGroupRecord stopPatrol() {
        return effectivePatrolPlan().map(plan -> withPatrolPlan(plan.stop())).orElse(this);
    }

    public ArmyGroupRecord withFormationSlotAssignments(List<ArmyFormationSlotAssignment> assignments) {
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, memberIds, order, simulation, snapshots,
                name, rallyPoint, patrolRoute, defendedClaimId, supplyUnits, Optional.of(List.copyOf(assignments)),
                patrolPlan, tactics);
    }

    public ArmyGroupRecord withTactics(ArmyGroupTactics tactics) {
        Objects.requireNonNull(tactics, "tactics");
        if (this.tactics.filter(tactics::equals).isPresent()) {
            return this;
        }
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, memberIds, order,
                simulation.touch(simulation.blockedReason()), snapshots,
                name, rallyPoint, patrolRoute, defendedClaimId, supplyUnits, formationSlotAssignments,
                patrolPlan, Optional.of(tactics));
    }

    /** A runtime-only legacy derivation; it does not materialize a new saved field. */
    public List<ArmyFormationSlotAssignment> effectiveFormationSlotAssignments() {
        return formationSlotAssignments.orElseGet(
                () -> ArmyFormationSlotAssignment.assignDeterministically(memberIds));
    }

    /** A runtime-only legacy derivation; it does not materialize a new saved field. */
    public Optional<ArmyPatrolPlan> effectivePatrolPlan() {
        return patrolPlan.or(() -> ArmyPatrolPlan.fromLegacyRoute(patrolRoute));
    }

    /** Legacy groups retain the existing balanced, free-fire doctrine. */
    public ArmyGroupTactics effectiveTactics() {
        return tactics.orElse(ArmyGroupTactics.DEFAULT);
    }

    public ArmyGroupRecord defendingClaim(UUID claimId) {
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, memberIds, order, simulation, snapshots,
                name, rallyPoint, patrolRoute, Optional.of(claimId), supplyUnits,
                formationSlotAssignments, patrolPlan, tactics);
    }

    public ArmyGroupRecord withSupplyUnits(int supplyUnits) {
        return new ArmyGroupRecord(id, ownerId, kingdomId, commanderId, memberIds, order, simulation, snapshots,
                name, rallyPoint, patrolRoute, defendedClaimId, supplyUnits,
                formationSlotAssignments, patrolPlan, tactics);
    }

    private static Optional<List<ArmyFormationSlotAssignment>> normalizeFormationSlots(
            List<UUID> memberIds,
            Optional<List<ArmyFormationSlotAssignment>> assignments
    ) {
        Optional<List<ArmyFormationSlotAssignment>> normalized = assignments == null ? Optional.empty() : assignments;
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ArmyFormationSlotAssignment.reconcile(memberIds, normalized.orElseThrow()));
    }

    private static Optional<ArmyPatrolPlan> normalizePatrolPlan(Optional<ArmyPatrolPlan> patrolPlan) {
        return patrolPlan == null ? Optional.empty() : patrolPlan;
    }
}
