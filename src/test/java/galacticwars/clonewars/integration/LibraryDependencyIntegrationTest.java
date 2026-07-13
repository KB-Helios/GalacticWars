package galacticwars.clonewars.integration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

public final class LibraryDependencyIntegrationTest {
    private LibraryDependencyIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        String build = Files.readString(Path.of("build.gradle"));
        String properties = Files.readString(Path.of("gradle.properties"));
        String metadata = Files.readString(
                Path.of("src/main/templates/META-INF/neoforge.mods.toml"));
        String notice = Files.readString(Path.of("NOTICE.md"));

        assertContains(build,
                "net.tslat:smartbrainlib-neoforge-${minecraft_version}:${smartbrainlib_version}",
                "SmartBrainLib coordinate");
        assertContains(build, "curse.maven:framework-549225:${framework_curse_file}",
                "Framework coordinate");
        assertContains(properties, "smartbrainlib_version=2.0.0", "SmartBrainLib pin");
        assertContains(properties, "framework_version=0.13.26", "Framework pin");
        assertContains(properties, "framework_curse_file=8403586", "Framework file pin");
        assertContains(metadata, "modId=\"smartbrainlib\"", "SmartBrainLib required metadata");
        assertContains(metadata, "versionRange=\"[${smartbrainlib_version},3.0.0)\"",
                "SmartBrainLib compatibility range");
        assertContains(metadata, "modId=\"framework\"", "Framework required metadata");
        assertContains(metadata, "versionRange=\"[${framework_version},0.14.0)\"",
                "Framework compatibility range");
        assertContains(notice, "MPL-2.0", "SmartBrainLib license notice");
        assertContains(notice, "LGPL-2.1", "Framework license notice");

        Path artifact;
        try (var files = Files.list(Path.of("build/libs"))) {
            artifact = files.filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("built Galactic Wars JAR missing"));
        }
        try (JarFile jar = new JarFile(artifact.toFile())) {
            if (jar.getEntry("META-INF/jarjar/metadata.json") != null) {
                throw new AssertionError("External libraries must not be embedded with jarJar");
            }
        }
        System.out.println("LibraryDependencyIntegrationTest passed");
    }

    private static void assertContains(String value, String expected, String label) {
        if (!value.contains(expected)) {
            throw new AssertionError(label + " missing <" + expected + ">");
        }
    }
}
