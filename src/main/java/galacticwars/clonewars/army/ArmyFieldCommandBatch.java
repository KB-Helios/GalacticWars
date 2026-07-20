package galacticwars.clonewars.army;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Pure preflight for a multi-squad field command. It creates no partial
 * result: callers either receive every replacement and expected revision, or
 * receive an empty result and must leave SavedData untouched.
 */
public record ArmyFieldCommandBatch(
        List<ArmyGroupRecord> replacements,
        Map<java.util.UUID, Long> expectedRevisions
) {
    public static final int MAX_GROUPS = 8;

    public ArmyFieldCommandBatch {
        replacements = List.copyOf(Objects.requireNonNull(replacements, "replacements"));
        expectedRevisions = Map.copyOf(Objects.requireNonNull(expectedRevisions, "expectedRevisions"));
        if (replacements.isEmpty() || replacements.size() > MAX_GROUPS
                || expectedRevisions.size() != replacements.size()) {
            throw new IllegalArgumentException("field command batch must contain 1-" + MAX_GROUPS + " groups");
        }
    }

    public static Optional<ArmyFieldCommandBatch> prepare(
            Collection<ArmyGroupRecord> groups,
            Function<ArmyGroupRecord, ArmyGroupRecord> updateFactory
    ) {
        Objects.requireNonNull(groups, "groups");
        Objects.requireNonNull(updateFactory, "updateFactory");
        if (groups.isEmpty() || groups.size() > MAX_GROUPS) {
            return Optional.empty();
        }

        ArrayList<ArmyGroupRecord> replacements = new ArrayList<>(groups.size());
        LinkedHashMap<java.util.UUID, Long> expectedRevisions = new LinkedHashMap<>();
        for (ArmyGroupRecord group : groups) {
            if (group == null || expectedRevisions.putIfAbsent(group.id(), group.simulation().revision()) != null) {
                return Optional.empty();
            }
            ArmyGroupRecord replacement = updateFactory.apply(group);
            if (!isValidReplacement(group, replacement)) {
                return Optional.empty();
            }
            replacements.add(replacement);
        }
        return Optional.of(new ArmyFieldCommandBatch(replacements, expectedRevisions));
    }

    private static boolean isValidReplacement(ArmyGroupRecord original, ArmyGroupRecord replacement) {
        return replacement != null
                && original.id().equals(replacement.id())
                && original.ownerId().equals(replacement.ownerId())
                && original.kingdomId().equals(replacement.kingdomId())
                && original.commanderId().equals(replacement.commanderId())
                && original.memberIds().equals(replacement.memberIds())
                && replacement.simulation().revision() == original.simulation().revision() + 1L
                && ArmyCommandPolicy.canIssue(
                        replacement.order().toCommand(original.ownerId(), original.id()),
                        original.commandValidationState()).accepted();
    }
}
