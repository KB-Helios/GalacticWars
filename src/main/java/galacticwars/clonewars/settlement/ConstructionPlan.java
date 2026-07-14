package galacticwars.clonewars.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** Server-authored instructions carried by a physical blueprint projector. */
public record ConstructionPlan(
        String blueprintId,
        int rotationSteps,
        UUID builderId,
        UUID kingdomId
) {
    public static final Codec<ConstructionPlan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("blueprint_id").forGetter(ConstructionPlan::blueprintId),
            Codec.intRange(0, 3).fieldOf("rotation_steps").forGetter(ConstructionPlan::rotationSteps),
            UUIDUtil.CODEC.fieldOf("builder_id").forGetter(ConstructionPlan::builderId),
            UUIDUtil.CODEC.fieldOf("kingdom_id").forGetter(ConstructionPlan::kingdomId)
    ).apply(instance, ConstructionPlan::new));

    public ConstructionPlan {
        blueprintId = KingdomBaseBlueprint.canonicalId(blueprintId);
        if (rotationSteps < 0 || rotationSteps > 3) {
            throw new IllegalArgumentException("rotationSteps must be between 0 and 3");
        }
        builderId = Objects.requireNonNull(builderId, "builderId");
        kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
    }
}
