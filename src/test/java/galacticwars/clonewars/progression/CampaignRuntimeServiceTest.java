package galacticwars.clonewars.progression;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.data.LaunchContentRuntime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CampaignRuntimeServiceTest {
    public static void main(String[] args) {
        LaunchContentRuntime.install(new LaunchContentDefinitions(
                Map.of(), Map.of(), Map.of(), Map.of(
                        "republic_chapter_1", new LaunchContentDefinitions.QuestDefinition(
                                "republic_chapter_1",
                                List.of("faction_pledged", "command_center", "clone_trooper"),
                                40, Set.of("workforce"))), Map.of(), Map.of()),
                List.of("galacticwars:republic"), Map.of());
        UUID player = UUID.randomUUID();
        ProgressionState state = ProgressionState.create(player);
        state = record(state, player, ProgressionEventType.FACTION_PLEDGED, "galacticwars:republic");
        state = record(state, player, ProgressionEventType.BUILDING_COMPLETED, "command_center");
        ProgressionEvent hire = new ProgressionEvent(UUID.randomUUID(), player,
                ProgressionEventType.RECRUIT_HIRED, "galacticwars:clone_trooper", 1);
        ProgressionDecision completed = CampaignRuntimeService.record(state, hire);
        assertTrue(completed.accepted() && completed.changed(), "qualifying event accepted");
        assertTrue(completed.state().hasSubject(
                ProgressionEventType.QUEST_ADVANCED, "republic_chapter_1"),
                "eligible chapter completes automatically");
        assertTrue(completed.state().pendingCreditRewards() == 40,
                "quest reward is pending physical currency");
        ProgressionDecision replay = CampaignRuntimeService.record(completed.state(), hire);
        assertTrue(replay.accepted() && !replay.changed()
                        && replay.state().pendingCreditRewards() == 40,
                "event and deterministic quest reward are replay safe");
        System.out.println("CampaignRuntimeServiceTest passed");
    }

    private static ProgressionState record(
            ProgressionState state, UUID player, ProgressionEventType type, String subject
    ) {
        ProgressionDecision decision = CampaignRuntimeService.record(state,
                new ProgressionEvent(UUID.randomUUID(), player, type, subject, 1));
        assertTrue(decision.accepted(), decision.reason());
        return decision.state();
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
