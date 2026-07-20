package galacticwars.clonewars.army;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Pure, deterministic formation geometry. */
public final class FormationPlanner {
    private static final double TWO_PI = Math.PI * 2.0D;

    private FormationPlanner() {
    }

    public static List<FormationSlot> planSlots(ArmyFormation formation, int unitCount, int spacing) {
        validateInputs(formation, unitCount, spacing);
        if (unitCount == 0) {
            return List.of();
        }

        return switch (formation) {
            case LINE -> planLine(unitCount, spacing);
            case COLUMN -> planColumn(unitCount, spacing);
            case WEDGE -> planWedge(unitCount, spacing);
            case SQUARE -> planSquare(unitCount, spacing);
            case CIRCLE -> planCircle(unitCount, spacing);
            case HOLLOW_CIRCLE -> planHollowCircle(unitCount, spacing);
            case HOLLOW_SQUARE -> planHollowSquare(unitCount, spacing);
            case MOVEMENT -> planMovement(unitCount, spacing);
        };
    }

    /**
     * Legacy world-axis layout. This overload is intentionally equivalent to a
     * Minecraft yaw of zero so existing groups do not move when loaded.
     */
    public static List<ArmyPosition> planPositions(
            ArmyPosition anchor,
            ArmyFormation formation,
            int unitCount,
            int spacing
    ) {
        return planPositions(anchor, formation, unitCount, spacing, 0.0F);
    }

    /**
     * Rotates slots by a Minecraft yaw: zero faces positive Z and ninety faces
     * negative X. The anchor height is retained because terrain resolution is a
     * runtime-world concern, not a pure formation concern.
     */
    public static List<ArmyPosition> planPositions(
            ArmyPosition anchor,
            ArmyFormation formation,
            int unitCount,
            int spacing,
            float yawDegrees
    ) {
        Objects.requireNonNull(anchor, "anchor");
        List<ArmyPosition> positions = new ArrayList<>();
        for (FormationSlot slot : planSlots(formation, unitCount, spacing)) {
            positions.add(positionFor(anchor, slot, yawDegrees));
        }
        return List.copyOf(positions);
    }

    public static ArmyPosition positionFor(ArmyPosition anchor, FormationSlot slot, float yawDegrees) {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(slot, "slot");
        if (!Float.isFinite(yawDegrees)) {
            throw new IllegalArgumentException("yawDegrees must be finite");
        }

        double radians = Math.toRadians(yawDegrees);
        double sideX = Math.cos(radians);
        double sideZ = Math.sin(radians);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        int x = anchor.x() + (int) Math.round((slot.sideOffset() * sideX) + (slot.forwardOffset() * forwardX));
        int z = anchor.z() + (int) Math.round((slot.sideOffset() * sideZ) + (slot.forwardOffset() * forwardZ));
        return new ArmyPosition(x, anchor.y(), z);
    }

    private static List<FormationSlot> planLine(int unitCount, int spacing) {
        ArrayList<FormationSlot> slots = new ArrayList<>(unitCount);
        int firstSideOffset = -((unitCount - 1) * spacing) / 2;
        for (int index = 0; index < unitCount; index++) {
            slots.add(new FormationSlot(index, firstSideOffset + (index * spacing), 0));
        }
        return List.copyOf(slots);
    }

    private static List<FormationSlot> planColumn(int unitCount, int spacing) {
        ArrayList<FormationSlot> slots = new ArrayList<>(unitCount);
        for (int index = 0; index < unitCount; index++) {
            slots.add(new FormationSlot(index, 0, index * spacing));
        }
        return List.copyOf(slots);
    }

    private static List<FormationSlot> planWedge(int unitCount, int spacing) {
        ArrayList<FormationSlot> slots = new ArrayList<>(unitCount);
        slots.add(new FormationSlot(0, 0, 0));
        for (int index = 1; index < unitCount; index++) {
            int rank = ((index + 1) / 2);
            int side = (index % 2 == 1 ? -rank : rank) * spacing;
            int forward = rank * spacing;
            slots.add(new FormationSlot(index, side, forward));
        }
        return List.copyOf(slots);
    }

    private static List<FormationSlot> planSquare(int unitCount, int spacing) {
        ArrayList<FormationSlot> slots = new ArrayList<>(unitCount);
        int width = (int) Math.ceil(Math.sqrt(unitCount));
        int firstSideOffset = -((width - 1) * spacing) / 2;
        for (int index = 0; index < unitCount; index++) {
            int column = index % width;
            int row = index / width;
            slots.add(new FormationSlot(index, firstSideOffset + (column * spacing), row * spacing));
        }
        return List.copyOf(slots);
    }

