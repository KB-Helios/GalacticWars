package galacticwars.clonewars.registry;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.entity.RecruitSpawnEggItem;
import galacticwars.clonewars.combat.BlasterItem;
import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.faction.IdentityChipItem;
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
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GalacticWars.MODID);

    public static final DeferredItem<BlockItem> DURACRETE =
            ITEMS.registerSimpleBlockItem("duracrete", ModBlocks.DURACRETE);
    public static final DeferredItem<BlockItem> BESKAR_ORE =
            ITEMS.registerSimpleBlockItem("beskar_ore", ModBlocks.BESKAR_ORE);
    public static final DeferredItem<BlockItem> NIGHTSISTER_WEAVE_LOG =
            ITEMS.registerSimpleBlockItem("nightsister_weave_log", ModBlocks.NIGHTSISTER_WEAVE_LOG);
    public static final DeferredItem<BlockItem> NIGHTSISTER_WEAVE_PLANKS =
            ITEMS.registerSimpleBlockItem("nightsister_weave_planks", ModBlocks.NIGHTSISTER_WEAVE_PLANKS);
    public static final DeferredItem<BlockItem> NIGHTSISTER_WEAVE_LEAVES =
            ITEMS.registerSimpleBlockItem("nightsister_weave_leaves", ModBlocks.NIGHTSISTER_WEAVE_LEAVES);
    public static final DeferredItem<BlockItem> NIGHTSISTER_WEAVE_SAPLING =
            ITEMS.registerSimpleBlockItem("nightsister_weave_sapling", ModBlocks.NIGHTSISTER_WEAVE_SAPLING);
    public static final DeferredItem<BlockItem> COMMAND_CENTER =
            ITEMS.registerSimpleBlockItem("command_center", ModBlocks.COMMAND_CENTER);
    public static final DeferredItem<Item> BESKAR_INGOT =
            ITEMS.registerSimpleItem("beskar_ingot", properties -> properties);
    public static final DeferredItem<Item> RAW_BESKAR =
            ITEMS.registerSimpleItem("raw_beskar", properties -> properties);
    public static final DeferredItem<Item> CREDIT_CHIP =
            ITEMS.registerSimpleItem("credit_chip", properties -> properties.stacksTo(64));
    public static final DeferredItem<Item> ENERGY_CELL =
            ITEMS.registerSimpleItem("energy_cell", properties -> properties.stacksTo(64));
    public static final DeferredItem<BlasterItem> DC15_BLASTER =
            ITEMS.registerItem("dc15_blaster", properties -> new BlasterItem(properties.durability(850), 6.0D, 3.4F, 0.7F));
    public static final DeferredItem<BlasterItem> E5_BLASTER =
            ITEMS.registerItem("e5_blaster", properties -> new BlasterItem(properties.durability(700), 5.0D, 3.2F, 1.0F));
    public static final DeferredItem<BlasterItem> WESTAR_BLASTER =
            ITEMS.registerItem("westar_blaster", properties -> new BlasterItem(properties.durability(900), 6.5D, 3.5F, 0.6F));
    public static final DeferredItem<BlasterItem> SCATTER_BLASTER =
            ITEMS.registerItem("scatter_blaster", properties -> new BlasterItem(properties.durability(650), 7.0D, 2.8F, 1.4F));
    public static final DeferredItem<Item> NIGHTSISTER_BOW =
            ITEMS.registerSimpleItem("nightsister_bow", properties -> properties.stacksTo(1).durability(600));
    public static final DeferredItem<Item> BLUE_LIGHTSABER =
            ITEMS.registerSimpleItem("blue_lightsaber", properties -> properties.sword(ModEquipmentMaterials.BESKAR.tool(), 5.0F, -2.1F));
    public static final DeferredItem<Item> GREEN_LIGHTSABER =
            ITEMS.registerSimpleItem("green_lightsaber", properties -> properties.sword(ModEquipmentMaterials.BESKAR.tool(), 5.0F, -2.1F));
    public static final DeferredItem<Item> RED_LIGHTSABER =
            ITEMS.registerSimpleItem("red_lightsaber", properties -> properties.sword(ModEquipmentMaterials.BESKAR.tool(), 5.0F, -2.1F));
    public static final DeferredItem<Item> VIBROBLADE =
            ITEMS.registerSimpleItem("vibroblade", properties -> properties.sword(ModEquipmentMaterials.MANDALORIAN.tool(), 3.0F, -2.4F));
    public static final DeferredItem<Item> POWER_DRILL =
            ITEMS.registerSimpleItem("power_drill", properties -> properties.pickaxe(ModEquipmentMaterials.REPUBLIC.tool(), 1.0F, -2.8F));
    public static final DeferredItem<Item> PLASMA_CUTTER =
            ITEMS.registerItem("plasma_cutter", properties -> new AxeItem(ModEquipmentMaterials.REPUBLIC.tool(), 6.0F, -3.0F, properties));
    public static final DeferredItem<Item> SONIC_EXCAVATOR =
            ITEMS.registerItem("sonic_excavator", properties -> new ShovelItem(ModEquipmentMaterials.REPUBLIC.tool(), 1.5F, -3.0F, properties));
    public static final DeferredItem<Item> HYDROSPANNER =
            ITEMS.registerItem("hydrospanner", properties -> new HoeItem(ModEquipmentMaterials.REPUBLIC.tool(), -2.0F, -1.0F, properties));
    public static final DeferredItem<Item> REPUBLIC_PLASTOID_INGOT =
            ITEMS.registerSimpleItem("republic_plastoid_ingot", properties -> properties);
    public static final DeferredItem<Item> MANDALORIAN_FIBER =
            ITEMS.registerSimpleItem("mandalorian_fiber", properties -> properties);
    public static final DeferredItem<Item> MANDALORIAN_ALLOY_INGOT =
            ITEMS.registerSimpleItem("mandalorian_alloy_ingot", properties -> properties);
    public static final DeferredItem<Item> SEPARATIST_ALLOY_SHARD =
            ITEMS.registerSimpleItem("separatist_alloy_shard", properties -> properties);
    public static final DeferredItem<Item> SEPARATIST_ALLOY_INGOT =
            ITEMS.registerSimpleItem("separatist_alloy_ingot", properties -> properties);
    public static final DeferredItem<Item> NIGHTSISTER_WEAVE =
            ITEMS.registerSimpleItem("nightsister_weave", properties -> properties);
    public static final java.util.Map<String, DeferredItem<Item>> FACTION_EQUIPMENT = registerFactionEquipment();
    public static final DeferredItem<IdentityChipItem> REPUBLIC_FACTION_TOKEN = factionToken("republic");
    public static final DeferredItem<IdentityChipItem> MANDALORIAN_FACTION_TOKEN = factionToken("mandalorian");
    public static final DeferredItem<IdentityChipItem> SEPARATIST_FACTION_TOKEN = factionToken("separatist");
    public static final DeferredItem<IdentityChipItem> HUTT_CARTEL_FACTION_TOKEN = factionToken("hutt_cartel");
    public static final DeferredItem<IdentityChipItem> NIGHTSISTER_FACTION_TOKEN = factionToken("nightsister");
    public static final DeferredItem<RecruitSpawnEggItem> CLONE_TROOPER_SPAWN_EGG =
            ITEMS.registerItem("clone_trooper_spawn_egg",
                    properties -> new RecruitSpawnEggItem(ModEntityTypes.CLONE_TROOPER.get(), properties));
    public static final DeferredItem<RecruitSpawnEggItem> MANDALORIAN_WARRIOR_SPAWN_EGG =
            ITEMS.registerItem("mandalorian_warrior_spawn_egg",
                    properties -> new RecruitSpawnEggItem(ModEntityTypes.MANDALORIAN_WARRIOR.get(), properties));
    public static final DeferredItem<RecruitSpawnEggItem> B1_BATTLE_DROID_SPAWN_EGG =
            ITEMS.registerItem("b1_battle_droid_spawn_egg",
                    properties -> new RecruitSpawnEggItem(ModEntityTypes.B1_BATTLE_DROID.get(), properties));
    public static final DeferredItem<RecruitSpawnEggItem> HUTT_ENFORCER_SPAWN_EGG =
            ITEMS.registerItem("hutt_enforcer_spawn_egg",
                    properties -> new RecruitSpawnEggItem(ModEntityTypes.HUTT_ENFORCER.get(), properties));
    public static final DeferredItem<RecruitSpawnEggItem> NIGHTSISTER_ACOLYTE_SPAWN_EGG =
            ITEMS.registerItem("nightsister_acolyte_spawn_egg",
                    properties -> new RecruitSpawnEggItem(ModEntityTypes.NIGHTSISTER_ACOLYTE.get(), properties));

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private static DeferredItem<IdentityChipItem> factionToken(String factionPath) {
        return ITEMS.registerItem(
                factionPath + "_identity_chip",
                properties -> new IdentityChipItem(FactionId.of(factionPath), properties.stacksTo(16)));
    }

    private static java.util.Map<String, DeferredItem<Item>> registerFactionEquipment() {
        java.util.LinkedHashMap<String, DeferredItem<Item>> items = new java.util.LinkedHashMap<>();
        registerFamily(items, ModEquipmentMaterials.MANDALORIAN);
        registerFamily(items, ModEquipmentMaterials.NIGHTSISTER_WEAVE);
        registerFamily(items, ModEquipmentMaterials.REPUBLIC);
        registerFamily(items, ModEquipmentMaterials.SEPARATIST);
        registerFamily(items, ModEquipmentMaterials.BESKAR);
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
