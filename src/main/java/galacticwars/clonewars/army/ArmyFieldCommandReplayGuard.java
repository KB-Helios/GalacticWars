package galacticwars.clonewars.army;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UUID;

/** Bounded per-player replay guard shared by the server-only field command service. */
public final class ArmyFieldCommandReplayGuard {
    private final int maxActors;
    private final int maxReplayIdsPerActor;
    private final LinkedHashMap<UUID, LinkedHashMap<UUID, Boolean>> histories =
            new LinkedHashMap<>(16, 0.75F, true);

    public ArmyFieldCommandReplayGuard(int maxActors, int maxReplayIdsPerActor) {
        if (maxActors < 1 || maxReplayIdsPerActor < 1) {
            throw new IllegalArgumentException("replay guard bounds must be positive");
        }
        this.maxActors = maxActors;
        this.maxReplayIdsPerActor = maxReplayIdsPerActor;
    }

    /** Returns true exactly once for an actor/replay pair. */
    public synchronized boolean claim(UUID actorId, UUID replayId) {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(replayId, "replayId");
        LinkedHashMap<UUID, Boolean> history = histories.computeIfAbsent(actorId,
                ignored -> new LinkedHashMap<>(16, 0.75F, true));
        if (history.containsKey(replayId)) {
            history.get(replayId);
            return false;
        }
        history.put(replayId, Boolean.TRUE);
        while (history.size() > maxReplayIdsPerActor) {
            history.remove(history.keySet().iterator().next());
        }
        while (histories.size() > maxActors) {
            histories.remove(histories.keySet().iterator().next());
        }
        return true;
    }

    public synchronized void clear(UUID actorId) {
        histories.remove(Objects.requireNonNull(actorId, "actorId"));
    }

    public synchronized void clearAll() {
        histories.clear();
    }
}
