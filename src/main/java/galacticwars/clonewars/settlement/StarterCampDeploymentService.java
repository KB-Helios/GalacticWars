package galacticwars.clonewars.settlement;

import galacticwars.clonewars.army.ArmyUnitDefinition;
import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.faction.FactionDefinition;
import galacticwars.clonewars.kingdom.KingdomActionId;
import galacticwars.clonewars.kingdom.KingdomGameplayAction;
import galacticwars.clonewars.kingdom.KingdomGameplayResult;
import galacticwars.clonewars.kingdom.KingdomGameplayRuntimeService;
import galacticwars.clonewars.kingdom.KingdomGameplayTransactionService;
import galacticwars.clonewars.kingdom.BuildProject;
import galacticwars.clonewars.kingdom.KingdomRecord;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.recruitment.RecruitDuty;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.particles.ParticleTypes;

/** Orchestrates the one-time embodied onboarding camp without bypassing server authority. */
public final class StarterCampDeploymentService {
    private StarterCampDeploymentService() {
    }

    public static Result deploy(
            ServerLevel level,
            ServerPlayer actor,
            CommandCenterBlockEntity hall
    ) {
        return deploy(level, actor, hall, rotationSteps(hall));
    }

    public static Result deploy(
            ServerLevel level,
            ServerPlayer actor,
            CommandCenterBlockEntity hall,
            int requestedRotationSteps
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(hall, "hall");
        if (requestedRotationSteps < 0 || requestedRotationSteps > 3) {
            return Result.rejected("invalid_rotation", Optional.empty());
        }
        KingdomSavedData data = KingdomSavedData.get(level);
        KingdomRecord kingdom = data.kingdomForOwner(actor.getUUID()).orElse(null);
        if (kingdom == null || !hall.isOwner(actor)
                || kingdom.settlement().hallX() != hall.getBlockPos().getX()
                || kingdom.settlement().hallY() != hall.getBlockPos().getY()
                || kingdom.settlement().hallZ() != hall.getBlockPos().getZ()
                || !kingdom.settlement().dimensionId().equals(level.dimension().identifier().toString())) {
            return Result.rejected("kingdom_changed", Optional.empty());
        }
        KingdomBaseBlueprint blueprint = GameplayDataManager.snapshot()
                .blueprint(KingdomBaseBlueprint.STARTER_CAMP_ID).orElse(null);
        FactionDefinition faction = GameplayDataManager.snapshot().faction(kingdom.factionId()).orElse(null);
        if (blueprint == null || faction == null) {
            return Result.rejected("data_missing", Optional.empty());
        }

        StarterCampDeployment deployment = data.starterCampDeployment(kingdom.id()).orElse(null);
        if (deployment == null) {
            deployment = StarterCampDeployment.awaiting(
                    kingdom.id(), level.dimension().identifier().toString(),
                    hall.getBlockPos().getX(), hall.getBlockPos().getY(), hall.getBlockPos().getZ(),
                    requestedRotationSteps);
            if (!data.storeStarterCampDeployment(deployment, -1)) {
                return Result.rejected("state_changed", data.starterCampDeployment(kingdom.id()));
            }
        }
        if (deployment.phase() == StarterCampDeploymentPhase.COMPLETE) {
            return Result.accepted("already_complete", deployment);
        }
        if (deployment.phase() == StarterCampDeploymentPhase.PACKED_UP) {
            StarterCampDeployment relocated = deployment.relocate(
                    level.dimension().identifier().toString(),
                    hall.getBlockPos().getX(), hall.getBlockPos().getY(), hall.getBlockPos().getZ(),
                    requestedRotationSteps);
            deployment = store(data, deployment, relocated);
        }
        if (deployment.projectId().isEmpty()
                && deployment.rotationSteps() != requestedRotationSteps) {
            deployment = store(data, deployment, deployment.reorient(requestedRotationSteps));
        }

        BlockPos origin = new BlockPos(deployment.originX(), deployment.originY(), deployment.originZ());
        ConstructionProjectService.ActionResult preflight = ConstructionProjectService.preflight(
                level, actor, kingdom.id(), blueprint, origin, deployment.rotationSteps());
        if (!preflight.accepted()) {
            showFootprint(level, blueprint, origin, deployment.rotationSteps(), false);
            deployment = store(data, deployment, deployment.blocked(preflight.reason()));
            return Result.rejected(preflight.reason(), Optional.of(deployment));
        }
        showFootprint(level, blueprint, origin, deployment.rotationSteps(), true);

        if (!deployment.suppliesGranted()) {
            if (!hall.depositStarterSupplies(blueprint.requiredResources())) {
                deployment = store(data, deployment, deployment.blocked("storage_full"));
                return Result.rejected("storage_full", Optional.of(deployment));
            }
            deployment = store(data, deployment, deployment.withSuppliesGranted());
        }

        GalacticRecruitEntity builder = resolveBuilder(level, actor, kingdom, faction, deployment);
        if (builder == null) {
            String reason = deployment.contractGranted() ? "builder_unavailable" : "builder_spawn_failed";
            deployment = store(data, deployment, deployment.blocked(reason));
            return Result.rejected(reason, Optional.of(deployment));
        }
        if (deployment.builderId().filter(builder.getUUID()::equals).isEmpty()) {
            deployment = store(data, deployment, deployment.withBuilder(builder.getUUID()));
        }

        ConstructionProjectService.StartResult start = ConstructionProjectService.startStarter(
                level, actor, builder, blueprint, origin, deployment.rotationSteps());
        if (!start.accepted()) {
            deployment = store(data, deployment, deployment.blocked(start.reason()));
            return Result.rejected(start.reason(), Optional.of(deployment));
        }
        deployment = store(data, deployment, deployment.building(start.project().orElseThrow().id()));
        return Result.accepted("building", deployment);
    }

