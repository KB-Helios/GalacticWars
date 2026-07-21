package galacticwars.clonewars.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FactionContentRegistrationTest {
    private static final String[] ENTITY_IDS = {
            "clone_trooper",
            "arc_trooper",
            "phase_i_clone_trooper",
            "phase_i_arc_trooper",
            "senate_commando",
            "republic_honor_guard",
            "jedi_knight",
            "mandalorian_warrior",
            "mandalorian_marksman",
            "mandalorian_heavy",
            "b1_battle_droid",
            "b1_security_droid",
            "b2_super_battle_droid",
            "commando_droid",
            "hutt_enforcer",
            "bounty_hunter",
            "smuggler",
            "nightsister_acolyte",
            "nightsister_archer",
            "nightbrother_brute",
            "republic_civilian",
            "togruta_civilian",
            "separatist_technician",
            "mandalorian_clansperson",
            "hutt_civilian",
            "nightsister_civilian"
    };
    private static final String[] FACTION_ITEMS = {
            "republic_plastoid_ingot",
            "mandalorian_fiber",
            "separatist_alloy_shard"
    };
    private static final String[] PHASE_I_ARMOR_ITEMS = {
            "phase_i_clone_helmet",
            "phase_i_clone_chestplate",
            "phase_i_clone_leggings",
            "phase_i_clone_boots"
    };

    private FactionContentRegistrationTest() {
    }

    public static void main(String[] args) throws IOException {
        factionRecruitEntitiesAreRegistered();
        factionRecruitEggsAndTexturesExist();
        factionItemsAreRegisteredAndModeled();
        phaseIArmorIsRegisteredCraftableAndModeled();

        System.out.println("FactionContentRegistrationTest passed");
    }

    private static void factionRecruitEntitiesAreRegistered() throws IOException {
        String entityTypes = read("src/main/java/galacticwars/clonewars/registry/ModEntityTypes.java");
        String items = read("src/main/java/galacticwars/clonewars/registry/ModItems.java");
        String client = read("src/main/java/galacticwars/clonewars/GalacticWarsClient.java");
        String common = read("src/main/java/galacticwars/clonewars/GalacticWars.java");
        String language = read("src/main/resources/assets/galacticwars/lang/en_us.json");

        for (String id : ENTITY_IDS) {
            assertContains(entityTypes, "\"" + id + "\"", "entity registration " + id);
            assertContains(items, "\"" + id + "_spawn_egg\"", "spawn egg registration " + id);
            assertContains(language, "\"entity.galacticwars." + id + "\"", "entity translation " + id);
        }
        assertContains(language,
                "\"item.galacticwars.hutt_enforcer_spawn_egg\": \"Trandoshan Enforcer Spawn Egg\"",
                "Trandoshan enforcer spawn egg translation");
        assertContains(client, "ModEntityTypes.recruits()", "data-driven renderer registration");
        assertContains(common, "ModEntityTypes.recruits()", "data-driven attributes and spawn placement");
    }

    private static void factionRecruitEggsAndTexturesExist() {
        for (String id : ENTITY_IDS) {
            assertRegularFile("src/main/resources/assets/galacticwars/items/" + id + "_spawn_egg.json");
            assertRegularFile("src/main/resources/assets/galacticwars/models/item/" + id + "_spawn_egg.json");
            assertRegularFile("src/main/resources/assets/galacticwars/textures/item/" + id + "_spawn_egg.png");
            assertRegularFile("src/main/resources/assets/galacticwars/textures/entity/" + id + ".png");
        }
    }

    private static void factionItemsAreRegisteredAndModeled() throws IOException {
        String items = read("src/main/java/galacticwars/clonewars/registry/ModItems.java");
        String creativeTabs = read("src/main/java/galacticwars/clonewars/registry/ModCreativeTabs.java");
        String language = read("src/main/resources/assets/galacticwars/lang/en_us.json");

        for (String id : FACTION_ITEMS) {
            assertContains(items, "\"" + id + "\"", "item registration " + id);
            assertContains(creativeTabs, id.toUpperCase(), "creative tab item " + id);
            assertContains(language, "\"item.galacticwars." + id + "\"", "item translation " + id);
            assertRegularFile("src/main/resources/assets/galacticwars/items/" + id + ".json");
            assertRegularFile("src/main/resources/assets/galacticwars/models/item/" + id + ".json");
            assertRegularFile("src/main/resources/assets/galacticwars/textures/item/" + id + ".png");
        }
    }

    private static void phaseIArmorIsRegisteredCraftableAndModeled() throws IOException {
        String items = read("src/main/java/galacticwars/clonewars/registry/ModItems.java");
        String creativeTabs = read("src/main/java/galacticwars/clonewars/registry/ModCreativeTabs.java");
        String language = read("src/main/resources/assets/galacticwars/lang/en_us.json");

        for (String id : PHASE_I_ARMOR_ITEMS) {
            assertContains(items, id.toUpperCase(), "Phase I armor registration " + id);
            assertContains(creativeTabs, id.toUpperCase(), "Phase I creative-tab entry " + id);
            assertContains(language, "\"item.galacticwars." + id + "\"", "Phase I armor translation " + id);
            assertRegularFile("src/main/resources/assets/galacticwars/items/" + id + ".json");
            assertRegularFile("src/main/resources/assets/galacticwars/models/item/" + id + ".json");
            assertRegularFile("src/main/resources/assets/galacticwars/textures/item/" + id + ".png");
            assertRegularFile("src/main/resources/data/galacticwars/recipe/" + id + ".json");
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
