package galacticwars.clonewars.integration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ModelAssetQualityTest {
    private static final Path GECKOLIB_MODELS =
            Path.of("src/main/resources/assets/galacticwars/geckolib/models");
    private static final Pattern BOX_UV_CUBE = Pattern.compile(
            "\\\"size\\\"\\s*:\\s*\\[\\s*([0-9.]+)\\s*,\\s*([0-9.]+)\\s*,\\s*([0-9.]+)\\s*]"
                    + ".*?\\\"uv\\\"\\s*:\\s*\\[\\s*([0-9]+)\\s*,\\s*([0-9]+)\\s*]",
            Pattern.DOTALL);

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
            assertDistinctBoundedBoxUvs(entry.getKey(), geometry, cubes);
        }
    }

    private static void assertDistinctBoundedBoxUvs(String vehicleId, String geometry, int cubeCount) {
        Matcher matcher = BOX_UV_CUBE.matcher(geometry);
        Set<String> origins = new HashSet<>();
        int mappedCubes = 0;
        while (matcher.find()) {
            int width = (int) Math.ceil(Double.parseDouble(matcher.group(1)));
            int height = (int) Math.ceil(Double.parseDouble(matcher.group(2)));
            int depth = (int) Math.ceil(Double.parseDouble(matcher.group(3)));
            int u = Integer.parseInt(matcher.group(4));
            int v = Integer.parseInt(matcher.group(5));
            require(u + 2 * (width + depth) <= 256 && v + height + depth <= 256,
                    vehicleId + " cube " + mappedCubes + " extends outside its 256x256 atlas");
            require(origins.add(u + "," + v),
                    vehicleId + " reuses box-UV origin " + u + "," + v);
            mappedCubes++;
        }
        require(mappedCubes == cubeCount,
                vehicleId + " must provide one box-UV mapping per cube");
    }

    private static void assertLongBladeLightsaber() throws Exception {
        Set<Integer> distinct = new HashSet<>();
        for (String color : Set.of("blue", "green", "red", "purple", "yellow", "white")) {
            String geometry = Files.readString(
                    GECKOLIB_MODELS.resolve("item/lightsaber/" + color + ".geo.json"));
            require(geometry.contains("\"name\": \"hilt\"")
                            && geometry.contains("\"name\": \"blade\""),
                    color + " lightsaber must use separate hilt and blade bones");
            require(geometry.contains("36.0") && occurrences(geometry, "\"origin\"") >= 26,
                    color + " lightsaber must keep its long blade and original volumetric hilt");
            distinct.add(geometry.hashCode());
        }
        require(distinct.size() == 6, "all six lightsaber hilts must have distinct geometry");
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
