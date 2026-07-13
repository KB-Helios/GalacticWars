package galacticwars.clonewars.kingdom;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.data.LaunchContentRuntime;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionState;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class KingdomGameplayTransactionTest {
    public static void main(String[] args) {
        LaunchContentRuntime.install(
                LaunchContentDefinitions.empty(), List.of("galacticwars:republic"), Map.of());
        UUID player = UUID.randomUUID();
        KingdomActionId first = KingdomActionId.of("faction_pledge", player, "galacticwars:republic");
        KingdomActionId replay = KingdomActionId.of("faction_pledge", player, "galacticwars:republic");
        assertTrue(first.equals(replay), "stable action value");
        assertTrue(first.progressionEventId().equals(replay.progressionEventId()), "stable event UUID");
        KingdomActionId slashPath = KingdomActionId.of(
                "command_center_claim", player, "galacticwars:planets/tatooine", -42L);
        assertTrue(slashPath.value().contains("planets/tatooine"), "resource path slash supported");

        ProgressionState progression = ProgressionState.create(player);
        KingdomGameplayAction action = new KingdomGameplayAction(
                first, player, ProgressionEventType.FACTION_PLEDGED, "galacticwars:republic", 1);
        KingdomGameplayResult accepted = KingdomGameplayTransactionService.evaluate(progression, action);
        KingdomGameplayResult duplicate = KingdomGameplayTransactionService.evaluate(accepted.progressionState(), action);
        assertTrue(accepted.accepted() && accepted.changed(), "first action accepted");
        assertTrue(duplicate.accepted() && !duplicate.changed(), "replay is idempotent");
        assertTrue(duplicate.reason().equals("duplicate_action"), "duplicate reason");
        System.out.println("KingdomGameplayTransactionTest passed");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
