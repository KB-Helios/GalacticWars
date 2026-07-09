package middleearth.lotr.warmod.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

public final class AssetReferenceIntegrityTest {
    private static final Path RESOURCE_ROOT = Path.of("src/main/resources");
    private static final Path MOD_ASSET_ROOT = RESOURCE_ROOT.resolve("assets/kingdomwarsmiddleearth");
    private static final Path MOD_DATA_ROOT = RESOURCE_ROOT.resolve("data/kingdomwarsmiddleearth");
    private static final Path BUILT_JAR = Path.of("build/libs/kingdomwarsmiddleearth-1.0.0.jar");
    private static final Pattern MOD_REFERENCE = Pattern.compile("\"(kingdomwarsmiddleearth:(?:block|item)/[^\"]+)\"");

    private AssetReferenceIntegrityTest() {
    }

    public static void main(String[] args) throws IOException {
        blockstatesReferenceExistingBlockModels();
        itemDefinitionsReferenceExistingItemModels();
        modelsReferenceExistingModTextures();
        recruitRenderTexturesExist();
        geckoRecruitAssetsExist();
        biomeSpawnModifiersReferenceRegisteredEntities();
        builtJarContainsModAssetsAndData();

        System.out.println("AssetReferenceIntegrityTest passed");
    }

