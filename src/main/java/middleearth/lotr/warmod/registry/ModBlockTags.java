package middleearth.lotr.warmod.registry;

import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags {
    public static final TagKey<Block> WORKER_LOGS = create("worker_logs");
    public static final TagKey<Block> WORKER_MINEABLE = create("worker_mineable");

    private ModBlockTags() {
    }

    private static TagKey<Block> create(String path) {
        return TagKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(KingdomWarsMiddleEarth.MODID, path));
    }
}
