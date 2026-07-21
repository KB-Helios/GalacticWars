package galacticwars.clonewars.kingdom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import galacticwars.clonewars.recruitment.NpcServiceBranch;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/** Dependency-light persisted fixtures that do not require the datapack reload runtime. */
final class KingdomTestFixtures {
    private KingdomTestFixtures() {
    }

    static KingdomSavedData withCivilianRecruit(
            UUID ownerId,
            UUID recruitId,
            String factionId,
            String dimensionId,
            BlockPos hallPosition
    ) {
        KingdomSavedData seed = new KingdomSavedData();
        KingdomRecord founded = seed.foundKingdom(
                ownerId, factionId, dimensionId, hallPosition);
        KingdomRecord populated = founded
                .withSettlement(founded.settlement().withRecruit(recruitId))
                .withNpcBranch(recruitId, NpcServiceBranch.CIVILIAN);

        JsonArray kingdoms = new JsonArray();
        kingdoms.add(KingdomCodecs.KINGDOM_RECORD
                .encodeStart(JsonOps.INSTANCE, populated)
                .getOrThrow());
        JsonObject persisted = new JsonObject();
        persisted.addProperty("schema_version", KingdomSavedData.CURRENT_SCHEMA_VERSION);
        persisted.add("kingdoms", kingdoms);
        return KingdomSavedData.CODEC.parse(JsonOps.INSTANCE, persisted).getOrThrow();
    }
}
