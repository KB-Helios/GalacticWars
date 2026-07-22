package galacticwars.clonewars.faction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class StarterFactionContractTest {
    private static final Path ROOT = Path.of(
            "src/main/resources/data/galacticwars/galacticwars/factions");

    private StarterFactionContractTest() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> expected = Map.of(
                "republic", "galacticwars:clone_trooper",
                "separatist", "galacticwars:b1_battle_droid",
                "mandalorian", "galacticwars:mandalorian_warrior",
                "hutt_cartel", "galacticwars:hutt_enforcer",
                "nightsister", "galacticwars:nightsister_acolyte");
        for (var entry : expected.entrySet()) {
            JsonObject faction = JsonParser.parseString(Files.readString(ROOT.resolve(entry.getKey() + ".json")))
                    .getAsJsonObject();
            String actual = faction.get("starter_unit").getAsString();
            if (!entry.getValue().equals(actual)) {
                throw new AssertionError(entry.getKey() + " starter unit expected <"
                        + entry.getValue() + "> but was <" + actual + ">");
            }
        }
        String manager = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/data/GameplayDataManager.java"));
        if (!manager.contains("starter_unit belongs to") || !manager.contains("has unknown starter_unit")) {
            throw new AssertionError("gameplay reload must validate starter unit existence and faction ownership");
        }
        System.out.println("StarterFactionContractTest passed");
    }
}
