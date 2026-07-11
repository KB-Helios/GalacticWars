package galacticwars.clonewars.recruitment;

public final class NpcServiceBranchTest {
    private NpcServiceBranchTest() {
    }

    public static void main(String[] args) {
        assertEquals(NpcServiceBranch.CIVILIAN, NpcServiceBranch.migrate(RecruitDuty.WORKER),
                "worker migration");
        assertEquals(NpcServiceBranch.MILITARY, NpcServiceBranch.migrate(RecruitDuty.SOLDIER),
                "soldier migration");
        assertEquals(NpcServiceBranch.MILITARY, NpcServiceBranch.migrate(RecruitDuty.COMMANDER),
                "commander migration");
        assertEquals(NpcServiceBranch.CIVILIAN, NpcServiceBranch.byId("civilian"), "branch id");
        System.out.println("NpcServiceBranchTest passed");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }
}
