package galacticwars.clonewars.registry;

import galacticwars.clonewars.GalacticWars;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags {
    public static final TagKey<Block> WORKER_LOGS = create("worker_logs");
    public static final TagKey<Block> WORKER_MINEABLE = create("worker_mineable");
    public static final TagKey<Block> FORCE_MOVABLE = create("force_movable");
    public static final TagKey<Block> FORCE_IMMOVABLE = create("force_immovable");

    private ModBlockTags() {
    }

    private static TagKey<Block> create(String path) {
        return TagKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(GalacticWars.MODID, path));
    }
}
