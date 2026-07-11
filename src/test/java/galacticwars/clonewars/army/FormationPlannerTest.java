package galacticwars.clonewars.army;

import java.util.List;

public final class FormationPlannerTest {
    private FormationPlannerTest() {
    }

    public static void main(String[] args) {
        plansCenteredLineOffsets();
        plansColumnOffsets();
        plansWedgeOffsets();
        plansSquareOffsets();
        mapsSlotsToAnchoredPositions();
        handlesEmptyAndInvalidInputs();

        System.out.println("FormationPlannerTest passed");
    }

    private static void plansCenteredLineOffsets() {
        List<FormationSlot> slots = FormationPlanner.planSlots(ArmyFormation.LINE, 5, 2);

        assertEquals(new FormationSlot(0, -4, 0), slots.get(0), "line slot 0");
        assertEquals(new FormationSlot(1, -2, 0), slots.get(1), "line slot 1");
        assertEquals(new FormationSlot(2, 0, 0), slots.get(2), "line slot 2");
        assertEquals(new FormationSlot(3, 2, 0), slots.get(3), "line slot 3");
        assertEquals(new FormationSlot(4, 4, 0), slots.get(4), "line slot 4");
    }

    private static void plansColumnOffsets() {
        List<FormationSlot> slots = FormationPlanner.planSlots(ArmyFormation.COLUMN, 4, 3);

        assertEquals(new FormationSlot(0, 0, 0), slots.get(0), "column slot 0");
        assertEquals(new FormationSlot(1, 0, 3), slots.get(1), "column slot 1");
        assertEquals(new FormationSlot(2, 0, 6), slots.get(2), "column slot 2");
        assertEquals(new FormationSlot(3, 0, 9), slots.get(3), "column slot 3");
    }

    private static void plansWedgeOffsets() {
        List<FormationSlot> slots = FormationPlanner.planSlots(ArmyFormation.WEDGE, 5, 2);

        assertEquals(new FormationSlot(0, 0, 0), slots.get(0), "wedge slot 0");
        assertEquals(new FormationSlot(1, -2, 2), slots.get(1), "wedge slot 1");
        assertEquals(new FormationSlot(2, 2, 2), slots.get(2), "wedge slot 2");
        assertEquals(new FormationSlot(3, -4, 4), slots.get(3), "wedge slot 3");
        assertEquals(new FormationSlot(4, 4, 4), slots.get(4), "wedge slot 4");
    }

    private static void plansSquareOffsets() {
        List<FormationSlot> slots = FormationPlanner.planSlots(ArmyFormation.SQUARE, 6, 2);

        assertEquals(new FormationSlot(0, -2, 0), slots.get(0), "square slot 0");
        assertEquals(new FormationSlot(1, 0, 0), slots.get(1), "square slot 1");
        assertEquals(new FormationSlot(2, 2, 0), slots.get(2), "square slot 2");
        assertEquals(new FormationSlot(3, -2, 2), slots.get(3), "square slot 3");
        assertEquals(new FormationSlot(4, 0, 2), slots.get(4), "square slot 4");
        assertEquals(new FormationSlot(5, 2, 2), slots.get(5), "square slot 5");
    }

    private static void mapsSlotsToAnchoredPositions() {
        ArmyPosition anchor = new ArmyPosition(100, 64, -20);
        List<ArmyPosition> positions = FormationPlanner.planPositions(anchor, ArmyFormation.WEDGE, 3, 2);

        assertEquals(new ArmyPosition(100, 64, -20), positions.get(0), "anchored position 0");
        assertEquals(new ArmyPosition(98, 64, -18), positions.get(1), "anchored position 1");
        assertEquals(new ArmyPosition(102, 64, -18), positions.get(2), "anchored position 2");
    }

    private static void handlesEmptyAndInvalidInputs() {
        assertTrue(FormationPlanner.planSlots(ArmyFormation.LINE, 0, 2).isEmpty(), "empty formation");
        assertThrows(IllegalArgumentException.class, () -> FormationPlanner.planSlots(ArmyFormation.LINE, -1, 2),
                "negative unit count");
        assertThrows(IllegalArgumentException.class, () -> FormationPlanner.planSlots(ArmyFormation.LINE, 1, 0),
                "zero spacing");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label + " expected to be true");
        }
    }

    private static <T extends Throwable> void assertThrows(Class<T> expectedType, ThrowingRunnable runnable, String label) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                return;
            }
            throw new AssertionError(label + " threw " + throwable.getClass().getName() + " instead of "
                    + expectedType.getName(), throwable);
        }

        throw new AssertionError(label + " did not throw " + expectedType.getName());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