    /** Validates and renders a proposed footprint without granting or consuming anything. */
    public static Result preview(
            ServerLevel level,
            ServerPlayer actor,
            CommandCenterBlockEntity hall,
            int requestedRotationSteps
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(hall, "hall");
        if (requestedRotationSteps < 0 || requestedRotationSteps > 3) {
            return Result.rejected("invalid_rotation", Optional.empty());
        }
        KingdomSavedData data = KingdomSavedData.get(level);
        KingdomRecord kingdom = data.kingdomForOwner(actor.getUUID()).orElse(null);
        if (kingdom == null || !hall.isOwner(actor)
                || kingdom.settlement().hallX() != hall.getBlockPos().getX()
                || kingdom.settlement().hallY() != hall.getBlockPos().getY()
                || kingdom.settlement().hallZ() != hall.getBlockPos().getZ()
                || !kingdom.settlement().dimensionId().equals(level.dimension().identifier().toString())) {
            return Result.rejected("kingdom_changed", Optional.empty());
        }
        KingdomBaseBlueprint blueprint = GameplayDataManager.snapshot()
                .blueprint(KingdomBaseBlueprint.STARTER_CAMP_ID).orElse(null);
        if (blueprint == null) {
            return Result.rejected("data_missing", Optional.empty());
        }
        StarterCampDeployment existing = data.starterCampDeployment(kingdom.id()).orElse(null);
        if (existing != null && existing.phase() == StarterCampDeploymentPhase.COMPLETE) {
            return Result.accepted("already_complete", existing);
        }
        if (existing != null && existing.projectId().isPresent()) {
            return Result.rejected("project_active", Optional.of(existing));
        }
        BlockPos origin = hall.getBlockPos();
        ConstructionProjectService.ActionResult preflight = ConstructionProjectService.preflight(
                level, actor, kingdom.id(), blueprint, origin, requestedRotationSteps);
        showFootprint(level, blueprint, origin, requestedRotationSteps, preflight.accepted());
        return preflight.accepted()
                ? Result.accepted("footprint_valid", existing == null
                        ? StarterCampDeployment.awaiting(
                                kingdom.id(), level.dimension().identifier().toString(),
                                origin.getX(), origin.getY(), origin.getZ(), requestedRotationSteps)
                        : existing.reorient(requestedRotationSteps))
                : Result.rejected(preflight.reason(), Optional.ofNullable(existing));
    }

