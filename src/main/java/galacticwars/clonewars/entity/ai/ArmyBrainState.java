package galacticwars.clonewars.entity.ai;

import galacticwars.clonewars.army.ArmyCommand;
import galacticwars.clonewars.army.ArmyGroupRecord;
import galacticwars.clonewars.army.ArmyPosition;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A transient projection of a persisted squad order for one loaded recruit.
 *
 * <p>This is intentionally not serialized. It is rebuilt by
 * {@link ArmyGroupStateSensor} from {@code KingdomSavedData} so unloads,
 * reloads, and cross-dimension travel cannot create a competing army source
 * of truth.</p>
 */
public record ArmyBrainState(
        ArmyGroupRecord group,
        ArmyCommand memberCommand,
        ArmyPosition behaviorAnchor,
        ArmyPosition fallbackPosition,
        int followRange,
        boolean commander,
        @Nullable UUID selectedTargetId,
        @Nullable UUID retreatThreatId
) {
    public ArmyBrainState {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(memberCommand, "memberCommand");
        Objects.requireNonNull(behaviorAnchor, "behaviorAnchor");
        Objects.requireNonNull(fallbackPosition, "fallbackPosition");
        if (followRange < 1) {
            throw new IllegalArgumentException("followRange must be positive");
        }
    }

    public ArmyBrainState withSelectedTarget(@Nullable UUID targetId) {
        return new ArmyBrainState(
                group, memberCommand, behaviorAnchor, fallbackPosition, followRange, commander, targetId, null);
    }

    /**
     * Records a seen hostile that should make the squad fall back instead of
     * placing it in the normal attack-target memory.
     */
    public ArmyBrainState withRetreatThreat(@Nullable UUID targetId) {
        return new ArmyBrainState(
                group, memberCommand, behaviorAnchor, fallbackPosition, followRange, commander, null, targetId);
    }
}
