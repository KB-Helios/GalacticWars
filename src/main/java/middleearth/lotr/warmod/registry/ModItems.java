package middleearth.lotr.warmod.registry;

import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KingdomWarsMiddleEarth.MODID);

    public static final DeferredItem<BlockItem> MIDDLE_EARTH_STONE =
            ITEMS.registerSimpleBlockItem("middle_earth_stone", ModBlocks.MIDDLE_EARTH_STONE);
    public static final DeferredItem<BlockItem> MITHRIL_ORE =
            ITEMS.registerSimpleBlockItem("mithril_ore", ModBlocks.MITHRIL_ORE);
    public static final DeferredItem<BlockItem> MALLORN_LOG =
            ITEMS.registerSimpleBlockItem("mallorn_log", ModBlocks.MALLORN_LOG);
    public static final DeferredItem<BlockItem> KINGDOM_HALL =
            ITEMS.registerSimpleBlockItem("kingdom_hall", ModBlocks.KINGDOM_HALL);
    public static final DeferredItem<Item> MITHRIL_INGOT =
            ITEMS.registerSimpleItem("mithril_ingot", properties -> properties);
    public static final DeferredItem<Item> GONDOR_STEEL_INGOT =
            ITEMS.registerSimpleItem("gondor_steel_ingot", properties -> properties);
    public static final DeferredItem<Item> ROHAN_HORSEHAIR =
            ITEMS.registerSimpleItem("rohan_horsehair", properties -> properties);
    public static final DeferredItem<Item> MORDOR_IRON_SHARD =
            ITEMS.registerSimpleItem("mordor_iron_shard", properties -> properties);
    public static final DeferredItem<SpawnEggItem> GONDOR_RECRUIT_SPAWN_EGG =
            ITEMS.registerItem("gondor_recruit_spawn_egg",
                    properties -> new SpawnEggItem(properties.spawnEgg(ModEntityTypes.GONDOR_RECRUIT.get())));
    public static final DeferredItem<SpawnEggItem> ROHAN_RECRUIT_SPAWN_EGG =
            ITEMS.registerItem("rohan_recruit_spawn_egg",
                    properties -> new SpawnEggItem(properties.spawnEgg(ModEntityTypes.ROHAN_RECRUIT.get())));
    public static final DeferredItem<SpawnEggItem> MORDOR_ORC_RECRUIT_SPAWN_EGG =
            ITEMS.registerItem("mordor_orc_recruit_spawn_egg",
                    properties -> new SpawnEggItem(properties.spawnEgg(ModEntityTypes.MORDOR_ORC_RECRUIT.get())));
    public static final DeferredItem<SpawnEggItem> DWARF_RECRUIT_SPAWN_EGG =
            ITEMS.registerItem("dwarf_recruit_spawn_egg",
                    properties -> new SpawnEggItem(properties.spawnEgg(ModEntityTypes.DWARF_RECRUIT.get())));
    public static final DeferredItem<SpawnEggItem> ELF_RECRUIT_SPAWN_EGG =
            ITEMS.registerItem("elf_recruit_spawn_egg",
                    properties -> new SpawnEggItem(properties.spawnEgg(ModEntityTypes.ELF_RECRUIT.get())));

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
