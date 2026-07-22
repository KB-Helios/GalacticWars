package galacticwars.clonewars.network;

import galacticwars.clonewars.army.FieldCommandAction;
import galacticwars.clonewars.army.FieldCommandResult;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Verifies bounded C2S/S2C field-command packets reject malformed wire input before allocation. */
public final class FieldCommandPayloadStreamCodecTest {
    private FieldCommandPayloadStreamCodecTest() {
    }

    public static void main(String[] args) {
        requestRoundTripsAndRejectsBadWireValues();
        stateRoundTripsAndRejectsOversizedSquadArrays();

        System.out.println("FieldCommandPayloadStreamCodecTest passed");
    }

    private static void requestRoundTripsAndRejectsBadWireValues() {
        FieldCommandRequestPayload expected = new FieldCommandRequestPayload(
                UUID.fromString("00000000-0000-0000-0000-00000000e001"),
                FieldCommandAction.SET_PATROL_WAYPOINT_WAIT,
                List.of(
                        UUID.fromString("00000000-0000-0000-0000-00000000e002"),
                        UUID.fromString("00000000-0000-0000-0000-00000000e003")),
                "Landing Pad Sweep",
                2,
                240);
        RegistryFriendlyByteBuf encoded = buffer();
        try {
            FieldCommandRequestPayload.STREAM_CODEC.encode(encoded, expected);
            assertEquals(expected, FieldCommandRequestPayload.STREAM_CODEC.decode(encoded), "request round trip");
            assertFalse(encoded.isReadable(), "request decode consumes exactly one packet");
        } finally {
            encoded.release();
        }

        RegistryFriendlyByteBuf oversized = buffer();
        try {
            oversized.writeUUID(UUID.randomUUID());
            oversized.writeVarInt(FieldCommandAction.FOLLOW.wireId());
            oversized.writeVarInt(FieldCommandRequestPayload.MAX_GROUPS + 1);
            assertThrows(IllegalArgumentException.class,
                    () -> FieldCommandRequestPayload.STREAM_CODEC.decode(oversized),
                    "oversized command selection");
        } finally {
            oversized.release();
        }

        RegistryFriendlyByteBuf invalidAction = buffer();
        try {
            invalidAction.writeUUID(UUID.randomUUID());
            invalidAction.writeVarInt(999);
            invalidAction.writeVarInt(0);
            assertThrows(IllegalArgumentException.class,
                    () -> FieldCommandRequestPayload.STREAM_CODEC.decode(invalidAction),
                    "unknown action id");
        } finally {
            invalidAction.release();
        }
        try {
            FieldCommandAction.fromWireId(999);
            throw new AssertionError("unknown action id did not throw");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().contains("999"), "unknown action id is included in diagnostics");
        }

        RegistryFriendlyByteBuf truncated = buffer();
        try {
            truncated.writeUUID(UUID.randomUUID());
            truncated.writeVarInt(FieldCommandAction.FOLLOW.wireId());
            assertThrows(IndexOutOfBoundsException.class,
                    () -> FieldCommandRequestPayload.STREAM_CODEC.decode(truncated),
                    "truncated request");
        } finally {
            truncated.release();
        }

        RegistryFriendlyByteBuf invalidPatrolWait = buffer();
        try {
            invalidPatrolWait.writeUUID(UUID.randomUUID());
            invalidPatrolWait.writeVarInt(FieldCommandAction.SET_PATROL_WAYPOINT_WAIT.wireId());
            invalidPatrolWait.writeVarInt(1);
            invalidPatrolWait.writeUUID(UUID.randomUUID());
            invalidPatrolWait.writeUtf("Landing Pad Sweep", FieldCommandRequestPayload.MAX_PATROL_ROUTE_NAME_BYTES);
            invalidPatrolWait.writeVarInt(0);
            invalidPatrolWait.writeVarInt(12_001);
            invalidPatrolWait.writeUtf("", FieldCommandRequestPayload.MAX_OPTION_ID_BYTES);
            assertThrows(IllegalArgumentException.class,
                    () -> FieldCommandRequestPayload.STREAM_CODEC.decode(invalidPatrolWait),
                    "out-of-range patrol wait");
        } finally {
            invalidPatrolWait.release();
        }
    }

    private static void stateRoundTripsAndRejectsOversizedSquadArrays() {
        FieldCommandStatePayload expected = new FieldCommandStatePayload(
                UUID.fromString("00000000-0000-0000-0000-00000000e004"),
                FieldCommandResult.REPLAY_REJECTED,
                List.of(new FieldCommandStatePayload.Squad(
                        UUID.fromString("00000000-0000-0000-0000-00000000e005"),
                        "501st Command", 7, "FOLLOW_OWNER", "MOVEMENT", "LIVE",
                        "AGGRESSIVE", "LOWEST_HEALTH", "HOLD_FIRE", true, true,
                        Optional.of(new FieldCommandStatePayload.Squad.Patrol("Landing Pad Sweep", 4)))),
                false, true, List.of("formation_basic", "formation_advanced"));
        RegistryFriendlyByteBuf encoded = buffer();
        try {
            FieldCommandStatePayload.STREAM_CODEC.encode(encoded, expected);
            assertEquals(expected, FieldCommandStatePayload.STREAM_CODEC.decode(encoded), "state round trip");
            assertFalse(encoded.isReadable(), "state decode consumes exactly one packet");
        } finally {
            encoded.release();
        }

        RegistryFriendlyByteBuf oversized = buffer();
        try {
            oversized.writeUUID(UUID.randomUUID());
            oversized.writeVarInt(FieldCommandResult.ACCEPTED.wireId());
            oversized.writeBoolean(false);
            oversized.writeBoolean(false);
            oversized.writeVarInt(0);
            oversized.writeVarInt(FieldCommandStatePayload.MAX_SQUADS + 1);
            assertThrows(IllegalArgumentException.class,
                    () -> FieldCommandStatePayload.STREAM_CODEC.decode(oversized),
                    "oversized squad projection");
        } finally {
            oversized.release();
        }
    }

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertFalse(boolean condition, String label) {
        if (condition) {
            throw new AssertionError(label + " expected false");
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label + " expected true");
        }
    }

    private static <T extends Throwable> void assertThrows(
            Class<T> expectedType,
            ThrowingRunnable action,
            String label
    ) {
        try {
            action.run();
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                return;
            }
            throw new AssertionError(label + " threw " + throwable.getClass().getName()
                    + " instead of " + expectedType.getName(), throwable);
        }
        throw new AssertionError(label + " did not throw");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
