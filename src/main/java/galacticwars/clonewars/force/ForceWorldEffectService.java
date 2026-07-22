package galacticwars.clonewars.force;

import galacticwars.clonewars.Config;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.combat.LightsaberDeflectionService;
import galacticwars.clonewars.data.CoreContentBindings;
import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.faction.FactionRelation;
import galacticwars.clonewars.network.ForceHudPayload;
import galacticwars.clonewars.network.GalacticNetwork;
import galacticwars.clonewars.progression.ForceAbilityRuntimeService;
import galacticwars.clonewars.progression.ForceProgressionService;
import galacticwars.clonewars.progression.ForceRuntimeState;
import galacticwars.clonewars.progression.ForceSavedData;
import galacticwars.clonewars.progression.GalacticSystemsService;
import galacticwars.clonewars.progression.LaunchContentCatalog;
import galacticwars.clonewars.progression.ProgressionDecision;
import galacticwars.clonewars.progression.ProgressionEvent;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.progression.ProgressionState;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/** Server-authoritative Force activation, charge/channel sessions, and atomic state commit. */
public final class ForceWorldEffectService {
    private static final Map<UUID, ActiveCast> ACTIVE_CASTS = new LinkedHashMap<>();
    private static final Map<UUID, FailureNotice> FAILURE_NOTICES = new LinkedHashMap<>();
    private static final int MAX_FAILURE_NOTICES = 128;
    private static final int FAILURE_DISPLAY_TICKS = 40;

    private ForceWorldEffectService() {
    }

    /** Compatibility surface for existing GameTests and old one-click callers. */
    public static synchronized boolean activate(ServerPlayer player, UUID activationId, int slot) {
        ResolvedCast cast = resolve(player, slot);
        if (cast == null) return false;
        int ticks = Math.max(cast.ability().minChargeTicks(), cast.ability().maxChargeTicks());
        return perform(player, activationId, slot, cast, ticks, false);
    }

