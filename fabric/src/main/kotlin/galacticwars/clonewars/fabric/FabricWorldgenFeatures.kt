package galacticwars.clonewars.fabric

import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import java.util.concurrent.atomic.AtomicBoolean

/** Fabric counterpart to the NeoForge add_features datapack modifiers. */
object FabricWorldgenFeatures {
    private val registered = AtomicBoolean()

    @JvmStatic
    fun register() {
        if (!registered.compareAndSet(false, true)) return
        addFeature("galacticwars:beskar_ore", GenerationStep.Decoration.UNDERGROUND_ORES,
            "minecraft:stony_peaks", "minecraft:jagged_peaks", "minecraft:badlands", "minecraft:wooded_badlands")
        addFeature("galacticwars:nightsister_weave_grove", GenerationStep.Decoration.VEGETAL_DECORATION,
            "minecraft:dark_forest", "minecraft:swamp", "minecraft:old_growth_pine_taiga")
    }

    private fun addFeature(featureId: String, step: GenerationStep.Decoration, vararg biomeIds: String) {
        val feature = ResourceKey.create(Registries.PLACED_FEATURE, Identifier.parse(featureId))
        val biomes: List<ResourceKey<Biome>> = biomeIds.map { id ->
            ResourceKey.create(Registries.BIOME, Identifier.parse(id))
        }
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(biomes), step, feature)
    }
}
