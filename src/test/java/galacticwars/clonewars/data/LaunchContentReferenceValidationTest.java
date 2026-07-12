package galacticwars.clonewars.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LaunchContentReferenceValidationTest {
    private LaunchContentReferenceValidationTest() {
    }

    public static void main(String[] args) {
        var quest = new LaunchContentDefinitions.QuestDefinition(
                "chapter_1", List.of("command_center"), 10, Set.of("vehicle_mastery", "trading"));
        var vehicle = new LaunchContentDefinitions.VehicleDefinition(
                "speeder", "hover", 1, 40, 100, "chapter_1");
        var ability = new LaunchContentDefinitions.ForceAbilityDefinition(
                "push", "light", 10, 20, "chapter_1", false);
        var trade = new LaunchContentDefinitions.TradeDefinition(
                "quartermaster", "republic", 1, "galacticwars:energy_cell", 1, "faction_intro");
        Map<String, LaunchContentDefinitions.QuestDefinition> quests = Map.of(quest.id(), quest);

        LaunchContentValidator.validateReferences(
                Map.of(vehicle.id(), vehicle), Map.of(ability.id(), ability), quests, Map.of(trade.id(), trade));
        assertRejected(() -> LaunchContentValidator.validateReferences(
                Map.of("bad", new LaunchContentDefinitions.VehicleDefinition(
                        "bad", "hover", 1, 1, 1, "missing")), Map.of(), quests, Map.of()),
                "unknown vehicle unlock");
        assertRejected(() -> LaunchContentValidator.validateReferences(
                Map.of(), Map.of(), quests, Map.of("bad", new LaunchContentDefinitions.TradeDefinition(
                        "bad", "republic", 1, "galacticwars:energy_cell", 1, "missing"))),
                "unknown trade unlock");
        assertRejected(() -> LaunchContentValidator.validateReferences(
                Map.of(), Map.of("bad", new LaunchContentDefinitions.ForceAbilityDefinition(
                        "bad", "light", 1, 1, "missing", false)), quests, Map.of()),
                "unknown Force ability quest");
        System.out.println("LaunchContentReferenceValidationTest passed");
    }

    private static void assertRejected(Runnable action, String label) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(label);
    }
}
