package galacticwars.clonewars.army;

import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Prepares nearby follow/protect squads for failure-atomic cross-dimension travel. */
public final class ArmyTravelService {
    private static final double TRANSFER_RADIUS_SQUARED = 128.0D * 128.0D;

    private ArmyTravelService() {
    }

    public static TravelPlan prepare(
            KingdomSavedData data,
            ServerPlayer owner,
            ServerLevel destination,
            BlockPos arrival
    ) {
        ArmyGroupRecord group = data.armyGroupForOwner(owner.getUUID()).orElse(null);
        if (group == null || !eligible(owner, group)) {
            return TravelPlan.noop(data);
        }

        Set<UUID> expected = expectedMembers(group);
        long generation = group.simulation().snapshotGeneration() + 1L;
        LinkedHashMap<UUID, ArmyMemberSnapshot> snapshots = new LinkedHashMap<>();
        ArrayList<GalacticRecruitEntity> liveMembers = new ArrayList<>();
        if (group.simulation().lifecycleState() == ArmyGroupLifecycleState.LIVE) {
            for (UUID recruitId : expected) {
                Entity entity = owner.level().getEntity(recruitId);
                if (!(entity instanceof GalacticRecruitEntity recruit)) {
                    return TravelPlan.rejected(data, "squad_snapshot_incomplete");
                }
                ArmyMemberSnapshot snapshot = recruit.createArmySnapshot(generation).orElse(null);
                if (snapshot == null) {
                    return TravelPlan.rejected(data, "squad_snapshot_incomplete");
                }
                snapshots.put(recruitId, snapshot);
                liveMembers.add(recruit);
            }
        } else {
            for (ArmyMemberSnapshot snapshot : group.snapshots()) {
                if (expected.contains(snapshot.recruitId())) {
                    snapshots.put(snapshot.recruitId(), withGeneration(snapshot, generation));
                }
            }
            if (!snapshots.keySet().containsAll(expected)) {
                return TravelPlan.rejected(data, "squad_snapshot_incomplete");
            }
        }

        ArmyLocation destinationAnchor = new ArmyLocation(
                destination.dimension().identifier().toString(),
                arrival.getX() + 0.5D,
                arrival.getY(),
                arrival.getZ() + 0.5D);
        ArmyGroupSimulation transferredSimulation = new ArmyGroupSimulation(
                ArmyGroupLifecycleState.VIRTUAL,
                destinationAnchor,
                destination.getGameTime(),
                group.simulation().revision() + 1L,
                generation,
                "planet_transfer");
        ArmyGroupRecord transferred = group.withSimulation(
                transferredSimulation, orderedSnapshots(group, snapshots));
        return TravelPlan.ready(data, group, transferred, liveMembers);
    }

    private static boolean eligible(ServerPlayer owner, ArmyGroupRecord group) {
        ArmyGroupLifecycleState lifecycle = group.simulation().lifecycleState();
        ArmyCommandType order = group.order().type();
        ArmyLocation anchor = group.simulation().anchor();
        return (lifecycle == ArmyGroupLifecycleState.LIVE || lifecycle == ArmyGroupLifecycleState.VIRTUAL)
                && (order == ArmyCommandType.FOLLOW_OWNER || order == ArmyCommandType.PROTECT_OWNER)
                && anchor.dimensionId().equals(owner.level().dimension().identifier().toString())
                && owner.distanceToSqr(anchor.x(), anchor.y(), anchor.z()) <= TRANSFER_RADIUS_SQUARED;
    }

    private static Set<UUID> expectedMembers(ArmyGroupRecord group) {
        LinkedHashSet<UUID> expected = new LinkedHashSet<>();
        group.commanderId().ifPresent(expected::add);
        expected.addAll(group.memberIds());
        return Set.copyOf(expected);
    }

    private static List<ArmyMemberSnapshot> orderedSnapshots(
            ArmyGroupRecord group,
            Map<UUID, ArmyMemberSnapshot> snapshots
    ) {
        ArrayList<ArmyMemberSnapshot> ordered = new ArrayList<>();
        group.commanderId().map(snapshots::get).ifPresent(ordered::add);
        group.memberIds().stream().map(snapshots::get).filter(java.util.Objects::nonNull).forEach(ordered::add);
        return List.copyOf(ordered);
    }

    private static ArmyMemberSnapshot withGeneration(ArmyMemberSnapshot snapshot, long generation) {
        return new ArmyMemberSnapshot(
                snapshot.recruitId(), snapshot.entityTypeId(), snapshot.unitId(), snapshot.ownerId(), snapshot.kingdomId(),
                snapshot.duty(), snapshot.health(), snapshot.morale(), snapshot.hunger(), snapshot.unpaidTicks(),
                generation, snapshot.equipment(), snapshot.customName());
    }

    public static final class TravelPlan {
        private final KingdomSavedData data;
        private final boolean accepted;
        private final String reason;
        private final ArmyGroupRecord original;
        private final ArmyGroupRecord transferred;
        private final List<GalacticRecruitEntity> liveMembers;
        private boolean reserved;

        private TravelPlan(
                KingdomSavedData data,
                boolean accepted,
                String reason,
                ArmyGroupRecord original,
                ArmyGroupRecord transferred,
                List<GalacticRecruitEntity> liveMembers
        ) {
            this.data = data;
            this.accepted = accepted;
            this.reason = reason;
            this.original = original;
            this.transferred = transferred;
            this.liveMembers = List.copyOf(liveMembers);
        }

        private static TravelPlan noop(KingdomSavedData data) {
            return new TravelPlan(data, true, "no_eligible_squad", null, null, List.of());
        }

        private static TravelPlan rejected(KingdomSavedData data, String reason) {
            return new TravelPlan(data, false, reason, null, null, List.of());
        }

        private static TravelPlan ready(
                KingdomSavedData data,
                ArmyGroupRecord original,
                ArmyGroupRecord transferred,
                List<GalacticRecruitEntity> liveMembers
        ) {
            return new TravelPlan(data, true, "ready", original, transferred, liveMembers);
        }

        public boolean accepted() {
            return accepted;
        }

        public String reason() {
            return reason;
        }

        public boolean transfersSquad() {
            return transferred != null;
        }

        public boolean reserve() {
            if (!accepted) {
                return false;
            }
            if (transferred == null) {
                return true;
            }
            reserved = data.replaceArmyGroup(transferred, original.simulation().revision());
            return reserved;
        }

        public void commit() {
            if (!reserved || transferred == null) {
                return;
            }
            liveMembers.forEach(GalacticRecruitEntity::discard);
            reserved = false;
        }

        public boolean rollback(long gameTime) {
            if (!reserved || transferred == null) {
                return true;
            }
            ArmyGroupSimulation previous = original.simulation();
            ArmyGroupSimulation restoredSimulation = new ArmyGroupSimulation(
                    previous.lifecycleState(), previous.anchor(), Math.max(previous.lastSimulationGameTime(), gameTime),
                    transferred.simulation().revision() + 1L, previous.snapshotGeneration(), previous.blockedReason());
            ArmyGroupRecord restored = original.withSimulation(restoredSimulation, original.snapshots());
            boolean restoredSuccessfully = data.replaceArmyGroup(restored, transferred.simulation().revision());
            if (restoredSuccessfully) {
                reserved = false;
            }
            return restoredSuccessfully;
        }
    }
}
