package galacticwars.clonewars.army;

import java.util.List;
import java.util.Objects;

/** Pure adaptive-march planner; world collision sampling remains in the runtime adapter. */
public final class ArmyMarchCoordinator {
    private static final int FORMATION_COHESION = 70;
    private static final int HALT_COHESION = 88;

    private ArmyMarchCoordinator() {
    }

    public static Decision advance(
            ArmyGroupRecord group,
            ArmyLocation liveAnchor,
            ArmyLocation destination,
            List<ArmyPosition> memberPositions,
            boolean formationFootprintClear,
            boolean engaged,
            double movementSpeed,
            double readinessMultiplier,
            long gameTime
    ) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(liveAnchor, "liveAnchor");
        Objects.requireNonNull(destination, "destination");
        memberPositions = List.copyOf(Objects.requireNonNull(memberPositions, "memberPositions"));
        if (!liveAnchor.dimensionId().equals(destination.dimensionId())) {
            return new Decision(liveAnchor, group.simulation().marchState().transition(
                    ArmyMarchPhase.HALTED, group.order().formation(), 0,
                    group.simulation().marchState().yawDegrees(), gameTime));
        }
        if (!Double.isFinite(movementSpeed) || movementSpeed < 0.0D
                || !Double.isFinite(readinessMultiplier) || readinessMultiplier <= 0.0D) {
            throw new IllegalArgumentException("march movement inputs must be finite and positive");
        }

        ArmyMarchState previous = group.simulation().marchState();
        ArmyLocation current = previous.phase() == ArmyMarchPhase.HALTED
                || previous.phase() == ArmyMarchPhase.FORMING
                ? liveAnchor
                : group.simulation().anchor();
        int cohesion = cohesionPercent(current.blockPosition(), memberPositions,
                group.order().spacing(), group.memberIds().size());
        float yaw = ArmyLocation.yawToward(current, destination, previous.yawDegrees());

        if (engaged) {
            return decision(liveAnchor, previous, ArmyMarchPhase.ENGAGED,
                    group.order().formation(), cohesion, yaw, gameTime);
        }

        double distance = distance(current, destination);
        if (distance <= 1.5D) {
            ArmyMarchPhase phase = cohesion >= HALT_COHESION
                    ? ArmyMarchPhase.HALTED : ArmyMarchPhase.REFORMING;
            return decision(destination, previous, phase,
                    group.order().formation(), cohesion, yaw, gameTime);
        }

        boolean compressed = !formationFootprintClear || cohesion < 45
                || previous.phase() == ArmyMarchPhase.COMPRESSED && cohesion < FORMATION_COHESION;
        ArmyFormation activeFormation = compressed ? ArmyFormation.COLUMN : group.order().formation();
        if (previous.phase() == ArmyMarchPhase.COMPRESSED && !compressed
                && cohesion < HALT_COHESION) {
            return decision(current, previous, ArmyMarchPhase.REFORMING,
                    group.order().formation(), cohesion, yaw, gameTime);
        }
        if (!compressed && previous.phase() == ArmyMarchPhase.MARCHING
                && cohesion < FORMATION_COHESION) {
            return decision(current, previous, ArmyMarchPhase.REFORMING,
                    group.order().formation(), cohesion, yaw, gameTime);
        }
        if ((previous.phase() == ArmyMarchPhase.FORMING || previous.phase() == ArmyMarchPhase.REFORMING)
                && cohesion < FORMATION_COHESION) {
            return decision(current, previous, previous.phase(), activeFormation, cohesion, yaw, gameTime);
        }

        double maximumStep = VirtualArmyMovementPlanner.blocksPerSecond(movementSpeed)
                * Math.max(0.5D, Math.min(1.15D, readinessMultiplier));
        double ratio = Math.min(1.0D, maximumStep / distance);
        ArmyLocation next = new ArmyLocation(
                current.dimensionId(),
                current.x() + (destination.x() - current.x()) * ratio,
                current.y() + (destination.y() - current.y()) * ratio,
                current.z() + (destination.z() - current.z()) * ratio);
        return decision(next, previous,
                compressed ? ArmyMarchPhase.COMPRESSED : ArmyMarchPhase.MARCHING,
                activeFormation, cohesion, yaw, gameTime);
    }

    public static int cohesionPercent(
            ArmyPosition anchor,
            List<ArmyPosition> members,
            int spacing,
            int expectedMembers
    ) {
        Objects.requireNonNull(anchor, "anchor");
        members = List.copyOf(Objects.requireNonNull(members, "members"));
        if (expectedMembers <= 0) {
            return 100;
        }
        int radius = Math.max(6, spacing * Math.max(2, (int)Math.ceil(Math.sqrt(expectedMembers))) + 3);
        long radiusSquared = (long)radius * radius;
        long close = members.stream().filter(position -> distanceSquared(anchor, position) <= radiusSquared).count();
        return (int)Math.min(100L, close * 100L / expectedMembers);
    }

    private static Decision decision(
            ArmyLocation anchor,
            ArmyMarchState previous,
            ArmyMarchPhase phase,
            ArmyFormation formation,
            int cohesion,
            float yaw,
            long gameTime
    ) {
        return new Decision(anchor, previous.transition(phase, formation, cohesion, yaw, gameTime));
    }

    private static double distance(ArmyLocation first, ArmyLocation second) {
        double x = first.x() - second.x();
        double y = first.y() - second.y();
        double z = first.z() - second.z();
        return Math.sqrt(x * x + y * y + z * z);
    }

    private static long distanceSquared(ArmyPosition first, ArmyPosition second) {
        long x = (long)first.x() - second.x();
        long y = (long)first.y() - second.y();
        long z = (long)first.z() - second.z();
        return x * x + y * y + z * z;
    }

    public record Decision(ArmyLocation anchor, ArmyMarchState marchState) {
        public Decision {
            Objects.requireNonNull(anchor, "anchor");
            Objects.requireNonNull(marchState, "marchState");
        }
    }
}
