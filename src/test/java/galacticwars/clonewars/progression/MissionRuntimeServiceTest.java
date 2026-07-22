package galacticwars.clonewars.progression;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;

public final class MissionRuntimeServiceTest {
    private MissionRuntimeServiceTest() {
    }

    public static void main(String[] args) {
        UUID playerId = UUID.randomUUID();
        var mission = new LaunchContentDefinitions.MissionDefinition(
                "test_escort", "test_chapter_2", "escort", "tatooine", "",
                List.of(
                        new LaunchContentDefinitions.MissionRequirementDefinition(
                                "delivery_completed", Set.of(), 3),
                        new LaunchContentDefinitions.MissionRequirementDefinition(
                                "building_completed", Set.of("supply_depot"), 1)),
                400);
        ProgressionState state = new ProgressionState(
                ProgressionState.CURRENT_SCHEMA_VERSION, playerId, "galacticwars:test", 0,
                Set.of(), Map.of(ProgressionEventType.DELIVERY_COMPLETED, 3,
                        ProgressionEventType.BUILDING_COMPLETED, 1),
                Map.of(ProgressionEventType.DELIVERY_COMPLETED, Set.of("route_a"),
                        ProgressionEventType.BUILDING_COMPLETED, Set.of("supply_depot")),
                Map.of(ProgressionEventType.DELIVERY_COMPLETED, Map.of("route_a", 3),
                        ProgressionEventType.BUILDING_COMPLETED, Map.of("supply_depot", 1)),
                Set.of());
        if (!MissionRuntimeService.requirementsComplete(state, mission)) {
            throw new AssertionError("authoritative mission requirements did not complete");
        }
        if (!MissionRuntimeService.deterministicEventId(playerId, mission.id()).equals(
                MissionRuntimeService.deterministicEventId(playerId, mission.id()))) {
            throw new AssertionError("mission reward identity is not replay stable");
        }
        ProgressionState incomplete = ProgressionState.create(playerId);
        if (MissionRuntimeService.requirementsComplete(incomplete, mission)) {
            throw new AssertionError("incomplete mission requirements were accepted");
        }

        String secureMission = "republic_secure_starter_camp";
        ProgressionState campaign = new ProgressionState(
                ProgressionState.CURRENT_SCHEMA_VERSION, playerId, "galacticwars:republic", 0,
                Set.of(), Map.of(ProgressionEventType.MISSION_STARTED, 1),
                Map.of(ProgressionEventType.MISSION_STARTED, Set.of(secureMission)),
                Map.of(ProgressionEventType.MISSION_STARTED, Map.of(secureMission, 1)),
                Set.of());
        if (!MissionRuntimeService.active(campaign, secureMission)
                || campaign.subjectTotal(
                ProgressionEventType.MISSION_STARTED, Set.of(secureMission)) != 1) {
            throw new AssertionError("first unlocked mission was not started persistently");
        }
        campaign = new ProgressionState(
                ProgressionState.CURRENT_SCHEMA_VERSION, playerId, "galacticwars:republic", 0,
                Set.of(), Map.of(ProgressionEventType.MISSION_STARTED, 1,
                        ProgressionEventType.MISSION_FAILED, 1),
                Map.of(ProgressionEventType.MISSION_STARTED, Set.of(secureMission),
                        ProgressionEventType.MISSION_FAILED, Set.of(secureMission)),
                Map.of(ProgressionEventType.MISSION_STARTED, Map.of(secureMission, 1),
                        ProgressionEventType.MISSION_FAILED, Map.of(secureMission, 1)),
                Set.of());
        if (MissionRuntimeService.active(campaign, secureMission)
                || campaign.subjectTotal(
                ProgressionEventType.MISSION_FAILED, Set.of(secureMission)) != 1) {
            throw new AssertionError("mission failure did not suspend the active attempt");
        }
        campaign = new ProgressionState(
                ProgressionState.CURRENT_SCHEMA_VERSION, playerId, "galacticwars:republic", 0,
                Set.of(), Map.of(ProgressionEventType.MISSION_STARTED, 2,
                        ProgressionEventType.MISSION_FAILED, 1),
                Map.of(ProgressionEventType.MISSION_STARTED, Set.of(secureMission),
                        ProgressionEventType.MISSION_FAILED, Set.of(secureMission)),
                Map.of(ProgressionEventType.MISSION_STARTED, Map.of(secureMission, 2),
                        ProgressionEventType.MISSION_FAILED, Map.of(secureMission, 1)),
                Set.of());
        if (!MissionRuntimeService.active(campaign, secureMission)
                || campaign.subjectTotal(
                ProgressionEventType.MISSION_STARTED, Set.of(secureMission)) != 2) {
            throw new AssertionError("bounded retry did not create a second active attempt");
        }

        MissionAttemptSavedData.MissionAttempt returned =
                new MissionAttemptSavedData.MissionAttempt(
                        playerId, secureMission, 2, "hold", "minecraft:overworld",
                        BlockPos.ZERO, 20L, 0L, 40, 80, true, "", 100L)
                        .present();
        if (returned.absentTicks() != 0 || returned.holdTicks() != 40
                || !returned.waveSpawned()) {
            throw new AssertionError("returning to a mission did not reset only absence time");
        }
        System.out.println("MissionRuntimeServiceTest passed");
    }
}
