package galacticwars.clonewars.progression;

import java.util.UUID;

public final class GalacticSystemsIntegrationTest {
    public static void main(String[] args) {
        UUID player = UUID.randomUUID();
        ProgressionState state = ProgressionState.create(player);
        state = event(state, ProgressionEventType.FACTION_PLEDGED, "galacticwars:republic", 1);
        state = event(state, ProgressionEventType.CREDIT_TRANSACTION, "starter_reward", 100);
        state = event(state, ProgressionEventType.BUILDING_COMPLETED, "command_center", 1);
        state = event(state, ProgressionEventType.BUILDING_COMPLETED, "forward_base", 1);
        assertTrue(!state.unlocks().contains("vehicle_crafting"),
                "vehicle acquisition test starts without Supply Depot crafting access");

        GalacticSystemsService.SystemDecision prematureVehicle = GalacticSystemsService.acquireVehicle(
                state, UUID.randomUUID(), "barc_speeder");
        assertTrue(!prematureVehicle.accepted() && prematureVehicle.reason().equals("vehicle_quest_locked"),
                "vehicle quest requirement cannot be bypassed");
        state = event(state, ProgressionEventType.QUEST_ADVANCED, "republic_chapter_1", 1);
        state = event(state, ProgressionEventType.QUEST_ADVANCED, "republic_chapter_2", 1);
        assertTrue(!state.unlocks().contains("vehicle_crafting"),
                "Republic chapter 2 does not synthesize the unrelated crafting unlock");

        GalacticSystemsService.SystemDecision vehicle = GalacticSystemsService.acquireVehicle(
                state, UUID.randomUUID(), "barc_speeder");
        assertTrue(vehicle.accepted() && vehicle.changed(),
                "quest-declared BARC unlock bypasses unrelated Supply Depot gate: " + vehicle.reason());
        state = vehicle.state();
        assertTrue(state.unlocks().contains("vehicle_control"), "vehicle acquisition connects to controls");

        UUID tradeEventId = UUID.randomUUID();
        GalacticSystemsService.SystemDecision trade = GalacticSystemsService.purchase(
                state, tradeEventId, "republic_quartermaster");
        assertTrue(trade.accepted() && trade.changed() && trade.state().credits() == 88,
                "trade charges exactly once");
        ProgressionState afterTrade = trade.state();
        GalacticSystemsService.SystemDecision replayedTrade = GalacticSystemsService.purchase(
                afterTrade, tradeEventId, "republic_quartermaster");
        assertTrue(replayedTrade.accepted() && !replayedTrade.changed()
                        && replayedTrade.resultId().isEmpty() && replayedTrade.state().credits() == 88,
                "duplicate trade cannot charge or grant twice");

        state = afterTrade;
        GalacticSystemsService.SystemDecision force = GalacticSystemsService.unlockForceAbility(
                state, UUID.randomUUID(), "light_push");
        assertTrue(force.accepted() && force.changed(), force.reason());
        GalacticSystemsService.SystemDecision duplicateForce = GalacticSystemsService.unlockForceAbility(
                force.state(), UUID.randomUUID(), "light_push");
        assertTrue(duplicateForce.accepted() && !duplicateForce.changed()
                        && duplicateForce.resultId().isEmpty(),
                "Force unlock cannot be rewarded twice with a new event id");
        GalacticSystemsService.SystemDecision wrongPath = GalacticSystemsService.unlockForceAbility(
                force.state(), UUID.randomUUID(), "dark_choke");
        assertTrue(!wrongPath.accepted(), "locked or opposing Force path cannot be mixed");
        GalacticSystemsService.SystemDecision prematureLeap = GalacticSystemsService.unlockForceAbility(
                force.state(), UUID.randomUUID(), "light_leap");
        assertTrue(!prematureLeap.accepted() && prematureLeap.reason().equals("force_quest_locked"),
                "advanced Force ability requires its exact quest");

        state = event(force.state(), ProgressionEventType.QUEST_ADVANCED, "republic_chapter_3", 1);
        GalacticSystemsService.SystemDecision conquest = GalacticSystemsService.captureRegion(
                state, UUID.randomUUID(), "kamino_platform");
        assertTrue(conquest.accepted() && conquest.changed()
                        && conquest.state().unlocks().contains("veteran_trades"),
                "quest progression enables conquest and its rewards");
        System.out.println("GalacticSystemsIntegrationTest passed");
    }

    private static ProgressionState event(
            ProgressionState state,
            ProgressionEventType type,
            String subject,
            int amount
    ) {
        ProgressionDecision decision = GalacticProgressionCoordinator.apply(state,
                new ProgressionEvent(UUID.randomUUID(), state.playerId(), type, subject, amount));
        assertTrue(decision.accepted(), decision.reason());
        return decision.state();
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
