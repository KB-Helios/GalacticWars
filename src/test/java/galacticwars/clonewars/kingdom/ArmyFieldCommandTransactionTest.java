package galacticwars.clonewars.kingdom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import galacticwars.clonewars.army.ArmyCommandType;
import galacticwars.clonewars.army.ArmyFormation;
import galacticwars.clonewars.army.ArmyGroupLifecycleState;
import galacticwars.clonewars.army.ArmyGroupOrder;
import galacticwars.clonewars.army.ArmyGroupRecord;
import galacticwars.clonewars.army.ArmyGroupSimulation;
import galacticwars.clonewars.army.ArmyLocation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Verifies the SavedData compare-and-swap used by field commands is authority-safe and atomic. */
public final class ArmyFieldCommandTransactionTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-00000000f001");
    private static final UUID FOREIGN_PLAYER = UUID.fromString("00000000-0000-0000-0000-00000000f002");
    private static final UUID KINGDOM = UUID.fromString("00000000-0000-0000-0000-00000000f003");
    private static final UUID COMMANDER_ONE = UUID.fromString("00000000-0000-0000-0000-00000000f004");
    private static final UUID COMMANDER_TWO = UUID.fromString("00000000-0000-0000-0000-00000000f005");
    private static final UUID MEMBER_ONE = UUID.fromString("00000000-0000-0000-0000-00000000f006");
    private static final UUID MEMBER_TWO = UUID.fromString("00000000-0000-0000-0000-00000000f007");

    private ArmyFieldCommandTransactionTest() {
    }

    public static void main(String[] args) {
        acceptsAnAuthorizedRevisionMatchedBatch();
        rejectsForeignAndStaleBatchesWithoutPartialWrites();

        System.out.println("ArmyFieldCommandTransactionTest passed");
    }

    private static void acceptsAnAuthorizedRevisionMatchedBatch() {
        Fixture fixture = fixture();
        ArmyGroupRecord first = fixture.data().armyGroup(fixture.first().id()).orElseThrow();
        ArmyGroupRecord second = fixture.data().armyGroup(fixture.second().id()).orElseThrow();
        ArmyGroupRecord updatedFirst = hold(first);
        ArmyGroupRecord updatedSecond = hold(second);

        assertTrue(fixture.data().replaceArmyGroupsAtomically(
                OWNER,
                List.of(updatedFirst, updatedSecond),
                Map.of(first.id(), first.simulation().revision(), second.id(), second.simulation().revision())),
                "owner may apply the complete expected-revision batch");
        assertEquals(updatedFirst, fixture.data().armyGroup(first.id()).orElseThrow(), "first replacement persisted");
        assertEquals(updatedSecond, fixture.data().armyGroup(second.id()).orElseThrow(), "second replacement persisted");
    }

    private static void rejectsForeignAndStaleBatchesWithoutPartialWrites() {
        Fixture fixture = fixture();
        ArmyGroupRecord first = fixture.data().armyGroup(fixture.first().id()).orElseThrow();
        ArmyGroupRecord second = fixture.data().armyGroup(fixture.second().id()).orElseThrow();
        ArmyGroupRecord updatedFirst = hold(first);
        ArmyGroupRecord updatedSecond = hold(second);

        assertFalse(fixture.data().replaceArmyGroupsAtomically(
                FOREIGN_PLAYER,
                List.of(updatedFirst, updatedSecond),
                Map.of(first.id(), first.simulation().revision(), second.id(), second.simulation().revision())),
                "foreign player cannot issue a batch");
        assertEquals(first, fixture.data().armyGroup(first.id()).orElseThrow(), "foreign request leaves first intact");
        assertEquals(second, fixture.data().armyGroup(second.id()).orElseThrow(), "foreign request leaves second intact");

        assertFalse(fixture.data().replaceArmyGroupsAtomically(
                OWNER,
                List.of(updatedFirst, updatedSecond),
                Map.of(first.id(), first.simulation().revision(), second.id(), second.simulation().revision() - 1L)),
                "one stale group rejects the whole owner batch");
        assertEquals(first, fixture.data().armyGroup(first.id()).orElseThrow(), "stale batch leaves first intact");
        assertEquals(second, fixture.data().armyGroup(second.id()).orElseThrow(), "stale batch leaves second intact");
    }

    private static Fixture fixture() {
        ArmyLocation firstAnchor = new ArmyLocation("minecraft:overworld", 0.0D, 64.0D, 0.0D);
        ArmyLocation secondAnchor = new ArmyLocation("minecraft:overworld", 16.0D, 64.0D, 0.0D);
        SettlementRecord settlement = new SettlementRecord(
                UUID.fromString("00000000-0000-0000-0000-00000000f008"),
                "minecraft:overworld", 0, 64, 0, 48, 8,
                List.of(COMMANDER_ONE, COMMANDER_TWO, MEMBER_ONE, MEMBER_TWO), Optional.of(COMMANDER_ONE),
                CommanderPolicy.defaults(), List.of(), List.of(), List.of(), List.of(),
                new SettlementRewards(0, 2), 0, List.of(COMMANDER_TWO));
        KingdomRecord kingdom = new KingdomRecord(KINGDOM, OWNER, "galacticwars:republic", settlement);
        ArmyGroupRecord first = group(COMMANDER_ONE, MEMBER_ONE, firstAnchor, 4L);
        ArmyGroupRecord second = group(COMMANDER_TWO, MEMBER_TWO, secondAnchor, 11L);

        JsonObject encoded = new JsonObject();
        JsonArray kingdoms = new JsonArray();
        kingdoms.add(KingdomCodecs.KINGDOM_RECORD.encodeStart(JsonOps.INSTANCE, kingdom).getOrThrow());
        encoded.add("kingdoms", kingdoms);
        JsonArray groups = new JsonArray();
        groups.add(KingdomCodecs.ARMY_GROUP.encodeStart(JsonOps.INSTANCE, first).getOrThrow());
        groups.add(KingdomCodecs.ARMY_GROUP.encodeStart(JsonOps.INSTANCE, second).getOrThrow());
        encoded.add("army_groups", groups);
        KingdomSavedData data = KingdomSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        return new Fixture(data, first, second);
    }

    private static ArmyGroupRecord group(UUID commander, UUID member, ArmyLocation anchor, long revision) {
        ArmyGroupRecord created = ArmyGroupRecord.create(
                OWNER, KINGDOM, commander, List.of(member), ArmyFormation.LINE, anchor, 0L);
        return new ArmyGroupRecord(
                created.id(), OWNER, KINGDOM, Optional.of(commander), List.of(member), created.order(),
                new ArmyGroupSimulation(ArmyGroupLifecycleState.LIVE, anchor, 0L, revision, 0L, ""),
                List.of(), created.name(), Optional.of(anchor), List.of(), Optional.empty(), 0,
                created.formationSlotAssignments(), Optional.empty(), Optional.empty());
    }

    private static ArmyGroupRecord hold(ArmyGroupRecord group) {
        return group.withOrder(new ArmyGroupOrder(
                ArmyCommandType.HOLD_POSITION, Optional.of(group.simulation().anchor()), Optional.empty(),
                group.order().formation(), group.order().spacing()));
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertFalse(boolean condition, String label) {
        if (condition) {
            throw new AssertionError(label + " expected false");
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label + " expected true");
        }
    }

    private record Fixture(KingdomSavedData data, ArmyGroupRecord first, ArmyGroupRecord second) {
    }
}
