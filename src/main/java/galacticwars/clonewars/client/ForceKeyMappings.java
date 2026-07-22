package galacticwars.clonewars.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.force.ForceActivationPhase;
import galacticwars.clonewars.network.ForceActivatePayload;
import galacticwars.clonewars.network.GalacticNetwork;
import galacticwars.clonewars.network.VehicleInputPayload;
import galacticwars.clonewars.vehicle.GalacticVehicleEntity;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ForceKeyMappings {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "abilities"));
    private static final KeyMapping[] KEYS = {
            new KeyMapping("key.galacticwars.force_1", InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_Z, CATEGORY),
            new KeyMapping("key.galacticwars.force_2", InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_X, CATEGORY),
            new KeyMapping("key.galacticwars.force_3", InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_C, CATEGORY)
    };
    private static final KeyMapping VEHICLE_FIRE = new KeyMapping(
            "key.galacticwars.vehicle_fire", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, CATEGORY);
    private static final KeyMapping VEHICLE_DESCEND = new KeyMapping(
            "key.galacticwars.vehicle_descend", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL, CATEGORY);
    private static final List<KeyMapping> MAPPINGS = List.of(
            KEYS[0], KEYS[1], KEYS[2], VEHICLE_FIRE, VEHICLE_DESCEND);
    private static final AtomicBoolean MAPPINGS_REGISTERED = new AtomicBoolean();
    private static final AtomicBoolean TICK_HANDLER_REGISTERED = new AtomicBoolean();
    private static final UUID[] ACTIVE_CAST_IDS = new UUID[KEYS.length];
    private static final boolean[] WAS_DOWN = new boolean[KEYS.length];

    private ForceKeyMappings() {
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

    public static void registerTickHandler() {
        if (TICK_HANDLER_REGISTERED.compareAndSet(false, true)) {
            ClientTickEvent.CLIENT_POST.register(ForceKeyMappings::tick);
        }
    }

    private static void tick(Minecraft minecraft) {
        boolean acceptsGameplayInput = minecraft.player != null
                && minecraft.gui.screen() == null
                && minecraft.gui.overlay() == null;
        for (int slot = 0; slot < KEYS.length; slot++) {
            if (minecraft.player == null) {
                ACTIVE_CAST_IDS[slot] = null;
                WAS_DOWN[slot] = false;
                while (KEYS[slot].consumeClick()) { /* clear stale disconnected input */ }
                continue;
            }
            boolean down = acceptsGameplayInput && KEYS[slot].isDown();
            if (down && !WAS_DOWN[slot]) {
                ACTIVE_CAST_IDS[slot] = UUID.randomUUID();
                GalacticNetwork.CHANNEL.sendToServer(new ForceActivatePayload(
                        ACTIVE_CAST_IDS[slot], slot, ForceActivationPhase.PRESS));
            } else if (!down && WAS_DOWN[slot] && ACTIVE_CAST_IDS[slot] != null) {
                GalacticNetwork.CHANNEL.sendToServer(new ForceActivatePayload(
                        ACTIVE_CAST_IDS[slot], slot,
                        acceptsGameplayInput
                                ? ForceActivationPhase.RELEASE
                                : ForceActivationPhase.CANCEL));
                ACTIVE_CAST_IDS[slot] = null;
            }
            WAS_DOWN[slot] = down;
            while (KEYS[slot].consumeClick()) { /* consume vanilla's queued clicks */ }
        }
        boolean fire = VEHICLE_FIRE.consumeClick();
        if (acceptsGameplayInput
                && minecraft.player.getVehicle() instanceof GalacticVehicleEntity vehicle) {
            float forward = (minecraft.options.keyUp.isDown() ? 1.0F : 0.0F)
                    - (minecraft.options.keyDown.isDown() ? 1.0F : 0.0F);
            float strafe = (minecraft.options.keyLeft.isDown() ? 1.0F : 0.0F)
                    - (minecraft.options.keyRight.isDown() ? 1.0F : 0.0F);
            GalacticNetwork.CHANNEL.sendToServer(new VehicleInputPayload(
                    UUID.randomUUID(), vehicle.getId(), forward, strafe,
                    minecraft.options.keyJump.isDown(), VEHICLE_DESCEND.isDown(), fire));
        }
    }
}
