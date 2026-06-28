package middleearth.lotr.warmod.faction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FactionAlignmentUpdater {
    private FactionAlignmentUpdater() {
    }

    public static FactionAlignmentUpdateResult apply(
            FactionAlignment alignment,
            FactionCatalog catalog,
            FactionId sourceFaction,
            FactionAlignmentRule rule
    ) {
        Objects.requireNonNull(alignment, "alignment");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(sourceFaction, "sourceFaction");
        Objects.requireNonNull(rule, "rule");
        FactionDefinition sourceDefinition = catalog.definition(sourceFaction)
                .orElseThrow(() -> new IllegalArgumentException("Unknown source faction: " + sourceFaction));

        FactionAlignment updated = alignment;
        ArrayList<FactionAlignmentChange> changes = new ArrayList<>();
        AlignmentApplication direct = applyDelta(updated, sourceFaction, rule.directDelta(), rule.reasonCode());
        updated = direct.alignment();
        changes.addAll(direct.changes());

        for (FactionId ally : sourceDefinition.allies()) {
            AlignmentApplication application = applyRelatedDelta(
                    updated, catalog, sourceFaction, ally, rule.allyDelta(), rule.reasonCode());
            updated = application.alignment();
            changes.addAll(application.changes());
        }

        for (FactionId enemy : sourceDefinition.enemies()) {
            AlignmentApplication application = applyRelatedDelta(
                    updated, catalog, sourceFaction, enemy, rule.enemyDelta(), rule.reasonCode());
            updated = application.alignment();
            changes.addAll(application.changes());
        }

        return new FactionAlignmentUpdateResult(updated, changes);
    }

    private static AlignmentApplication applyRelatedDelta(
            FactionAlignment alignment,
            FactionCatalog catalog,
            FactionId sourceFaction,
            FactionId relatedFaction,
            int delta,
            String reasonCode
    ) {
        if (relatedFaction.equals(sourceFaction) || !catalog.contains(relatedFaction)) {
            return new AlignmentApplication(alignment, List.of());
        }
        return applyDelta(alignment, relatedFaction, delta, reasonCode);
    }

    private static AlignmentApplication applyDelta(
            FactionAlignment alignment,
            FactionId factionId,
            int delta,
            String reasonCode
    ) {
        if (delta == 0) {
            return new AlignmentApplication(alignment, List.of());
        }

        int beforeScore = alignment.score(factionId);
        FactionAlignment updated = alignment.withAddedScore(factionId, delta);
        return new AlignmentApplication(updated, List.of(new FactionAlignmentChange(
                factionId,
                beforeScore,
                delta,
                updated.score(factionId),
                reasonCode)));
    }

    private record AlignmentApplication(FactionAlignment alignment, List<FactionAlignmentChange> changes) {
    }
}
