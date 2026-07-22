package galacticwars.clonewars.settlement;

import java.util.UUID;

public final class StarterCampDeploymentTest {
    private StarterCampDeploymentTest() {
    }

    public static void main(String[] args) {
        UUID kingdom = UUID.fromString("00000000-0000-0000-0000-00000000c101");
        UUID builder = UUID.fromString("00000000-0000-0000-0000-00000000c102");
        UUID project = UUID.fromString("00000000-0000-0000-0000-00000000c103");
        StarterCampDeployment awaiting = StarterCampDeployment.awaiting(
                kingdom, "minecraft:overworld", 4, 64, 8, 1);
        StarterCampDeployment building = awaiting.withSuppliesGranted().withBuilder(builder).building(project);
        assertTrue(building.contractGranted() && building.suppliesGranted(), "one-time grants recorded");
        assertEquals(StarterCampDeploymentPhase.BUILDING, building.phase(), "building phase");
        assertEquals(3, building.revision(), "every durable transition increments revision");
        assertTrue(building.projectId().isPresent() && building.projectId().get().equals(project),
                "building deployment retains project ID");

        StarterCampDeployment packed = building.packedUp();
        StarterCampDeployment relocated = packed.relocate("galacticwars:tatooine", 20, 70, -4, 3);
        assertTrue(relocated.contractGranted() && relocated.suppliesGranted(), "relocation cannot duplicate grants");
        assertTrue(relocated.projectId().isEmpty(), "cancelled project is not reused at the new site");
        assertEquals(StarterCampDeploymentPhase.AWAITING_CONFIRMATION, relocated.phase(), "relocated phase");

        StarterCampDeployment complete = relocated.building(project).complete();
        assertTrue(complete.terminal(), "completion is terminal");
        System.out.println("StarterCampDeploymentTest passed");
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label);
        }
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
