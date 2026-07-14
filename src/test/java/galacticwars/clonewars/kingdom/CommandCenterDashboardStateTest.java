package galacticwars.clonewars.kingdom;

import galacticwars.clonewars.army.ArmyFormation;
import galacticwars.clonewars.army.ArmyGroupRecord;
import galacticwars.clonewars.army.ArmyLocation;
import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.data.LaunchContentRuntime;
import galacticwars.clonewars.progression.CampaignRuntimeService;
import galacticwars.clonewars.progression.ProgressionEvent;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionState;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CommandCenterDashboardStateTest {
    private CommandCenterDashboardStateTest() {
    }

    public static void main(String[] args) {
        installCampaign();
        UUID ownerId = UUID.randomUUID();
        UUID kingdomId = UUID.randomUUID();
        UUID commanderId = UUID.randomUUID();
        UUID squadMemberId = UUID.randomUUID();
        UUID commandCandidateId = UUID.randomUUID();
        UUID combatTargetId = UUID.randomUUID();
        SettlementRecord settlement = SettlementRecord.create("minecraft:overworld", 10, 64, 10)
                .withRecruit(commanderId)
                .withRecruit(squadMemberId)
                .withRecruit(commandCandidateId);
        KingdomRecord kingdom = new KingdomRecord(
                kingdomId, ownerId, "galacticwars:republic", settlement,
                List.of(new KingdomMember(ownerId, KingdomMemberRole.OWNER)),
                List.of(), List.of(), List.of());
        ArmyGroupRecord squad = ArmyGroupRecord.create(
                ownerId, kingdomId, commanderId, List.of(squadMemberId), ArmyFormation.LINE,
                new ArmyLocation("minecraft:overworld", 10.5, 64.0, 10.5), 20L);
        ProgressionState progression = CampaignRuntimeService.record(
                ProgressionState.create(ownerId),
                new ProgressionEvent(
                        UUID.randomUUID(), ownerId, ProgressionEventType.FACTION_PLEDGED,
                        "galacticwars:republic", 1)).state();

        CommandCenterDashboardState dashboard = CommandCenterDashboardState.capture(
                ownerId, kingdom, List.of(squad), List.of(commanderId, commandCandidateId),
                List.of(new CommandCenterDashboardState.CombatTargetSummary(
                        combatTargetId, "B1 Battle Droid", "galacticwars:separatist", 12)),
                List.of(new CommandCenterDashboardState.WorkerSummary(
                        commandCandidateId, "CT-6116", "builder", "work_at_site", "interact",
                        "build_place", Optional.of(new CommandCenterDashboardState.PositionSummary(
                        "minecraft:overworld", 12, 64, 12)), 8,
                        Optional.of(new CommandCenterDashboardState.PositionSummary(
                                "minecraft:overworld", 10, 64, 10)),
                        Optional.of(new CommandCenterDashboardState.PositionSummary(
                                "minecraft:overworld", 13, 64, 12)), Optional.empty(), 4, 28, 6)),
                List.of(commandCandidateId),
                galacticwars.clonewars.settlement.KingdomBaseBlueprint.builtIns(),
                List.of(new CommandCenterDashboardState.NearbyPlayerSummary(
                        UUID.randomUUID(), "Nearby Player", 6)),
                progression, 47, true,
                List.of(kingdom), List.of(), List.of(), 100L);

        require(dashboard.kingdomAvailable(), "kingdom should be available");
        require(dashboard.kingdomId().equals(kingdomId), "kingdom id should be preserved");
        require(dashboard.actorId().equals(ownerId), "actor id should be preserved for safe UI filtering");
        require(dashboard.actorRole().equals("owner"), "owner role should be visible");
        require(dashboard.treasuryCredits() == 47, "treasury should be visible");
        require(dashboard.recruitCount() == 3 && dashboard.housingCapacity() == 4,
                "settlement capacity should be visible");
        require(dashboard.squads().size() == 1
                        && dashboard.squads().getFirst().commanderId().filter(commanderId::equals).isPresent()
                        && dashboard.squads().getFirst().memberIds().contains(squadMemberId),
                "squad identity, commander, and selectable members should be visible");
        require(dashboard.commandCandidateIds().equals(List.of(commandCandidateId)),
                "only live, ungrouped military recruits should be command candidates");
        require(dashboard.combatTargets().size() == 1
                        && dashboard.combatTargets().getFirst().entityId().equals(combatTargetId),
                "explicit nearby combat targets should be visible");
        require(dashboard.workers().size() == 1
                        && dashboard.workers().getFirst().profession().equals("builder")
                        && dashboard.workers().getFirst().carriedItemCount() == 4,
                "live worker status and inventory counts should be visible");
        require(dashboard.claims().size() == 1 && dashboard.claims().getFirst().capital(),
                "claim selectors should include the capital");
        require(!dashboard.blueprints().isEmpty(), "construction UI should expose authoritative blueprints");
        require(dashboard.constructionBuilderIds().equals(List.of(commandCandidateId)),
                "construction UI should expose live builder candidates");
        require(dashboard.nearbyPlayers().size() == 1
                        && dashboard.nearbyPlayers().getFirst().distanceBlocks() == 6,
                "kingdom UI should expose bounded explicit invite targets");
        require(dashboard.campaign().size() == 3,
                "faction campaign should expose three chapters but was " + dashboard.campaign()
                        + " from " + galacticwars.clonewars.progression.LaunchContentCatalog.quests());
        require(dashboard.activeQuest().isPresent(), "campaign should expose the active chapter");
        require(dashboard.nextObjective().isPresent(), "campaign should expose the next objective");

        boolean immutable = false;
        try {
            dashboard.squads().clear();
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        require(immutable, "dashboard collections must be immutable");
        System.out.println("CommandCenterDashboardStateTest passed");
    }

    private static void installCampaign() {
        Map<String, LaunchContentDefinitions.QuestDefinition> quests = Map.of(
                "republic_chapter_1", new LaunchContentDefinitions.QuestDefinition(
                        "republic_chapter_1", List.of("faction_pledged", "command_center"),
                        40, Set.of("workforce")),
                "republic_chapter_2", new LaunchContentDefinitions.QuestDefinition(
                        "republic_chapter_2", List.of("delivery_completed", "forward_base"),
                        70, Set.of("planet_travel")),
                "republic_chapter_3", new LaunchContentDefinitions.QuestDefinition(
                        "republic_chapter_3", List.of("vehicle_acquired", "region_captured"),
                        120, Set.of("conquest")));
        LaunchContentRuntime.install(
                new LaunchContentDefinitions(Map.of(), Map.of(), Map.of(), quests, Map.of(), Map.of()),
                List.of("galacticwars:republic"), Map.of());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
