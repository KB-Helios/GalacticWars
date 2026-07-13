package galacticwars.clonewars.network;

import galacticwars.clonewars.force.ForceWorldEffectService;
import net.minecraft.server.level.ServerPlayer;
import galacticwars.clonewars.vehicle.GalacticVehicleEntity;
import galacticwars.clonewars.menu.CommandCenterOperationsMenu;
import galacticwars.clonewars.menu.MerchantTradeMenu;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class GalacticNetwork {
    private GalacticNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                ForceActivatePayload.TYPE,
                ForceActivatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        ForceWorldEffectService.activate(player, payload.activationId(), payload.slot());
                    }
                }).playToServer(VehicleInputPayload.TYPE, VehicleInputPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.level().getEntity(payload.entityId()) instanceof GalacticVehicleEntity vehicle) {
                        vehicle.applyInput(player, payload.replayId(), payload.forward(), payload.strafe(),
                                payload.ascend(), payload.descend(), payload.fire());
                    }
                }).playToServer(MenuActionPayload.TYPE, MenuActionPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)
                            || player.containerMenu.containerId != payload.containerId()
                            || payload.actionId() < 0 || payload.actionId() > 255) return;
                    if (player.containerMenu instanceof CommandCenterOperationsMenu operations) {
                        operations.handleReplayAction(player, payload.replayId(), payload.actionId());
                    } else if (player.containerMenu instanceof MerchantTradeMenu merchant) {
                        merchant.handleReplayAction(player, payload.replayId(), payload.actionId());
                    }
                }).playToClient(ForceHudPayload.TYPE, ForceHudPayload.STREAM_CODEC);
    }
}
