package galacticwars.clonewars.progression;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Server-authoritative campaign reducer, including automatic chapter completion. */
public final class CampaignRuntimeService {
    private CampaignRuntimeService() {
    }

    public static ProgressionDecision record(ProgressionState state, ProgressionEvent event) {
        ProgressionDecision initial = GalacticProgressionCoordinator.apply(state, event);
        if (!initial.accepted()) {
            return initial;
        }
        ProgressionState current = initial.state();
        boolean changed = initial.changed();
        boolean advanced;
        do {
            advanced = false;
            for (String questId : LaunchContentCatalog.quests().stream().sorted().toList()) {
                if (current.hasSubject(ProgressionEventType.QUEST_ADVANCED, questId)) {
                    continue;
                }
                ProgressionEvent completion = new ProgressionEvent(
                        deterministicQuestEventId(current.playerId(), questId),
                        current.playerId(), ProgressionEventType.QUEST_ADVANCED, questId, 1);
                ProgressionDecision candidate = GalacticProgressionCoordinator.apply(current, completion);
                if (candidate.accepted() && candidate.changed()) {
                    current = candidate.state();
                    changed = true;
                    advanced = true;
                    break;
                }
            }
        } while (advanced);
        return new ProgressionDecision(true, changed,
                changed ? "accepted" : "duplicate_event", current);
    }

    public static ProgressionState completeEligibleQuests(ProgressionState state) {
        ProgressionEvent replay = new ProgressionEvent(
                UUID.nameUUIDFromBytes(("campaign:recheck:" + state.playerId())
                        .getBytes(StandardCharsets.UTF_8)),
                state.playerId(), ProgressionEventType.CAMPAIGN_RECHECK, "eligible_quests", 0);
        return record(state, replay).state();
    }

    public static UUID deterministicQuestEventId(UUID playerId, String questId) {
        return UUID.nameUUIDFromBytes(("campaign:quest:" + playerId + ":" + questId)
                .getBytes(StandardCharsets.UTF_8));
    }
}
