package galacticwars.clonewars.classes;

import java.util.Set;

public final class ClassAbilityExecutorCoverageTest {
    private static final Set<String> ENABLED_LAUNCH_ABILITIES = Set.of(
            "suppressive_fire", "formation_discipline", "tactical_scan", "combat_mobility",
            "saber_guard", "guardian_rally", "networked_volley", "droid_link", "brace",
            "heavy_fire", "ambush_dash", "target_disruption", "close_assault_roll",
            "beskar_training", "target_paint", "steady_aim", "braced_barrage",
            "heavy_resistance", "intimidation", "streetwise", "marked_quarry",
            "bounty_focus", "quick_draw", "smoke_charge", "blade_dance", "defensive_ward",
            "crippling_shot", "hunter_patience", "berserk", "knockback_resistance",
            "jedi_weapon_mastery", "sith_weapon_mastery", "nightsister_magick_affinity");

    public static void main(String[] args) {
        if (!ClassAbilityExecutorCatalog.registeredIds().equals(ENABLED_LAUNCH_ABILITIES)) {
            throw new AssertionError("Every enabled launch ability must have exactly one executor");
        }
        if (ClassAbilityExecutorCatalog.registeredIds().stream()
                .map(ClassAbilityExecutorCatalog::family).collect(java.util.stream.Collectors.toSet()).size() != 6) {
            throw new AssertionError("All six class-effect families must be represented");
        }
        System.out.println("ClassAbilityExecutorCoverageTest passed");
    }
}
