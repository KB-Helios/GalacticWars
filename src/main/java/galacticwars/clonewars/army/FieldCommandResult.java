package galacticwars.clonewars.army;

import java.util.Locale;

/** Result codes sent back to the field-command screen after an authoritative request. */
public enum FieldCommandResult {
    ACCEPTED,
    KINGDOM_UNAVAILABLE,
    PERMISSION_DENIED,
    SQUAD_SELECTION_REQUIRED,
    SQUAD_UNAVAILABLE,
    TARGET_REQUIRED,
    TARGET_UNAVAILABLE,
    INVALID_ACTION,
    ATOMIC_APPLY_FAILED,
    REPLAY_REJECTED;

    public int wireId() {
        return ordinal();
    }

    public String translationKey() {
        return "screen.galacticwars.field_command.reason."
                + name().toLowerCase(Locale.ROOT);
    }

    public static FieldCommandResult fromWireId(int wireId) {
        FieldCommandResult[] values = values();
        if (wireId < 0 || wireId >= values.length) {
            throw new IllegalArgumentException("unknown field command result");
        }
        return values[wireId];
    }
}
