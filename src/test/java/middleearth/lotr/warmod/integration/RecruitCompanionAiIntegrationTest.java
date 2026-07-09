package middleearth.lotr.warmod.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RecruitCompanionAiIntegrationTest {
    private RecruitCompanionAiIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        recruitUsesCompanionGoalInsteadOfDogFollowGoal();
        companionGoalKeepsNaturalFormationDistance();
        companionCommandsDoNotDependOnWorksiteState();
        workOrdersStayServerSide();

        System.out.println("RecruitCompanionAiIntegrationTest passed");
    }

    private static void recruitUsesCompanionGoalInsteadOfDogFollowGoal() throws IOException {
        String entity = read("src/main/java/middleearth/lotr/warmod/entity/MiddleEarthRecruitEntity.java");

        assertContains(entity, "RecruitCompanionGoal", "custom companion goal");
        assertContains(entity, "new RecruitCompanionGoal(this, 1.0)", "custom companion registration");
        assertNotContains(entity, "FollowOwnerGoal", "vanilla dog-like follow goal");
    }

    private static void companionGoalKeepsNaturalFormationDistance() throws IOException {
        String goal = read("src/main/java/middleearth/lotr/warmod/entity/ai/RecruitCompanionGoal.java");

        assertContains(goal, "COMFORT_DISTANCE_SQUARED", "comfort distance");
        assertContains(goal, "TELEPORT_DISTANCE_SQUARED", "teleport fallback distance");
        assertContains(goal, "findCompanionAnchor", "formation anchor selection");
        assertContains(goal, "shouldUseCompanionAi", "entity companion guard");
        assertContains(goal, "distanceToSqr", "distance-based pathing");
    }

    private static void companionCommandsDoNotDependOnWorksiteState() throws IOException {
        String entity = read("src/main/java/middleearth/lotr/warmod/entity/MiddleEarthRecruitEntity.java")
                .replace("\r\n", "\n");

        assertContains(entity, "command == RecruitmentAction.FOLLOW_OWNER || command == RecruitmentAction.PROTECT_OWNER", "companion command gate");
        assertNotContains(entity, "&& this.workTarget == null\n                && (command == RecruitmentAction.FOLLOW_OWNER", "worksite-dependent companion follow gate");
    }

    private static void workOrdersStayServerSide() throws IOException {
        String entity = read("src/main/java/middleearth/lotr/warmod/entity/MiddleEarthRecruitEntity.java");

        assertContains(entity, "this.level().isClientSide()", "client-side worker cycle guard");
        assertContains(entity, "state.isAir() && !state.canBeReplaced()", "replaceable build target check");
        assertContains(entity, "dropMalformedResourceEntry", "malformed resource NBT guard");
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
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
}
