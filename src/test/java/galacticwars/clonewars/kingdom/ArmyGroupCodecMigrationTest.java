package galacticwars.clonewars.kingdom;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import galacticwars.clonewars.army.ArmyCommandType;
import galacticwars.clonewars.army.ArmyEngagementStance;
import galacticwars.clonewars.army.ArmyFormation;
import galacticwars.clonewars.army.ArmyGroupLifecycleState;
import galacticwars.clonewars.army.ArmyGroupOrder;
import galacticwars.clonewars.army.ArmyGroupRecord;
import galacticwars.clonewars.army.ArmyGroupSimulation;
import galacticwars.clonewars.army.ArmyGroupTactics;
import galacticwars.clonewars.army.ArmyLocation;
import galacticwars.clonewars.army.ArmyMarchPhase;
import galacticwars.clonewars.army.ArmyPatrolEnemyPolicy;
import galacticwars.clonewars.army.ArmyPatrolMode;
import galacticwars.clonewars.army.ArmyPatrolPlan;
import galacticwars.clonewars.army.ArmyPatrolState;
import galacticwars.clonewars.army.ArmyPatrolStatus;
import galacticwars.clonewars.army.ArmyPatrolWaypoint;
import galacticwars.clonewars.army.ArmyRangedFirePolicy;
import galacticwars.clonewars.army.ArmyTargetPriority;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Guards lazy optional-field migration for army records already present in saves. */
public final class ArmyGroupCodecMigrationTest {
    private static final UUID GROUP = UUID.fromString("00000000-0000-0000-0000-00000000d001");
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-00000000d002");
    private static final UUID KINGDOM = UUID.fromString("00000000-0000-0000-0000-00000000d003");
    private static final UUID COMMANDER = UUID.fromString("00000000-0000-0000-0000-00000000d004");
    private static final UUID MEMBER = UUID.fromString("00000000-0000-0000-0000-00000000d005");

    private ArmyGroupCodecMigrationTest() {
    }

    public static void main(String[] args) {
        legacyGroupsDecodeWithoutMaterializingNewFields();
        malformedLegacyPatrolRoutesDecodeWithoutMaterializingAPlan();
        enhancedPlansAndTacticsRoundTripLosslessly();

        System.out.println("ArmyGroupCodecMigrationTest passed");
    }

    private static void legacyGroupsDecodeWithoutMaterializingNewFields() {
        ArmyLocation first = location(0.0D, 64.0D, 0.0D);
        ArmyLocation second = location(16.0D, 64.0D, 0.0D);
        ArmyGroupRecord legacy = new ArmyGroupRecord(
                GROUP, OWNER, KINGDOM, Optional.of(COMMANDER), List.of(MEMBER),
                new ArmyGroupOrder(ArmyCommandType.PATROL_ROUTE, Optional.of(first), Optional.empty(),
                        ArmyFormation.LINE, 2),
                new ArmyGroupSimulation(ArmyGroupLifecycleState.LIVE, first, 40L, 7L, 0L, ""),
                List.of(), "Legacy Squad", Optional.of(first), List.of(first, second), Optional.empty(), 3);

        JsonObject encoded = encode(legacy);
        assertFalse(encoded.has("formation_slots"), "legacy formation slots stay absent");
        assertFalse(encoded.has("patrol_plan"), "legacy patrol plan stays absent");
        assertFalse(encoded.has("tactics"), "legacy tactics stay absent");
        assertFalse(encoded.getAsJsonObject("simulation").has("march"), "legacy march state stays absent");

        ArmyGroupRecord decoded = KingdomCodecs.ARMY_GROUP.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertTrue(decoded.formationSlotAssignments().isEmpty(), "legacy slots remain optional after decode");
        assertTrue(decoded.patrolPlan().isEmpty(), "legacy patrol remains optional after decode");
        assertTrue(decoded.tactics().isEmpty(), "legacy tactics remain optional after decode");
        assertEquals(1, decoded.effectiveFormationSlotAssignments().size(), "derived legacy slot");
        assertEquals(ArmyPatrolMode.LOOP, decoded.effectivePatrolPlan().orElseThrow().mode(),
                "derived legacy patrol mode");
        assertEquals(ArmyGroupTactics.DEFAULT, decoded.effectiveTactics(), "derived legacy doctrine");
        assertEquals(ArmyMarchPhase.HALTED, decoded.simulation().marchState().phase(),
                "legacy groups receive a safe halted march default");

        JsonObject reencoded = encode(decoded);
        assertFalse(reencoded.has("formation_slots"), "read-only legacy load does not materialize slots");
        assertFalse(reencoded.has("patrol_plan"), "read-only legacy load does not materialize patrol state");
        assertFalse(reencoded.has("tactics"), "read-only legacy load does not materialize doctrine");
    }