    public static boolean packUp(ServerLevel level, UUID ownerId) {
        KingdomSavedData data = KingdomSavedData.get(level);
        KingdomRecord kingdom = data.kingdomForOwner(ownerId).orElse(null);
        if (kingdom == null) {
            return false;
        }
        StarterCampDeployment deployment = data.starterCampDeployment(kingdom.id()).orElse(null);
        if (deployment == null || deployment.terminal()
                || deployment.phase() == StarterCampDeploymentPhase.PACKED_UP) {
            return false;
        }
        GalacticRecruitEntity builder = deployment.builderId()
                .map(builderId -> findLoadedRecruit(level, builderId)).orElse(null);
        if (deployment.builderId().isPresent()
                && (builder == null || !builder.packUpStarterConstruction())) {
            return false;
        }
        deployment.projectId().flatMap(projectId -> kingdom.settlement().buildProjects().stream()
                        .filter(project -> project.id().equals(projectId)).findFirst())
                .filter(project -> !project.state().terminal())
                .ifPresent(project -> data.replaceBuildProject(ownerId, project.cancel()));
        return data.storeStarterCampDeployment(deployment.packedUp(), deployment.revision());
    }

    /**
     * Transfers an unfinished starter project to the nearest loaded reserve soldier. The original
     * free contract and sealed supply grant stay untouched; only the embodied worker assignment is
     * replaced.
     */
    public static Result reassign(
            ServerLevel level,
            ServerPlayer actor,
            CommandCenterBlockEntity hall
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(hall, "hall");
        KingdomSavedData data = KingdomSavedData.get(level);
        KingdomRecord kingdom = data.kingdomForOwner(actor.getUUID()).orElse(null);
        if (kingdom == null || !hall.isOwner(actor)
                || kingdom.settlement().hallX() != hall.getBlockPos().getX()
                || kingdom.settlement().hallY() != hall.getBlockPos().getY()
                || kingdom.settlement().hallZ() != hall.getBlockPos().getZ()
                || !kingdom.settlement().dimensionId().equals(level.dimension().identifier().toString())) {
            return Result.rejected("kingdom_changed", Optional.empty());
        }
        StarterCampDeployment deployment = data.starterCampDeployment(kingdom.id()).orElse(null);
        if (deployment == null || deployment.terminal() || deployment.projectId().isEmpty()) {
            return Result.rejected("builder_reassignment_unavailable", Optional.ofNullable(deployment));
        }
        BuildProject project = kingdom.settlement().buildProjects().stream()
                .filter(candidate -> candidate.id().equals(deployment.projectId().orElseThrow()))
                .filter(candidate -> !candidate.state().terminal())
                .findFirst().orElse(null);
        KingdomBaseBlueprint blueprint = GameplayDataManager.snapshot()
                .blueprint(KingdomBaseBlueprint.STARTER_CAMP_ID).orElse(null);
        if (project == null || blueprint == null
                || !project.blueprintId().equals(blueprint.id())
                || !blueprint.matchesDefinitionHash(project.definitionHash())) {
            return Result.rejected("blueprint_changed", Optional.of(deployment));
        }

        UUID previousBuilderId = deployment.builderId().orElse(null);
        GalacticRecruitEntity replacement = kingdom.settlement().recruitIds().stream()
                .filter(recruitId -> !recruitId.equals(previousBuilderId))
                .filter(recruitId -> data.armyGroupForRecruit(recruitId).isEmpty())
                .map(level::getEntity)
                .filter(GalacticRecruitEntity.class::isInstance)
                .map(GalacticRecruitEntity.class::cast)
                .filter(GalacticRecruitEntity::isAlive)
                .filter(candidate -> kingdom.id().equals(candidate.getKingdomId()))
                .filter(candidate -> candidate.isOwnedBy(actor))
                .filter(candidate -> candidate.getRecruitDuty() == RecruitDuty.SOLDIER)
                .min(Comparator.<GalacticRecruitEntity>comparingDouble(candidate ->
                                candidate.distanceToSqr(
                                        hall.getBlockPos().getX() + 0.5D,
                                        hall.getBlockPos().getY() + 0.5D,
                                        hall.getBlockPos().getZ() + 0.5D))
                        .thenComparing(candidate -> candidate.getUUID().toString()))
                .orElse(null);
        if (replacement == null) {
            return Result.rejected("builder_reassignment_unavailable", Optional.of(deployment));
        }

        GalacticRecruitEntity previousBuilder = previousBuilderId == null
                ? null : findLoadedRecruit(level, previousBuilderId);
        if (previousBuilder != null) {
            if (!previousBuilder.packUpStarterConstruction()) {
                return Result.rejected("builder_material_return_failed", Optional.of(deployment));
            }
        } else if (previousBuilderId != null) {
            return Result.rejected("builder_unavailable", Optional.of(deployment));
        }
        if (!replacement.assignStarterConstructionProject(actor, project, blueprint)) {
            if (previousBuilder == null
                    || !previousBuilder.assignStarterConstructionProject(actor, project, blueprint)) {
                return markBuilderUnassigned(data, deployment, project.id(), previousBuilderId,
                        replacement.getUUID());
            }
            return Result.rejected("builder_assignment_failed", Optional.of(deployment));
        }

        StarterCampDeployment reassigned = deployment.withBuilder(replacement.getUUID())
                .building(project.id());
        if (!data.storeStarterCampDeployment(reassigned, deployment.revision())) {
            replacement.packUpStarterConstruction();
            StarterCampDeployment current = data.starterCampDeployment(kingdom.id()).orElse(deployment);
            if (current.projectId().filter(project.id()::equals).isPresent()
                    && current.builderId().filter(builderId -> builderId.equals(previousBuilderId)).isPresent()
                    && (previousBuilder == null
                    || !previousBuilder.assignStarterConstructionProject(actor, project, blueprint))) {
                return markBuilderUnassigned(data, current, project.id(), previousBuilderId,
                        replacement.getUUID());
            }
            return Result.rejected("state_changed", Optional.of(current));
        }
        return Result.accepted("builder_reassigned", reassigned);
    }

