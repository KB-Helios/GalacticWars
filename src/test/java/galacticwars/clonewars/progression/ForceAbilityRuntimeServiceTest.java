package galacticwars.clonewars.progression;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import java.util.Map;
import java.util.UUID;

public final class ForceAbilityRuntimeServiceTest {
    public static void main(String[] args) {
        UUID player = UUID.randomUUID();
        ProgressionState progression = ProgressionState.create(player);
        LaunchContentDefinitions content = new LaunchContentDefinitions(
                Map.of(), Map.of(), Map.of("light_push",
                new LaunchContentDefinitions.ForceAbilityDefinition(
                        "light_push", "light", 20, 60, "republic_chapter_2", false)),
                Map.of(), Map.of(), Map.of());

        ForceRuntimeState runtime = ForceRuntimeState.full();
        ForceAbilityRuntimeService.ActivationDecision activation = ForceAbilityRuntimeService.activate(
                progression, runtime, "light_push", 100L, false, true, content);
        assertTrue(!activation.accepted() && activation.reason().equals("force_runtime_disabled")
                        && activation.state() == runtime && activation.energySpent() == 0,
                "reserved Force definitions cannot execute or consume resources");
        ForceAbilityRuntimeService.ActivationDecision locked = ForceAbilityRuntimeService.activate(
                progression, runtime, "dark_choke", 200L, false, true, content);
        assertTrue(!locked.accepted() && locked.reason().equals("unknown_force_ability"),
                "unknown Force IDs fail closed");
        System.out.println("ForceAbilityRuntimeServiceTest passed");
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
