package galacticwars.clonewars.settlement;

import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.kingdom.BuildProject;
import galacticwars.clonewars.kingdom.BuildProjectState;
import galacticwars.clonewars.kingdom.KingdomPermission;
import galacticwars.clonewars.kingdom.KingdomRecord;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;

/** Starts an authoritative build and hands it to the existing embodied builder AI. */
public final class ConstructionProjectService {
    private ConstructionProjectService() {
    }

    public static StartResult start(
            ServerLevel level,
            ServerPlayer actor,
            ConstructionPlan plan,
            BlockPos origin
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(origin, "origin");
        KingdomSavedData data = KingdomSavedData.get(level);
        KingdomRecord kingdom = data.kingdomForPlayer(actor.getUUID()).orElse(null);
        if (kingdom == null || !kingdom.id().equals(plan.kingdomId())) {
            return StartResult.rejected("kingdom_changed");
        }
        if (!kingdom.allows(actor.getUUID(), KingdomPermission.BUILD)
                || !kingdom.allows(actor.getUUID(), KingdomPermission.MANAGE_WORKSITES)) {
            return StartResult.rejected("permission_denied");
        }
        KingdomBaseBlueprint blueprint = GameplayDataManager.snapshot()
                .blueprint(plan.blueprintId()).orElse(null);
        if (blueprint == null || !blueprint.supportsRotationSteps(plan.rotationSteps())) {
            return StartResult.rejected("blueprint_changed");
        }
        GalacticRecruitEntity builder = level.getEntity(plan.builderId())
                instanceof GalacticRecruitEntity recruit ? recruit : null;
        if (builder == null || !builder.isAlive()
                || !kingdom.id().equals(builder.getKingdomId())
                || kingdom.npc(builder.getUUID()).isEmpty()) {
            return StartResult.rejected("builder_unavailable");
        }
        String dimensionId = level.dimension().identifier().toString();
        String invalidPlacement = validatePlacements(
                level, data, kingdom, blueprint, origin, plan.rotationSteps(), dimensionId);
        if (!invalidPlacement.isEmpty()) {
            return StartResult.rejected(invalidPlacement);
        }
        boolean alreadyExisted = kingdom.settlement().buildProjects().stream()
                .filter(project -> project.state() == BuildProjectState.ACTIVE
                        || project.state() == BuildProjectState.BLOCKED)
                .anyMatch(project -> project.dimensionId().equals(dimensionId)
                        && project.originX() == origin.getX()
                        && project.originY() == origin.getY()
                        && project.originZ() == origin.getZ());
        Optional<BuildProject> started = data.startBuildProject(
                kingdom.ownerId(), blueprint, dimensionId, origin, plan.rotationSteps());
        if (started.isEmpty()) {
            return StartResult.rejected("project_conflict");
        }
        BuildProject project = started.orElseThrow();
        if (!builder.assignConstructionProject(actor, project, blueprint)) {
            if (!alreadyExisted) {
                data.replaceBuildProject(kingdom.ownerId(), project.cancel());
            }
            return StartResult.rejected("builder_assignment_failed");
        }
        return StartResult.accepted(project);
    }

