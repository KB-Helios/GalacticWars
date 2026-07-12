package galacticwars.clonewars.progression;

import java.util.UUID;

public final class ForceAbilityRuntimeServiceTest {
    public static void main(String[] args) {
        UUID player = UUID.randomUUID();
        ProgressionState progression = ProgressionState.create(player);
        progression = apply(progression, player, ProgressionEventType.FACTION_PLEDGED,
                "galacticwars:republic");
        progression = apply(progression, player, ProgressionEventType.BUILDING_COMPLETED,
                "command_center");
        progression = apply(progression, player, ProgressionEventType.RECRUIT_HIRED,
                "galacticwars:clone_trooper");
        progression = apply(progression, player, ProgressionEventType.QUEST_ADVANCED,
                "republic_chapter_1");
        progression = apply(progression, player, ProgressionEventType.DELIVERY_COMPLETED,
                "starter_delivery");
        progression = apply(progression, player, ProgressionEventType.BUILDING_COMPLETED,
                "forward_base");
        progression = apply(progression, player, ProgressionEventType.PLANET_VISITED,
                "kamino");
        progression = apply(progression, player, ProgressionEventType.QUEST_ADVANCED,
                "republic_chapter_2");
        progression = GalacticSystemsService.unlockForceAbility(
                progression, UUID.randomUUID(), "light_push").state();

        ForceRuntimeState runtime = ForceRuntimeState.full();
        ForceAbilityRuntimeService.ActivationDecision activation = ForceAbilityRuntimeService.activate(
                progression, runtime, "light_push", 100L, false, true);
        assertTrue(activation.accepted() && activation.state().energy() == 80
                        && activation.cooldownTicks() == 60,
                "unlocked ability spends energy and starts cooldown");
        ForceAbilityRuntimeService.ActivationDecision replay = ForceAbilityRuntimeService.activate(
                progression, activation.state(), "light_push", 100L, false, true);
        assertTrue(!replay.accepted() && replay.reason().equals("force_ability_cooldown")
                        && replay.state() == activation.state(),
                "duplicate activation cannot spend twice");
        ForceAbilityRuntimeService.ActivationDecision pvpBlocked = ForceAbilityRuntimeService.activate(
                progression, ForceRuntimeState.full(), "light_push", 200L, true, false);
        assertTrue(!pvpBlocked.accepted() && pvpBlocked.reason().equals("force_pvp_disabled"),
                "PvP configuration is server-authoritative");
        ForceAbilityRuntimeService.ActivationDecision locked = ForceAbilityRuntimeService.activate(
                progression, ForceRuntimeState.full(), "dark_choke", 200L, false, true);
        assertTrue(!locked.accepted() && locked.reason().equals("force_ability_locked"),
                "locked ability cannot be packet-bypassed");
        assertTrue(activation.state().regenerate(20).energy() == 100,
                "energy regeneration caps at maximum");
        assertTrue(activation.state().regenerate(Integer.MAX_VALUE).energy() == 100,
                "energy regeneration saturates without integer overflow");
        System.out.println("ForceAbilityRuntimeServiceTest passed");
    }

    private static ProgressionState apply(
            ProgressionState state,
            UUID player,
            ProgressionEventType type,
            String subject
    ) {
        ProgressionDecision decision = GalacticProgressionCoordinator.apply(state,
                new ProgressionEvent(UUID.randomUUID(), player, type, subject, 1));
        if (!decision.accepted()) throw new AssertionError(decision.reason());
        return decision.state();
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
