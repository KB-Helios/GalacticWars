package galacticwars.clonewars.world;

import galacticwars.clonewars.recruitment.NpcServiceBranch;
import java.util.Map;
import java.util.UUID;

public final class FactionOutpostRecordTest {
    private FactionOutpostRecordTest() {
    }

    public static void main(String[] args) {
        rosterSeparatesMilitaryAndCivilianNpcs();
        rosterRejectsNullIdentityAndBranch();
        profilesMapEntityTypesToFactionBranches();
        System.out.println("FactionOutpostRecordTest passed");
    }

    private static void rosterRejectsNullIdentityAndBranch() {
        FactionOutpostRecord outpost = FactionOutpostRecord.create(
                "galacticwars:republic", "minecraft:overworld", 0, 64, 0, 96, 10L);
        assertThrows(() -> outpost.withNpc(null, NpcServiceBranch.MILITARY, 20L), "null npc");
        assertThrows(() -> outpost.withNpc(UUID.randomUUID(), null, 20L), "null branch");
    }

    private static void rosterSeparatesMilitaryAndCivilianNpcs() {
        UUID soldier = UUID.randomUUID();
        UUID civilian = UUID.randomUUID();
        FactionOutpostRecord outpost = FactionOutpostRecord.create(
                        "galacticwars:hutt_cartel", "minecraft:overworld", 0, 64, 0, 96, 10L)
                .withNpc(soldier, NpcServiceBranch.MILITARY, 20L)
                .withNpc(civilian, NpcServiceBranch.CIVILIAN, 30L);
        assertTrue(outpost.militaryNpcIds().contains(soldier), "military roster");
        assertTrue(outpost.civilianNpcIds().contains(civilian), "civilian roster");
        assertTrue(outpost.withoutNpc(soldier, 40L).militaryNpcIds().isEmpty(), "despawn release");
    }

    private static void profilesMapEntityTypesToFactionBranches() {
        OverworldFactionSpawnProfile profile = new OverworldFactionSpawnProfile(
                "galacticwars:hutt_cartel",
                Map.of(
                        "galacticwars:hutt_enforcer", NpcServiceBranch.MILITARY,
                        "galacticwars:hutt_civilian", NpcServiceBranch.CIVILIAN),
                96, 320, 10, 6);
        assertTrue(profile.branchFor("galacticwars:hutt_enforcer") == NpcServiceBranch.MILITARY,
                "military mapping");
        assertTrue(profile.branchFor("galacticwars:hutt_civilian") == NpcServiceBranch.CIVILIAN,
                "civilian mapping");
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }

    private static void assertThrows(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " did not throw");
        } catch (NullPointerException expected) {
            // Expected validation failure.
        }
    }
}