    public static ActionResult cancel(
            ServerLevel level,
            ServerPlayer actor,
            java.util.UUID projectId
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(projectId, "projectId");
        KingdomSavedData data = KingdomSavedData.get(level);
        KingdomRecord kingdom = data.kingdomForPlayer(actor.getUUID()).orElse(null);
        if (kingdom == null) {
            return ActionResult.rejected("kingdom_changed");
        }
        if (!kingdom.allows(actor.getUUID(), KingdomPermission.BUILD)
                || !kingdom.allows(actor.getUUID(), KingdomPermission.MANAGE_WORKSITES)) {
            return ActionResult.rejected("permission_denied");
        }
        BuildProject project = kingdom.settlement().buildProjects().stream()
                .filter(candidate -> candidate.id().equals(projectId))
                .filter(candidate -> candidate.state() == BuildProjectState.ACTIVE
                        || candidate.state() == BuildProjectState.BLOCKED)
                .findFirst().orElse(null);
        if (project == null) {
            return ActionResult.rejected("project_unavailable");
        }
        var projectWorksites = kingdom.settlement().worksites().stream()
                .filter(worksite -> worksite.sourceProjectId().filter(projectId::equals).isPresent())
                .toList();
        java.util.LinkedHashSet<java.util.UUID> assignedBuilders = new java.util.LinkedHashSet<>();
        projectWorksites.forEach(worksite -> assignedBuilders.addAll(worksite.assignmentIds()));
        kingdom.settlement().workOrders().stream()
                .filter(order -> !order.state().terminal())
                .filter(order -> order.projectId().filter(projectId::equals).isPresent())
                .forEach(order -> data.cancelWorkOrder(
                        kingdom.ownerId(), order.id(), order.revision()));
        boolean liveBuilderHandled = false;
        for (java.util.UUID builderId : assignedBuilders) {
            GalacticRecruitEntity recruit = findLoadedRecruit(level, builderId);
            if (recruit != null && recruit.isAlive()) {
                liveBuilderHandled |= recruit.cancelConstructionProject(actor, projectId);
            } else {
                data.releaseWorkerAssignments(actor.getUUID(), builderId);
            }
        }
        if (liveBuilderHandled) {
            return ActionResult.acceptedResult();
        }
        return data.replaceBuildProject(kingdom.ownerId(), project.cancel())
                ? ActionResult.acceptedResult()
                : ActionResult.rejected("project_changed");
    }

    private static GalacticRecruitEntity findLoadedRecruit(
            ServerLevel level,
            java.util.UUID recruitId
    ) {
        if (level.getEntity(recruitId) instanceof GalacticRecruitEntity recruit) {
            return recruit;
        }
        for (var entity : level.getAllEntities()) {
            if (entity.getUUID().equals(recruitId) && entity instanceof GalacticRecruitEntity recruit) {
                return recruit;
            }
        }
        return null;
    }

    private static String validatePlacements(
            ServerLevel level,
            KingdomSavedData data,
            KingdomRecord kingdom,
            KingdomBaseBlueprint blueprint,
            BlockPos origin,
            int rotationSteps,
            String dimensionId
    ) {
        for (int index = 0; index < blueprint.placements().size(); index++) {
            BaseBlockPlacement placement = blueprint.rotatedPlacement(index, rotationSteps);
            BlockPos target = origin.offset(placement.x(), placement.y(), placement.z());
            if (data.claimAt(dimensionId, new ChunkPos(target.getX() >> 4, target.getZ() >> 4))
                    .filter(claim -> claim.kingdomId().equals(kingdom.id())).isEmpty()) {
                return "outside_claim";
            }
            if (!level.isLoaded(target)) {
                return "chunk_unloaded";
            }
            Block required;
            try {
                required = BuiltInRegistries.BLOCK.getValue(Identifier.parse(placement.blockId()));
            } catch (RuntimeException exception) {
                return "blueprint_changed";
            }
            if (required == null) {
                return "blueprint_changed";
            }
            var existing = level.getBlockState(target);
            if (!existing.is(required) && !existing.canBeReplaced()) {
                return "site_obstructed";
            }
        }
        return "";
    }

    public record StartResult(boolean accepted, String reason, Optional<BuildProject> project) {
        public StartResult {
            reason = Objects.requireNonNull(reason, "reason");
            project = project == null ? Optional.empty() : project;
            if (accepted != project.isPresent()) {
                throw new IllegalArgumentException("accepted construction must contain a project");
            }
        }

        static StartResult accepted(BuildProject project) {
            return new StartResult(true, "accepted", Optional.of(project));
        }

        static StartResult rejected(String reason) {
            return new StartResult(false, reason, Optional.empty());
        }
    }

    public record ActionResult(boolean accepted, String reason) {
        public ActionResult {
            reason = Objects.requireNonNull(reason, "reason");
        }

        static ActionResult acceptedResult() {
            return new ActionResult(true, "accepted");
        }

        static ActionResult rejected(String reason) {
            return new ActionResult(false, reason);
        }
    }
}
