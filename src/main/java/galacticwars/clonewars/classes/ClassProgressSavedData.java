package galacticwars.clonewars.classes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.data.SavedDataSchemaPolicy;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

public final class ClassProgressSavedData extends SavedData {
    private static final Codec<ActorState> ACTOR_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("actor_id").forGetter(ActorState::actorId),
            ClassProgressCodecs.CODEC.fieldOf("progress").forGetter(ActorState::progress)
    ).apply(instance, ActorState::new));

    public static final Codec<ClassProgressSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", ClassProgressState.CURRENT_SCHEMA_VERSION)
                    .forGetter(data -> ClassProgressState.CURRENT_SCHEMA_VERSION),
            ACTOR_CODEC.listOf().optionalFieldOf("actors", List.of())
                    .forGetter(ClassProgressSavedData::serialized)
    ).apply(instance, ClassProgressSavedData::new));

    public static final SavedDataType<ClassProgressSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "class_progress"),
            ClassProgressSavedData::new,
            CODEC);

    private final Map<UUID, ClassProgressState> states = new LinkedHashMap<>();

    public ClassProgressSavedData() {
    }

    private ClassProgressSavedData(int schemaVersion, List<ActorState> actors) {
        SavedDataSchemaPolicy.migrate(
                schemaVersion, ClassProgressState.CURRENT_SCHEMA_VERSION, "class_progress");
        for (ActorState actor : actors) {
            states.put(actor.actorId(), actor.progress());
        }
    }

    public static ClassProgressSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public ClassProgressState state(UUID actorId) {
        return states.getOrDefault(actorId, ClassProgressState.unassigned());
    }

    public ClassProgressState update(UUID actorId, UnaryOperator<ClassProgressState> action) {
        ClassProgressState before = state(actorId);
        ClassProgressState after = action.apply(before);
        if (!after.equals(before)) {
            states.put(actorId, after);
            setDirty();
        }
        return after;
    }

    private List<ActorState> serialized() {
        ArrayList<ActorState> actors = new ArrayList<>();
        states.forEach((actorId, progress) -> actors.add(new ActorState(actorId, progress)));
        return List.copyOf(actors);
    }

    private record ActorState(UUID actorId, ClassProgressState progress) {
    }
}