    private static Result markBuilderUnassigned(
            KingdomSavedData data,
            StarterCampDeployment deployment,
            UUID projectId,
            UUID previousBuilderId,
            UUID replacementBuilderId
    ) {
        StarterCampDeployment current = data.starterCampDeployment(deployment.kingdomId()).orElse(deployment);
        if (canMarkBuilderUnassigned(current, projectId, previousBuilderId, replacementBuilderId)) {
            StarterCampDeployment blocked = current.blockedWithoutBuilder(
                    "builder_reassignment_rollback_failed");
            if (data.storeStarterCampDeployment(blocked, current.revision())) {
                return Result.rejected("builder_reassignment_rollback_failed", Optional.of(blocked));
            }
        }
        return Result.rejected("state_changed", data.starterCampDeployment(deployment.kingdomId()));
    }

    static boolean canMarkBuilderUnassigned(
            StarterCampDeployment current,
            UUID projectId,
            UUID previousBuilderId,
            UUID replacementBuilderId
    ) {
        boolean sameProject = current.projectId().filter(projectId::equals).isPresent();
        boolean expectedBuilder = current.builderId().isEmpty()
                || current.builderId().filter(builderId -> builderId.equals(previousBuilderId)
                        || builderId.equals(replacementBuilderId)).isPresent();
        return !current.terminal()
                && current.phase() != StarterCampDeploymentPhase.PACKED_UP
                && sameProject
                && expectedBuilder;
    }

