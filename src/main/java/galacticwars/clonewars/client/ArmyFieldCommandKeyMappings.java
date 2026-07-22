package galacticwars.clonewars.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.client.gui.ArmyFieldCommandScreen;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Rebindable in-world command panel toggle. */
public final class ArmyFieldCommandKeyMappings {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "army_commands"));
    private static final KeyMapping COMMAND_SCREEN = new KeyMapping(
            "key.galacticwars.army_command", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    private static final List<KeyMapping> MAPPINGS = List.of(COMMAND_SCREEN);
    private static final AtomicBoolean MAPPINGS_REGISTERED = new AtomicBoolean();
    private static final AtomicBoolean TICK_HANDLER_REGISTERED = new AtomicBoolean();

    private ArmyFieldCommandKeyMappings() {
    }

    public static void register() {
        if (MAPPINGS_REGISTERED.compareAndSet(false, true)) {
            MAPPINGS.forEach(KeyMappingRegistry::register);
        }
        registerTickHandler();
    }

    public static List<KeyMapping> mappings() {
        return MAPPINGS;
    }

    public static boolean matchesCommandScreen(KeyEvent event) {
        return COMMAND_SCREEN.matches(event);
    }

    /** Shared entry point for the key mapping and the Tactical Command Marker. */
    public static void openCommandScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && minecraft.gui.screen() == null
                && minecraft.gui.overlay() == null) {
            minecraft.setScreenAndShow(new ArmyFieldCommandScreen());
        }
    }

    public static void registerTickHandler() {
        if (TICK_HANDLER_REGISTERED.compareAndSet(false, true)) {
            ClientTickEvent.CLIENT_POST.register(ArmyFieldCommandKeyMappings::tick);
        }
    }

    private static void tick(Minecraft minecraft) {
        while (COMMAND_SCREEN.consumeClick()) {
            openCommandScreen();
        }
    }
}
