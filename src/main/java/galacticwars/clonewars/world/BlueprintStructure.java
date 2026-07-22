package galacticwars.clonewars.world;

import com.mojang.serialization.MapCodec;
import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.registry.ModWorldgenTypes;
import galacticwars.clonewars.settlement.BaseBlockPlacement;
import galacticwars.clonewars.settlement.KingdomBaseBlueprint;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** One sparse structure type selecting a data-defined faction template for the local biome. */
public final class BlueprintStructure extends Structure {
    public static final MapCodec<BlueprintStructure> CODEC = simpleCodec(BlueprintStructure::new);

    public BlueprintStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        int y = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        Holder<?> biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y),
                QuartPos.fromBlock(z), context.randomState().sampler());
        String biomeId = biome.unwrapKey().map(key -> key.identifier().toString()).orElse("");
        List<KingdomBaseBlueprint> eligible = GameplayDataManager.snapshot().blueprints().values().stream()
                .filter(blueprint -> blueprint.worldgen().isPresent())
                .filter(blueprint -> blueprint.worldgen().orElseThrow().biomes().contains(biomeId))
                .filter(blueprint -> y >= blueprint.terrainConstraints().minY()
                        && y <= blueprint.terrainConstraints().maxY())
                .sorted(Comparator.comparing(KingdomBaseBlueprint::id))
                .toList();
        if (eligible.isEmpty()) {
            return Optional.empty();
        }
        RandomSource random = RandomSource.create(context.seed()
                ^ ((long) context.chunkPos().x() * 341873128712L)
                ^ ((long) context.chunkPos().z() * 132897987541L));
        int totalWeight = eligible.stream().mapToInt(value -> value.worldgen().orElseThrow().placementWeight()).sum();
        int selectedWeight = random.nextInt(totalWeight);
        KingdomBaseBlueprint selected = eligible.getFirst();
        for (KingdomBaseBlueprint candidate : eligible) {
            selectedWeight -= candidate.worldgen().orElseThrow().placementWeight();
            if (selectedWeight < 0) {
                selected = candidate;
                break;
            }
        }
        int degrees = selected.allowedRotations().get(random.nextInt(selected.allowedRotations().size()));
        int rotationSteps = degrees / 90;
        int minX = selected.placements().stream().mapToInt(BaseBlockPlacement::x).min()
                .orElse(selected.anchor().x());
        int maxX = selected.placements().stream().mapToInt(BaseBlockPlacement::x).max()
                .orElse(selected.anchor().x());
        int minZ = selected.placements().stream().mapToInt(BaseBlockPlacement::z).min()
                .orElse(selected.anchor().z());
        int maxZ = selected.placements().stream().mapToInt(BaseBlockPlacement::z).max()
                .orElse(selected.anchor().z());
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int templateX : new int[]{minX, maxX}) {
            for (int templateZ : new int[]{minZ, maxZ}) {
                int relativeX = templateX - selected.anchor().x();
                int relativeZ = templateZ - selected.anchor().z();
                int dx = switch (rotationSteps) {
                    case 1 -> -relativeZ;
                    case 2 -> -relativeX;
                    case 3 -> relativeZ;
                    default -> relativeX;
                };
                int dz = switch (rotationSteps) {
                    case 1 -> relativeX;
                    case 2 -> -relativeZ;
                    case 3 -> -relativeX;
                    default -> relativeZ;
                };
                int corner = context.chunkGenerator().getBaseHeight(x + dx, z + dz,
                        Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                min = Math.min(min, corner);
                max = Math.max(max, corner);
            }
        }
        if (max - min > selected.terrainConstraints().maxSlope()) {
            return Optional.empty();
        }
        BlockPos position = new BlockPos(x - selected.anchor().x(), y - selected.anchor().y(),
                z - selected.anchor().z());
        KingdomBaseBlueprint blueprint = selected;
        return Optional.of(new GenerationStub(new BlockPos(x, y, z), builder -> builder.addPiece(
                new BlueprintStructurePiece(context.structureTemplateManager(), blueprint, rotationSteps, position))));
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgenTypes.BLUEPRINT_STRUCTURE.get();
    }
}
