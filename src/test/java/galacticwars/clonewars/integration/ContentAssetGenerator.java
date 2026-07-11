package galacticwars.clonewars.integration;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/** Deterministically generates the project-owned survival-content pixel art and data files. */
public final class ContentAssetGenerator {
    // PNGs are authored through the manifest-driven art workflow and must never be overwritten here.
    private static final boolean WRITE_TEXTURES = false;
    private static final String MOD = "galacticwars";
    private static final Path ROOT = Path.of("src/main/resources");
    private static final Map<String, Palette> FAMILIES = Map.of(
            "mandalorian_alloy", new Palette(0x6F4A2C, 0xC7A66A, 0xE4D5A2),
            "nightsister_weave", new Palette(0xB88C22, 0xF2D65A, 0xFFF0A0),
            "republic_plastoid", new Palette(0x526477, 0xAEBFCC, 0xE6F0F3),
            "separatist_alloy", new Palette(0x281F22, 0x713C34, 0xB95A36),
            "beskar", new Palette(0x466E86, 0x9ED8E8, 0xE9FCFF));
    private static final List<String> EQUIPMENT_TYPES = List.of(
            "sword", "axe", "pickaxe", "shovel", "hoe",
            "helmet", "chestplate", "leggings", "boots");
    private static final List<String> HANDHELD_TYPES = List.of(
            "sword", "axe", "pickaxe", "shovel", "hoe");

    private ContentAssetGenerator() {
    }

    public static void main(String[] args) throws Exception {
        generateHall();
        generateNightsisterWeave();
        generateMaterials();
        generateEquipment();
        generateRecipes();
        generateLootAndTags();
        generateTree();
        System.out.println("ContentAssetGenerator passed");
    }

    private static void generateHall() throws IOException {
        write("assets/" + MOD + "/models/block/command_center.json", """
                {
                  "textures": {
                    "side": "%1$s:block/command_center_side",
                    "top": "%1$s:block/command_center_top",
                    "bottom": "%1$s:block/command_center_bottom",
                    "particle": "%1$s:block/command_center_side"
                  },
                  "elements": [
                    {"from":[0,0,0],"to":[16,12,16],"faces":{"down":{"texture":"#bottom"},"up":{"texture":"#top"},"north":{"texture":"#side"},"south":{"texture":"#side"},"west":{"texture":"#side"},"east":{"texture":"#side"}}},
                    {"from":[2,12,2],"to":[14,14,14],"faces":{"down":{"texture":"#side"},"up":{"texture":"#top"},"north":{"texture":"#side"},"south":{"texture":"#side"},"west":{"texture":"#side"},"east":{"texture":"#side"}}},
                    {"from":[0,12,0],"to":[3,16,3],"faces":{"down":{"texture":"#side"},"up":{"texture":"#top"},"north":{"texture":"#side"},"south":{"texture":"#side"},"west":{"texture":"#side"},"east":{"texture":"#side"}}},
                    {"from":[13,12,0],"to":[16,16,3],"faces":{"down":{"texture":"#side"},"up":{"texture":"#top"},"north":{"texture":"#side"},"south":{"texture":"#side"},"west":{"texture":"#side"},"east":{"texture":"#side"}}},
                    {"from":[0,12,13],"to":[3,16,16],"faces":{"down":{"texture":"#side"},"up":{"texture":"#top"},"north":{"texture":"#side"},"south":{"texture":"#side"},"west":{"texture":"#side"},"east":{"texture":"#side"}}},
                    {"from":[13,12,13],"to":[16,16,16],"faces":{"down":{"texture":"#side"},"up":{"texture":"#top"},"north":{"texture":"#side"},"south":{"texture":"#side"},"west":{"texture":"#side"},"east":{"texture":"#side"}}}
                  ]
                }
                """.formatted(MOD));
        writeBlockItemModel("command_center", MOD + ":block/command_center");
        texture("block/command_center_side", (image, palette) -> {
            fill(image, 0x354047);
            for (int y = 1; y < 16; y += 4) line(image, 0, y, 15, y, 0x65747A);
            for (int x = 2; x < 16; x += 5) line(image, x, 0, x, 15, 0x293238);
            rect(image, 6, 6, 9, 12, 0x9A7835);
            rect(image, 7, 7, 8, 11, 0xD1B564);
        });
        texture("block/command_center_top", (image, palette) -> {
            fill(image, 0x6B7676);
            rect(image, 1, 1, 14, 14, 0x4B575A);
            rect(image, 3, 3, 12, 12, 0x899395);
            rect(image, 5, 5, 10, 10, 0x657276);
            line(image, 0, 0, 15, 15, 0xB59B55);
            line(image, 15, 0, 0, 15, 0xB59B55);
        });
        texture("block/command_center_bottom", (image, palette) -> checker(image, 0x30383A, 0x465054));
    }

