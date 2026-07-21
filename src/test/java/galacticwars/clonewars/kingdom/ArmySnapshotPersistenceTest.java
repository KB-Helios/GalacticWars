package galacticwars.clonewars.kingdom;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import galacticwars.clonewars.army.ArmyMemberSnapshot;
import galacticwars.clonewars.army.ArmySnapshotEquipment;
import galacticwars.clonewars.recruitment.RecruitDuty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

public final class ArmySnapshotPersistenceTest {
    private ArmySnapshotPersistenceTest() {
    }

    public static void main(String[] args) {
        emptyEquipmentAndCargoStructureRoundTrip();
        legacyEmptyItemIdsDecodeWithEmptyOffhandAndCargo();
        oversizedCargoIsRejected();

        System.out.println("ArmySnapshotPersistenceTest passed");
    }

    private static void emptyEquipmentAndCargoStructureRoundTrip() {
        ArrayList<ItemStack> cargo = new ArrayList<>();
        cargo.add(ItemStack.EMPTY);
        ArmyMemberSnapshot snapshot = snapshot(ArmySnapshotEquipment.empty(), cargo);

        assertEquals(ArmyMemberSnapshot.CARGO_SLOT_COUNT, snapshot.cargo().size(), "cargo is padded to nine slots");
        try {
            snapshot.cargo().set(0, ItemStack.EMPTY);
            throw new AssertionError("cargo accessor exposed a mutable list");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }

        JsonObject encoded = encode(snapshot);
        assertTrue(encoded.has("cargo"), "cargo is encoded");

        ArmyMemberSnapshot decoded = KingdomCodecs.ARMY_MEMBER_SNAPSHOT
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();
        assertEquals(snapshot, decoded, "snapshot codec round trip");
    }

    private static void legacyEmptyItemIdsDecodeWithEmptyOffhandAndCargo() {
        JsonObject legacy = encode(snapshot(ArmySnapshotEquipment.empty(), List.of()));
        JsonObject equipment = new JsonObject();
        equipment.addProperty("main_hand", "");
        equipment.addProperty("head", "");
        equipment.addProperty("chest", "");
        equipment.addProperty("legs", "");
        equipment.addProperty("feet", "");
        legacy.add("equipment", equipment);
        legacy.remove("cargo");

        ArmyMemberSnapshot decoded = KingdomCodecs.ARMY_MEMBER_SNAPSHOT
                .parse(JsonOps.INSTANCE, legacy)
                .getOrThrow();
        assertTrue(decoded.equipment().mainHand().isEmpty(), "legacy mainhand default");
        assertTrue(decoded.equipment().head().isEmpty(), "legacy helmet default");
        assertTrue(decoded.equipment().feet().isEmpty(), "legacy boots default");
        assertTrue(decoded.equipment().offHand().isEmpty(), "legacy offhand default");
        assertEquals(ArmyMemberSnapshot.CARGO_SLOT_COUNT, decoded.cargo().size(), "legacy cargo slot count");
        assertTrue(decoded.cargo().stream().allMatch(ItemStack::isEmpty), "legacy cargo defaults empty");
    }

    private static void oversizedCargoIsRejected() {
        try {
            snapshot(ArmySnapshotEquipment.empty(),
                    java.util.Collections.nCopies(ArmyMemberSnapshot.CARGO_SLOT_COUNT + 1, ItemStack.EMPTY));
            throw new AssertionError("oversized cargo was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static ArmyMemberSnapshot snapshot(ArmySnapshotEquipment equipment, List<ItemStack> cargo) {
        return new ArmyMemberSnapshot(
                UUID.fromString("00000000-0000-0000-0000-00000000e001"),
                "galacticwars:clone_trooper",
                "galacticwars:clone_trooper",
                UUID.fromString("00000000-0000-0000-0000-00000000e002"),
                UUID.fromString("00000000-0000-0000-0000-00000000e003"),
                RecruitDuty.SOLDIER,
                14.5F,
                83,
                72,
                40,
                11L,
                equipment,
                cargo,
                "CT-6116");
    }

    private static JsonObject encode(ArmyMemberSnapshot snapshot) {
        JsonElement encoded = KingdomCodecs.ARMY_MEMBER_SNAPSHOT
                .encodeStart(JsonOps.INSTANCE, snapshot)
                .getOrThrow();
        if (!encoded.isJsonObject()) {
            throw new AssertionError("army snapshot codec did not produce an object");
        }
        return encoded.getAsJsonObject();
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label + " expected true");
        }
    }
}
