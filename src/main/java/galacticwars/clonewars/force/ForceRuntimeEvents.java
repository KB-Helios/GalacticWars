package galacticwars.clonewars.force;

import galacticwars.clonewars.progression.ForceSavedData;
import net.minecraft.server.MinecraftServer;

/** Loader-neutral Force regeneration and authoritative HUD synchronization. */
public final class ForceRuntimeEvents {
    private ForceRuntimeEvents() {
    }

    public static void onServerTick(MinecraftServer server) {
        ForceWorldEffectService.tickChannels(server);
        ForceCollisionDamageService.tick(server);
        ForceBlockTelekinesisService.tick(server);
        if (server.getTickCount() % 5 != 0) return;
        ForceSavedData data = ForceSavedData.get(server.overworld());
        boolean synchronizeHud = server.getTickCount() % 20 == 0;
        server.getPlayerList().getPlayers().forEach(player -> {
            data.regenerate(player.getUUID(), 1);
            if (synchronizeHud) {
                ForceWorldEffectService.syncSnapshot(player, data);
            }
        });
    }
}