    public static synchronized boolean activate(
            ServerPlayer player,
            UUID activationId,
            int slot,
            ForceActivationPhase phase
    ) {
        if (slot < 0 || slot >= ForceRuntimeState.MAX_EQUIPPED_ABILITIES
                || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        if (phase == ForceActivationPhase.CANCEL) {
            boolean cancelled = cancelMatching(player.getUUID(), activationId, true);
            ForceBlockTelekinesisService.cancel(player);
            return cancelled;
        }
        ResolvedCast cast = resolve(player, slot);
        if (cast == null) return false;
        if (phase == ForceActivationPhase.PRESS) {
            return switch (cast.ability().activation()) {
                case "instant" -> perform(player, activationId, slot, cast, 0, false);
                case "charged" -> beginCharged(player, activationId, slot, cast);
                case "channeled" -> canBegin(player)
                        && perform(player, activationId, slot, cast, 0, true)
                        && begin(player, activationId, slot, cast, true);
                default -> fail(player, "invalid_force_activation");
            };
        }
        ActiveCast active = ACTIVE_CASTS.get(player.getUUID());
        if (active == null || !active.activationId().equals(activationId) || active.slot() != slot) {
            return fail(player, "force_cast_not_active");
        }
        ACTIVE_CASTS.remove(player.getUUID());
        if (active.channeled()) {
            syncSnapshot(player, ForceSavedData.get(level));
            return true;
        }
        int heldTicks = ForcePhysicsRules.boundedChannelTicks(
                (int) Math.max(0L, level.getGameTime() - active.startedAt()));
        if (heldTicks < cast.ability().minChargeTicks()) {
            ForceBlockTelekinesisService.cancel(player);
            return fail(player, "force_charge_too_short");
        }
        return perform(player, activationId, slot, cast, heldTicks, false);
    }

    public static synchronized void tickChannels(MinecraftServer server) {
        if (ACTIVE_CASTS.isEmpty()) return;
        Iterator<Map.Entry<UUID, ActiveCast>> iterator = ACTIVE_CASTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveCast> entry = iterator.next();
            ActiveCast active = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()
                    || !player.level().dimension().identifier().toString().equals(active.dimensionId())
                    || player.containerMenu != player.inventoryMenu) {
                iterator.remove();
                continue;
            }
            long gameTime = player.level().getGameTime();
            int activeTicks = ForcePhysicsRules.boundedChannelTicks(
                    (int) Math.max(0L, gameTime - active.startedAt()));
            if (!active.channeled() || activeTicks >= active.maximumTicks()) {
                iterator.remove();
                continue;
            }
            if (gameTime % 5L != 0L) continue;
            LaunchContentDefinitions.ForceAbilityDefinition ability =
                    LaunchContentCatalog.data().forceAbilities().get(active.abilityId());
            if (ability == null) {
                iterator.remove();
                continue;
            }
            ForceSavedData data = ForceSavedData.get((ServerLevel) player.level());
            ForceRuntimeState before = data.state(player.getUUID());
            int sustain = ability.sustainEnergy();
            if (sustain > before.energy()) {
                iterator.remove();
                fail(player, "insufficient_force_energy");
                continue;
            }
            List<Entity> targets = ForceTargetingService.targets(
                    (ServerLevel) player.level(), player, ability.target(), ability.range());
            ForceEffectReport report = ForceEffectExecutorRegistry.execute(new ForceEffectContext(
                    (ServerLevel) player.level(), player, ability, targets, activeTicks));
            if (!report.succeeded()) {
                if (!ability.executor().equals("guard") && !ability.executor().equals("spirit_ward")) {
                    iterator.remove();
                }
                continue;
            }
            ForceRuntimeState after = before.spendSustain(sustain);
            after = awardMastery(after, ability, report, gameTime);
            data.compareAndSet(player.getUUID(), before, after);
        }
    }

    public static synchronized void cancelAll(UUID playerId) {
        ACTIVE_CASTS.remove(playerId);
        FAILURE_NOTICES.remove(playerId);
    }

    private static boolean begin(
            ServerPlayer player, UUID activationId, int slot,
            ResolvedCast cast, boolean channeled
    ) {
        if (!canBegin(player)) return false;
        ActiveCast existing = ACTIVE_CASTS.get(player.getUUID());
        if (existing != null) {
            if (existing.activationId().equals(activationId)) return true;
            return fail(player, "force_cast_already_active");
        }
        ServerLevel level = (ServerLevel) player.level();
        ACTIVE_CASTS.put(player.getUUID(), new ActiveCast(
                activationId, slot, cast.abilityId(), level.getGameTime(),
                Math.max(1, cast.ability().maxChargeTicks()), channeled,
                level.dimension().identifier().toString()));
        sendSnapshot(player, cast.forceState(), level.getGameTime(), slot,
                channeled ? 2 : 1, 0);
        return true;
    }

    private static boolean canBegin(ServerPlayer player) {
        if (ACTIVE_CASTS.size() >= ForcePhysicsRules.MAX_ACTIVE_CHANNELS
                && !ACTIVE_CASTS.containsKey(player.getUUID())) {
            return fail(player, "force_channel_limit");
        }
        return true;
    }

    private static boolean beginCharged(
            ServerPlayer player, UUID activationId, int slot, ResolvedCast cast
    ) {
        if (cast.ability().target().equals("held_object")) {
            ServerLevel level = (ServerLevel) player.level();
            List<Entity> entityTargets = ForceTargetingService.targets(
                    level, player, "held_object", cast.ability().range());
            if (entityTargets.isEmpty()) {
                ForceAbilityRuntimeService.ActivationDecision validation =
                        ForceAbilityRuntimeService.activate(
                                cast.progression(), cast.forceState(), activationId,
                                cast.abilityId(), level.getGameTime(), false,
                                Config.ALLOW_FORCE_PVP.getAsBoolean(), LaunchContentCatalog.data());
                if (!validation.accepted()) return fail(player, validation.reason());
                ForceEffectReport grip = ForceBlockTelekinesisService.grip(
                        player, cast.ability().range());
                if (!grip.succeeded()) return fail(player, grip.reason());
            }
        }
        if (begin(player, activationId, slot, cast, false)) return true;
        ForceBlockTelekinesisService.cancel(player);
        return false;
    }

    private static boolean perform(
            ServerPlayer player,
            UUID activationId,
            int slot,
            ResolvedCast cast,
            int activeTicks,
            boolean startingChannel
    ) {
        ServerLevel level = (ServerLevel) player.level();
        LaunchContentDefinitions content = LaunchContentCatalog.data();
        ProgressionState progression = cast.progression();
        ForceRuntimeState forceBefore = cast.forceState();
        List<Entity> targets = ForceTargetingService.targets(
                level, player, cast.ability().target(), cast.ability().range());
        boolean targetsPlayer = targets.stream().anyMatch(ServerPlayer.class::isInstance);
        ForceAbilityRuntimeService.ActivationDecision activation = ForceAbilityRuntimeService.activate(
                progression, forceBefore, activationId, cast.abilityId(), level.getGameTime(),
                targetsPlayer, Config.ALLOW_FORCE_PVP.getAsBoolean(), content);
        if (!activation.accepted()) {
            sendSnapshot(player, forceBefore, level.getGameTime(), slot, 0, 0);
            return fail(player, activation.reason());
        }
        if (activation.reason().equals("duplicate_activation")) return true;

        ForceEffectReport report = cast.ability().target().equals("held_object")
                && ForceBlockTelekinesisService.hasGrip(player)
                ? ForceBlockTelekinesisService.release(player, activeTicks)
                : ForceEffectExecutorRegistry.execute(new ForceEffectContext(
                        level, player, cast.ability(), targets, activeTicks));
        if (!report.succeeded()) {
            ForceBlockTelekinesisService.cancel(player);
            sendSnapshot(player, forceBefore, level.getGameTime(), slot, 0, 0);
            return fail(player, report.reason());
        }
        ForceRuntimeState forceAfter = awardMastery(
                activation.state(), cast.ability(), report, level.getGameTime());

        ProgressionSavedData progressionData = ProgressionSavedData.get(level);
        ProgressionState progressionAfter = progression;
        boolean progressionCommitted = false;
        UUID progressionId = UUID.nameUUIDFromBytes(("force:effect:" + activationId)
                .getBytes(StandardCharsets.UTF_8));
        if (report.progressionEligible()) {
            GalacticSystemsService.SystemDecision evaluation = GalacticSystemsService.useForceAbility(
                    progression, progressionId, cast.abilityId(), content);
            for (LaunchContentDefinitions.QuestDefinition quest : content.quests().values()) {
                if (quest.rewardMasteryExperience() > 0
                        && !progression.hasSubject(ProgressionEventType.QUEST_ADVANCED, quest.id())
                        && evaluation.state().hasSubject(
                                ProgressionEventType.QUEST_ADVANCED, quest.id())) {
                    ForceProgressionService.ProgressionDecision mastery =
                            ForceProgressionService.gainMastery(
                                    forceAfter, quest.rewardMasteryExperience(), content);
                    if (mastery.accepted()) forceAfter = mastery.state();
                }
            }
            if (evaluation.accepted() && evaluation.changed()) {
                ProgressionDecision committed = progressionData.commitEvaluated(
                        new ProgressionEvent(progressionId, player.getUUID(),
                                ProgressionEventType.FORCE_ABILITY_USED, cast.abilityId(), 1),
                        progression,
                        new ProgressionDecision(true, true, evaluation.reason(), evaluation.state()));
                progressionAfter = committed.state();
                progressionCommitted = committed.changed();
            }
        }
        ForceSavedData forceData = ForceSavedData.get(level);
        if (!forceData.compareAndSet(player.getUUID(), forceBefore, forceAfter)) {
            if (progressionCommitted) {
                progressionData.restoreAfterFailedTransaction(
                        player.getUUID(), progressionAfter, progression, true);
            }
            GalacticWars.LOGGER.error("Force state changed during {} for {}",
                    cast.abilityId(), player.getGameProfile().name());
            ForceBlockTelekinesisService.cancel(player);
            return fail(player, "transaction_failed");
        }
        FAILURE_NOTICES.remove(player.getUUID());
        sendSnapshot(player, forceAfter, level.getGameTime(), slot,
                startingChannel ? 2 : 0, activeTicks);
        emitVisuals(level, player, cast.ability(), report);
        player.sendOverlayMessage(Component.translatable(
                "message.galacticwars.force.activated", cast.abilityId(), forceAfter.energy()));
        return true;
    }

    private static ForceRuntimeState awardMastery(
            ForceRuntimeState state,
            LaunchContentDefinitions.ForceAbilityDefinition ability,
            ForceEffectReport report,
            long gameTime
    ) {
        if (!report.progressionEligible()) return state;
        ForceRuntimeState updated = state;
        for (UUID targetId : report.affectedEntities()) {
            ForceProgressionService.ProgressionDecision award =
                    ForceProgressionService.awardCombatMastery(
                            updated, targetId + ":" + ability.executor(), gameTime,
                            LaunchContentCatalog.data());
            if (award.accepted()) updated = award.state();
        }
        return updated;
    }

    private static ResolvedCast resolve(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= ForceRuntimeState.MAX_EQUIPPED_ABILITIES
                || !(player.level() instanceof ServerLevel level)) return null;
        ProgressionState progression = ProgressionSavedData.get(level).state(player.getUUID());
        ForceSavedData forceData = ForceSavedData.get(level);
        ForceRuntimeState forceState = migrateLegacyIfNeeded(
                player, progression, forceData, forceData.state(player.getUUID()));
        List<String> loadout = forceState.equippedAbilityIds();
        String abilityId = slot < loadout.size() ? loadout.get(slot) : "";
        if (abilityId.isBlank() && loadout.isEmpty()) {
            List<String> defaults = CoreContentBindings.forceSlots(forceState.traditionId());
            abilityId = slot < defaults.size() ? defaults.get(slot) : "";
        }
        LaunchContentDefinitions.ForceAbilityDefinition ability =
                LaunchContentCatalog.data().forceAbilities().get(abilityId);
        if (ability == null) {
            fail(player, forceState.initiated() ? "force_slot_empty" : "force_not_initiated");
            return null;
        }
        return new ResolvedCast(progression, forceState, abilityId, ability);
    }

    private static ForceRuntimeState migrateLegacyIfNeeded(
            ServerPlayer player,
            ProgressionState progression,
            ForceSavedData data,
            ForceRuntimeState state
    ) {
        if (state.initiated() || !progression.unlocks().contains("force_path")) return state;
        String tradition = ForceProgressionService.defaultTraditionForFaction(progression.factionId());
        String legacyPath = tradition.equals("jedi") ? "light"
                : tradition.equals("nightsister") ? "dark" : "";
        if (legacyPath.isBlank()) return state;
        ForceRuntimeState migrated = new ForceRuntimeState(
                legacyPath, state.energy(), state.cooldownEnds(), state.processedActivationIds());
        if (data.compareAndSet(player.getUUID(), state, migrated)) return migrated;
        return data.state(player.getUUID());
    }

    private static void emitVisuals(
            ServerLevel level,
            ServerPlayer player,
            LaunchContentDefinitions.ForceAbilityDefinition ability,
            ForceEffectReport report
    ) {
        level.sendParticles(
                ability.path().equals("nightsister") ? ParticleTypes.WITCH
                        : ability.path().equals("sith") ? ParticleTypes.ELECTRIC_SPARK
                        : ParticleTypes.END_ROD,
                player.getX(), player.getEyeY(), player.getZ(),
                Math.max(4, report.affectedEntities().size() * 2),
                0.35D, 0.35D, 0.35D, 0.04D);
        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 0.8F,
                ability.path().equals("sith") ? 0.65F
                        : ability.path().equals("nightsister") ? 0.85F : 1.2F);
    }

    private static boolean cancelMatching(UUID playerId, UUID activationId, boolean notify) {
        ActiveCast active = ACTIVE_CASTS.get(playerId);
        if (active == null || !active.activationId().equals(activationId)) return false;
        ACTIVE_CASTS.remove(playerId);
        return true;
    }

    private static void sendSnapshot(
            ServerPlayer player,
            ForceRuntimeState state,
            long gameTime,
            int activeSlot,
            int activeMode,
            int activeTicks
    ) {
        if (!GalacticNetwork.canPlayerReceive(player, ForceHudPayload.TYPE)) return;
        int[] cooldowns = new int[3];
        List<String> abilities = state.equippedAbilityIds();
        for (int index = 0; index < Math.min(abilities.size(), cooldowns.length); index++) {
            cooldowns[index] = (int) Math.max(0L, Math.min(Integer.MAX_VALUE,
                    state.cooldownEnds().getOrDefault(abilities.get(index), 0L) - gameTime));
        }
        int targetValidityMask = targetValidityMask(player, abilities);
        FailureNotice notice = FAILURE_NOTICES.get(player.getUUID());
        String failureReason = "";
        if (notice != null) {
            if (notice.expiresAt() > gameTime) {
                failureReason = notice.reason();
            } else {
                FAILURE_NOTICES.remove(player.getUUID());
            }
        }
        GalacticNetwork.CHANNEL.sendToPlayer(() -> player, new ForceHudPayload(
                state.energy(), state.rank(), state.masteryExperience(), state.unspentPoints(),
                state.traditionId(), abilities,
                cooldowns[0], cooldowns[1], cooldowns[2],
                activeSlot, activeMode, ForcePhysicsRules.boundedChannelTicks(activeTicks),
                targetValidityMask, failureReason));
    }

    public static synchronized void syncSnapshot(ServerPlayer player, ForceSavedData forceData) {
        if (!(player.level() instanceof ServerLevel level)) return;
        ForceRuntimeState state = forceData.state(player.getUUID());
        if (!state.initiated()) return;
        ActiveCast active = ACTIVE_CASTS.get(player.getUUID());
        int ticks = active == null ? 0 : ForcePhysicsRules.boundedChannelTicks(
                (int) Math.max(0L, level.getGameTime() - active.startedAt()));
        sendSnapshot(player, state, level.getGameTime(),
                active == null ? -1 : active.slot(), active == null ? 0 : active.channeled() ? 2 : 1,
                ticks);
    }

    private static boolean fail(ServerPlayer player, String reason) {
        if (player.level() instanceof ServerLevel level) {
            FAILURE_NOTICES.put(player.getUUID(),
                    new FailureNotice(reason, level.getGameTime() + FAILURE_DISPLAY_TICKS));
            while (FAILURE_NOTICES.size() > MAX_FAILURE_NOTICES) {
                FAILURE_NOTICES.remove(FAILURE_NOTICES.keySet().iterator().next());
            }
            syncSnapshot(player, ForceSavedData.get(level));
        }
        player.sendOverlayMessage(Component.translatable(
                "message.galacticwars.force.failed", reason));
        return false;
    }

    private static int targetValidityMask(ServerPlayer player, List<String> abilityIds) {
        if (!(player.level() instanceof ServerLevel level)) return 0;
        int mask = 0;
        LaunchContentDefinitions content = LaunchContentCatalog.data();
        for (int slot = 0; slot < Math.min(3, abilityIds.size()); slot++) {
            LaunchContentDefinitions.ForceAbilityDefinition ability =
                    content.forceAbilities().get(abilityIds.get(slot));
            if (ability != null && targetValid(level, player, ability)) mask |= 1 << slot;
        }
        return mask;
    }

    private static boolean targetValid(
            ServerLevel level,
            ServerPlayer player,
            LaunchContentDefinitions.ForceAbilityDefinition ability
    ) {
        if (ability.executor().equals("guard") || ability.executor().equals("saber_frenzy")) {
            return LightsaberDeflectionService.holdsLightsaber(player);
        }
        if (ability.target().equals("self") || ability.executor().equals("guardian_stand")) {
            return true;
        }
        List<Entity> targets = ForceTargetingService.targets(
                level, player, ability.target(), ability.range());
        if (ability.target().equals("held_object")) {
            return ForceBlockTelekinesisService.hasGrip(player)
                    || !targets.isEmpty()
                    || ForceBlockTelekinesisService.hasMovableTarget(player, ability.range());
        }
        if (ability.executor().equals("healing_meditation")
                || ability.executor().equals("life_weave")) {
            if (player.getHealth() < player.getMaxHealth()) return true;
            boolean drainEnemies = ability.executor().equals("life_weave");
            return targets.stream().anyMatch(target -> target instanceof net.minecraft.world.entity.LivingEntity living
                    && (drainEnemies && !allied(player, living)
                    || allied(player, living) && living.getHealth() < living.getMaxHealth()));
        }
        if (ability.executor().equals("spirit_ward")) return true;
        return targets.stream().anyMatch(target -> !(target instanceof GalacticRecruitEntity recruit)
                || !allied(player, recruit));
    }

    private static boolean allied(ServerPlayer player, net.minecraft.world.entity.LivingEntity target) {
        if (target == player) return true;
        if (!(target instanceof GalacticRecruitEntity recruit)) return false;
        FactionRelation relation = recruit.factionRelationTo(player);
        return recruit.isOwnedBy(player) || relation == FactionRelation.SAME
                || relation == FactionRelation.ALLY;
    }

    private record ActiveCast(
            UUID activationId,
            int slot,
            String abilityId,
            long startedAt,
            int maximumTicks,
            boolean channeled,
            String dimensionId
    ) {
    }

    private record ResolvedCast(
            ProgressionState progression,
            ForceRuntimeState forceState,
            String abilityId,
            LaunchContentDefinitions.ForceAbilityDefinition ability
    ) {
    }

    private record FailureNotice(String reason, long expiresAt) {
    }
}
