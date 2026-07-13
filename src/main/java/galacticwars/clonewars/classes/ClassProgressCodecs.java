package galacticwars.clonewars.classes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

public final class ClassProgressCodecs {
    public static final Codec<ClassProgressState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", ClassProgressState.CURRENT_SCHEMA_VERSION)
                    .forGetter(ClassProgressState::schemaVersion),
            Codec.STRING.optionalFieldOf("class_id", "").forGetter(ClassProgressState::classId),
            Codec.INT.optionalFieldOf("rank", 0).forGetter(ClassProgressState::rank),
            Codec.LONG.optionalFieldOf("experience", 0L).forGetter(ClassProgressState::experience),
            Codec.INT.optionalFieldOf("resource", ClassProgressState.MAX_RESOURCE)
                    .forGetter(ClassProgressState::resource),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("cooldown_ends", Map.of())
                    .forGetter(ClassProgressState::cooldownEnds)
    ).apply(instance, ClassProgressState::new));

    private ClassProgressCodecs() {
    }
}
