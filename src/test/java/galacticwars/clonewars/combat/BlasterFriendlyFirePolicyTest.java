package galacticwars.clonewars.combat;

import galacticwars.clonewars.faction.FactionRelation;

public final class BlasterFriendlyFirePolicyTest {
    public static void main(String[] args) {
        assertTrue(BlasterFriendlyFirePolicy.blocksHit(false, true, FactionRelation.ENEMY, false, true),
                "owner protection wins over faction relation");
        assertTrue(BlasterFriendlyFirePolicy.blocksHit(false, false, FactionRelation.SAME, false, true),
                "same faction is protected");
        assertTrue(BlasterFriendlyFirePolicy.blocksHit(false, false, FactionRelation.ALLY, false, true),
                "allied faction is protected");
        assertFalse(BlasterFriendlyFirePolicy.blocksHit(false, false, FactionRelation.ENEMY, false, true),
                "enemy recruits remain valid targets");
        assertFalse(BlasterFriendlyFirePolicy.blocksHit(false, true, FactionRelation.SAME, true, true),
                "friendly-fire configuration permits recruit damage");
        assertTrue(BlasterFriendlyFirePolicy.blocksHit(true, false, FactionRelation.NEUTRAL, false, false),
                "PvP configuration blocks player damage");
        assertFalse(BlasterFriendlyFirePolicy.blocksHit(true, false, FactionRelation.NEUTRAL, false, true),
                "PvP configuration permits player damage");
        assertTrue(BlasterFriendlyFirePolicy.blocksRecruitHitOnPlayer(
                        true, FactionRelation.ENEMY, true),
                "recruits cannot shoot their owner");
        assertTrue(BlasterFriendlyFirePolicy.blocksRecruitHitOnPlayer(
                        false, FactionRelation.ALLY, true),
                "recruits cannot shoot allied players");
        assertTrue(BlasterFriendlyFirePolicy.blocksRecruitHitOnPlayer(
                        false, FactionRelation.NEUTRAL, true),
                "recruits cannot shoot neutral players");
        assertTrue(BlasterFriendlyFirePolicy.blocksRecruitHitOnPlayer(
                        false, FactionRelation.ENEMY, false),
                "PvP configuration blocks recruit damage");
        assertFalse(BlasterFriendlyFirePolicy.blocksRecruitHitOnPlayer(
                        false, FactionRelation.ENEMY, true),
                "hostile faction recruits can shoot enemy players when PvP is enabled");
        System.out.println("BlasterFriendlyFirePolicyTest passed");
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void assertFalse(boolean value, String message) {
        assertTrue(!value, message);
    }
}
