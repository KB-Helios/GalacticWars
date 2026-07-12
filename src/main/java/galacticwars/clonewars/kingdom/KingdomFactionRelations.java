package galacticwars.clonewars.kingdom;

import galacticwars.clonewars.faction.FactionCatalog;
import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.faction.FactionRelation;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Resolves player-kingdom diplomacy before falling back to faction defaults. */
public final class KingdomFactionRelations {
    private KingdomFactionRelations() {
    }

    public static FactionRelation resolve(
            FactionCatalog factions,
            KingdomSavedData kingdoms,
            @Nullable UUID firstKingdomId,
            FactionId firstFaction,
            @Nullable UUID secondKingdomId,
            FactionId secondFaction,
            long gameTime
    ) {
        Objects.requireNonNull(factions, "factions");
        Objects.requireNonNull(kingdoms, "kingdoms");
        Objects.requireNonNull(firstFaction, "firstFaction");
        Objects.requireNonNull(secondFaction, "secondFaction");
        if (firstKingdomId != null && firstKingdomId.equals(secondKingdomId)) {
            return FactionRelation.SAME;
        }
        if (firstKingdomId != null && secondKingdomId != null) {
            KingdomDiplomacy diplomacy = kingdoms.diplomacyBetween(firstKingdomId, secondKingdomId).orElse(null);
            if (diplomacy != null) {
                return switch (diplomacy.effectiveRelation(gameTime)) {
                    case ALLY -> FactionRelation.ALLY;
                    case ENEMY -> FactionRelation.ENEMY;
                    case NEUTRAL -> FactionRelation.NEUTRAL;
                };
            }
        }
        return factions.relation(firstFaction, secondFaction);
    }
}