    private static void malformedLegacyPatrolRoutesDecodeWithoutMaterializingAPlan() {
        ArmyLocation first = location(0.0D, 64.0D, 0.0D);
        ArmyLocation second = location(16.0D, 64.0D, 0.0D);
        ArmyGroupRecord legacy = new ArmyGroupRecord(
                GROUP, OWNER, KINGDOM, Optional.of(COMMANDER), List.of(MEMBER),
                new ArmyGroupOrder(ArmyCommandType.PATROL_ROUTE, Optional.of(first), Optional.empty(),
                        ArmyFormation.LINE, 2),
                new ArmyGroupSimulation(ArmyGroupLifecycleState.LIVE, first, 40L, 7L, 0L, ""),
                List.of(), "Incomplete Legacy Squad", Optional.of(first), List.of(first, second), Optional.empty(), 3);
        JsonObject encoded = encode(legacy);
        JsonArray incompleteRoute = new JsonArray();
        incompleteRoute.add(KingdomCodecs.ARMY_LOCATION.encodeStart(JsonOps.INSTANCE, first).getOrThrow());
        encoded.add("patrol_route", incompleteRoute);

        ArmyGroupRecord decoded = KingdomCodecs.ARMY_GROUP.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertTrue(decoded.patrolRoute().isEmpty(), "incomplete legacy route is discarded safely");
        assertTrue(decoded.effectivePatrolPlan().isEmpty(), "incomplete route does not create an invalid plan");

        JsonArray oversizedRoute = new JsonArray();
        for (ArmyLocation waypoint : Collections.nCopies(33, first)) {
            oversizedRoute.add(KingdomCodecs.ARMY_LOCATION.encodeStart(JsonOps.INSTANCE, waypoint).getOrThrow());
        }
        encoded.add("patrol_route", oversizedRoute);
        ArmyGroupRecord oversized = KingdomCodecs.ARMY_GROUP.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertTrue(oversized.patrolRoute().isEmpty(), "oversized legacy route is discarded safely");
        assertTrue(oversized.effectivePatrolPlan().isEmpty(), "oversized route does not create an invalid plan");
    }

    private static void enhancedPlansAndTacticsRoundTripLosslessly() {
        ArmyLocation first = location(4.0D, 65.0D, -4.0D);
        ArmyLocation second = location(20.0D, 65.0D, -4.0D);
        ArmyPatrolPlan plan = new ArmyPatrolPlan(
                List.of(new ArmyPatrolWaypoint(first, 7), new ArmyPatrolWaypoint(second, 13)),
                ArmyPatrolMode.PING_PONG,
                new ArmyPatrolState(1, -1, 5, ArmyPatrolStatus.PAUSED),
                3,
                1.25D,
                ArmyPatrolEnemyPolicy.RETREAT_FROM_HOSTILES,
                "Forward Route");
        ArmyGroupTactics tactics = new ArmyGroupTactics(
                Optional.of(450.0F), true, true, ArmyEngagementStance.AGGRESSIVE,
                ArmyTargetPriority.LOWEST_HEALTH, ArmyRangedFirePolicy.HOLD_FIRE);
        ArmyGroupRecord configured = ArmyGroupRecord.create(
                        OWNER, KINGDOM, COMMANDER, List.of(MEMBER), ArmyFormation.HOLLOW_SQUARE, first, 100L)
                .withPatrolPlanAndOrder(plan, new ArmyGroupOrder(
                        ArmyCommandType.PATROL_ROUTE, Optional.of(second), Optional.empty(),
                        ArmyFormation.HOLLOW_SQUARE, 3))
                .withTactics(tactics);

        JsonObject encoded = encode(configured);
        assertTrue(encoded.has("formation_slots"), "new group persists deterministic slots");
        assertTrue(encoded.has("patrol_plan"), "new plan persists");
        assertTrue(encoded.has("tactics"), "new tactics persist");
        assertTrue(encoded.getAsJsonObject("simulation").has("march"), "preferred formation march state persists");
        assertEquals("Forward Route", encoded.getAsJsonObject("patrol_plan").get("name").getAsString(),
                "named route persists");

        ArmyGroupRecord decoded = KingdomCodecs.ARMY_GROUP.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(configured.formationSlotAssignments(), decoded.formationSlotAssignments(), "slot round trip");
        assertEquals(Optional.of(plan), decoded.patrolPlan(), "patrol plan round trip");
        assertEquals(Optional.of(tactics), decoded.tactics(), "tactics round trip");
        assertEquals(90.0F, decoded.effectiveTactics().formationYawDegrees().orElseThrow(),
                "yaw is normalized before persistence");

        JsonObject preNamedRoute = encoded.deepCopy();
        preNamedRoute.getAsJsonObject("patrol_plan").remove("name");
        ArmyGroupRecord decodedWithoutRouteName = KingdomCodecs.ARMY_GROUP.parse(JsonOps.INSTANCE, preNamedRoute)
                .getOrThrow();
        assertEquals(ArmyPatrolPlan.DEFAULT_NAME,
                decodedWithoutRouteName.patrolPlan().orElseThrow().name(),
                "pre-named patrol plan gets lazy default name");
    }

    private static JsonObject encode(ArmyGroupRecord group) {
        JsonElement encoded = KingdomCodecs.ARMY_GROUP.encodeStart(JsonOps.INSTANCE, group).getOrThrow();
        if (!encoded.isJsonObject()) {
            throw new AssertionError("army group codec did not produce an object");
        }
        return encoded.getAsJsonObject();
    }

    private static ArmyLocation location(double x, double y, double z) {
        return new ArmyLocation("minecraft:overworld", x, y, z);
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
}
