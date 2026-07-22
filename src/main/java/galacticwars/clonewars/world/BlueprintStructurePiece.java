package galacticwars.clonewars.world;

import galacticwars.clonewars.registry.ModBlocks;
import galacticwars.clonewars.registry.ModWorldgenTypes;
import galacticwars.clonewars.settlement.BlueprintAnchor;
import galacticwars.clonewars.settlement.KingdomBaseBlueprint;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public final class BlueprintStructurePiece extends TemplateStructurePiece {
    private final String blueprintId;
    private final int rotationSteps;
    private final String contentHash;

    public BlueprintStructurePiece(
            StructureTemplateManager manager,
            KingdomBaseBlueprint blueprint,
            int rotationSteps,
            BlockPos position
    ) {
        super(ModWorldgenTypes.BLUEPRINT_STRUCTURE_PIECE.get(), 0, manager,
                Identifier.parse(blueprint.templateId()), blueprint.templateId(),
                settings(rotationSteps, blueprint.anchor()), position);
        this.blueprintId = blueprint.id();
        this.rotationSteps = rotationSteps;
        this.contentHash = blueprint.contentHash();
    }

    public BlueprintStructurePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModWorldgenTypes.BLUEPRINT_STRUCTURE_PIECE.get(), tag, context.structureTemplateManager(),
                ignored -> new StructurePlaceSettings().setIgnoreEntities(true));
        this.blueprintId = tag.getStringOr("BlueprintId", "galacticwars:republic_field_base");
        this.rotationSteps = tag.getIntOr("BlueprintRotation", 0);
        this.contentHash = tag.getStringOr("BlueprintContentHash", "");
    }

    private static StructurePlaceSettings settings(int rotationSteps, BlueprintAnchor anchor) {
        return new StructurePlaceSettings()
                .setIgnoreEntities(true)
                .setRotation(switch (Math.floorMod(rotationSteps, 4)) {
                    case 1 -> Rotation.CLOCKWISE_90;
                    case 2 -> Rotation.CLOCKWISE_180;
                    case 3 -> Rotation.COUNTERCLOCKWISE_90;
                    default -> Rotation.NONE;
                })
                .setRotationPivot(new BlockPos(anchor.x(), anchor.y(), anchor.z()));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("BlueprintId", blueprintId);
        tag.putInt("BlueprintRotation", rotationSteps);
        tag.putString("BlueprintContentHash", contentHash);
    }

    @Override
    protected void handleDataMarker(
            String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box
    ) {
        if (marker.equals("site_anchor")) {
            level.setBlock(pos, ModBlocks.BLUEPRINT_SITE_ANCHOR.get().defaultBlockState(), 2);
            if (level.getBlockEntity(pos) instanceof BlueprintSiteAnchorBlockEntity anchor) {
                anchor.configure(blueprintId, rotationSteps, contentHash);
            }
            return;
        }
        if (marker.startsWith("loot:")) {
            level.setBlock(pos, ModBlocks.BLUEPRINT_SITE_LOOT.get().defaultBlockState(), 2);
            return;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    }
}
