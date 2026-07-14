package galacticwars.clonewars.network;

import com.mrcrayfish.framework.api.FrameworkAPI;
import com.mrcrayfish.framework.api.network.FrameworkNetwork;
import com.mrcrayfish.framework.api.network.PlayMessageContext;
import com.mrcrayfish.framework.api.registry.RegistryContainer;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.client.ForceClientState;
import galacticwars.clonewars.force.ForceWorldEffectService;
import galacticwars.clonewars.menu.CommandCenterOperationsMenu;
import galacticwars.clonewars.menu.MerchantTradeMenu;
import galacticwars.clonewars.vehicle.GalacticVehicleEntity;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

@RegistryContainer
public final class GalacticNetwork {
    public static final FrameworkNetwork CHANNEL = FrameworkAPI
            .createNetworkBuilder(
                    Identifier.fromNamespaceAndPath(GalacticWars.MODID, "main"), 1)
            .registerPlayMessage("force_activate", ForceActivatePayload.class,
                    ForceActivatePayload.STREAM_CODEC, GalacticNetwork::handleForceActivate,
                    PacketFlow.SERVERBOUND)
            .registerPlayMessage("vehicle_input", VehicleInputPayload.class,
                    VehicleInputPayload.STREAM_CODEC, GalacticNetwork::handleVehicleInput,
                    PacketFlow.SERVERBOUND)
            .registerPlayMessage("menu_action", MenuActionPayload.class,
                    MenuActionPayload.STREAM_CODEC, GalacticNetwork::handleMenuAction,
                    PacketFlow.SERVERBOUND)
            .registerPlayMessage("force_hud", ForceHudPayload.class,
                    ForceHudPayload.STREAM_CODEC, GalacticNetwork::handleForceHud,
                    PacketFlow.CLIENTBOUND)
            .registerPlayMessage("command_center_state", CommandCenterStatePayload.class,
                    CommandCenterStatePayload.STREAM_CODEC, GalacticNetwork::handleCommandCenterState,
                    PacketFlow.CLIENTBOUND)
            .build();

    private GalacticNetwork() {
    }

    private static void handleForceActivate(
            ForceActivatePayload payload, PlayMessageContext context
    ) {
        if (context.getPlayer().orElse(null) instanceof ServerPlayer player) {
            context.execute(() -> ForceWorldEffectService.activate(
                    player, payload.activationId(), payload.slot()));
        }
        context.setHandled(true);
    }

    private static void handleVehicleInput(
            VehicleInputPayload payload, PlayMessageContext context
    ) {
        if (context.getPlayer().orElse(null) instanceof ServerPlayer player) {
            context.execute(() -> {
                if (player.level().getEntity(payload.entityId()) instanceof GalacticVehicleEntity vehicle) {
                    vehicle.applyInput(player, payload.replayId(), payload.forward(), payload.strafe(),
                            payload.ascend(), payload.descend(), payload.fire());
                }
            });
        }
        context.setHandled(true);
    }

    private static void handleMenuAction(
            MenuActionPayload payload, PlayMessageContext context
    ) {
        if (context.getPlayer().orElse(null) instanceof ServerPlayer player) {
            context.execute(() -> {
                if (player.containerMenu.containerId != payload.containerId()
                        || payload.actionId() < 0 || payload.actionId() > 255) {
                    return;
                }
                if (player.containerMenu instanceof CommandCenterOperationsMenu operations) {
                    operations.handleReplayAction(
                            player, payload.replayId(), payload.actionId(),
                            payload.primaryTargetId(), payload.secondaryTargetId());
                } else if (player.containerMenu instanceof MerchantTradeMenu merchant) {
                    merchant.handleReplayAction(player, payload.replayId(), payload.actionId());
                }
            });
        }
        context.setHandled(true);
    }

    private static void handleForceHud(ForceHudPayload payload, PlayMessageContext context) {
        context.execute(() -> ForceClientState.update(payload));
        context.setHandled(true);
    }

    private static void handleCommandCenterState(
            CommandCenterStatePayload payload,
            PlayMessageContext context
    ) {
        context.execute(() -> context.getPlayer().ifPresent(player -> {
            if (player.containerMenu instanceof CommandCenterOperationsMenu operations
                    && operations.containerId == payload.containerId()) {
                operations.applyClientDashboard(payload.state());
            }
        }));
        context.setHandled(true);
    }
}
