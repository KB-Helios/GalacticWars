package galacticwars.clonewars.army;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FormationPlanner {
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
        };
    }

    public static List<ArmyPosition> planPositions(ArmyPosition anchor, ArmyFormation formation, int unitCount, int spacing) {
        Objects.requireNonNull(anchor, "anchor");
        List<ArmyPosition> positions = new ArrayList<>();
        for (FormationSlot slot : planSlots(formation, unitCount, spacing)) {
            positions.add(new ArmyPosition(anchor.x() + slot.sideOffset(), anchor.y(), anchor.z() + slot.forwardOffset()));
        }
        return List.copyOf(positions);
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

    private static void validateInputs(ArmyFormation formation, int unitCount, int spacing) {
        Objects.requireNonNull(formation, "formation");
        if (unitCount < 0) {
            throw new IllegalArgumentException("unitCount cannot be negative");
        }
        if (spacing < 1) {
            throw new IllegalArgumentException("spacing must be at least 1");
        }
    }
}
