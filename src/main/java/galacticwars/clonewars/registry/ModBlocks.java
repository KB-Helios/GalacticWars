package galacticwars.clonewars.registry;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.settlement.CommandCenterBlock;
import galacticwars.clonewars.world.NightsisterWeaveTreeGrower;
import galacticwars.clonewars.world.PlayerTriggeredSaplingBlock;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GalacticWars.MODID);

    public static final DeferredBlock<Block> DURACRETE =
            BLOCKS.registerSimpleBlock("duracrete", properties -> properties.mapColor(MapColor.STONE));
    public static final DeferredBlock<Block> BESKAR_ORE =
            BLOCKS.registerSimpleBlock("beskar_ore", properties -> properties
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> NIGHTSISTER_WEAVE_LOG =
            BLOCKS.registerBlock("nightsister_weave_log", properties -> new RotatedPillarBlock(
                    properties.mapColor(MapColor.COLOR_YELLOW).strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> NIGHTSISTER_WEAVE_PLANKS =
            BLOCKS.registerSimpleBlock("nightsister_weave_planks", properties -> properties
                    .mapColor(MapColor.COLOR_YELLOW).strength(2.0F, 3.0F).sound(SoundType.WOOD));
    public static final DeferredBlock<UntintedParticleLeavesBlock> NIGHTSISTER_WEAVE_LEAVES =
            BLOCKS.registerBlock("nightsister_weave_leaves", properties -> new UntintedParticleLeavesBlock(
                    0.02F, ParticleTypes.CHERRY_LEAVES,
                    properties.mapColor(MapColor.COLOR_YELLOW).strength(0.2F).randomTicks()
                            .sound(SoundType.GRASS).noOcclusion()));
    public static final DeferredBlock<PlayerTriggeredSaplingBlock> NIGHTSISTER_WEAVE_SAPLING =
            BLOCKS.registerBlock("nightsister_weave_sapling", properties -> new PlayerTriggeredSaplingBlock(
                    NightsisterWeaveTreeGrower.INSTANCE,
                    properties.mapColor(MapColor.PLANT).noCollision().randomTicks()
                            .instabreak().sound(SoundType.GRASS)));
    public static final DeferredBlock<CommandCenterBlock> COMMAND_CENTER =
            BLOCKS.registerBlock("command_center", properties -> new CommandCenterBlock(
                    properties.mapColor(MapColor.STONE).strength(4.0F, 1200.0F).sound(SoundType.STONE)));

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
