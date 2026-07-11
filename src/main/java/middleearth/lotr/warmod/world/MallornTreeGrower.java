package middleearth.lotr.warmod.world;

import java.util.Optional;

import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public final class MallornTreeGrower {
    public static final ResourceKey<ConfiguredFeature<?, ?>> MALLORN_TREE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            Identifier.fromNamespaceAndPath(KingdomWarsMiddleEarth.MODID, "mallorn_tree"));
    public static final TreeGrower INSTANCE = new TreeGrower(
            "kingdomwarsmiddleearth:mallorn",
            Optional.empty(),
            Optional.of(MALLORN_TREE),
            Optional.empty());

    private MallornTreeGrower() {
    }
}
