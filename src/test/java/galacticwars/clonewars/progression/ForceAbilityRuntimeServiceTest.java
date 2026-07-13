package galacticwars.clonewars.progression;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ForceAbilityRuntimeServiceTest {
    public static void main(String[] args) {
        UUID player = UUID.randomUUID();
        ProgressionState progression = new ProgressionState(
                ProgressionState.CURRENT_SCHEMA_VERSION, player, "galacticwars:republic", 0,
                Set.of(), Map.of(), Map.of(ProgressionEventType.QUEST_ADVANCED,
                Set.of("republic_chapter_2")), Set.of("force_path"));
        LaunchContentDefinitions content = new LaunchContentDefinitions(
                Map.of(), Map.of(), Map.of("light_push",
                new LaunchContentDefinitions.ForceAbilityDefinition(
                        "light_push", "light", 20, 60, "republic_chapter_2", true)),
                Map.of(), Map.of(), Map.of());

        ForceRuntimeState runtime = ForceRuntimeState.full();
        UUID activationId = UUID.randomUUID();
        ForceAbilityRuntimeService.ActivationDecision activation = ForceAbilityRuntimeService.activate(
                progression, runtime, activationId, "light_push", 100L, false, true, content);
        assertTrue(activation.accepted() && activation.state().energy() == 80
                        && activation.energySpent() == 20 && activation.cooldownTicks() == 60,
                "unlocked Force definition consumes energy and starts cooldown");
        ForceAbilityRuntimeService.ActivationDecision replay = ForceAbilityRuntimeService.activate(
                progression, activation.state(), activationId, "light_push", 101L, false, true, content);
        assertTrue(replay.accepted() && replay.reason().equals("duplicate_activation")
                        && replay.energySpent() == 0,
                "activation replay cannot consume energy twice");
        ForceAbilityRuntimeService.ActivationDecision cooldown = ForceAbilityRuntimeService.activate(
                progression, activation.state(), UUID.randomUUID(), "light_push", 101L, false, true, content);
        assertTrue(!cooldown.accepted() && cooldown.reason().equals("force_cooldown"),
                "cooldown enforced authoritatively");
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
