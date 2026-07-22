package galacticwars.clonewars.classes;

import galacticwars.clonewars.ability.AbilityActivation;
import galacticwars.clonewars.ability.AbilityDefinition;
import galacticwars.clonewars.ability.AbilityId;
import galacticwars.clonewars.ability.AbilityKind;
import galacticwars.clonewars.army.ArmyUnitId;
import galacticwars.clonewars.faction.FactionId;

import java.util.List;

public final class ClassAbilityRuntimeServiceTest {
    private ClassAbilityRuntimeServiceTest() {
    }

    public static void main(String[] args) {
        activationIsServerAuthoritativeAndAtomic();
        areaAbilitiesDoNotRequireAnAimedTarget();
        npcEvaluationIsStaggered();
        experienceRanksAreBounded();
        playerLoadoutSwitchPreservesProgressAndCombatState();
        System.out.println("ClassAbilityRuntimeServiceTest passed");
    }

    private static void areaAbilitiesDoNotRequireAnAimedTarget() {
        AbilityDefinition area = new AbilityDefinition(
                AbilityId.of("tactical_scan"), "Tactical Scan", AbilityKind.TACTICAL,
                AbilityActivation.AREA, 160, 20, 20.0D, 40, true);
        UnitClassDefinition areaClass = unitClass(area.id());
        ClassAbilityRuntimeService.ActivationDecision decision = ClassAbilityRuntimeService.activate(
                areaClass, area, ClassProgressState.unassigned().assign(areaClass.id()),
                100L, false, 0.0D, false, false);
        assertTrue(decision.accepted(), "area ability does not require a crosshair target");
        assertEquals(80, decision.state().resource(), "area ability resource charge");
    }

    private static void activationIsServerAuthoritativeAndAtomic() {
        AbilityDefinition ability = ability();
        UnitClassDefinition unitClass = unitClass(ability.id());
        ClassProgressState state = ClassProgressState.unassigned().assign(unitClass.id());
        ClassAbilityRuntimeService.ActivationDecision missingTarget = ClassAbilityRuntimeService.activate(
                unitClass, ability, state, 100L, false, 0.0D, false, false);
        assertEquals("target_required", missingTarget.reason(), "missing target");
        ClassAbilityRuntimeService.ActivationDecision accepted = ClassAbilityRuntimeService.activate(
                unitClass, ability, state, 100L, true, 12.0D, false, false);
        assertTrue(accepted.accepted(), "valid activation accepted");
        assertEquals(85, accepted.state().resource(), "resource charged once");
        ClassAbilityRuntimeService.ActivationDecision replay = ClassAbilityRuntimeService.activate(
                unitClass, ability, accepted.state(), 100L, true, 12.0D, false, false);
        assertEquals("ability_cooldown", replay.reason(), "same-tick replay rejected");
        assertEquals(accepted.state(), replay.state(), "replay does not mutate state");
        ClassAbilityRuntimeService.ActivationDecision pvp = ClassAbilityRuntimeService.activate(
                unitClass, ability, state, 100L, true, 12.0D, true, false);
        assertEquals("class_pvp_disabled", pvp.reason(), "PvP policy enforced");
        AbilityDefinition self = new AbilityDefinition(
                AbilityId.of("brace"), "Brace", AbilityKind.MARTIAL,
                AbilityActivation.SELF, 100, 15, 0.0D, 20, true);
        UnitClassDefinition selfClass = unitClass(self.id());
        ClassProgressState selfState = ClassProgressState.unassigned().assign(selfClass.id());
        ClassAbilityRuntimeService.ActivationDecision selfActivation = ClassAbilityRuntimeService.activate(
                selfClass, self, selfState, 100L, true, 100.0D, true, false);
        assertTrue(selfActivation.accepted(), "self ability ignores unrelated combat target range and PvP");
    }

    private static void npcEvaluationIsStaggered() {
        AbilityDefinition ability = ability();
        int evaluations = 0;
        for (int actor = 0; actor < 20; actor++) {
            if (ClassAbilityRuntimeService.shouldEvaluateNpc(ability, actor, 100L)) {
                evaluations++;
            }
        }
        assertEquals(1, evaluations, "one of twenty actors evaluates per tick phase");
    }

    private static void experienceRanksAreBounded() {
        ClassProgressState state = ClassProgressState.unassigned().assign(UnitClassId.of("clone_trooper"));
        state = state.gainExperience(100L);
        assertEquals(2, state.rank(), "first promotion");
        state = state.gainExperience(Long.MAX_VALUE);
        assertEquals(ClassProgressState.MAX_RANK, state.rank(), "rank cap");
        assertEquals(0L, state.experience(), "experience stops accumulating at cap");
        ClassProgressState spent = ClassProgressState.unassigned()
                .assign(UnitClassId.of("clone_trooper"))
                .activate(AbilityId.of("suppressive_fire"), 15, 200L, 100L);
        assertEquals(ClassProgressState.MAX_RESOURCE, spent.regenerate(Integer.MAX_VALUE).resource(),
                "resource regeneration saturates without overflow");
        ClassProgressState missionCredit = ClassProgressState.unassigned()
                .assign(UnitClassId.of("clone_trooper"))
                .creditCompletedMissions(Integer.MAX_VALUE);
        assertEquals(1_000_000, missionCredit.creditedMissions(), "mission credit cap");
    }

    private static void playerLoadoutSwitchPreservesProgressAndCombatState() {
        ClassProgressState current = ClassProgressState.unassigned()
                .assign(UnitClassId.of("clone_trooper"))
                .gainExperience(100L)
                .activate(AbilityId.of("suppressive_fire"), 15, 200L, 100L);
        ClassProgressState switched = current.switchClass(UnitClassId.of("arc_trooper"));
        assertEquals("galacticwars:arc_trooper", switched.classId(), "switched class id");
        assertEquals(current.rank(), switched.rank(), "rank survives a loadout switch");
        assertEquals(current.experience(), switched.experience(), "experience survives a loadout switch");
        assertEquals(current.resource(), switched.resource(), "focus survives a loadout switch");
        assertEquals(current.cooldownEnds(), switched.cooldownEnds(), "cooldowns survive a loadout switch");
        assertEquals(switched, switched.switchClass(UnitClassId.of("arc_trooper")),
                "reselecting the active class is idempotent");
    }

    private static AbilityDefinition ability() {
        return new AbilityDefinition(
                AbilityId.of("suppressive_fire"), "Suppressive Fire", AbilityKind.MARTIAL,
                AbilityActivation.TARGET, 100, 15, 24.0D, 20, true);
    }

    private static UnitClassDefinition unitClass(AbilityId abilityId) {
        return new UnitClassDefinition(
                UnitClassId.of("clone_trooper"), "Clone Trooper", FactionId.of("republic"),
                ArmyUnitId.of("clone_trooper"), true, List.of(abilityId), List.of(), "");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label);
        }
    }
}
