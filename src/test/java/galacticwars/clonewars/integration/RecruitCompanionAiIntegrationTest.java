package galacticwars.clonewars.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RecruitCompanionAiIntegrationTest {
    private RecruitCompanionAiIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        recruitUsesSinglePlannerRuntimeController();
        controllerIntegratesExistingPlannerPipeline();
        controllerUsesDataDrivenRangedCombat();
        groupCommandsPersistBeforeRuntimeMutation();
        explicitAttackTargetsAreGuarded();
        missingGroupsReleaseStaleRuntimeOwnership();
        groupedNavigationDoesNotDependOnWorksiteState();
        workOrdersStayServerSide();
        workerRuntimeConsumesSavedDataAuthority();

        System.out.println("RecruitCompanionAiIntegrationTest passed");
    }

    private static void recruitUsesSinglePlannerRuntimeController() throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");

        assertContains(entity, "ArmyRecruitRuntimeController", "army runtime controller");
        assertContains(entity, "this.armyRuntimeController.tick(this, serverLevel)", "army runtime invocation");
        assertNotContains(entity, "new RecruitCompanionGoal", "competing companion goal");
        assertNotContains(entity, "new RecruitMoveToCommandGoal", "competing move goal");
        assertNotContains(entity, "FollowOwnerGoal", "vanilla dog-like follow goal");
        assertTrue(count(entity, "!GalacticRecruitEntity.this.hasAuthoritativeArmyGroup()") >= 4,
                "grouped melee and random-stroll goals are disabled");
    }

    private static void controllerIntegratesExistingPlannerPipeline() throws IOException {
        String controller = read("src/main/java/galacticwars/clonewars/army/ArmyRecruitRuntimeController.java");

        assertContains(controller, "ArmyGroupOrderPlanner.plan", "formation planner");
        assertContains(controller, "ArmyBehaviorPlanner.plan", "behavior planner");
        assertContains(controller, "ArmyEngagementPlanner.plan", "engagement planner");
        assertContains(controller, "ArmyTacticalPlanner.plan", "tactical planner");
        assertContains(controller, "TARGET_INTERVAL = 20", "bounded target cadence");
        assertContains(controller, "BEHAVIOR_INTERVAL = 10", "bounded behavior cadence");
        assertContains(controller, "recruit.isWithinMeleeAttackRange(target)", "runtime melee range");
        assertContains(controller, "recruit.doHurtTarget(level, target)", "runtime melee execution");
    }

    private static void controllerUsesDataDrivenRangedCombat() throws IOException {
        String controller = read("src/main/java/galacticwars/clonewars/army/ArmyRecruitRuntimeController.java");
        String events = read("src/main/java/galacticwars/clonewars/combat/BlasterCombatEvents.java");

        assertContains(controller, "FactionRangedWeaponService.supportsRecruitRangedCombat",
                "loadout-driven faction ranged-weapon detection");
        assertContains(controller, "blaster.fireAt(level, recruit, target", "server-authoritative ranged attack");
        assertContains(controller, "BlasterHeatPolicy.canFire", "ranged heat gate");
        assertContains(controller, "FactionRangedWeaponService.supportsRecruitRangedCombat",
                "data-driven blaster and Nightsister bow detection");
        assertContains(controller, "FactionRangedWeaponService.fireNightsisterBow",
                "Nightsister archer ranged execution");
        assertContains(events, "arrow.getOwner() instanceof LivingEntity shooter", "recruit projectile filtering");
        assertContains(events, "sameOwner(other, recruit)", "same-owner recruit projectile protection");
    }

    private static void groupCommandsPersistBeforeRuntimeMutation() throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");
        String orderMethod = section(entity, "private boolean persistArmyGroupOrder", "private boolean persistArmyGroupAttack");
        String attackMethod = section(entity, "private boolean persistArmyGroupAttack", "private boolean canAttackTarget");

        assertBefore(orderMethod, "issueArmyOrder", "reconcileArmyGroupOrder", "order persistence before reconciliation");
        assertBefore(attackMethod, "issueArmyOrder", "reconcileArmyGroupOrder", "attack persistence before reconciliation");
        String attackButton = section(
                entity,
                "case RecruitCommandMenu.BUTTON_ATTACK ->",
                "case RecruitCommandMenu.BUTTON_CLEAR ->");
        assertContains(attackButton, "applyMenuArmyAttack", "attack button authoritative path");
        assertNotContains(attackButton, "this.setTarget", "attack button pre-persistence target mutation");
        assertNotContains(attackButton, "this.setRecruitCommand", "attack button pre-persistence command mutation");
    }

    private static void explicitAttackTargetsAreGuarded() throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");
        String validation = section(entity, "private boolean canAttackTarget", "private boolean cycleArmyFormation");
        String policy = read("src/main/java/galacticwars/clonewars/army/ArmyAttackTargetPolicy.java");

        assertContains(validation, "ArmyAttackTargetPolicy.canAttackRecruit", "faction target policy");
        assertContains(policy, "targetDuty != RecruitDuty.WORKER", "worker target rejection");
        assertContains(policy, "!sameOwner", "same-owner target rejection");
        assertContains(policy, "FactionRelation.ENEMY", "allied and neutral target rejection");
        assertContains(validation, "target instanceof Monster", "non-faction hostile target rule");

        String controller = read("src/main/java/galacticwars/clonewars/army/ArmyRecruitRuntimeController.java");
        assertContains(controller, "target instanceof Player", "retaliatory player rejection");
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
        String controller = read("src/main/java/galacticwars/clonewars/army/ArmyRecruitRuntimeController.java");

        assertContains(controller, "group.order().type()", "persisted group order");
        assertNotContains(controller, "workTarget", "worksite-independent army control");
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
