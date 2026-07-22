package galacticwars.clonewars.fabric

import galacticwars.clonewars.registry.ModEntityTypes
import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.biome.Biome
import java.util.concurrent.atomic.AtomicBoolean

/** Fabric equivalents of the NeoForge data-driven Overworld biome modifiers. */
object FabricBiomeSpawns {
    private val registered = AtomicBoolean()

    @JvmStatic
    fun register() {
        if (!registered.compareAndSet(false, true)) return

        addSpawn(ModEntityTypes.ARC_TROOPER.get(), 2, 1, 1,
            "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow")
        addSpawn(ModEntityTypes.B1_SECURITY_DROID.get(), 3, 1, 3,
            "minecraft:desert", "minecraft:badlands", "minecraft:wooded_badlands")
        addSpawn(ModEntityTypes.B1_BATTLE_DROID.get(), 8, 2, 5,
            "minecraft:desert", "minecraft:badlands", "minecraft:wooded_badlands")
        addSpawn(ModEntityTypes.B2_SUPER_BATTLE_DROID.get(), 2, 1, 2,
            "minecraft:desert", "minecraft:badlands", "minecraft:wooded_badlands")
        addSpawn(ModEntityTypes.CLONE_TROOPER.get(), 10, 2, 4,
            "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow")
        addSpawn(ModEntityTypes.HUTT_CIVILIAN.get(), 6, 1, 3,
            "minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:desert")
        addSpawn(ModEntityTypes.HUTT_ENFORCER.get(), 9, 1, 3,
            "minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:desert")
        addSpawn(ModEntityTypes.MANDALORIAN_CLANSPERSON.get(), 4, 1, 2,
            "minecraft:stony_peaks", "minecraft:jagged_peaks", "minecraft:windswept_hills")
        addSpawn(ModEntityTypes.MANDALORIAN_MARKSMAN.get(), 3, 1, 2,
            "minecraft:stony_peaks", "minecraft:jagged_peaks", "minecraft:windswept_hills")
        addSpawn(ModEntityTypes.MANDALORIAN_WARRIOR.get(), 10, 1, 3,
            "minecraft:stony_peaks", "minecraft:jagged_peaks", "minecraft:windswept_hills")
        addSpawn(ModEntityTypes.NIGHTSISTER_ACOLYTE.get(), 5, 1, 3,
            "minecraft:dark_forest", "minecraft:swamp", "minecraft:old_growth_pine_taiga")
        addSpawn(ModEntityTypes.NIGHTSISTER_ARCHER.get(), 5, 1, 3,
            "minecraft:dark_forest", "minecraft:swamp", "minecraft:old_growth_pine_taiga")
        addSpawn(ModEntityTypes.NIGHTSISTER_CIVILIAN.get(), 5, 1, 3,
            "minecraft:dark_forest", "minecraft:swamp", "minecraft:old_growth_pine_taiga")
        addSpawn(ModEntityTypes.REPUBLIC_CIVILIAN.get(), 5, 1, 3,
            "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow")
        addSpawn(ModEntityTypes.REPUBLIC_HONOR_GUARD.get(), 1, 1, 1,
            "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow")
        addSpawn(ModEntityTypes.SENATE_COMMANDO.get(), 2, 1, 2,
            "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow")
        addSpawn(ModEntityTypes.SEPARATIST_TECHNICIAN.get(), 5, 1, 3,
            "minecraft:desert", "minecraft:badlands", "minecraft:wooded_badlands")
        addSpawn(ModEntityTypes.SITH_ACOLYTE.get(), 2, 1, 1,
            "minecraft:desert", "minecraft:badlands", "minecraft:wooded_badlands")
        addSpawn(ModEntityTypes.SMUGGLER.get(), 5, 1, 3,
            "minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:desert")
        addSpawn(ModEntityTypes.TOGRUTA_CIVILIAN.get(), 3, 1, 3,
            "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow")
    }

    private fun addSpawn(
        entityType: EntityType<*>,
        weight: Int,
        minimumGroupSize: Int,
        maximumGroupSize: Int,
        vararg biomeIds: String,
    ) {
        require(weight > 0) { "spawn weight must be positive" }
        require(minimumGroupSize > 0 && maximumGroupSize >= minimumGroupSize) {
            "invalid spawn group range $minimumGroupSize..$maximumGroupSize"
        }
        val biomeKeys: List<ResourceKey<Biome>> = biomeIds.map { id ->
            ResourceKey.create(Registries.BIOME, Identifier.parse(id))
        }
        BiomeModifications.addSpawn(
            BiomeSelectors.includeByKey(biomeKeys),
            MobCategory.CREATURE,
            entityType,
            weight,
            minimumGroupSize,
            maximumGroupSize,
        )
    }
}
