package galacticwars.clonewars.vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.AABB;

/** Preflighted owner-driven vehicle transfer with compensating rollback. */
public final class VehiclePlanetTravelPlan {
    private final ServerPlayer owner;
    private final ServerLevel source;
    private final ServerLevel destination;
    private final BlockPos target;
    private final GalacticVehicleEntity vehicle;
    private final List<ServerPlayer> passengers;
    private final double sourceX;
    private final double sourceY;
    private final double sourceZ;
    private final boolean accepted;
    private final String reason;

    private VehiclePlanetTravelPlan(
            ServerPlayer owner, ServerLevel destination, BlockPos target,
            GalacticVehicleEntity vehicle, List<ServerPlayer> passengers,
            boolean accepted, String reason
    ) {
        this.owner = owner;
        this.source = owner.level();
        this.destination = destination;
        this.target = target;
        this.vehicle = vehicle;
        this.passengers = List.copyOf(passengers);
        this.sourceX = vehicle == null ? owner.getX() : vehicle.getX();
        this.sourceY = vehicle == null ? owner.getY() : vehicle.getY();
        this.sourceZ = vehicle == null ? owner.getZ() : vehicle.getZ();
        this.accepted = accepted;
        this.reason = reason;
    }

    public static VehiclePlanetTravelPlan prepare(
            ServerPlayer owner, ServerLevel destination, BlockPos target
    ) {
        if (!(owner.getVehicle() instanceof GalacticVehicleEntity vehicle)) {
            return new VehiclePlanetTravelPlan(owner, destination, target, null, List.of(), true, "accepted");
        }
        if (vehicle.getFirstPassenger() != owner
                || vehicle.ownerId().filter(owner.getUUID()::equals).isEmpty()) {
            return new VehiclePlanetTravelPlan(owner, destination, target, vehicle, List.of(),
                    false, "vehicle_driver_not_authorized");
        }
        ArrayList<ServerPlayer> passengers = new ArrayList<>();
        for (Entity passenger : vehicle.getPassengers()) {
            if (!(passenger instanceof ServerPlayer player) || !vehicle.canTransferPassenger(player)) {
                return new VehiclePlanetTravelPlan(owner, destination, target, vehicle, List.of(),
                        false, "vehicle_passenger_not_transferable");
            }
            passengers.add(player);
        }
        AABB clearance = new AABB(target).inflate(
                Math.max(1.5D, vehicle.getBbWidth()), Math.max(2.0D, vehicle.getBbHeight()),
                Math.max(1.5D, vehicle.getBbWidth()));
        if (!destination.noCollision(vehicle, clearance)) {
            return new VehiclePlanetTravelPlan(owner, destination, target, vehicle, passengers,
                    false, "vehicle_clearance_unavailable");
        }
        return new VehiclePlanetTravelPlan(owner, destination, target, vehicle, passengers, true, "accepted");
    }

    public boolean accepted() { return accepted; }
    public String reason() { return reason; }
    public boolean transfersVehicle() { return vehicle != null; }

    public boolean transfer() {
        if (!accepted || vehicle == null) return accepted;
        vehicle.ejectPassengers();
        if (!vehicle.teleportTo(destination, target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D,
                Set.<Relative>of(), vehicle.getYRot(), vehicle.getXRot(), false)) {
            remountAtSource();
            return false;
        }
        ArrayList<ServerPlayer> moved = new ArrayList<>();
        for (int index = 0; index < passengers.size(); index++) {
            ServerPlayer passenger = passengers.get(index);
            boolean teleported = passenger.teleportTo(destination,
                    target.getX() + 0.5D + (index % 2) * 0.5D,
                    target.getY() + 0.2D,
                    target.getZ() + 0.5D + (index / 2) * 0.5D,
                    Set.<Relative>of(), passenger.getYRot(), passenger.getXRot(), false);
            if (!teleported) {
                moved.forEach(this::rollbackPlayer);
                rollbackVehicle();
                return false;
            }
            moved.add(passenger);
        }
        passengers.forEach(passenger -> passenger.startRiding(vehicle, true));
        return true;
    }

    private void rollbackVehicle() {
        vehicle.teleportTo(source, sourceX, sourceY, sourceZ, Set.<Relative>of(),
                vehicle.getYRot(), vehicle.getXRot(), false);
        passengers.forEach(this::rollbackPlayer);
        passengers.forEach(passenger -> passenger.startRiding(vehicle, true));
    }

    private void rollbackPlayer(ServerPlayer player) {
        player.teleportTo(source, sourceX, sourceY + 0.5D, sourceZ,
                Set.<Relative>of(), player.getYRot(), player.getXRot(), false);
    }

    private void remountAtSource() {
        passengers.forEach(passenger -> passenger.startRiding(vehicle, true, true));
    }
}
