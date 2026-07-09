package middleearth.lotr.warmod.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FactionContentRegistrationTest {
    private static final String[] ENTITY_IDS = {
            "gondor_recruit",
            "rohan_recruit",
            "mordor_orc_recruit",
            "dwarf_recruit",
            "elf_recruit"
    };
    private static final String[] FACTION_ITEMS = {
            "gondor_steel_ingot",
            "rohan_horsehair",
            "mordor_iron_shard"
    };

    private FactionContentRegistrationTest() {
    }

    public static void main(String[] args) throws IOException {
        factionRecruitEntitiesAreRegistered();
        factionRecruitEggsAndTexturesExist();
        factionItemsAreRegisteredAndModeled();

        System.out.println("FactionContentRegistrationTest passed");
    }

    private static void factionRecruitEntitiesAreRegistered() throws IOException {
        String entityTypes = read("src/main/java/middleearth/lotr/warmod/registry/ModEntityTypes.java");
        String items = read("src/main/java/middleearth/lotr/warmod/registry/ModItems.java");
        String client = read("src/main/java/middleearth/lotr/warmod/KingdomWarsMiddleEarthClient.java");
        String common = read("src/main/java/middleearth/lotr/warmod/KingdomWarsMiddleEarth.java");
        String language = read("src/main/resources/assets/kingdomwarsmiddleearth/lang/en_us.json");

        for (String id : ENTITY_IDS) {
            assertContains(entityTypes, "\"" + id + "\"", "entity registration " + id);
            assertContains(items, "\"" + id + "_spawn_egg\"", "spawn egg registration " + id);
            assertContains(client, id.toUpperCase().replace("MORDOR_ORC_RECRUIT", "MORDOR_ORC_RECRUIT"), "renderer registration " + id);
            assertContains(common, id.toUpperCase().replace("MORDOR_ORC_RECRUIT", "MORDOR_ORC_RECRUIT"), "attribute registration " + id);
            assertContains(language, "\"entity.kingdomwarsmiddleearth." + id + "\"", "entity translation " + id);
        }
    }

    private static void factionRecruitEggsAndTexturesExist() {
        for (String id : ENTITY_IDS) {
            assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/items/" + id + "_spawn_egg.json");
            assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/models/item/" + id + "_spawn_egg.json");
            assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/textures/item/" + id + "_spawn_egg.png");
            assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/textures/entity/" + id + ".png");
            assertRegularFile("src/main/resources/data/kingdomwarsmiddleearth/neoforge/biome_modifier/" + id + "_spawns.json");
        }
    }

    private static void factionItemsAreRegisteredAndModeled() throws IOException {
        String items = read("src/main/java/middleearth/lotr/warmod/registry/ModItems.java");
        String creativeTabs = read("src/main/java/middleearth/lotr/warmod/registry/ModCreativeTabs.java");
        String language = read("src/main/resources/assets/kingdomwarsmiddleearth/lang/en_us.json");

        for (String id : FACTION_ITEMS) {
            assertContains(items, "\"" + id + "\"", "item registration " + id);
            assertContains(creativeTabs, id.toUpperCase(), "creative tab item " + id);
            assertContains(language, "\"item.kingdomwarsmiddleearth." + id + "\"", "item translation " + id);
            assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/items/" + id + ".json");
            assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/models/item/" + id + ".json");
            assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/textures/item/" + id + ".png");
        }
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private static void assertContains(String haystack, String needle, String label) {
        if (!haystack.contains(needle)) {
            throw new AssertionError(label + " missing <" + needle + ">");
        }
    }

    private static void assertRegularFile(String relativePath) {
        if (!Files.isRegularFile(Path.of(relativePath))) {
            throw new AssertionError("missing file <" + relativePath + ">");
        }
    }
}
