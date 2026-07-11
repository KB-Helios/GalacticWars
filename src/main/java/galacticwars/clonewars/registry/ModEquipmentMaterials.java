package galacticwars.clonewars.registry;

import java.util.Map;

import galacticwars.clonewars.GalacticWars;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;

public final class ModEquipmentMaterials {
    public static final EquipmentFamily MANDALORIAN = family(
            "mandalorian_alloy", BlockTags.INCORRECT_FOR_IRON_TOOL,
            400, 6.5F, 2.0F, 18, 20, 2, 6, 5, 2, 0.0F, 0.0F);
    public static final EquipmentFamily NIGHTSISTER_WEAVE = family(
            "nightsister_weave", BlockTags.INCORRECT_FOR_IRON_TOOL,
            300, 8.0F, 1.5F, 25, 18, 2, 5, 4, 2, 0.0F, 0.0F);
    public static final EquipmentFamily REPUBLIC = family(
            "republic_plastoid", BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            1000, 7.5F, 3.0F, 16, 30, 3, 7, 6, 3, 1.0F, 0.0F);
    public static final EquipmentFamily SEPARATIST = family(
            "separatist_alloy", BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            650, 7.0F, 4.0F, 8, 25, 2, 7, 5, 2, 1.0F, 0.0F);
    public static final EquipmentFamily BESKAR = family(
            "beskar", BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            1900, 9.5F, 4.0F, 22, 40, 3, 8, 6, 3, 3.0F, 0.1F);

    private ModEquipmentMaterials() {
    }

    private static EquipmentFamily family(
            String id,
            TagKey<Block> incorrectBlocks,
            int durability,
            float speed,
            float attackBonus,
            int enchantment,
            int armorDurability,
            int helmet,
            int chest,
            int leggings,
            int boots,
            float toughness,
            float knockback
    ) {
        TagKey<Item> repairs = TagKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(GalacticWars.MODID, "repairs_" + id + "_equipment"));
        ToolMaterial tool = new ToolMaterial(
                incorrectBlocks, durability, speed, attackBonus, enchantment, repairs);
        ResourceKey<EquipmentAsset> asset = ResourceKey.create(
                EquipmentAssets.ROOT_ID,
                Identifier.fromNamespaceAndPath(GalacticWars.MODID, id));
        ArmorMaterial armor = new ArmorMaterial(
                armorDurability,
                Map.of(
                        ArmorType.HELMET, helmet,
                        ArmorType.CHESTPLATE, chest,
                        ArmorType.LEGGINGS, leggings,
                        ArmorType.BOOTS, boots,
                        ArmorType.BODY, chest),
                enchantment,
                SoundEvents.ARMOR_EQUIP_IRON,
                toughness,
                knockback,
                repairs,
                asset);
        return new EquipmentFamily(id, tool, armor);
    }

    public record EquipmentFamily(String id, ToolMaterial tool, ArmorMaterial armor) {
    }
}
