package galacticwars.clonewars.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RecruitCompanionAiIntegrationTest {
    private RecruitCompanionAiIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        recruitSeparatesGroupedAndLocalRuntimeControl();
        squadBrainIntegratesExistingPlannerPipeline();
        patrolRetreatUsesTransientCohesiveAnchors();
        squadBrainUsesDataDrivenRangedCombat();
        groupCommandsPersistBeforeRuntimeMutation();
        explicitAttackTargetsAreGuarded();
        missingGroupsReleaseStaleRuntimeOwnership();
        groupedNavigationDoesNotDependOnWorksiteState();
        workOrdersStayServerSide();
        workerRuntimeConsumesSavedDataAuthority();
        localCommandNavigationRecoversFromStalls();
        walkTargetsRespectTheirArrivalRadius();

        System.out.println("RecruitCompanionAiIntegrationTest passed");
    }

    private static void recruitSeparatesGroupedAndLocalRuntimeControl() throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");
        String brain = read("src/main/java/galacticwars/clonewars/entity/ai/RecruitBrain.java");
        String stateSensor = read(
                "src/main/java/galacticwars/clonewars/entity/ai/ArmyGroupStateSensor.java");
        String threatSensor = read(
                "src/main/java/galacticwars/clonewars/entity/ai/ArmyThreatSensor.java");
        String orderBehaviour = read(
                "src/main/java/galacticwars/clonewars/entity/ai/ArmyOrderBehaviour.java");
        String brainSupport = read(
                "src/main/java/galacticwars/clonewars/entity/ai/ArmyBrainSupport.java");
        String compatibilityController = read(
                "src/main/java/galacticwars/clonewars/army/ArmyRecruitRuntimeController.java");

        assertNotContains(entity, "ArmyRecruitRuntimeController", "legacy army runtime controller");
        assertNotContains(entity, "tickArmyRuntimeController", "legacy army runtime invocation");
        assertContains(brain, "new ArmyGroupStateSensor()", "group-state sensor registration");
        assertContains(brain, "new ArmyThreatSensor()", "threat sensor registration");
        assertContains(brain, "new ArmyOrderBehaviour()", "group order behaviour registration");
        assertContains(brain, "new ArmyCombatBehaviour()", "group combat behaviour registration");
        assertContains(brainSupport, "KingdomSavedData.get(level)", "saved-data squad authority");
        assertContains(stateSensor, "ArmyBrainMemoryTypes.ARMY_STATE", "ephemeral squad state memory");
        assertContains(stateSensor, "scanRate(2)", "bounded squad state cadence");
        assertContains(threatSensor, "scanRate(10)", "bounded squad threat cadence");
        assertContains(orderBehaviour, "ArmyBrainMemoryTypes.PATH_STATUS", "ephemeral path status memory");
        assertContains(brain, "new RecruitMoveToCommandBehaviour(1.05D)",
                "local move behaviour registration");
        assertContains(brain, "new RecruitCompanionBehaviour(1.0D)",
                "local companion behaviour registration");
        assertContains(read("src/main/java/galacticwars/clonewars/entity/ai/RecruitCompanionBehaviour.java"),
                "shouldUseCompanionAi", "companion ownership guard");
        assertContains(read("src/main/java/galacticwars/clonewars/entity/ai/RecruitMoveToCommandBehaviour.java"),
                "shouldMoveToCommandTarget", "move command guard");
        String walkTargetBridge = read(
                "src/main/java/galacticwars/clonewars/entity/ai/RecruitWalkTargetBehaviour.java");
        assertContains(walkTargetBridge, "&& !recruit.hasAuthoritativeArmyGroup()",
                "grouped hold formation bypasses only the local sit veto");
        assertNotContains(walkTargetBridge, "releaseWithoutStopping(recruit);\n            return;",
                "grouped walk-target bridge bypass");
        assertContains(compatibilityController, "Compatibility facade",
                "legacy controller is compatibility-only");
        assertNotContains(compatibilityController, "void tick(",
                "legacy controller runtime tick");
        assertNotContains(compatibilityController, "getNavigation()",
                "legacy controller navigation path");
        assertNotContains(compatibilityController, "fireAt(",
                "legacy controller firing path");
        assertNotContains(compatibilityController, "doHurtTarget(",
                "legacy controller melee path");
        assertNotContains(entity, "FollowOwnerGoal", "vanilla dog-like follow goal");
        assertNotContains(entity, "goalSelector.addGoal", "vanilla goal scheduling");
        assertContains(entity, "SmartBrainOwner<GalacticRecruitEntity>", "SmartBrain owner contract");
    }

    private static void squadBrainIntegratesExistingPlannerPipeline() throws IOException {
        String support = read("src/main/java/galacticwars/clonewars/entity/ai/ArmyBrainSupport.java");
        String order = read("src/main/java/galacticwars/clonewars/entity/ai/ArmyOrderBehaviour.java");
        String combat = read("src/main/java/galacticwars/clonewars/entity/ai/ArmyCombatBehaviour.java");

        assertContains(support, "ArmyGroupOrderPlanner.plan", "formation planner");
        assertContains(order, "ArmyBehaviorPlanner.plan", "behavior planner");
        assertContains(support, "ArmyEngagementPlanner.plan", "engagement planner");
        assertContains(order, "ArmyTacticalPlanner.plan", "tactical planner");
        assertContains(order, "STALL_TIMEOUT = 200", "bounded group movement recovery");
        assertContains(order, "ArmyBrainSupport.shouldMaintainFormation",
                "hold formation target arbitration");
        assertContains(combat, "ArmyBrainSupport.shouldMaintainFormation",
                "hold formation prevents pursuit");
        assertContains(combat, "recruit.isWithinMeleeAttackRange(target)", "runtime melee range");
        assertContains(combat, "recruit.doHurtTarget(level, target)", "runtime melee execution");
    }

    private static void patrolRetreatUsesTransientCohesiveAnchors() throws IOException {
        String order = read("src/main/java/galacticwars/clonewars/entity/ai/ArmyOrderBehaviour.java");
        String retreatPlanner = read("src/main/java/galacticwars/clonewars/army/ArmyPatrolRetreatPlanner.java");

        assertContains(order, "ArmyPatrolRetreatPlanner.retreatPosition",
                "loaded patrol retreat formation placement");
        assertContains(retreatPlanner, "ArmyGroupOrderPlanner.formationPositionForMember",
                "retreat preserves member formation slots");
        assertContains(order, "never persisted as an order",
                "retreat remains one-shot runtime state");
    }

    private static void squadBrainUsesDataDrivenRangedCombat() throws IOException {
        String combat = read("src/main/java/galacticwars/clonewars/entity/ai/ArmyCombatBehaviour.java");
        String events = read("src/main/java/galacticwars/clonewars/combat/BlasterCombatEvents.java");

        assertContains(combat, "FactionRangedWeaponService.supportsRecruitRangedCombat",
                "loadout-driven faction ranged-weapon detection");
        assertContains(combat, "ArmyBrainSupport.canUseRangedFire",
                "server-side ranged doctrine gate");
        assertContains(read("src/main/java/galacticwars/clonewars/entity/ai/ArmyBrainSupport.java"),
                "allowsRangedFire(returnFireTarget, commandTarget)", "ranged policy resolution");
        assertContains(combat, "blaster.fireAt(level, recruit, target", "server-authoritative ranged attack");
        assertContains(combat, "BlasterHeatPolicy.canFire", "ranged heat gate");
        assertContains(combat, "ArmySupplyPolicy::canFireBlaster", "persisted squad supply gate");
        assertContains(combat, "data.changeArmySupply(", "authoritative shot supply transaction");
        assertBefore(combat, "trySpendBlasterSupply(data, group)",
                "blaster.fireAt(level, recruit, target", "supply spend before accepted blaster shot");
        assertContains(combat, "meleeOrClose(recruit, level, data, group, state, target)",
                "empty-supply melee fallback");
        assertContains(combat, "FactionRangedWeaponService.supportsRecruitRangedCombat",
                "data-driven blaster and Nightsister bow detection");
        assertContains(combat, "FactionRangedWeaponService.fireNightsisterBow",
                "Nightsister archer ranged execution");
        assertContains(events, "projectile.getOwner() instanceof LivingEntity shooter",
                "recruit projectile filtering");
        assertContains(events, "sameOwner(other, recruit)", "same-owner recruit projectile protection");
    }

    private static void groupCommandsPersistBeforeRuntimeMutation() throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");
        String orderMethod = section(entity, "private boolean persistArmyGroupOrder", "private boolean persistArmyGroupAttack");
        String attackMethod = section(entity, "private boolean persistArmyGroupAttack", "private boolean canAttackTarget");

        assertBefore(orderMethod, "issueArmyOrder", "reconcileArmyGroupOrder", "order persistence before reconciliation");
        assertBefore(attackMethod, "issueArmyOrder", "reconcileArmyGroupOrder", "attack persistence before reconciliation");
        String attackButton = section(entity, "case ATTACK ->", "case CLEAR ->");
        assertContains(attackButton, "applyMenuArmyAttack", "attack button authoritative path");
        assertNotContains(attackButton, "this.setTarget", "attack button pre-persistence target mutation");
        assertNotContains(attackButton, "this.setRecruitCommand", "attack button pre-persistence command mutation");
    }

    private static void explicitAttackTargetsAreGuarded() throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");
        String validation = section(entity, "private boolean canAttackTarget", "private boolean cycleArmyFormation");

        assertContains(validation, "recruit.getRecruitDuty() != RecruitDuty.WORKER", "worker target rejection");
        assertContains(validation, "!sameOwner", "same-owner target rejection");
        assertContains(validation, "this.factionRelationTo(recruit) == FactionRelation.ENEMY",
                "kingdom-aware faction target policy");
        assertContains(validation, "this.canAttackFactionPlayer(player)", "hostile player target policy");
        assertContains(entity, "KingdomFactionRelations.resolve", "dynamic kingdom diplomacy resolver");
        assertContains(validation, "target instanceof Monster", "non-faction hostile target rule");

        String support = read("src/main/java/galacticwars/clonewars/entity/ai/ArmyBrainSupport.java");
        assertContains(support, "recruit.canAttackFactionPlayer(player)", "retaliatory player diplomacy guard");
    }

    private static void missingGroupsReleaseStaleRuntimeOwnership() throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");
        String reconciliation = section(
                entity,
                "private void reconcileArmyGroupOrder",
                "private static RecruitmentAction recruitmentAction");

        assertContains(reconciliation, "this.armyGroupId = null", "stale group cache release");
        assertContains(reconciliation, "this.navigation.stop()", "stale grouped navigation release");
        assertContains(reconciliation, "this.setRecruitCommand(RecruitmentAction.FOLLOW_OWNER)",
                "safe local fallback command");
    }

    private static void groupedNavigationDoesNotDependOnWorksiteState() throws IOException {
        String stateSensor = read("src/main/java/galacticwars/clonewars/entity/ai/ArmyGroupStateSensor.java");
        String order = read("src/main/java/galacticwars/clonewars/entity/ai/ArmyOrderBehaviour.java");

        assertContains(stateSensor, "resolveState", "persisted group order projection");
        assertNotContains(order, "workTarget", "worksite-independent army control");
    }

    private static void workOrdersStayServerSide() throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");

        assertContains(entity, "this.level().isClientSide()", "client-side worker cycle guard");
        assertContains(entity, "state.isAir() && !state.canBeReplaced()", "replaceable build target check");
        assertContains(entity, "dataVersion < 3", "legacy resource migration guard");
        assertContains(entity, "Reset legacy synthetic worker resource counters", "legacy resource migration warning");
        assertNotContains(entity, "decodeResources", "synthetic resource counter decoder");
    }

    private static void workerRuntimeConsumesSavedDataAuthority() throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");
        String savedData = read("src/main/java/galacticwars/clonewars/kingdom/KingdomSavedData.java");

        assertContains(entity, "hasAuthoritativeWorkerAssignment", "runtime worksite guard");
        assertContains(entity, "queueAndClaimWorkOrder", "persisted order acquisition");
        assertContains(entity, "progressWorkOrder", "persisted order progress");
        assertContains(entity, "blockWorkOrder", "persisted blocked state");
        assertContains(entity, "releaseWorkerAssignments", "profession release transaction");
        assertContains(entity, "isRegisteredStorage", "registered storage enforcement");
        assertContains(savedData, "validWorkOrderReferences", "order reference validation");
        assertContains(savedData, "insideSettlementClaim", "frontier worksite claim validation");
    }

    private static void localCommandNavigationRecoversFromStalls() throws IOException {
        String behaviour = read(
                "src/main/java/galacticwars/clonewars/entity/ai/RecruitMoveToCommandBehaviour.java");

        assertContains(behaviour, "REPATH_INTERVAL = 20", "bounded local command repathing");
        assertContains(behaviour, "STALL_TIMEOUT = 200", "local command navigation timeout");
        assertContains(behaviour, "RETRY_BACKOFF = 40", "failed path retry backoff");
        assertContains(behaviour, "recruit.getNavigation().isDone()", "failed path restart");
        assertContains(behaviour, "stalledTicks >= STALL_TIMEOUT", "no-progress recovery");
        assertContains(behaviour, "if (retryTicks == 0)",
                "stalled-target retention during retry backoff");
    }

    private static void walkTargetsRespectTheirArrivalRadius() throws IOException {
        String behaviour = read(
                "src/main/java/galacticwars/clonewars/entity/ai/RecruitWalkTargetBehaviour.java")
                .replace("\r\n", "\n");

        assertContains(behaviour,
                "targetPos.distManhattan(recruit.blockPosition())\n                <= walkTarget.getCloseEnoughDist()",
                "walk-target arrival radius guard");
        assertContains(behaviour,
                "recruit.getNavigation().moveTo(\n                targetPos.getX() + 0.5D,",
                "coordinate walk-target navigation bridge");
        assertNotContains(behaviour, "createPath(targetPos,",
                "precomputed walk-target path bridge");
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private static String section(String value, String start, String end) {
        int startIndex = value.indexOf(start);
        int endIndex = value.indexOf(end, startIndex + start.length());
        if (startIndex < 0 || endIndex < 0) {
            throw new AssertionError("missing source section " + start + " -> " + end);
        }
        return value.substring(startIndex, endIndex);
    }

    private static int count(String value, String needle) {
        int matches = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            matches++;
            index += needle.length();
        }
        return matches;
    }

    private static void assertBefore(String value, String first, String second, String label) {
        int firstIndex = value.indexOf(first);
        int secondIndex = value.indexOf(second);
        assertTrue(firstIndex >= 0 && secondIndex > firstIndex, label);
    }

    private static void assertContains(String haystack, String needle, String label) {
        if (!haystack.contains(needle)) {
            throw new AssertionError(label + " missing <" + needle + ">");
        }
    }

    private static void assertNotContains(String haystack, String needle, String label) {
        if (haystack.contains(needle)) {
            throw new AssertionError(label + " contains forbidden <" + needle + ">");
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
