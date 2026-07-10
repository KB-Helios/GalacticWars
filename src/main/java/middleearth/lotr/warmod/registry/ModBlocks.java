package middleearth.lotr.warmod.registry;

import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import middleearth.lotr.warmod.settlement.KingdomHallBlock;
import middleearth.lotr.warmod.world.MallornTreeGrower;
import middleearth.lotr.warmod.world.PlayerTriggeredSaplingBlock;
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
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KingdomWarsMiddleEarth.MODID);

    public static final DeferredBlock<Block> MIDDLE_EARTH_STONE =
            BLOCKS.registerSimpleBlock("middle_earth_stone", properties -> properties.mapColor(MapColor.STONE));
    public static final DeferredBlock<Block> MITHRIL_ORE =
            BLOCKS.registerSimpleBlock("mithril_ore", properties -> properties
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> MALLORN_LOG =
            BLOCKS.registerBlock("mallorn_log", properties -> new RotatedPillarBlock(
                    properties.mapColor(MapColor.COLOR_YELLOW).strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> MALLORN_PLANKS =
            BLOCKS.registerSimpleBlock("mallorn_planks", properties -> properties
                    .mapColor(MapColor.COLOR_YELLOW).strength(2.0F, 3.0F).sound(SoundType.WOOD));
    public static final DeferredBlock<UntintedParticleLeavesBlock> MALLORN_LEAVES =
            BLOCKS.registerBlock("mallorn_leaves", properties -> new UntintedParticleLeavesBlock(
                    0.02F, ParticleTypes.CHERRY_LEAVES,
                    properties.mapColor(MapColor.COLOR_YELLOW).strength(0.2F).randomTicks()
                            .sound(SoundType.GRASS).noOcclusion()));
    public static final DeferredBlock<PlayerTriggeredSaplingBlock> MALLORN_SAPLING =
            BLOCKS.registerBlock("mallorn_sapling", properties -> new PlayerTriggeredSaplingBlock(
                    MallornTreeGrower.INSTANCE,
                    properties.mapColor(MapColor.PLANT).noCollision().randomTicks()
                            .instabreak().sound(SoundType.GRASS)));
    public static final DeferredBlock<KingdomHallBlock> KINGDOM_HALL =
            BLOCKS.registerBlock("kingdom_hall", properties -> new KingdomHallBlock(
                    properties.mapColor(MapColor.STONE).strength(4.0F, 1200.0F).sound(SoundType.STONE)));

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