    private static void generateNightsisterWeave() throws IOException {
        write("assets/" + MOD + "/blockstates/nightsister_weave_planks.json", variant(MOD + ":block/nightsister_weave_planks"));
        write("assets/" + MOD + "/blockstates/nightsister_weave_leaves.json", variant(MOD + ":block/nightsister_weave_leaves"));
        write("assets/" + MOD + "/blockstates/nightsister_weave_sapling.json", variant(MOD + ":block/nightsister_weave_sapling"));
        write("assets/" + MOD + "/models/block/nightsister_weave_planks.json", cubeAll(MOD + ":block/nightsister_weave_planks"));
        write("assets/" + MOD + "/models/block/nightsister_weave_leaves.json", cubeAll(MOD + ":block/nightsister_weave_leaves"));
        write("assets/" + MOD + "/models/block/nightsister_weave_sapling.json", """
                {"parent":"minecraft:block/cross","textures":{"cross":"%s:block/nightsister_weave_sapling"}}
                """.formatted(MOD));
        writeBlockItemModel("nightsister_weave_planks", MOD + ":block/nightsister_weave_planks");
        writeBlockItemModel("nightsister_weave_leaves", MOD + ":block/nightsister_weave_leaves");
        writeBlockItemModel("nightsister_weave_sapling", MOD + ":block/nightsister_weave_sapling");
        texture("block/nightsister_weave_planks", (image, palette) -> {
            fill(image, 0xCBAA3D);
            for (int y = 3; y < 16; y += 4) line(image, 0, y, 15, y, 0x8E6D22);
            for (int x = 1; x < 16; x += 6) line(image, x, 0, x, 15, 0xE5CA62);
        });
        texture("block/nightsister_weave_leaves", (image, palette) -> {
            clear(image);
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                if ((x * 3 + y * 5) % 7 != 0) image.setRGB(x, y, ((x + y) % 3 == 0 ? 0xFFF2A83B : 0xFFD6B432));
            }
        });
        texture("block/nightsister_weave_sapling", (image, palette) -> {
            clear(image);
            line(image, 8, 15, 8, 5, 0x7E5425);
            line(image, 7, 11, 4, 8, 0x7E5425);
            line(image, 9, 9, 12, 6, 0x7E5425);
            rect(image, 2, 5, 6, 9, 0xD8B832);
            rect(image, 9, 3, 13, 7, 0xF1D85A);
            rect(image, 6, 1, 10, 5, 0xE6C542);
        });
    }

    private static void generateMaterials() throws IOException {
        Map<String, Palette> items = new LinkedHashMap<>();
        items.put("raw_beskar", new Palette(0x3C5260, 0x78B5CA, 0xD5F4FF));
        items.put("mandalorian_alloy_ingot", FAMILIES.get("mandalorian_alloy"));
        items.put("separatist_alloy_ingot", FAMILIES.get("separatist_alloy"));
        items.put("nightsister_weave", FAMILIES.get("nightsister_weave"));
        for (Map.Entry<String, Palette> entry : items.entrySet()) {
            writeGeneratedItemModel(entry.getKey());
            texture("item/" + entry.getKey(), (image, ignored) -> materialIcon(image, entry.getValue(), entry.getKey()));
        }
        for (String faction : List.of("republic", "mandalorian", "separatist", "hutt_cartel", "nightsister")) {
            String id = faction + "_identity_chip";
            writeGeneratedItemModel(id);
            Palette palette = switch (faction) {
                case "mandalorian" -> FAMILIES.get("mandalorian_alloy");
                case "separatist" -> FAMILIES.get("separatist_alloy");
                case "hutt_cartel" -> FAMILIES.get("beskar");
                case "nightsister" -> FAMILIES.get("nightsister_weave");
                default -> FAMILIES.get("republic_plastoid");
            };
            texture("item/" + id, (image, ignored) -> tokenIcon(image, palette));
        }
    }

    private static void generateEquipment() throws IOException {
        for (Map.Entry<String, Palette> family : FAMILIES.entrySet()) {
            write("assets/" + MOD + "/equipment/" + family.getKey() + ".json", """
                    {"layers":{"humanoid":[{"texture":"%1$s:%2$s"}],"humanoid_baby":[{"texture":"%1$s:%2$s"}],"humanoid_leggings":[{"texture":"%1$s:%2$s"}]}}
                    """.formatted(MOD, family.getKey()));
            for (String type : EQUIPMENT_TYPES) {
                String id = family.getKey() + "_" + type;
                if (HANDHELD_TYPES.contains(type)) {
                    writeHandheldItemModel(id);
                } else {
                    writeGeneratedItemModel(id);
                }
                texture("item/" + id, (image, ignored) -> equipmentIcon(image, family.getValue(), type));
            }
            armorTexture("entity/equipment/humanoid/" + family.getKey(), family.getValue(), ArmorLayer.HUMANOID);
            armorTexture("entity/equipment/humanoid_baby/" + family.getKey(), family.getValue(), ArmorLayer.HUMANOID_BABY);
            armorTexture("entity/equipment/humanoid_leggings/" + family.getKey(), family.getValue(), ArmorLayer.LEGGINGS);
        }
    }

    private static void generateRecipes() throws IOException {
        shaped("command_center", "[\"LSL\",\"SES\",\"LSL\"]",
                "{\"L\":\"" + MOD + ":nightsister_weave_log\",\"S\":\"" + MOD + ":duracrete\",\"E\":\"galacticwars:credit_chip\"}",
                MOD + ":command_center", 1, "building");
        write("data/" + MOD + "/recipe/duracrete_stonecutting.json", """
                {"type":"minecraft:stonecutting","ingredient":"minecraft:stone","result":{"id":"%s:duracrete"}}
                """.formatted(MOD));
        shapeless("nightsister_weave_sapling", List.of("minecraft:oak_sapling", "minecraft:gold_nugget",
                "minecraft:gold_nugget", "minecraft:gold_nugget", "minecraft:gold_nugget"), MOD + ":nightsister_weave_sapling", 1);
        shapeless("nightsister_weave_planks", List.of(MOD + ":nightsister_weave_log"), MOD + ":nightsister_weave_planks", 4);
        shapeless("nightsister_weave", List.of(MOD + ":nightsister_weave_leaves", MOD + ":nightsister_weave_leaves",
                MOD + ":nightsister_weave_leaves", MOD + ":nightsister_weave_leaves", "minecraft:string"), MOD + ":nightsister_weave", 2);
        shaped("beskar_ore", "[\"DDD\",\"DXD\",\"DDD\"]",
                "{\"D\":\"minecraft:deepslate\",\"X\":\"minecraft:diamond\"}", MOD + ":beskar_ore", 1, "misc");
        smelting("beskar_ingot_from_smelting", "minecraft:smelting", MOD + ":raw_beskar", MOD + ":beskar_ingot", 0.7F, 200);
        smelting("beskar_ingot_from_blasting", "minecraft:blasting", MOD + ":raw_beskar", MOD + ":beskar_ingot", 0.7F, 100);
        shapeless("republic_plastoid_ingot", List.of("minecraft:iron_ingot", "minecraft:charcoal", "minecraft:quartz"), MOD + ":republic_plastoid_ingot", 1);
        shapeless("mandalorian_alloy_ingot", List.of("minecraft:iron_ingot", MOD + ":mandalorian_fiber"), MOD + ":mandalorian_alloy_ingot", 1);
        shapeless("separatist_alloy_shard", List.of("minecraft:iron_ingot", "minecraft:netherrack"), MOD + ":separatist_alloy_shard", 4);
        shaped("separatist_alloy_ingot", "[\"XX\",\"XX\"]", "{\"X\":\"" + MOD + ":separatist_alloy_shard\"}", MOD + ":separatist_alloy_ingot", 1, "misc");

        Map<String, String> tokenMaterials = Map.of(
                "republic", MOD + ":republic_plastoid_ingot",
                "mandalorian", MOD + ":mandalorian_fiber",
                "separatist", MOD + ":separatist_alloy_ingot",
                "hutt_cartel", MOD + ":beskar_ingot",
                "nightsister", MOD + ":nightsister_weave");
        for (Map.Entry<String, String> token : tokenMaterials.entrySet()) {
            shapeless(token.getKey() + "_identity_chip", List.of("minecraft:paper", "galacticwars:credit_chip", token.getValue()),
                    MOD + ":" + token.getKey() + "_identity_chip", 1);
        }

        Map<String, String> familyMaterials = Map.of(
                "mandalorian_alloy", MOD + ":mandalorian_alloy_ingot",
                "nightsister_weave", MOD + ":nightsister_weave",
                "republic_plastoid", MOD + ":republic_plastoid_ingot",
                "separatist_alloy", MOD + ":separatist_alloy_ingot",
                "beskar", MOD + ":beskar_ingot");
        for (Map.Entry<String, String> family : familyMaterials.entrySet()) {
            equipmentRecipes(family.getKey(), family.getValue());
        }
    }

    private static void equipmentRecipes(String family, String material) throws IOException {
        shaped(family + "_sword", "[\"X\",\"X\",\"S\"]", "{\"X\":\"" + material + "\",\"S\":\"minecraft:stick\"}", MOD + ":" + family + "_sword", 1, "equipment");
        shaped(family + "_pickaxe", "[\"XXX\",\" S \",\" S \"]", "{\"X\":\"" + material + "\",\"S\":\"minecraft:stick\"}", MOD + ":" + family + "_pickaxe", 1, "equipment");
        shaped(family + "_axe", "[\"XX\",\"XS\",\" S\"]", "{\"X\":\"" + material + "\",\"S\":\"minecraft:stick\"}", MOD + ":" + family + "_axe", 1, "equipment");
        shaped(family + "_shovel", "[\"X\",\"S\",\"S\"]", "{\"X\":\"" + material + "\",\"S\":\"minecraft:stick\"}", MOD + ":" + family + "_shovel", 1, "equipment");
        shaped(family + "_hoe", "[\"XX\",\" S\",\" S\"]", "{\"X\":\"" + material + "\",\"S\":\"minecraft:stick\"}", MOD + ":" + family + "_hoe", 1, "equipment");
        shaped(family + "_helmet", "[\"XXX\",\"X X\"]", "{\"X\":\"" + material + "\"}", MOD + ":" + family + "_helmet", 1, "equipment");
        shaped(family + "_chestplate", "[\"X X\",\"XXX\",\"XXX\"]", "{\"X\":\"" + material + "\"}", MOD + ":" + family + "_chestplate", 1, "equipment");
        shaped(family + "_leggings", "[\"XXX\",\"X X\",\"X X\"]", "{\"X\":\"" + material + "\"}", MOD + ":" + family + "_leggings", 1, "equipment");
        shaped(family + "_boots", "[\"X X\",\"X X\"]", "{\"X\":\"" + material + "\"}", MOD + ":" + family + "_boots", 1, "equipment");
    }

    private static void generateLootAndTags() throws IOException {
        simpleBlockLoot("nightsister_weave_planks");
        simpleBlockLoot("nightsister_weave_sapling");
        write("data/" + MOD + "/loot_table/blocks/nightsister_weave_leaves.json", """
                {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:alternatives","children":[{"type":"minecraft:item","conditions":[{"condition":"minecraft:any_of","terms":[{"condition":"minecraft:match_tool","predicate":{"items":"minecraft:shears"}},{"condition":"minecraft:match_tool","predicate":{"predicates":{"minecraft:enchantments":[{"enchantments":"minecraft:silk_touch","levels":{"min":1}}]}}}]}],"name":"%1$s:nightsister_weave_leaves"},{"type":"minecraft:item","conditions":[{"condition":"minecraft:survives_explosion"},{"condition":"minecraft:random_chance","chance":0.08}],"name":"%1$s:nightsister_weave_sapling"}]}]}]}
                """.formatted(MOD));
        write("data/" + MOD + "/loot_table/blocks/beskar_ore.json", """
                {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:alternatives","children":[{"type":"minecraft:item","conditions":[{"condition":"minecraft:match_tool","predicate":{"predicates":{"minecraft:enchantments":[{"enchantments":"minecraft:silk_touch","levels":{"min":1}}]}}}],"name":"%1$s:beskar_ore"},{"type":"minecraft:item","functions":[{"function":"minecraft:apply_bonus","enchantment":"minecraft:fortune","formula":"minecraft:ore_drops"},{"function":"minecraft:explosion_decay"}],"name":"%1$s:raw_beskar"}]}]}]}
                """.formatted(MOD));
        tag("block", "minecraft:logs", List.of(MOD + ":nightsister_weave_log"));
        tag("block", "minecraft:leaves", List.of(MOD + ":nightsister_weave_leaves"));
        tag("block", "minecraft:saplings", List.of(MOD + ":nightsister_weave_sapling"));
        tag("block", "minecraft:planks", List.of(MOD + ":nightsister_weave_planks"));
        tag("block", "minecraft:mineable/pickaxe", List.of(MOD + ":beskar_ore", MOD + ":duracrete"));
        tag("block", "minecraft:needs_iron_tool", List.of(MOD + ":beskar_ore"));
        tag("item", "minecraft:logs", List.of(MOD + ":nightsister_weave_log"));
        tag("item", "minecraft:leaves", List.of(MOD + ":nightsister_weave_leaves"));
        tag("item", "minecraft:saplings", List.of(MOD + ":nightsister_weave_sapling"));
        tag("item", "minecraft:planks", List.of(MOD + ":nightsister_weave_planks"));
        Map<String, String> repairs = Map.of(
                "mandalorian_alloy", MOD + ":mandalorian_alloy_ingot",
                "nightsister_weave", MOD + ":nightsister_weave",
                "republic_plastoid", MOD + ":republic_plastoid_ingot",
                "separatist_alloy", MOD + ":separatist_alloy_ingot",
                "beskar", MOD + ":beskar_ingot");
        for (Map.Entry<String, String> repair : repairs.entrySet()) {
            tag("item", MOD + ":repairs_" + repair.getKey() + "_equipment", List.of(repair.getValue()));
        }
    }

    private static void generateTree() throws IOException {
        write("data/" + MOD + "/worldgen/configured_feature/nightsister_weave_tree.json", """
                {"type":"minecraft:tree","config":{"below_trunk_provider":{"type":"minecraft:simple_state_provider","state":{"Name":"minecraft:dirt"}},"decorators":[],"foliage_placer":{"type":"minecraft:blob_foliage_placer","height":4,"offset":0,"radius":3},"foliage_provider":{"type":"minecraft:simple_state_provider","state":{"Name":"%1$s:nightsister_weave_leaves","Properties":{"distance":"7","persistent":"false","waterlogged":"false"}}},"ignore_vines":true,"minimum_size":{"type":"minecraft:two_layers_feature_size","limit":1,"lower_size":0,"upper_size":2},"trunk_placer":{"type":"minecraft:straight_trunk_placer","base_height":7,"height_rand_a":3,"height_rand_b":1},"trunk_provider":{"type":"minecraft:simple_state_provider","state":{"Name":"%1$s:nightsister_weave_log","Properties":{"axis":"y"}}}}}
                """.formatted(MOD));
    }

    private static void shaped(String name, String pattern, String key, String result, int count, String category) throws IOException {
        write("data/" + MOD + "/recipe/" + name + ".json", """
                {"type":"minecraft:crafting_shaped","category":"%s","pattern":%s,"key":%s,"result":{"id":"%s","count":%d}}
                """.formatted(category, pattern, key, result, count));
    }

    private static void shapeless(String name, List<String> ingredients, String result, int count) throws IOException {
        String values = ingredients.stream().map(value -> "\"" + value + "\"").collect(java.util.stream.Collectors.joining(","));
        write("data/" + MOD + "/recipe/" + name + ".json", """
                {"type":"minecraft:crafting_shapeless","category":"misc","ingredients":[%s],"result":{"id":"%s","count":%d}}
                """.formatted(values, result, count));
    }

    private static void smelting(String name, String type, String ingredient, String result, float xp, int time) throws IOException {
        write("data/" + MOD + "/recipe/" + name + ".json", """
                {"type":"%s","ingredient":"%s","result":{"id":"%s"},"experience":%s,"cookingtime":%d}
                """.formatted(type, ingredient, result, xp, time));
    }

    private static void simpleBlockLoot(String id) throws IOException {
        write("data/" + MOD + "/loot_table/blocks/" + id + ".json", """
                {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"%1$s:%2$s"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]}
                """.formatted(MOD, id));
    }

    private static void tag(String kind, String id, List<String> values) throws IOException {
        String namespace = id.contains(":") ? id.substring(0, id.indexOf(':')) : MOD;
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String entries = values.stream().map(value -> "\"" + value + "\"").collect(java.util.stream.Collectors.joining(","));
        write("data/" + namespace + "/tags/" + kind + "/" + path + ".json", "{\"replace\":false,\"values\":[" + entries + "]}");
    }

    private static String variant(String model) {
        return "{\"variants\":{\"\":{\"model\":\"" + model + "\"}}}";
    }

    private static String cubeAll(String texture) {
        return "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\"" + texture + "\"}}";
    }

    private static void writeGeneratedItemModel(String id) throws IOException {
        writeLayeredItemModel(id, "minecraft:item/generated");
    }

    private static void writeHandheldItemModel(String id) throws IOException {
        writeLayeredItemModel(id, "minecraft:item/handheld");
    }

    private static void writeLayeredItemModel(String id, String parent) throws IOException {
        String model = MOD + ":item/" + id;
        writeItemDefinition(id, model);
        write("assets/" + MOD + "/models/item/" + id + ".json",
                "{\"parent\":\"" + parent + "\",\"textures\":{\"layer0\":\"" + model + "\"}}");
    }

    private static void writeBlockItemModel(String id, String blockModel) throws IOException {
        String itemModel = MOD + ":item/" + id;
        writeItemDefinition(id, itemModel);
        write("assets/" + MOD + "/models/item/" + id + ".json",
                "{\"parent\":\"" + blockModel + "\"}");
    }

    private static void writeItemDefinition(String id, String model) throws IOException {
        write("assets/" + MOD + "/items/" + id + ".json",
                "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"" + model + "\"}}");
    }

    private static void texture(String relative, Painter painter) throws IOException {
        if (!WRITE_TEXTURES) {
            return;
        }
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        painter.paint(image, null);
        Path output = ROOT.resolve("assets/" + MOD + "/textures/" + relative + ".png");
        Files.createDirectories(output.getParent());
        ImageIO.write(image, "png", output.toFile());
    }

    private static void armorTexture(String relative, Palette palette, ArmorLayer layer) throws IOException {
        if (!WRITE_TEXTURES) {
            return;
        }
        BufferedImage image = new BufferedImage(64, layer.height, BufferedImage.TYPE_INT_ARGB);
        clear(image);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (!layer.includes(x, y)) {
                    continue;
                }
                boolean topEdge = !layer.includes(x, y - 1);
                boolean leftEdge = !layer.includes(x - 1, y);
                boolean bottomEdge = !layer.includes(x, y + 1);
                boolean rightEdge = !layer.includes(x + 1, y);
                int color;
                if (topEdge || leftEdge || Math.floorMod(x * 3 + y * 5, 29) == 0) {
                    color = palette.highlight();
                } else if (bottomEdge || rightEdge || Math.floorMod(x + y, 7) == 0) {
                    color = palette.base();
                } else {
                    color = palette.mid();
                }
                image.setRGB(x, y, argb(color));
            }
        }
        Path output = ROOT.resolve("assets/" + MOD + "/textures/" + relative + ".png");
        Files.createDirectories(output.getParent());
        ImageIO.write(image, "png", output.toFile());
    }

    private enum ArmorLayer {
        HUMANOID(32) {
            @Override
            boolean includes(int x, int y) {
                return inMask(x, y, HUMANOID_MASK);
            }
        },
        LEGGINGS(32) {
            @Override
            boolean includes(int x, int y) {
                return inMask(x, y, LEGGINGS_MASK);
            }
        },
        HUMANOID_BABY(64) {
            @Override
            boolean includes(int x, int y) {
                return inMask(x, y, BABY_MASK);
            }
        };

        private static final String[] HUMANOID_MASK = {
                "8-15", "8-15", "8-15", "8-15", "8-15", "8-15", "8-15", "8-15",
                "0-31", "0-31", "0-31", "0-8,11-12,15-31", "0-3,11-12,20-31", "24-31",
                "26-29", "", "8-11,44-47", "8-11,44-47", "8-11,44-47", "8-11,44-47",
                "16-21,26-33,38-55", "16-22,25-55", "16-55", "16-55", "16-55", "16-39",
                "0-39", "0-39", "0-39", "0-15,21-26,33-38", "0-15,22-25", "0-15"
        };
        private static final String[] LEGGINGS_MASK = {
                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                "4-7", "4-7", "4-7", "4-7", "0-15", "0-15", "0-15", "0-15", "0-15", "0-15",
                "0-15", "0-39", "0-39", "16-39", "16-39", "16-39"
        };
        private static final String[] BABY_MASK = {
                "8-16", "8-16", "8-16", "8-16", "8-16", "8-16", "8-16", "8-16",
                "0-33", "0-33", "0-33", "0-8,11-13,16-33", "0-8,11-13,16-33",
                "0-8,16-33", "0-5,19-33", "27-31", "", "33-34", "33-34", "33-34",
                "0-4,7-39", "0-39", "0-39", "0-17", "0-17", "3-8,33-34", "3-8,33-34",
                "3-8,18-29,33-34", "0-11,18-39", "3-8,18-39", "3-8,30-39", "3-8",
                "0-11", "", "", "", "", "", "", "0-17", "0-17"
        };

        private final int height;

        ArmorLayer(int height) {
            this.height = height;
        }

        abstract boolean includes(int x, int y);

        private static boolean inMask(int x, int y, String[] mask) {
            if (x < 0 || x >= 64 || y < 0 || y >= mask.length) return false;
            String row = mask[y];
            if (row.isEmpty()) return false;
            for (String run : row.split(",")) {
                int separator = run.indexOf('-');
                int start = Integer.parseInt(run.substring(0, separator));
                int end = Integer.parseInt(run.substring(separator + 1));
                if (x >= start && x <= end) return true;
            }
            return false;
        }
    }

    private static void equipmentIcon(BufferedImage image, Palette palette, String type) {
        clear(image);
        int dark = palette.base();
        int mid = palette.mid();
        int light = palette.highlight();
        switch (type) {
            case "sword" -> { thickLine(image, 3, 13, 12, 4, mid); thickLine(image, 5, 14, 2, 11, dark); pixel(image, 12, 3, light); }
            case "axe" -> { thickLine(image, 5, 14, 10, 5, 0x75502A); rect(image, 8, 2, 14, 7, mid); rect(image, 11, 3, 14, 5, light); }
            case "pickaxe" -> { thickLine(image, 7, 14, 9, 4, 0x75502A); thickLine(image, 2, 4, 13, 2, mid); line(image, 2, 5, 2, 7, light); }
            case "shovel" -> { thickLine(image, 7, 14, 8, 6, 0x75502A); rect(image, 5, 2, 10, 6, mid); pixel(image, 7, 2, light); }
            case "hoe" -> { thickLine(image, 6, 14, 8, 5, 0x75502A); rect(image, 7, 3, 13, 5, mid); }
            case "helmet" -> { rect(image, 3, 4, 12, 10, mid); rect(image, 5, 8, 10, 12, dark); rect(image, 5, 8, 10, 9, light); }
            case "chestplate" -> { rect(image, 4, 3, 11, 13, mid); rect(image, 2, 4, 4, 9, dark); rect(image, 11, 4, 13, 9, dark); line(image, 5, 4, 10, 4, light); }
            case "leggings" -> { rect(image, 4, 3, 11, 8, mid); rect(image, 4, 8, 7, 14, dark); rect(image, 9, 8, 12, 14, dark); }
            case "boots" -> { rect(image, 3, 5, 6, 12, mid); rect(image, 9, 5, 12, 12, mid); rect(image, 2, 11, 6, 14, dark); rect(image, 9, 11, 13, 14, dark); }
        }
    }

    private static void materialIcon(BufferedImage image, Palette palette, String id) {
        clear(image);
        if (id.contains("weave")) {
            for (int i = 2; i < 14; i += 3) { line(image, 2, i, 13, i, palette.mid()); line(image, i, 2, i, 13, palette.highlight()); }
        } else if (id.contains("raw")) {
            rect(image, 3, 5, 12, 12, palette.base());
            rect(image, 5, 3, 10, 13, palette.mid());
            pixel(image, 6, 5, palette.highlight()); pixel(image, 10, 9, palette.highlight());
        } else {
            rect(image, 2, 6, 13, 12, palette.base());
            rect(image, 4, 4, 11, 10, palette.mid());
            line(image, 5, 5, 10, 5, palette.highlight());
        }
    }

    private static void tokenIcon(BufferedImage image, Palette palette) {
        clear(image);
        rect(image, 3, 2, 12, 13, 0xE8D8B0);
        rect(image, 4, 3, 11, 12, 0xC9B680);
        rect(image, 6, 5, 9, 10, palette.mid());
        pixel(image, 7, 4, palette.highlight()); pixel(image, 7, 11, palette.base());
    }

    private static void checker(BufferedImage image, int first, int second) {
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            image.setRGB(x, y, argb(((x / 4 + y / 4) & 1) == 0 ? first : second));
        }
    }

    private static void fill(BufferedImage image, int rgb) { rect(image, 0, 0, image.getWidth() - 1, image.getHeight() - 1, rgb); }
    private static void clear(BufferedImage image) { for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) image.setRGB(x, y, 0); }
    private static void pixel(BufferedImage image, int x, int y, int rgb) { if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) image.setRGB(x, y, argb(rgb)); }
    private static void rect(BufferedImage image, int x1, int y1, int x2, int y2, int rgb) { for (int y = y1; y <= y2; y++) for (int x = x1; x <= x2; x++) pixel(image, x, y, rgb); }
    private static void line(BufferedImage image, int x1, int y1, int x2, int y2, int rgb) {
        int dx = Math.abs(x2 - x1), sx = x1 < x2 ? 1 : -1, dy = -Math.abs(y2 - y1), sy = y1 < y2 ? 1 : -1, err = dx + dy;
        while (true) { pixel(image, x1, y1, rgb); if (x1 == x2 && y1 == y2) break; int e2 = 2 * err; if (e2 >= dy) { err += dy; x1 += sx; } if (e2 <= dx) { err += dx; y1 += sy; } }
    }
    private static void thickLine(BufferedImage image, int x1, int y1, int x2, int y2, int rgb) { line(image, x1, y1, x2, y2, rgb); line(image, x1 + 1, y1, x2 + 1, y2, rgb); }
    private static int argb(int rgb) { return 0xFF000000 | rgb; }

    private static void write(String relative, String content) throws IOException {
        Path output = ROOT.resolve(relative);
        Files.createDirectories(output.getParent());
        Files.writeString(output, content.strip() + System.lineSeparator());
    }

    @FunctionalInterface
    private interface Painter { void paint(BufferedImage image, Palette palette); }
    private record Palette(int base, int mid, int highlight) { }
}
