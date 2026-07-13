package galacticwars.clonewars.progression;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.data.LaunchContentRuntime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GalacticSystemsIntegrationTest {
    private static final LaunchContentDefinitions CONTENT = content();

    public static void main(String[] args) {
        LaunchContentRuntime.install(CONTENT, List.of("galacticwars:republic"), Map.of());
        UUID player = UUID.randomUUID();
        ProgressionState state = ProgressionState.create(player);
        state = event(state, ProgressionEventType.FACTION_PLEDGED, "galacticwars:republic", 1);
        state = event(state, ProgressionEventType.CREDIT_TRANSACTION, "starter_reward", 100);
        state = event(state, ProgressionEventType.BUILDING_COMPLETED, "command_center", 1);
        state = event(state, ProgressionEventType.BUILDING_COMPLETED, "forward_base", 1);
        state = event(state, ProgressionEventType.RECRUIT_HIRED, "galacticwars:clone_trooper", 1);
        state = event(state, ProgressionEventType.DELIVERY_COMPLETED, "starter_delivery", 1);
        state = event(state, ProgressionEventType.PLANET_VISITED, "kamino", 1);
        assertTrue(!state.unlocks().contains("vehicle_crafting"),
                "vehicle acquisition test starts without Supply Depot crafting access");

        GalacticSystemsService.SystemDecision prematureVehicle = GalacticSystemsService.acquireVehicle(
                state, UUID.randomUUID(), "barc_speeder", CONTENT);
        assertTrue(!prematureVehicle.accepted() && prematureVehicle.reason().equals("vehicle_quest_locked"),
                "vehicle quest requirement cannot be bypassed");
        state = event(state, ProgressionEventType.QUEST_ADVANCED, "republic_chapter_1", 1);
        state = event(state, ProgressionEventType.QUEST_ADVANCED, "republic_chapter_2", 1);
        assertTrue(!state.unlocks().contains("vehicle_crafting"),
                "Republic chapter 2 does not synthesize the unrelated crafting unlock");

        GalacticSystemsService.SystemDecision vehicle = GalacticSystemsService.acquireVehicle(
                state, UUID.randomUUID(), "barc_speeder", CONTENT);
        assertTrue(vehicle.accepted() && vehicle.changed(),
                "quest-declared BARC unlock bypasses unrelated Supply Depot gate: " + vehicle.reason());
        state = vehicle.state();
        assertTrue(state.unlocks().contains("vehicle_control"), "vehicle acquisition connects to controls");

        UUID tradeEventId = UUID.randomUUID();
        GalacticSystemsService.SystemDecision trade = GalacticSystemsService.purchase(
                state, tradeEventId, "republic_quartermaster", CONTENT);
        assertTrue(trade.accepted() && trade.changed() && trade.state().credits() == 198,
                "trade charges exactly once");
        assertTrue(trade.resultId().equals("galacticwars:energy_cell") && trade.resultCount() == 8,
                "trade returns the namespaced item and configured stack count");
        ProgressionState afterTrade = trade.state();
        GalacticSystemsService.SystemDecision replayedTrade = GalacticSystemsService.purchase(
                afterTrade, tradeEventId, "republic_quartermaster", CONTENT);
        assertTrue(replayedTrade.accepted() && !replayedTrade.changed()
                        && replayedTrade.resultId().isEmpty() && replayedTrade.state().credits() == 198,
                "duplicate trade cannot charge or grant twice");

        state = afterTrade;
        GalacticSystemsService.SystemDecision force = GalacticSystemsService.unlockForceAbility(
                state, UUID.randomUUID(), "light_push", CONTENT);
        assertTrue(!force.accepted() && force.reason().equals("force_runtime_disabled")
                        && force.state() == state,
                "Force definitions remain reserved but cannot unlock in this release");

        GalacticSystemsService.SystemDecision conquest = GalacticSystemsService.captureRegion(
                state, UUID.randomUUID(), "kamino_platform", CONTENT);
        assertTrue(conquest.accepted() && conquest.changed()
                        && conquest.state().unlocks().contains("veteran_trades"),
                "chapter 2 permits the campaign capture required by chapter 3");
        state = event(conquest.state(), ProgressionEventType.QUEST_ADVANCED, "republic_chapter_3", 1);
        assertTrue(state.unlocks().contains("conquest") && state.unlocks().contains("vehicle_mastery"),
                "chapter 3 enables conquest and the attainable LAAT mastery gate");
        GalacticSystemsService.SystemDecision laat = GalacticSystemsService.acquireVehicle(
                state, UUID.randomUUID(), "laat_gunship", CONTENT);
        assertTrue(laat.accepted(), "LAAT becomes attainable after chapter 3: " + laat.reason());
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

    private static LaunchContentDefinitions content() {
        Map<String, LaunchContentDefinitions.VehicleDefinition> vehicles = Map.of(
                "barc_speeder", new LaunchContentDefinitions.VehicleDefinition(
                        "barc_speeder", "hover", 1, 40, 1200, "republic_chapter_2"),
                "laat_gunship", new LaunchContentDefinitions.VehicleDefinition(
                        "laat_gunship", "flight", 8, 220, 5000, "vehicle_mastery"));
        Map<String, LaunchContentDefinitions.ForceAbilityDefinition> force = Map.of(
                "light_push", new LaunchContentDefinitions.ForceAbilityDefinition(
                        "light_push", "light", 20, 60, "republic_chapter_2", false));
        Map<String, LaunchContentDefinitions.TradeDefinition> trades = Map.of(
                "republic_quartermaster", new LaunchContentDefinitions.TradeDefinition(
                        "republic_quartermaster", "republic", 12, "galacticwars:energy_cell", 8,
                        "faction_intro"));
        Map<String, LaunchContentDefinitions.ConquestRegionDefinition> regions = Map.of(
                "kamino_platform", new LaunchContentDefinitions.ConquestRegionDefinition(
                        "kamino_platform", "kamino", 64, 1400, 100));
        Map<String, LaunchContentDefinitions.PlanetDefinition> planets = Map.of(
                "kamino", new LaunchContentDefinitions.PlanetDefinition(
                        "kamino", "galacticwars:kamino", "platform_city", "storm_ocean", "republic"));
        Map<String, LaunchContentDefinitions.QuestDefinition> quests = Map.of(
                "republic_chapter_1", new LaunchContentDefinitions.QuestDefinition(
                        "republic_chapter_1",
                        List.of("faction_pledged", "command_center", "clone_trooper"),
                        40, Set.of("workforce")),
                "republic_chapter_2", new LaunchContentDefinitions.QuestDefinition(
                        "republic_chapter_2",
                        List.of("delivery_completed", "forward_base", "kamino"),
                        70, Set.of("barc_speeder", "force_path")),
                "republic_chapter_3", new LaunchContentDefinitions.QuestDefinition(
                        "republic_chapter_3",
                        List.of("vehicle_acquired", "trade_completed", "region_captured"),
                        120, Set.of("conquest", "vehicle_mastery")));
        return new LaunchContentDefinitions(planets, vehicles, force, quests, trades, regions);
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
