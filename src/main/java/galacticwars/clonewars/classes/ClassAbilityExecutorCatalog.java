package galacticwars.clonewars.classes;

import java.util.Map;
import java.util.Set;

/** Minecraft-free executor catalog used by reload validation and harnesses. */
public final class ClassAbilityExecutorCatalog {
    public enum Family { PROJECTILE, MARK, MOBILITY, DEFENSIVE, SUPPORT, PASSIVE }

    private static final Map<String, Family> EXECUTORS = Map.ofEntries(
            Map.entry("suppressive_fire", Family.PROJECTILE), Map.entry("networked_volley", Family.PROJECTILE),
            Map.entry("heavy_fire", Family.PROJECTILE), Map.entry("braced_barrage", Family.PROJECTILE),
            Map.entry("crippling_shot", Family.PROJECTILE), Map.entry("tactical_scan", Family.MARK),
            Map.entry("target_disruption", Family.MARK), Map.entry("target_paint", Family.MARK),
            Map.entry("intimidation", Family.MARK), Map.entry("marked_quarry", Family.MARK),
            Map.entry("smoke_charge", Family.MARK), Map.entry("ambush_dash", Family.MOBILITY),
            Map.entry("close_assault_roll", Family.MOBILITY), Map.entry("quick_draw", Family.MOBILITY),
            Map.entry("blade_dance", Family.MOBILITY), Map.entry("saber_guard", Family.DEFENSIVE),
            Map.entry("brace", Family.DEFENSIVE), Map.entry("berserk", Family.DEFENSIVE),
            Map.entry("guardian_rally", Family.SUPPORT), Map.entry("formation_discipline", Family.PASSIVE),
            Map.entry("combat_mobility", Family.PASSIVE), Map.entry("droid_link", Family.PASSIVE),
            Map.entry("beskar_training", Family.PASSIVE), Map.entry("steady_aim", Family.PASSIVE),
            Map.entry("heavy_resistance", Family.PASSIVE), Map.entry("streetwise", Family.PASSIVE),
            Map.entry("bounty_focus", Family.PASSIVE), Map.entry("defensive_ward", Family.PASSIVE),
            Map.entry("hunter_patience", Family.PASSIVE), Map.entry("knockback_resistance", Family.PASSIVE));

    private ClassAbilityExecutorCatalog() {}

    public static boolean registered(String abilityId) { return EXECUTORS.containsKey(path(abilityId)); }
    public static Set<String> registeredIds() { return EXECUTORS.keySet(); }
    public static Family family(String abilityId) {
        Family result = EXECUTORS.get(path(abilityId));
        if (result == null) throw new IllegalArgumentException("Missing class ability executor " + abilityId);
        return result;
    }
    private static String path(String id) {
        int separator = id.indexOf(':');
        return separator < 0 ? id : id.substring(separator + 1);
    }
}
