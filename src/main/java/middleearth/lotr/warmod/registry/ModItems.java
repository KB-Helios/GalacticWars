package middleearth.lotr.warmod.registry;

import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import middleearth.lotr.warmod.entity.RecruitSpawnEggItem;
import middleearth.lotr.warmod.faction.FactionId;
import middleearth.lotr.warmod.faction.FactionTokenItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.equipment.ArmorType;
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
    public static final DeferredItem<BlockItem> MALLORN_PLANKS =
            ITEMS.registerSimpleBlockItem("mallorn_planks", ModBlocks.MALLORN_PLANKS);
    public static final DeferredItem<BlockItem> MALLORN_LEAVES =
            ITEMS.registerSimpleBlockItem("mallorn_leaves", ModBlocks.MALLORN_LEAVES);
    public static final DeferredItem<BlockItem> MALLORN_SAPLING =
            ITEMS.registerSimpleBlockItem("mallorn_sapling", ModBlocks.MALLORN_SAPLING);
    public static final DeferredItem<BlockItem> KINGDOM_HALL =
            ITEMS.registerSimpleBlockItem("kingdom_hall", ModBlocks.KINGDOM_HALL);
    public static final DeferredItem<Item> MITHRIL_INGOT =
            ITEMS.registerSimpleItem("mithril_ingot", properties -> properties);
    public static final DeferredItem<Item> RAW_MITHRIL =
            ITEMS.registerSimpleItem("raw_mithril", properties -> properties);
    public static final DeferredItem<Item> GONDOR_STEEL_INGOT =
            ITEMS.registerSimpleItem("gondor_steel_ingot", properties -> properties);
    public static final DeferredItem<Item> ROHAN_HORSEHAIR =
            ITEMS.registerSimpleItem("rohan_horsehair", properties -> properties);
    public static final DeferredItem<Item> ROHAN_REINFORCED_IRON_INGOT =
            ITEMS.registerSimpleItem("rohan_reinforced_iron_ingot", properties -> properties);
    public static final DeferredItem<Item> MORDOR_IRON_SHARD =
            ITEMS.registerSimpleItem("mordor_iron_shard", properties -> properties);
    public static final DeferredItem<Item> MORDOR_IRON_INGOT =
            ITEMS.registerSimpleItem("mordor_iron_ingot", properties -> properties);
    public static final DeferredItem<Item> MALLORN_WEAVE =
            ITEMS.registerSimpleItem("mallorn_weave", properties -> properties);
    public static final java.util.Map<String, DeferredItem<Item>> FACTION_EQUIPMENT = registerFactionEquipment();
    public static final DeferredItem<FactionTokenItem> GONDOR_FACTION_TOKEN = factionToken("gondor");
    public static final DeferredItem<FactionTokenItem> ROHAN_FACTION_TOKEN = factionToken("rohan");
    public static final DeferredItem<FactionTokenItem> MORDOR_FACTION_TOKEN = factionToken("mordor");
    public static final DeferredItem<FactionTokenItem> DWARF_FACTION_TOKEN = factionToken("dwarf");
    public static final DeferredItem<FactionTokenItem> ELF_FACTION_TOKEN = factionToken("elf");
    public static final DeferredItem<RecruitSpawnEggItem> GONDOR_RECRUIT_SPAWN_EGG =
            ITEMS.registerItem("gondor_recruit_spawn_egg",
                    properties -> new RecruitSpawnEggItem(ModEntityTypes.GONDOR_RECRUIT.get(), properties));
    public static final DeferredItem<RecruitSpawnEggItem> ROHAN_RECRUIT_SPAWN_EGG =
            ITEMS.registerItem("rohan_recruit_spawn_egg",
                    properties -> new RecruitSpawnEggItem(ModEntityTypes.ROHAN_RECRUIT.get(), properties));
    public static final DeferredItem<RecruitSpawnEggItem> MORDOR_ORC_RECRUIT_SPAWN_EGG =
            ITEMS.registerItem("mordor_orc_recruit_spawn_egg",
                    properties -> new RecruitSpawnEggItem(ModEntityTypes.MORDOR_ORC_RECRUIT.get(), properties));
    public static final DeferredItem<RecruitSpawnEggItem> DWARF_RECRUIT_SPAWN_EGG =
            ITEMS.registerItem("dwarf_recruit_spawn_egg",
                    properties -> new RecruitSpawnEggItem(ModEntityTypes.DWARF_RECRUIT.get(), properties));
    public static final DeferredItem<RecruitSpawnEggItem> ELF_RECRUIT_SPAWN_EGG =
            ITEMS.registerItem("elf_recruit_spawn_egg",
                    properties -> new RecruitSpawnEggItem(ModEntityTypes.ELF_RECRUIT.get(), properties));

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private static DeferredItem<FactionTokenItem> factionToken(String factionPath) {
        return ITEMS.registerItem(
                factionPath + "_faction_token",
                properties -> new FactionTokenItem(FactionId.of(factionPath), properties.stacksTo(16)));
    }

    private static java.util.Map<String, DeferredItem<Item>> registerFactionEquipment() {
        java.util.LinkedHashMap<String, DeferredItem<Item>> items = new java.util.LinkedHashMap<>();
        registerFamily(items, ModEquipmentMaterials.ROHAN);
        registerFamily(items, ModEquipmentMaterials.MALLORN);
        registerFamily(items, ModEquipmentMaterials.GONDOR);
        registerFamily(items, ModEquipmentMaterials.MORDOR);
        registerFamily(items, ModEquipmentMaterials.MITHRIL);
        return java.util.Collections.unmodifiableMap(items);
    }

    private static void registerFamily(
            java.util.Map<String, DeferredItem<Item>> items,
            ModEquipmentMaterials.EquipmentFamily family
    ) {
        String prefix = family.id();
        items.put(prefix + "_sword", ITEMS.registerSimpleItem(
                prefix + "_sword", properties -> properties.sword(family.tool(), 3.0F, -2.4F)));
        items.put(prefix + "_pickaxe", ITEMS.registerSimpleItem(
                prefix + "_pickaxe", properties -> properties.pickaxe(family.tool(), 1.0F, -2.8F)));
        items.put(prefix + "_axe", ITEMS.registerItem(
                prefix + "_axe", properties -> new AxeItem(family.tool(), 6.0F, -3.1F, properties)));
        items.put(prefix + "_shovel", ITEMS.registerItem(
                prefix + "_shovel", properties -> new ShovelItem(family.tool(), 1.5F, -3.0F, properties)));
        items.put(prefix + "_hoe", ITEMS.registerItem(
                prefix + "_hoe", properties -> new HoeItem(family.tool(), -2.0F, -1.0F, properties)));
        items.put(prefix + "_helmet", ITEMS.registerSimpleItem(
                prefix + "_helmet", properties -> properties.humanoidArmor(family.armor(), ArmorType.HELMET)));
        items.put(prefix + "_chestplate", ITEMS.registerSimpleItem(
                prefix + "_chestplate", properties -> properties.humanoidArmor(family.armor(), ArmorType.CHESTPLATE)));
        items.put(prefix + "_leggings", ITEMS.registerSimpleItem(
                prefix + "_leggings", properties -> properties.humanoidArmor(family.armor(), ArmorType.LEGGINGS)));
        items.put(prefix + "_boots", ITEMS.registerSimpleItem(
                prefix + "_boots", properties -> properties.humanoidArmor(family.armor(), ArmorType.BOOTS)));
    }
}
