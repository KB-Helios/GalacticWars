package middleearth.lotr.warmod.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GeckoLibDependencyMetadataTest {
    private GeckoLibDependencyMetadataTest() {
    }

    public static void main(String[] args) throws IOException {
        String gradleProperties = read("gradle.properties");
        String buildGradle = read("build.gradle");
        String modMetadata = read("src/main/templates/META-INF/neoforge.mods.toml");
        String notice = read("NOTICE.md");
        String readme = read("README.md");

        assertContains(gradleProperties, "geckolib_version=5.5.3", "GeckoLib version property");
        assertContains(buildGradle, "https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/", "GeckoLib Maven repository");
        assertContains(buildGradle,
                "implementation \"com.geckolib:geckolib-neoforge-${minecraft_version}:${geckolib_version}\"",
                "GeckoLib NeoForge dependency");
        assertContains(modMetadata, "modId=\"geckolib\"", "GeckoLib mod metadata dependency");
        assertContains(modMetadata, "versionRange=\"[${geckolib_version},)\"", "GeckoLib metadata version range");
        assertContains(notice, "GeckoLib", "GeckoLib notice");
        assertContains(readme, "GeckoLib", "GeckoLib README");

        System.out.println("GeckoLibDependencyMetadataTest passed");
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private static void assertContains(String haystack, String needle, String label) {
        if (!haystack.contains(needle)) {
            throw new AssertionError(label + " missing <" + needle + ">");
        }
    }
}
