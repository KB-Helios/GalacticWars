package galacticwars.clonewars;

import dev.architectury.platform.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/** Loader-neutral, YACL-backed common configuration. */
public final class Config {
    private static final Path CONFIG_PATH = Platform.getConfigFolder().resolve("galacticwars.properties");
    private static final Map<String, BooleanValue> VALUES = new LinkedHashMap<>();
    private static boolean loaded;

    public static final BooleanValue LOG_STARTUP = define("logStartup", true);
    public static final BooleanValue ALLOW_BLASTER_FRIENDLY_FIRE =
            define("allowBlasterFriendlyFire", false);
    public static final BooleanValue ALLOW_BLASTER_PVP = define("allowBlasterPvp", true);
    public static final BooleanValue ALLOW_FORCE_PVP = define("allowForcePvp", true);
    public static final BooleanValue ALLOW_FORCE_BLOCK_PHYSICS =
            define("allowForceBlockPhysics", true);
    public static final BooleanValue ALLOW_FORCE_VEHICLE_PHYSICS =
            define("allowForceVehiclePhysics", true);
    public static final BooleanValue ALLOW_CLASS_PVP = define("allowClassPvp", false);

    private Config() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.isRegularFile(CONFIG_PATH)) {
            save();
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
            properties.load(input);
        } catch (IOException exception) {
            GalacticWars.LOGGER.error("Unable to read {}", CONFIG_PATH, exception);
            return;
        }

        VALUES.forEach((key, value) -> {
            String encoded = properties.getProperty(key);
            if (encoded == null) {
                return;
            }
            if (encoded.equalsIgnoreCase("true") || encoded.equalsIgnoreCase("false")) {
                value.set(Boolean.parseBoolean(encoded));
            } else {
                GalacticWars.LOGGER.warn("Ignoring invalid boolean {}={} in {}", key, encoded, CONFIG_PATH);
            }
        });
    }

    public static synchronized void save() {
        Properties properties = new Properties();
        VALUES.forEach((key, value) -> properties.setProperty(key, Boolean.toString(value.get())));

        Path parent = CONFIG_PATH.getParent();
        Path temporary = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(parent);
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "Galactic Wars common configuration");
            }
            try {
                Files.move(temporary, CONFIG_PATH,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            GalacticWars.LOGGER.error("Unable to save {}", CONFIG_PATH, exception);
        }
    }

    private static BooleanValue define(String key, boolean defaultValue) {
        BooleanValue value = new BooleanValue(key, defaultValue);
        if (VALUES.putIfAbsent(key, value) != null) {
            throw new IllegalStateException("Duplicate configuration key " + key);
        }
        return value;
    }

    public static final class BooleanValue {
        private final String key;
        private final boolean defaultValue;
        private final AtomicBoolean value;

        private BooleanValue(String key, boolean defaultValue) {
            this.key = Objects.requireNonNull(key, "key");
            this.defaultValue = defaultValue;
            this.value = new AtomicBoolean(defaultValue);
        }

        public String key() {
            return key;
        }

        public boolean getDefault() {
            return defaultValue;
        }

        public boolean get() {
            return value.get();
        }

        public boolean getAsBoolean() {
            return get();
        }

        public void set(boolean nextValue) {
            value.set(nextValue);
        }
    }
}
