package galacticwars.clonewars.progression;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.data.LaunchContentRuntime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LaunchContentCatalog {
    private LaunchContentCatalog() {
    }

    public static LaunchContentDefinitions data() {
        return LaunchContentRuntime.current().definitions();
    }

    public static List<String> factions() {
        return LaunchContentRuntime.current().factions();
    }

    public static Map<String, List<String>> units() {
        return LaunchContentRuntime.current().units();
    }

    public static List<String> planets() { return data().planetIds(); }
    public static List<String> vehicles() { return data().vehicleIds(); }
    public static Set<String> forceAbilities() { return data().forceAbilityIds(); }
    public static List<String> quests() { return data().questIds(); }
    public static Map<String, LaunchContentDefinitions.TradeDefinition> trades() { return data().trades(); }
    public static Set<String> questUnlocks(String id) { return data().questUnlocks(id); }
    public static List<String> questObjectives(String id) { return data().questObjectives(id); }
    public static int questRewardCredits(String id) { return data().questRewardCredits(id); }
    public static int regionRewardCredits(String id) { return data().regionRewardCredits(id); }
}