    private static void blockstatesReferenceExistingBlockModels() throws IOException {
        try (Stream<Path> files = Files.walk(MOD_ASSET_ROOT.resolve("blockstates"))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                for (String reference : referencesIn(file)) {
                    if (reference.startsWith("kingdomwarsmiddleearth:block/")) {
                        assertRegularFile(modelPath(reference), "blockstate model reference " + reference);
                    }
                }
            }
        }
    }

    private static void itemDefinitionsReferenceExistingItemModels() throws IOException {
        try (Stream<Path> files = Files.walk(MOD_ASSET_ROOT.resolve("items"))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                for (String reference : referencesIn(file)) {
                    if (reference.startsWith("kingdomwarsmiddleearth:item/")) {
                        assertRegularFile(modelPath(reference), "item definition model reference " + reference);
                    }
                }
            }
        }
    }

    private static void modelsReferenceExistingModTextures() throws IOException {
        try (Stream<Path> files = Files.walk(MOD_ASSET_ROOT.resolve("models"))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                for (String reference : referencesIn(file)) {
                    assertRegularFile(texturePath(reference), "model texture reference " + reference);
                }
            }
        }
    }

    private static void recruitRenderTexturesExist() throws IOException {
        String client = Files.readString(Path.of("src/main/java/middleearth/lotr/warmod/KingdomWarsMiddleEarthClient.java"));
        String renderer = Files.readString(Path.of("src/main/java/middleearth/lotr/warmod/client/render/MiddleEarthRecruitRenderer.java"));

        assertContains(renderer, "GeoEntityRenderer", "GeckoLib recruit renderer");
        assertContains(client, "ModEntityTypes.GONDOR_RECRUIT.get()", "gondor recruit renderer");
        assertContains(client, "ModEntityTypes.ROHAN_RECRUIT.get()", "rohan recruit renderer");
        assertContains(client, "ModEntityTypes.MORDOR_ORC_RECRUIT.get()", "mordor recruit renderer");
        assertContains(client, "ModEntityTypes.DWARF_RECRUIT.get()", "dwarf recruit renderer");
        assertContains(client, "ModEntityTypes.ELF_RECRUIT.get()", "elf recruit renderer");

        assertRegularFile(MOD_ASSET_ROOT.resolve("textures/entity/gondor_recruit.png"), "gondor recruit texture");
        assertRegularFile(MOD_ASSET_ROOT.resolve("textures/entity/rohan_recruit.png"), "rohan recruit texture");
        assertRegularFile(MOD_ASSET_ROOT.resolve("textures/entity/mordor_orc_recruit.png"), "mordor recruit texture");
        assertRegularFile(MOD_ASSET_ROOT.resolve("textures/entity/dwarf_recruit.png"), "dwarf recruit texture");
        assertRegularFile(MOD_ASSET_ROOT.resolve("textures/entity/elf_recruit.png"), "elf recruit texture");
    }

    private static void geckoRecruitAssetsExist() {
        for (String recruit : Set.of("gondor_recruit", "rohan_recruit", "mordor_orc_recruit", "dwarf_recruit", "elf_recruit")) {
            assertRegularFile(MOD_ASSET_ROOT.resolve("geckolib/models/entity/" + recruit + ".geo.json"),
                    "GeckoLib model " + recruit);
            assertRegularFile(MOD_ASSET_ROOT.resolve("geckolib/animations/entity/" + recruit + ".animation.json"),
                    "GeckoLib animation " + recruit);
        }
    }

    private static void biomeSpawnModifiersReferenceRegisteredEntities() throws IOException {
        String entities = Files.readString(Path.of("src/main/java/middleearth/lotr/warmod/registry/ModEntityTypes.java"));
        assertContains(entities, "\"gondor_recruit\"", "gondor recruit entity registration");
        assertContains(entities, "\"rohan_recruit\"", "rohan recruit entity registration");
        assertContains(entities, "\"mordor_orc_recruit\"", "mordor recruit entity registration");
        assertContains(entities, "\"dwarf_recruit\"", "dwarf recruit entity registration");
        assertContains(entities, "\"elf_recruit\"", "elf recruit entity registration");

        try (Stream<Path> files = Files.walk(MOD_DATA_ROOT.resolve("neoforge/biome_modifier"))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String content = Files.readString(file);
                assertContains(content, "kingdomwarsmiddleearth:", "spawn modifier entity namespace");
                assertContains(content, "\"type\": \"neoforge:add_spawns\"", "spawn modifier type");
            }
        }
    }

    private static void builtJarContainsModAssetsAndData() throws IOException {
        assertRegularFile(BUILT_JAR, "built mod jar");
        try (ZipFile jar = new ZipFile(BUILT_JAR.toFile())) {
            for (String entry : sourceResourceEntries()) {
                if (jar.getEntry(entry) == null) {
                    throw new AssertionError("built jar missing resource <" + entry + ">");
                }
            }
        }
    }

    private static Set<String> sourceResourceEntries() throws IOException {
        HashSet<String> entries = new HashSet<>();
        try (Stream<Path> files = Files.walk(MOD_ASSET_ROOT)) {
            files.filter(Files::isRegularFile)
                    .map(RESOURCE_ROOT::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .forEach(entries::add);
        }
        try (Stream<Path> files = Files.walk(MOD_DATA_ROOT)) {
            files.filter(Files::isRegularFile)
                    .map(RESOURCE_ROOT::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .forEach(entries::add);
        }
        return entries;
    }

    private static Set<String> referencesIn(Path file) throws IOException {
        Matcher matcher = MOD_REFERENCE.matcher(Files.readString(file));
        HashSet<String> references = new HashSet<>();
        while (matcher.find()) {
            references.add(matcher.group(1));
        }
        return references;
    }

    private static Path modelPath(String reference) {
        String path = reference.substring("kingdomwarsmiddleearth:".length());
        return MOD_ASSET_ROOT.resolve("models").resolve(path + ".json");
    }

    private static Path texturePath(String reference) {
        String path = reference.substring("kingdomwarsmiddleearth:".length());
        return MOD_ASSET_ROOT.resolve("textures").resolve(path + ".png");
    }

    private static void assertContains(String haystack, String needle, String label) {
        if (!haystack.contains(needle)) {
            throw new AssertionError(label + " missing <" + needle + ">");
        }
    }

    private static void assertRegularFile(Path path, String label) {
        if (!Files.isRegularFile(path)) {
            throw new AssertionError(label + " missing <" + path + ">");
        }
    }
}
