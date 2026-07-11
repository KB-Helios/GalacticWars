package galacticwars.clonewars.progression;

import java.util.UUID;

public final class VehicleRuntimeServiceTest {
    public static void main(String[] args) {
        UUID owner = UUID.randomUUID();
        UUID ally = UUID.randomUUID();
        UUID hostile = UUID.randomUUID();
        VehicleRuntimeState state = VehicleRuntimeService.create(
                "aat", owner, "galacticwars:separatist", "galacticwars:geonosis");

        var boarded = VehicleRuntimeService.board(state, owner, false);
        assertTrue(boarded.accepted() && boarded.changed(), "owner boards vehicle");
        state = boarded.state();
        var replay = VehicleRuntimeService.board(state, owner, false);
        assertTrue(replay.accepted() && !replay.changed(), "duplicate boarding is idempotent");
        state = VehicleRuntimeService.board(state, ally, true).state();
        assertTrue(!VehicleRuntimeService.board(state, hostile, false).accepted(),
                "hostile player cannot board");
        assertTrue(!VehicleRuntimeService.board(state, UUID.randomUUID(), true).accepted(),
                "seat capacity cannot be bypassed");

        int fuelBefore = state.fuel();
        var movement = VehicleRuntimeService.consumeFuel(state, owner, 10);
        assertTrue(movement.accepted() && movement.state().fuel() == fuelBefore - 10,
                "authorized movement consumes exact fuel");
        assertTrue(!VehicleRuntimeService.consumeFuel(state, hostile, 10).accepted()
                        && state.fuel() == fuelBefore,
                "unauthorized movement consumes no fuel");

        var transferred = VehicleRuntimeService.transfer(
                movement.state(), owner, "galacticwars:coruscant");
        assertTrue(transferred.accepted() && transferred.state().passengers().isEmpty()
                        && transferred.state().dimensionId().equals("galacticwars:coruscant"),
                "dimension transfer clears transient seats without orphaning ownership");
        var destroyed = VehicleRuntimeService.damage(transferred.state(), 999);
        assertTrue(destroyed.state().health() == 0 && destroyed.state().paused()
                        && destroyed.state().passengers().isEmpty(),
                "destroyed vehicle is inert and releases seats");
        System.out.println("VehicleRuntimeServiceTest passed");
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
