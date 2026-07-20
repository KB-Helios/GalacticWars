package galacticwars.clonewars.army;

/** Commands exposed through the in-world, player-owned field command panel. */
public enum FieldCommandAction {
    REFRESH,
    FOLLOW,
    HOLD,
    MOVE_TO_MARKER,
    FACE_FORWARD,
    ADVANCE,
    RETREAT,
    RETURN_TO_RALLY,
    PROTECT_OWNER,
    PROTECT_MARKED_ENTITY,
    ATTACK_MARKED_TARGET,
    CLEAR_TARGET,
    CYCLE_FORMATION,
    TOGGLE_HOLD_FORMATION,
    TOGGLE_TIGHT_FORMATION,
    CYCLE_ENGAGEMENT,
    CYCLE_TARGET_PRIORITY,
    CYCLE_RANGED_FIRE,
    PATROL_MARKER,
    PAUSE_PATROL,
    RESUME_PATROL,
    STOP_PATROL,
    CYCLE_PATROL_MODE,
    CYCLE_PATROL_SPEED,
    CYCLE_PATROL_ENEMY_POLICY,
    RENAME_PATROL_ROUTE,
    SET_PATROL_WAYPOINT_WAIT;

    public int wireId() {
        return ordinal();
    }

    public static FieldCommandAction fromWireId(int wireId) {
        FieldCommandAction[] values = values();
        if (wireId < 0 || wireId >= values.length) {
            throw new IllegalArgumentException("unknown field command action");
        }
        return values[wireId];
    }
}
