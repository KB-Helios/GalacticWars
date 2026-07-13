package galacticwars.clonewars.conquest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.data.SavedDataSchemaPolicy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class ConquestSavedData extends SavedData {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Codec<ConquestControlState> STATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("region_id").forGetter(ConquestControlState::regionId),
            Codec.STRING.fieldOf("dimension_id").forGetter(ConquestControlState::dimensionId),
            Codec.INT.fieldOf("beacon_x").forGetter(ConquestControlState::beaconX),
            Codec.INT.fieldOf("beacon_y").forGetter(ConquestControlState::beaconY),
            Codec.INT.fieldOf("beacon_z").forGetter(ConquestControlState::beaconZ),
            Codec.STRING.optionalFieldOf("controlling_faction", "").forGetter(ConquestControlState::controllingFaction),
            Codec.STRING.optionalFieldOf("controlling_kingdom", "").forGetter(ConquestControlState::controllingKingdom),
            Codec.STRING.optionalFieldOf("capturing_player", "").forGetter(ConquestControlState::capturingPlayer),
            Codec.INT.optionalFieldOf("progress", 0).forGetter(ConquestControlState::progress),
            Codec.LONG.optionalFieldOf("revision", 0L).forGetter(ConquestControlState::revision)
    ).apply(instance, ConquestControlState::new));
    public static final Codec<ConquestSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION)
                    .forGetter(data -> CURRENT_SCHEMA_VERSION),
            STATE_CODEC.listOf().optionalFieldOf("regions", List.of()).forGetter(ConquestSavedData::states)
    ).apply(instance, ConquestSavedData::new));
    public static final SavedDataType<ConquestSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "conquest_control"),
            ConquestSavedData::new, CODEC);

    private final Map<String, ConquestControlState> byRegion = new LinkedHashMap<>();

    public ConquestSavedData() {
    }

    private ConquestSavedData(int schemaVersion, List<ConquestControlState> states) {
        SavedDataSchemaPolicy.migrate(schemaVersion, CURRENT_SCHEMA_VERSION, "conquest");
        states.forEach(state -> byRegion.put(state.regionId(), state));
    }

    public static ConquestSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<ConquestControlState> state(String regionId) {
        return Optional.ofNullable(byRegion.get(regionId));
    }

    public void put(ConquestControlState state) {
        byRegion.put(state.regionId(), state);
        setDirty();
    }

    public List<ConquestControlState> states() {
        return List.copyOf(byRegion.values());
    }
}
