package galacticwars.clonewars.network;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.data.GameplayDataSnapshot;
import galacticwars.clonewars.data.LaunchContentDefinitions;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Bounded client projection of server-owned datapack content needed by gameplay UI.
 * The full authoritative definitions remain server-side.
 */
public record GameplayCatalogPayload(
        long generation,
        String contentHash,
        List<ClassEntry> classes,
        List<VehicleEntry> vehicles,
        List<BlueprintEntry> blueprints
) implements CustomPacketPayload {
    public static final int MAX_CLASSES = 128;
    public static final int MAX_VEHICLES = 64;
    public static final int MAX_BLUEPRINTS = 128;
    public static final int MAX_ABILITIES_PER_CLASS = 8;
    public static final int MAX_REQUIREMENTS_PER_CLASS = 16;
    public static final int MAX_TEXT_BYTES = 192;
    public static final int MAX_REQUIREMENT_AMOUNT = 1_000_000;
    public static final int MAX_VEHICLE_STAT = 1_000_000;
    public static final int MAX_BLUEPRINT_PLACEMENTS = 16_384;
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z0-9_.:/-]+");

    public static final Type<GameplayCatalogPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "gameplay_catalog"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GameplayCatalogPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeVarLong(payload.generation());
                        buffer.writeUtf(payload.contentHash(), 64);
                        writeList(buffer, payload.classes(), MAX_CLASSES, ClassEntry::write);
                        writeList(buffer, payload.vehicles(), MAX_VEHICLES, VehicleEntry::write);
                        writeList(buffer, payload.blueprints(), MAX_BLUEPRINTS, BlueprintEntry::write);
                    },
                    buffer -> new GameplayCatalogPayload(
                            buffer.readVarLong(),
                            buffer.readUtf(64),
                            readList(buffer, MAX_CLASSES, ClassEntry::read),
                            readList(buffer, MAX_VEHICLES, VehicleEntry::read),
                            readList(buffer, MAX_BLUEPRINTS, BlueprintEntry::read)));

    public GameplayCatalogPayload {
        if (generation < 0L) {
            throw new IllegalArgumentException("gameplay catalog generation cannot be negative");
        }
        contentHash = Objects.requireNonNull(contentHash, "contentHash").trim().toLowerCase(Locale.ROOT);
        if ((generation == 0L && !contentHash.isEmpty())
                || (generation > 0L && !contentHash.matches("[0-9a-f]{64}"))) {
            throw new IllegalArgumentException("gameplay catalog content hash is invalid");
        }
        classes = boundedCopy(classes, MAX_CLASSES, "classes");
        vehicles = boundedCopy(vehicles, MAX_VEHICLES, "vehicles");
        blueprints = boundedCopy(blueprints, MAX_BLUEPRINTS, "blueprints");
        requireUnique(classes.stream().map(ClassEntry::classId).toList(), "class id");
        requireUnique(vehicles.stream().map(VehicleEntry::vehicleId).toList(), "vehicle id");
        requireUnique(blueprints.stream().map(BlueprintEntry::blueprintId).toList(), "blueprint id");
    }

    public static GameplayCatalogPayload fromSnapshot(
            GameplayDataSnapshot snapshot,
            long generation,
            String contentHash
    ) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<ClassEntry> classes = snapshot.unitClasses().values().stream()
                .filter(definition -> definition.playerAssignable())
                .sorted(java.util.Comparator.comparing(definition -> definition.id().toString()))
                .map(definition -> new ClassEntry(
                        definition.id().toString(),
                        definition.displayName(),
                        definition.factionId().toString(),
                        definition.forcePathSlot(),
                        definition.requirements().stream()
                                .map(requirement -> new RequirementEntry(
                                        requirement.type(),
                                        requirement.subjectId(),
                                        requirement.amount()))
                                .toList(),
                        definition.abilityIds().stream()
                                .map(snapshot.abilities()::get)
                                .filter(Objects::nonNull)
                                .filter(ability -> ability.enabled())
                                .map(ability -> ability.displayName())
                                .toList()))
                .toList();
        List<VehicleEntry> vehicles = snapshot.launchContent().vehicles().values().stream()
                .sorted(java.util.Comparator.comparing(
                        LaunchContentDefinitions.VehicleDefinition::id))
                .map(definition -> new VehicleEntry(
                        definition.id(), definition.maxHealth(), definition.fuelCapacity()))
                .toList();
        List<BlueprintEntry> blueprints = snapshot.blueprints().values().stream()
                .sorted(java.util.Comparator.comparing(
                        galacticwars.clonewars.settlement.KingdomBaseBlueprint::id))
                .map(definition -> new BlueprintEntry(
                        definition.id(), definition.displayName(), definition.placements().size()))
                .toList();
        return new GameplayCatalogPayload(generation, contentHash, classes, vehicles, blueprints);
    }

    @Override
    public Type<GameplayCatalogPayload> type() {
        return TYPE;
    }

    public record ClassEntry(
            String classId,
            String displayName,
            String factionId,
            String forcePathSlot,
            List<RequirementEntry> requirements,
            List<String> abilityDisplayNames
    ) {
        public ClassEntry {
            classId = identifier(classId, "classId");
            displayName = display(displayName, "displayName");
            factionId = identifier(factionId, "factionId");
            forcePathSlot = Objects.requireNonNull(forcePathSlot, "forcePathSlot")
                    .trim().toLowerCase(Locale.ROOT);
            if (forcePathSlot.equals("light")) {
                forcePathSlot = "jedi";
            } else if (forcePathSlot.equals("dark")) {
                forcePathSlot = "nightsister";
            }
            if (!forcePathSlot.isEmpty()
                    && !Set.of("jedi", "sith", "nightsister").contains(forcePathSlot)) {
                throw new IllegalArgumentException("invalid Force tradition slot " + forcePathSlot);
            }
            requirements = boundedCopy(
                    requirements, MAX_REQUIREMENTS_PER_CLASS, "class requirements");
            abilityDisplayNames = boundedCopy(
                    abilityDisplayNames, MAX_ABILITIES_PER_CLASS, "class abilities").stream()
                    .map(value -> display(value, "ability display name"))
                    .toList();
        }

        private static void write(RegistryFriendlyByteBuf buffer, ClassEntry value) {
            writeText(buffer, value.classId());
            writeText(buffer, value.displayName());
            writeText(buffer, value.factionId());
            writeText(buffer, value.forcePathSlot());
            writeList(buffer, value.requirements(), MAX_REQUIREMENTS_PER_CLASS,
                    RequirementEntry::write);
            writeList(buffer, value.abilityDisplayNames(), MAX_ABILITIES_PER_CLASS,
                    GameplayCatalogPayload::writeText);
        }

        private static ClassEntry read(RegistryFriendlyByteBuf buffer) {
            return new ClassEntry(
                    readText(buffer),
                    readText(buffer),
                    readText(buffer),
                    readText(buffer),
                    readList(buffer, MAX_REQUIREMENTS_PER_CLASS, RequirementEntry::read),
                    readList(buffer, MAX_ABILITIES_PER_CLASS, GameplayCatalogPayload::readText));
        }
    }

    public record RequirementEntry(String type, String subjectId, int amount) {
        public RequirementEntry {
            type = identifier(type, "requirement type");
            subjectId = identifier(subjectId, "requirement subject");
            if (amount < 1 || amount > MAX_REQUIREMENT_AMOUNT) {
                throw new IllegalArgumentException("requirement amount is outside the supported range");
            }
        }

        private static void write(RegistryFriendlyByteBuf buffer, RequirementEntry value) {
            writeText(buffer, value.type());
            writeText(buffer, value.subjectId());
            buffer.writeVarInt(value.amount());
        }

        private static RequirementEntry read(RegistryFriendlyByteBuf buffer) {
            return new RequirementEntry(readText(buffer), readText(buffer), buffer.readVarInt());
        }
    }

    public record VehicleEntry(String vehicleId, int maxHealth, int fuelCapacity) {
        public VehicleEntry {
            vehicleId = identifier(vehicleId, "vehicleId");
            if (maxHealth < 1 || maxHealth > MAX_VEHICLE_STAT
                    || fuelCapacity < 1 || fuelCapacity > MAX_VEHICLE_STAT) {
                throw new IllegalArgumentException("vehicle HUD maxima are outside the supported range");
            }
        }

        private static void write(RegistryFriendlyByteBuf buffer, VehicleEntry value) {
            writeText(buffer, value.vehicleId());
            buffer.writeVarInt(value.maxHealth());
            buffer.writeVarInt(value.fuelCapacity());
        }

        private static VehicleEntry read(RegistryFriendlyByteBuf buffer) {
            return new VehicleEntry(readText(buffer), buffer.readVarInt(), buffer.readVarInt());
        }
    }

    public record BlueprintEntry(String blueprintId, String displayName, int placementCount) {
        public BlueprintEntry {
            blueprintId = identifier(blueprintId, "blueprintId");
            displayName = display(displayName, "blueprint display name");
            if (placementCount < 1 || placementCount > MAX_BLUEPRINT_PLACEMENTS) {
                throw new IllegalArgumentException("blueprint placement count is outside the supported range");
            }
        }

        private static void write(RegistryFriendlyByteBuf buffer, BlueprintEntry value) {
            writeText(buffer, value.blueprintId());
            writeText(buffer, value.displayName());
            buffer.writeVarInt(value.placementCount());
        }

        private static BlueprintEntry read(RegistryFriendlyByteBuf buffer) {
            return new BlueprintEntry(readText(buffer), readText(buffer), buffer.readVarInt());
        }
    }

    private static String identifier(String value, String label) {
        String normalized = Objects.requireNonNull(value, label).trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || !IDENTIFIER.matcher(normalized).matches()) {
            throw new IllegalArgumentException(label + " is not a valid identifier");
        }
        requireUtf8Bound(normalized, label);
        return normalized;
    }

    private static String display(String value, String label) {
        String normalized = Objects.requireNonNull(value, label).trim();
        if (normalized.isEmpty() || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(label + " is not safe display text");
        }
        requireUtf8Bound(normalized, label);
        return normalized;
    }

    private static void requireUtf8Bound(String value, String label) {
        if (value.getBytes(StandardCharsets.UTF_8).length > MAX_TEXT_BYTES) {
            throw new IllegalArgumentException(label + " exceeds the payload text bound");
        }
    }

    private static void writeText(RegistryFriendlyByteBuf buffer, String value) {
        buffer.writeUtf(value, MAX_TEXT_BYTES);
    }

    private static String readText(RegistryFriendlyByteBuf buffer) {
        return buffer.readUtf(MAX_TEXT_BYTES);
    }

    private static <T> void writeList(
            RegistryFriendlyByteBuf buffer,
            List<T> values,
            int maximum,
            BiConsumer<RegistryFriendlyByteBuf, T> writer
    ) {
        if (values.size() > maximum) {
            throw new IllegalArgumentException("payload list exceeds its entry bound");
        }
        buffer.writeVarInt(values.size());
        values.forEach(value -> writer.accept(buffer, value));
    }

    private static <T> List<T> readList(
            RegistryFriendlyByteBuf buffer,
            int maximum,
            Function<RegistryFriendlyByteBuf, T> reader
    ) {
        int size = buffer.readVarInt();
        if (size < 0 || size > maximum) {
            throw new IllegalArgumentException("payload list size is outside its entry bound");
        }
        ArrayList<T> values = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            values.add(reader.apply(buffer));
        }
        return List.copyOf(values);
    }

    private static <T> List<T> boundedCopy(List<T> values, int maximum, String label) {
        List<T> copy = List.copyOf(Objects.requireNonNull(values, label));
        if (copy.size() > maximum) {
            throw new IllegalArgumentException(label + " exceeds " + maximum + " entries");
        }
        return copy;
    }

    private static void requireUnique(List<String> values, String label) {
        if (new HashSet<>(values).size() != values.size()) {
            throw new IllegalArgumentException("duplicate " + label + " in gameplay catalog payload");
        }
    }
}
