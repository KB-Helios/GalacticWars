package galacticwars.clonewars.kingdom;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.settlement.CommandCenterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/** Bounded simulation for kingdom systems that must not depend on a menu interaction. */
@EventBusSubscriber(modid = GalacticWars.MODID)
public final class KingdomSimulationEvents {
    private static final int INTERVAL_TICKS = 20;
    private static final int KINGDOM_BUDGET = 4;
    private static int cursor;

    private KingdomSimulationEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % INTERVAL_TICKS != 0) {
            return;
        }
        KingdomSavedData data = KingdomSavedData.get(server.overworld());
        List<KingdomRecord> kingdoms = data.kingdoms();
        if (kingdoms.isEmpty()) {
            cursor = 0;
            return;
        }
        int processed = Math.min(KINGDOM_BUDGET, kingdoms.size());
        for (int offset = 0; offset < processed; offset++) {
            simulateLoadedKingdom(server, data, kingdoms.get((cursor + offset) % kingdoms.size()));
        }
        cursor = (cursor + processed) % kingdoms.size();
    }

    private static void simulateLoadedKingdom(
            MinecraftServer server,
            KingdomSavedData data,
            KingdomRecord kingdom
    ) {
        if (!data.isHallActive(kingdom.ownerId())) {
            return;
        }
        SettlementRecord settlement = kingdom.settlement();
        ServerLevel level;
        try {
            ResourceKey<Level> dimension = ResourceKey.create(
                    Registries.DIMENSION, Identifier.parse(settlement.dimensionId()));
            level = server.getLevel(dimension);
        } catch (RuntimeException exception) {
            GalacticWars.LOGGER.error("Skipping kingdom {} with invalid hall dimension {}",
                    kingdom.id(), settlement.dimensionId(), exception);
            return;
        }
        BlockPos hallPos = new BlockPos(settlement.hallX(), settlement.hallY(), settlement.hallZ());
        if (level == null || !level.hasChunkAt(hallPos)) {
            return;
        }
        if (level.getBlockEntity(hallPos) instanceof CommandCenterBlockEntity hall
                && kingdom.ownerId().equals(hall.ownerId())) {
            hall.chargeDailyUpkeep(level.getGameTime(), settlement.recruitIds().size());
        }
    }
}
