package galacticwars.clonewars.network;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.army.ArmyFieldCommandService;
import galacticwars.clonewars.force.ForceWorldEffectService;
import galacticwars.clonewars.classes.PlayerClassRuntime;
import galacticwars.clonewars.menu.CommandCenterOperationsMenu;
import galacticwars.clonewars.menu.MerchantTradeMenu;
import galacticwars.clonewars.vehicle.GalacticVehicleEntity;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** Loader-neutral play networking backed by Architectury's custom-payload bridge. */
public final class GalacticNetwork {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(GalacticWars.MODID, "main");
    public static final Channel CHANNEL = new Channel();

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private GalacticNetwork() {
    }

    public static void init() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        NetworkManager.registerC2S(
                ForceActivatePayload.TYPE,
                ForceActivatePayload.STREAM_CODEC,
                GalacticNetwork::handleForceActivate);
        NetworkManager.registerC2S(
                ClassActivatePayload.TYPE,
                ClassActivatePayload.STREAM_CODEC,
                GalacticNetwork::handleClassActivate);
        NetworkManager.registerC2S(
                ClassSelectPayload.TYPE,
                ClassSelectPayload.STREAM_CODEC,
                GalacticNetwork::handleClassSelect);
        NetworkManager.registerC2S(
                VehicleInputPayload.TYPE,
                VehicleInputPayload.STREAM_CODEC,
                GalacticNetwork::handleVehicleInput);
        NetworkManager.registerC2S(
                MenuActionPayload.TYPE,
                MenuActionPayload.STREAM_CODEC,
                GalacticNetwork::handleMenuAction);
        NetworkManager.registerC2S(
                FieldCommandRequestPayload.TYPE,
                FieldCommandRequestPayload.STREAM_CODEC,
                GalacticNetwork::handleFieldCommand);
        NetworkManager.registerS2C(
                ForceHudPayload.TYPE,
                ForceHudPayload.STREAM_CODEC,
                GalacticNetwork::handleForceHud);
        NetworkManager.registerS2C(
                ClassHudPayload.TYPE,
                ClassHudPayload.STREAM_CODEC,
                GalacticNetwork::handleClassHud);
        NetworkManager.registerS2C(
                CommandCenterStatePayload.TYPE,
                CommandCenterStatePayload.STREAM_CODEC,
                GalacticNetwork::handleCommandCenterState);
        NetworkManager.registerS2C(
                FieldCommandStatePayload.TYPE,
                FieldCommandStatePayload.STREAM_CODEC,
                GalacticNetwork::handleFieldCommandState);
        NetworkManager.registerS2C(
                GameplayCatalogPayload.TYPE,
                GameplayCatalogPayload.STREAM_CODEC,
                GalacticNetwork::handleGameplayCatalog);
        PlayerEvent.PLAYER_QUIT.register(player -> ArmyFieldCommandService.clearReplayHistory(player.getUUID()));
        GameplayCatalogSync.register();
    }

    public static boolean canPlayerReceive(
            ServerPlayer player,
            CustomPacketPayload.Type<?> payloadType
    ) {
        return NetworkManager.canPlayerReceive(
                Objects.requireNonNull(player, "player"),
                Objects.requireNonNull(payloadType, "payloadType"));
    }

    private static void handleForceActivate(
            ForceActivatePayload payload,
            NetworkManager.PacketContext context
    ) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            context.queue(() -> ForceWorldEffectService.activate(
                    player, payload.activationId(), payload.slot()));
        }
    }

    private static void handleClassActivate(
            ClassActivatePayload payload,
            NetworkManager.PacketContext context
    ) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            context.queue(() -> PlayerClassRuntime.activate(
                    player, payload.activationId(), payload.slot()));
        }
    }

    private static void handleClassSelect(
            ClassSelectPayload payload,
            NetworkManager.PacketContext context
    ) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            context.queue(() -> PlayerClassRuntime.select(
                    player, payload.requestId(), payload.classId()));
        }
    }

    private static void handleVehicleInput(
            VehicleInputPayload payload,
            NetworkManager.PacketContext context
    ) {
        if (!(context.getPlayer() instanceof ServerPlayer player)
                || !Float.isFinite(payload.forward())
                || !Float.isFinite(payload.strafe())) {
            return;
        }
        context.queue(() -> {
            if (player.level().getEntity(payload.entityId()) instanceof GalacticVehicleEntity vehicle) {
                vehicle.applyInput(
                        player,
                        payload.replayId(),
                        payload.forward(),
                        payload.strafe(),
                        payload.ascend(),
                        payload.descend(),
                        payload.fire());
            }
        });
    }

    private static void handleMenuAction(
            MenuActionPayload payload,
            NetworkManager.PacketContext context
    ) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        context.queue(() -> {
            if (player.containerMenu.containerId != payload.containerId()
                    || payload.actionId() < 0 || payload.actionId() > 255) {
                return;
            }
            if (player.containerMenu instanceof CommandCenterOperationsMenu operations) {
                operations.handleReplayAction(
                        player,
                        payload.replayId(),
                        payload.actionId(),
                        payload.primaryTargetId(),
                        payload.secondaryTargetId());
            } else if (player.containerMenu instanceof MerchantTradeMenu merchant) {
                merchant.handleReplayAction(player, payload.replayId(), payload.actionId());
            }
        });
    }

    private static void handleFieldCommand(
            FieldCommandRequestPayload payload,
            NetworkManager.PacketContext context
    ) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            context.queue(() -> CHANNEL.sendToPlayer(
                    () -> player,
                    ArmyFieldCommandService.execute(player, payload)));
        }
    }

    private static void handleForceHud(
            ForceHudPayload payload,
            NetworkManager.PacketContext context
    ) {
        context.queue(() -> ClientPacketBridge.handleForceHud(payload));
    }

    private static void handleClassHud(
            ClassHudPayload payload,
            NetworkManager.PacketContext context
    ) {
        context.queue(() -> ClientPacketBridge.handleClassHud(payload));
    }

    private static void handleCommandCenterState(
            CommandCenterStatePayload payload,
            NetworkManager.PacketContext context
    ) {
        context.queue(() -> {
            var player = context.getPlayer();
            if (player == null) {
                return;
            }
            if (player.containerMenu instanceof CommandCenterOperationsMenu operations
                    && operations.containerId == payload.containerId()) {
                operations.applyClientDashboard(payload.state());
            }
        });
    }

    private static void handleFieldCommandState(
            FieldCommandStatePayload payload,
            NetworkManager.PacketContext context
    ) {
        context.queue(() -> ClientPacketBridge.handleFieldCommandState(payload));
    }

    private static void handleGameplayCatalog(
            GameplayCatalogPayload payload,
            NetworkManager.PacketContext context
    ) {
        context.queue(() -> ClientPacketBridge.handleGameplayCatalog(payload));
    }

    /** Compatibility surface retained for existing `.get()`-style menu and keybinding call sites. */
    public static final class Channel {
        private Channel() {
        }

        public Identifier id() {
            return ID;
        }

        public void sendToPlayer(
                Supplier<ServerPlayer> playerSupplier,
                CustomPacketPayload payload
        ) {
            ServerPlayer player = Objects.requireNonNull(playerSupplier, "playerSupplier").get();
            Objects.requireNonNull(payload, "payload");
            if (player != null && NetworkManager.canPlayerReceive(player, payload.type())) {
                NetworkManager.sendToPlayer(player, payload);
            }
        }

        public void sendToServer(CustomPacketPayload payload) {
            Objects.requireNonNull(payload, "payload");
            if (NetworkManager.canServerReceive(payload.type())) {
                NetworkManager.sendToServer(payload);
            }
        }
    }
}
