package galacticwars.clonewars.progression;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record ProgressionState(
        int schemaVersion,
        UUID playerId,
        String factionId,
        int credits,
        Set<UUID> processedEventIds,
        Map<ProgressionEventType, Integer> eventTotals,
        Map<ProgressionEventType, Set<String>> eventSubjects,
        Set<String> unlocks
) {
    public static final int CURRENT_SCHEMA_VERSION = 3;

    public ProgressionState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported progression schema " + schemaVersion);
        }
        Objects.requireNonNull(playerId, "playerId");
        factionId = factionId == null ? "" : factionId;
        if (credits < 0) {
            throw new IllegalArgumentException("credits cannot be negative");
        }
        processedEventIds = Set.copyOf(processedEventIds);
        eventTotals = Map.copyOf(eventTotals);
        HashMap<ProgressionEventType, Set<String>> subjects = new HashMap<>();
        eventSubjects.forEach((type, values) -> subjects.put(type, Set.copyOf(values)));
        eventSubjects = Map.copyOf(subjects);
        unlocks = Set.copyOf(unlocks);
    }

    public static ProgressionState create(UUID playerId) {
        return new ProgressionState(CURRENT_SCHEMA_VERSION, playerId, "", 0,
                Set.of(), Map.of(), Map.of(), Set.of("intro_quest"));
    }

    public boolean processed(UUID eventId) {
        return processedEventIds.contains(eventId);
    }

    /** Pending physical Credit Chips earned by campaign rewards. */
    public int pendingCreditRewards() {
        return credits;
    }

    ProgressionState clearPendingCreditRewards() {
        if (credits == 0) {
            return this;
        }
        return new ProgressionState(schemaVersion, playerId, factionId, 0,
                processedEventIds, eventTotals, eventSubjects, unlocks);
    }

    public int total(ProgressionEventType type) {
        return eventTotals.getOrDefault(type, 0);
    }

    public boolean hasSubject(ProgressionEventType type, String subjectId) {
        return eventSubjects.getOrDefault(type, Set.of()).contains(subjectId);
    }

    public boolean hasSubjectPath(ProgressionEventType type, String subjectPath) {
        return eventSubjects.getOrDefault(type, Set.of()).stream().anyMatch(subject -> {
            int separator = subject.indexOf(':');
            return (separator < 0 ? subject : subject.substring(separator + 1)).equals(subjectPath);
        });
    }

    ProgressionState apply(ProgressionEvent event, String faction, Set<String> addedUnlocks) {
        if (!playerId.equals(event.playerId())) {
            throw new SecurityException("Progression event belongs to another player");
        }
        if (processed(event.id())) {
            return this;
        }
        int creditDelta = switch (event.type()) {
            case CREDIT_TRANSACTION -> 0;
            case QUEST_ADVANCED -> LaunchContentCatalog.questRewardCredits(event.subjectId());
            case REGION_CAPTURED -> LaunchContentCatalog.regionRewardCredits(event.subjectId());
            default -> 0;
        };
        int updatedCredits = Math.addExact(credits, creditDelta);
        if (updatedCredits < 0) {
            throw new IllegalStateException("insufficient_credits");
        }
        HashSet<UUID> ids = new HashSet<>(processedEventIds);
        ids.add(event.id());
        HashMap<ProgressionEventType, Integer> totals = new HashMap<>(eventTotals);
        totals.merge(event.type(), Math.max(1, event.amount()), Math::addExact);
        HashMap<ProgressionEventType, Set<String>> subjects = new HashMap<>(eventSubjects);
        HashSet<String> values = new HashSet<>(subjects.getOrDefault(event.type(), Set.of()));
        values.add(event.subjectId());
        subjects.put(event.type(), Set.copyOf(values));
        HashSet<String> unlocked = new HashSet<>(unlocks);
        unlocked.addAll(addedUnlocks);
        return new ProgressionState(schemaVersion, playerId, faction, updatedCredits,
                ids, totals, subjects, unlocked);
    }
}