    private static GalacticRecruitEntity resolveBuilder(
            ServerLevel level,
            ServerPlayer actor,
            KingdomRecord kingdom,
            FactionDefinition faction,
            StarterCampDeployment deployment
    ) {
        UUID builderId = deployment.builderId().orElseGet(() -> deterministicBuilderId(kingdom.id()));
        if (level.getEntity(builderId) instanceof GalacticRecruitEntity existing && existing.isAlive()) {
            return existing;
        }
        if (deployment.contractGranted()) {
            return kingdom.settlement().recruitIds().stream()
                    .filter(recruitId -> !recruitId.equals(builderId))
                    .filter(recruitId -> KingdomSavedData.get(level).armyGroupForRecruit(recruitId).isEmpty())
                    .map(level::getEntity)
                    .filter(GalacticRecruitEntity.class::isInstance)
                    .map(GalacticRecruitEntity.class::cast)
                    .filter(GalacticRecruitEntity::isAlive)
                    .filter(candidate -> kingdom.id().equals(candidate.getKingdomId()))
                    .filter(candidate -> candidate.isOwnedBy(actor))
                    .filter(candidate -> candidate.getRecruitDuty() == RecruitDuty.SOLDIER)
                    .min(Comparator.<GalacticRecruitEntity>comparingDouble(candidate -> candidate.distanceToSqr(
                                    deployment.originX() + 0.5D,
                                    deployment.originY() + 0.5D,
                                    deployment.originZ() + 0.5D))
                            .thenComparing(candidate -> candidate.getUUID().toString()))
                    .orElse(null);
        }
        ArmyUnitDefinition unit = GameplayDataManager.snapshot().unit(faction.starterUnitId()).orElse(null);
        if (unit == null) {
            return null;
        }
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(unit.entityTypeId()));
        if (entityType == null || !(entityType.create(level, EntitySpawnReason.EVENT)
                instanceof GalacticRecruitEntity recruit)) {
            return null;
        }
        Optional<BlockPos> spawn = findSafeSpawn(level, actor.blockPosition());
        if (spawn.isEmpty()) {
            return null;
        }
        recruit.setUUID(builderId);
        BlockPos spawnPos = spawn.orElseThrow();
        recruit.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        if (!recruit.initializeStarterContract(actor, kingdom)) {
            return null;
        }
        ProgressionSavedData progression = ProgressionSavedData.get(level);
        KingdomGameplayAction action = new KingdomGameplayAction(
                KingdomActionId.of("starter_recruit_hired", kingdom.id()),
                actor.getUUID(), ProgressionEventType.RECRUIT_HIRED, unit.id().path(), 1);
        KingdomGameplayResult evaluation = KingdomGameplayTransactionService.evaluate(
                progression.state(actor.getUUID()), action);
        if (!evaluation.accepted()) {
            return null;
        }
        if (!level.addFreshEntity(recruit)) {
            return null;
        }
        KingdomSavedData data = KingdomSavedData.get(level);
        if (!data.registerRecruit(actor.getUUID(), recruit.getUUID())) {
            recruit.discard();
            return null;
        }
        if (evaluation.changed()) {
            KingdomGameplayResult committed = KingdomGameplayRuntimeService.applyProgression(progression, action);
            if (!committed.accepted() || !committed.changed()) {
                data.unregisterRecruit(actor.getUUID(), recruit.getUUID());
                recruit.discard();
                return null;
            }
        }
        return recruit;
    }

    private static GalacticRecruitEntity findLoadedRecruit(ServerLevel level, UUID recruitId) {
        for (ServerLevel candidateLevel : level.getServer().getAllLevels()) {
            if (candidateLevel.getEntity(recruitId) instanceof GalacticRecruitEntity recruit
                    && recruit.isAlive()) {
                return recruit;
            }
        }
        return null;
    }

    private static Optional<BlockPos> findSafeSpawn(ServerLevel level, BlockPos center) {
        for (int radius = 2; radius <= 5; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos candidate = center.offset(x, 0, z);
                    if (level.isLoaded(candidate)
                            && level.getBlockState(candidate).isAir()
                            && level.getBlockState(candidate.above()).isAir()
                            && level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), Direction.UP)) {
                        return Optional.of(candidate);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static void showFootprint(
            ServerLevel level,
            KingdomBaseBlueprint blueprint,
            BlockPos origin,
            int rotationSteps,
            boolean valid
    ) {
        var particle = valid ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ANGRY_VILLAGER;
        for (int index = 0; index < blueprint.placements().size(); index++) {
            BaseBlockPlacement placement = blueprint.rotatedPlacement(index, rotationSteps);
            BlockPos target = origin.offset(placement.x(), placement.y(), placement.z());
            level.sendParticles(particle,
                    target.getX() + 0.5D, target.getY() + 0.6D, target.getZ() + 0.5D,
                    2, 0.18D, 0.18D, 0.18D, 0.0D);
        }
    }

    private static StarterCampDeployment store(
            KingdomSavedData data,
            StarterCampDeployment previous,
            StarterCampDeployment next
    ) {
        return data.storeStarterCampDeployment(next, previous.revision())
                ? next
                : data.starterCampDeployment(previous.kingdomId()).orElse(previous);
    }

    private static UUID deterministicBuilderId(UUID kingdomId) {
        return UUID.nameUUIDFromBytes(("galacticwars:starter_builder/" + kingdomId)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static int rotationSteps(CommandCenterBlockEntity hall) {
        Direction facing = hall.getBlockState().getValue(CommandCenterBlock.FACING);
        return switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    public record Result(boolean accepted, String reason, Optional<StarterCampDeployment> deployment) {
        public Result {
            reason = Objects.requireNonNull(reason, "reason");
            deployment = deployment == null ? Optional.empty() : deployment;
        }

        static Result accepted(String reason, StarterCampDeployment deployment) {
            return new Result(true, reason, Optional.of(deployment));
        }

        static Result rejected(String reason, Optional<StarterCampDeployment> deployment) {
            return new Result(false, reason, deployment);
        }
    }
}
