package galacticwars.clonewars.integration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModelAssetQualityTest {
    private static final Path GECKOLIB_MODELS =
            Path.of("src/main/resources/assets/galacticwars/geckolib/models");

    private ModelAssetQualityTest() {
    }

    public static void main(String[] args) throws Exception {
        assertDetailedVehicles();
        assertLongBladeLightsaber();
        System.out.println("ModelAssetQualityTest passed");
    }

    private static void assertDetailedVehicles() throws Exception {
        Map<String, Integer> minimumCubes = new LinkedHashMap<>();
        minimumCubes.put("barc_speeder", 20);
        minimumCubes.put("at_rt", 28);
        minimumCubes.put("stap", 20);
        minimumCubes.put("aat", 23);
        minimumCubes.put("laat_gunship", 40);
        for (Map.Entry<String, Integer> entry : minimumCubes.entrySet()) {
            Path path = GECKOLIB_MODELS.resolve("entity/vehicle/" + entry.getKey() + ".geo.json");
            String geometry = Files.readString(path);
            int cubes = occurrences(geometry, "\"origin\"");
            require(cubes >= entry.getValue(),
                    entry.getKey() + " remains placeholder-grade: " + cubes + " cubes");
            require(geometry.contains("\"texture_width\": 256")
                            && geometry.contains("\"texture_height\": 256"),
                    entry.getKey() + " must preserve its authored 256x256 atlas contract");
        }
    }

    private static void assertLongBladeLightsaber() throws Exception {
        String geometry = Files.readString(GECKOLIB_MODELS.resolve("item/lightsaber.geo.json"));
        require(geometry.contains("\"name\": \"hilt\"")
                        && geometry.contains("\"name\": \"blade\""),
                "lightsaber must use separate GeckoLib hilt and blade bones");
        require(geometry.contains("36.0") && occurrences(geometry, "\"origin\"") >= 24,
                "lightsaber must keep its long blade and segmented three-dimensional hilt");
    }

    private static int occurrences(String content, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = content.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
