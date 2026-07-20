package galacticwars.clonewars.client;

import galacticwars.clonewars.network.FieldCommandStatePayload;
import java.util.concurrent.atomic.AtomicLong;

/** Client-only cache of the most recent server-owned field-command projection. */
public final class FieldCommandClientState {
    private static volatile FieldCommandStatePayload snapshot = FieldCommandStatePayload.awaitingServer();
    private static final AtomicLong revision = new AtomicLong();

    private FieldCommandClientState() {
    }

    public static void update(FieldCommandStatePayload payload) {
        snapshot = payload;
        revision.incrementAndGet();
    }

    public static FieldCommandStatePayload snapshot() {
        return snapshot;
    }

    public static long revision() {
        return revision.get();
    }

    public static void clear() {
        snapshot = FieldCommandStatePayload.awaitingServer();
        revision.incrementAndGet();
    }
}
