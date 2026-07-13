package galacticwars.clonewars.force;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.progression.ForceSavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = GalacticWars.MODID)
public final class ForceRuntimeEvents {
    private ForceRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 5 != 0) return;
        ForceSavedData data = ForceSavedData.get(event.getServer().overworld());
        event.getServer().getPlayerList().getPlayers().forEach(player ->
                data.regenerate(player.getUUID(), 1));
    }
}
