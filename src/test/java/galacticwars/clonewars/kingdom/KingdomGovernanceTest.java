package galacticwars.clonewars.kingdom;

import java.util.List;
import java.util.UUID;
import galacticwars.clonewars.recruitment.NpcServiceBranch;
import galacticwars.clonewars.workforce.WorkerProfession;

public final class KingdomGovernanceTest {
    private KingdomGovernanceTest() {
    }

    public static void main(String[] args) {
        membershipRolesAreAuthoritative();
        claimsRequireContiguousExpansionAndTransferExplicitly();
        diplomacyIsSymmetricAndCooldownAware();
        siegesRequireMilitaryAdvantageToProgress();
        siegesRejectNullParticipantLists();
        npcRosterMigratesWorkersAndSoldiers();
        System.out.println("KingdomGovernanceTest passed");
    }

    private static void npcRosterMigratesWorkersAndSoldiers() {
        UUID worker = UUID.randomUUID();
        UUID soldier = UUID.randomUUID();
        SettlementRecord settlement = SettlementRecord.create("minecraft:overworld", 0, 64, 0)
                .withRecruit(worker)
                .withRecruit(soldier)
                .reserveWorksite(worker, WorkerProfession.FARMER);
        KingdomRecord kingdom = new KingdomRecord(
                UUID.randomUUID(), UUID.randomUUID(), "galacticwars:republic", settlement);
        assertEquals(NpcServiceBranch.CIVILIAN,
                kingdom.npc(worker).orElseThrow().serviceBranch(), "worker branch migration");
        assertEquals(NpcServiceBranch.MILITARY,
                kingdom.npc(soldier).orElseThrow().serviceBranch(), "soldier branch migration");
    }

    private static void membershipRolesAreAuthoritative() {
        UUID kingdomId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID builder = UUID.randomUUID();
        SettlementRecord capital = SettlementRecord.create("minecraft:overworld", 0, 64, 0);
        KingdomRecord kingdom = new KingdomRecord(kingdomId, owner, "galacticwars:republic", capital)
                .withMember(builder, KingdomMemberRole.BUILDER);
        assertEquals(KingdomMemberRole.OWNER, kingdom.member(owner).orElseThrow().role(), "legacy owner migration");
        assertTrue(kingdom.allows(builder, KingdomPermission.BUILD), "builder build permission");
        assertTrue(!kingdom.allows(builder, KingdomPermission.MANAGE_MEMBERS), "builder membership denial");
        assertEquals(9, kingdom.claims().getFirst().chunks().size(), "migrated capital claim size");
        assertTrue(kingdom.withoutMember(builder).member(builder).isEmpty(), "member removal");
    }

    private static void claimsRequireContiguousExpansionAndTransferExplicitly() {
        UUID firstKingdom = UUID.randomUUID();
        UUID secondKingdom = UUID.randomUUID();
        SettlementRecord outpost = SettlementRecord.create("minecraft:overworld", 320, 64, 0);
        KingdomClaim claim = KingdomClaim.outpost(firstKingdom, outpost);
        ClaimedChunk adjacent = new ClaimedChunk(claim.center().x() + 1, claim.center().z());
        ClaimedChunk disconnected = new ClaimedChunk(claim.center().x() + 3, claim.center().z());
        assertTrue(claim.canExpandTo(adjacent), "adjacent expansion");
        assertTrue(!claim.canExpandTo(disconnected), "disconnected expansion denial");
        KingdomClaim expanded = claim.expandedTo(adjacent);
        assertEquals(2, expanded.chunks().size(), "expanded chunk count");
        assertEquals(secondKingdom, expanded.transferredTo(secondKingdom).kingdomId(), "explicit transfer");
    }

    private static void diplomacyIsSymmetricAndCooldownAware() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        KingdomDiplomacy diplomacy = KingdomDiplomacy.neutral(first, second)
                .withTreaty(1200L, 200L)
                .withEmbargo(true);
        assertTrue(diplomacy.matches(second, first), "symmetric pair lookup");
        assertEquals(KingdomRelation.ALLY, diplomacy.relation(), "treaty relation");
        assertTrue(diplomacy.treatyActive(1199L), "active treaty");
        assertTrue(diplomacy.embargo(), "embargo state");
    }

    private static void siegesRequireMilitaryAdvantageToProgress() {
        KingdomSiege siege = KingdomSiege.start(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 10, 100L);
        KingdomSiege contested = siege.progress(4, 4, 120L, List.of(), List.of());
        assertEquals(0, contested.captureProgress(), "contested progress");
        KingdomSiege captured = contested.progress(12, 2, 140L, List.of(), List.of());
        assertEquals(SiegeState.CAPTURED, captured.state(), "capture state");
        assertEquals(10, captured.captureProgress(), "bounded capture progress");
    }

    private static void siegesRejectNullParticipantLists() {
        assertThrows(() -> new KingdomSiege(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                SiegeState.ACTIVE, 0, 10, 0L, null, List.of()), "null attackers");
        assertThrows(() -> new KingdomSiege(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                SiegeState.ACTIVE, 0, 10, 0L, List.of(), null), "null defenders");
    }

    private static void assertThrows(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " did not throw");
        } catch (NullPointerException expected) {
            // Expected validation failure.
        }
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
