package galacticwars.clonewars.client;

import com.mojang.blaze3d.platform.InputConstants;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.network.ForceActivatePayload;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

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
            "key.galacticwars.vehicle_fire", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    private ForceKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        for (KeyMapping key : KEYS) event.register(key);
        event.register(VEHICLE_FIRE);
    }

    public static void tick(ClientTickEvent.Post event) {
        for (int slot = 0; slot < KEYS.length; slot++) {
            while (KEYS[slot].consumeClick()) {
                ClientPacketDistributor.sendToServer(new ForceActivatePayload(UUID.randomUUID(), slot));
            }
        }
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player != null
                && minecraft.player.getVehicle() instanceof galacticwars.clonewars.vehicle.GalacticVehicleEntity vehicle) {
            float forward = (minecraft.options.keyUp.isDown() ? 1.0F : 0.0F)
                    - (minecraft.options.keyDown.isDown() ? 1.0F : 0.0F);
            float strafe = (minecraft.options.keyLeft.isDown() ? 1.0F : 0.0F)
                    - (minecraft.options.keyRight.isDown() ? 1.0F : 0.0F);
            boolean fire = VEHICLE_FIRE.consumeClick();
            ClientPacketDistributor.sendToServer(new galacticwars.clonewars.network.VehicleInputPayload(
                    UUID.randomUUID(), vehicle.getId(), forward, strafe,
                    minecraft.options.keyJump.isDown(), minecraft.options.keyShift.isDown(), fire));
        }
    }
}
