package galacticwars.clonewars.vehicle;

import galacticwars.clonewars.progression.ProgressionEvent;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionSavedData;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public final class VehicleDeploymentKitItem extends Item {
    private final Supplier<EntityType<GalacticVehicleEntity>> entityType;
    private final String vehicleId;

    public VehicleDeploymentKitItem(
            Supplier<EntityType<GalacticVehicleEntity>> entityType,
            String vehicleId,
            Properties properties
    ) {
        super(properties.stacksTo(1));
        this.entityType = entityType;
        this.vehicleId = vehicleId;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null) {
            return context.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        var player = context.getPlayer();
        ProgressionSavedData progression = ProgressionSavedData.get(level);
        var state = progression.state(player.getUUID());
        var definition = galacticwars.clonewars.progression.LaunchContentCatalog.data().vehicles().get(vehicleId);
        boolean missingRequirement = definition != null && definition.deploymentRequirements().stream()
                .anyMatch(requirement -> !requirementSatisfied(state, requirement));
        if (definition == null || state.factionId().isEmpty()
                || (!player.hasInfiniteMaterials() && missingRequirement)) {
            return InteractionResult.FAIL;
        }
        GalacticVehicleEntity vehicle = entityType.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (vehicle == null) return InteractionResult.FAIL;
        vehicle.setPos(Vec3.atCenterOf(context.getClickedPos().relative(context.getClickedFace())));
        vehicle.setYRot(player.getYRot());
        vehicle.deploy(player.getUUID(), state.factionId());
        if (!level.addFreshEntity(vehicle)) return InteractionResult.FAIL;
        UUID eventId = UUID.nameUUIDFromBytes(("vehicle:deployed:" + vehicle.getUUID())
                .getBytes(StandardCharsets.UTF_8));
        progression.apply(new ProgressionEvent(eventId, player.getUUID(),
                ProgressionEventType.VEHICLE_ACQUIRED, vehicleId, 1));
        if (!player.hasInfiniteMaterials()) context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    private static boolean requirementSatisfied(
            galacticwars.clonewars.progression.ProgressionState state, String requirement
    ) {
        return requirement.equals("supply_depot")
                ? state.hasSubjectPath(ProgressionEventType.BUILDING_COMPLETED, "supply_depot")
                : state.unlocks().contains(requirement)
                || state.hasSubject(ProgressionEventType.QUEST_ADVANCED, requirement);
    }
}
