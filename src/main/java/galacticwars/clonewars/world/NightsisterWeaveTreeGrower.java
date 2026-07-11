package galacticwars.clonewars.world;

import java.util.Optional;

import galacticwars.clonewars.GalacticWars;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public final class NightsisterWeaveTreeGrower {
    public static final ResourceKey<ConfiguredFeature<?, ?>> NIGHTSISTER_WEAVE_TREE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "nightsister_weave_tree"));
    public static final TreeGrower INSTANCE = new TreeGrower(
            "galacticwars:nightsister_weave",
            Optional.empty(),
            Optional.of(NIGHTSISTER_WEAVE_TREE),
            Optional.empty());

    private NightsisterWeaveTreeGrower() {
    }
}