    /** Filled concentric rings, beginning with a center slot. */
    private static List<FormationSlot> planCircle(int unitCount, int spacing) {
        ArrayList<Offset> offsets = new ArrayList<>(unitCount);
        Set<Offset> occupied = new HashSet<>();
        addUnique(offsets, occupied, 0, 0, unitCount);

        for (int ring = 1; offsets.size() < unitCount; ring++) {
            int radius = ring * spacing;
            int samples = Math.max(8 * ring, (int) Math.ceil(TWO_PI * ring));
            appendCircularRing(offsets, occupied, unitCount, radius, samples, 0.0D);
        }
        return toSlots(offsets);
    }

    /** One circular perimeter sized to keep adjacent integer slots distinct. */
    private static List<FormationSlot> planHollowCircle(int unitCount, int spacing) {
        if (unitCount == 1) {
            return List.of(new FormationSlot(0, 0, 0));
        }

        int radius = Math.max(spacing, (int) Math.ceil((unitCount * (double) spacing) / TWO_PI));
        while (true) {
            ArrayList<Offset> offsets = new ArrayList<>(unitCount);
            Set<Offset> occupied = new HashSet<>();
            appendCircularRing(offsets, occupied, unitCount, radius, unitCount, 0.0D);
            if (offsets.size() == unitCount) {
                return toSlots(offsets);
            }
            radius++;
        }
    }

    /** A square perimeter sampled evenly around its four sides. */
    private static List<FormationSlot> planHollowSquare(int unitCount, int spacing) {
        if (unitCount == 1) {
            return List.of(new FormationSlot(0, 0, 0));
        }

        int radius = Math.max(spacing, ((unitCount + 7) / 8) * spacing);
        while (true) {
            ArrayList<Offset> offsets = new ArrayList<>(unitCount);
            Set<Offset> occupied = new HashSet<>();
            for (int index = 0; index < unitCount; index++) {
                double angle = ((TWO_PI * index) / unitCount) + (Math.PI / 4.0D);
                double cosine = Math.cos(angle);
                double sine = Math.sin(angle);
                double scale = radius / Math.max(Math.abs(cosine), Math.abs(sine));
                addUnique(offsets, occupied, (int) Math.round(cosine * scale),
                        (int) Math.round(sine * scale), unitCount);
            }
            if (offsets.size() == unitCount) {
                return toSlots(offsets);
            }
            radius++;
        }
    }

    /** A compact three-wide travelling formation, with later rows behind the anchor. */
    private static List<FormationSlot> planMovement(int unitCount, int spacing) {
        ArrayList<FormationSlot> slots = new ArrayList<>(unitCount);
        int maxInRow = Math.min(3, unitCount);
        for (int index = 0; index < unitCount; index++) {
            int row = index / maxInRow;
            int rowStart = row * maxInRow;
            int membersInRow = Math.min(maxInRow, unitCount - rowStart);
            int positionInRow = index % maxInRow;
            int firstSideOffset = -((membersInRow - 1) * spacing) / 2;
            slots.add(new FormationSlot(index, firstSideOffset + (positionInRow * spacing), -row * spacing));
        }
        return List.copyOf(slots);
    }

    private static void appendCircularRing(
            List<Offset> offsets,
            Set<Offset> occupied,
            int unitCount,
            int radius,
            int samples,
            double phase
    ) {
        for (int index = 0; index < samples && offsets.size() < unitCount; index++) {
            double angle = ((TWO_PI * index) / samples) + phase;
            addUnique(offsets, occupied, (int) Math.round(radius * Math.cos(angle)),
                    (int) Math.round(radius * Math.sin(angle)), unitCount);
        }
    }

    private static void addUnique(
            List<Offset> offsets,
            Set<Offset> occupied,
            int side,
            int forward,
            int unitCount
    ) {
        Offset offset = new Offset(side, forward);
        if (offsets.size() < unitCount && occupied.add(offset)) {
            offsets.add(offset);
        }
    }

    private static List<FormationSlot> toSlots(List<Offset> offsets) {
        ArrayList<FormationSlot> slots = new ArrayList<>(offsets.size());
        for (int index = 0; index < offsets.size(); index++) {
            Offset offset = offsets.get(index);
            slots.add(new FormationSlot(index, offset.side(), offset.forward()));
        }
        return List.copyOf(slots);
    }

    private static void validateInputs(ArmyFormation formation, int unitCount, int spacing) {
        Objects.requireNonNull(formation, "formation");
        if (unitCount < 0) {
            throw new IllegalArgumentException("unitCount cannot be negative");
        }
        if (spacing < 1) {
            throw new IllegalArgumentException("spacing must be at least 1");
        }
    }

    private record Offset(int side, int forward) {
    }
}
