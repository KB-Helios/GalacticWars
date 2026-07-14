package galacticwars.clonewars.force;

import galacticwars.clonewars.Config;
import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.faction.FactionRelation;
import galacticwars.clonewars.progression.ForceAbilityRuntimeService;
import galacticwars.clonewars.progression.ForceSavedData;
import galacticwars.clonewars.progression.LaunchContentCatalog;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.progression.ProgressionEvent;
import galacticwars.clonewars.progression.ProgressionEventType;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import galacticwars.clonewars.network.ForceHudPayload;
import galacticwars.clonewars.network.GalacticNetwork;

public final class ForceWorldEffectService {
    private ForceWorldEffectService() {
    }

    public static boolean activate(ServerPlayer player, UUID activationId, int slot) {
        if (slot < 0 || slot > 2 || !(player.level() instanceof ServerLevel level)) return false;
        var progression = ProgressionSavedData.get(level).state(player.getUUID());
        String faction = path(progression.factionId());
        String abilityId = abilityFor(faction, slot);
        LaunchContentDefinitions.ForceAbilityDefinition ability =
                LaunchContentCatalog.data().forceAbilities().get(abilityId);
        if (ability == null) return fail(player, "unknown_force_ability");
        LivingEntity target = requiresTarget(ability.effect()) ? target(player, ability.range()) : null;
        if (requiresTarget(ability.effect()) && target == null) return fail(player, "force_target_required");
        boolean targetsPlayer = target instanceof Player;
        ForceAbilityRuntimeService.ActivationDecision decision = ForceSavedData.get(level).activate(
                progression, activationId, abilityId, level.getGameTime(), targetsPlayer,
                Config.ALLOW_FORCE_PVP.getAsBoolean());
        sendSnapshot(player, decision.state(), faction, level.getGameTime());
        if (!decision.accepted()) return fail(player, decision.reason());
        if (decision.reason().equals("duplicate_activation")) return true;
        UUID progressionId = UUID.nameUUIDFromBytes(("force:first-use:" + player.getUUID()
                + ":" + abilityId).getBytes(StandardCharsets.UTF_8));
        ProgressionSavedData.get(level).apply(new ProgressionEvent(
                progressionId, player.getUUID(), ProgressionEventType.FORCE_ABILITY_UNLOCKED,
                abilityId, 1));
        apply(player, target, ability);
        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 0.8F, ability.path().equals("dark") ? 0.7F : 1.2F);
        player.sendOverlayMessage(Component.translatable(
                "message.galacticwars.force.activated", abilityId, decision.state().energy()));
        return true;
    }

    private static void sendSnapshot(
            ServerPlayer player, galacticwars.clonewars.progression.ForceRuntimeState state,
            String faction, long gameTime
    ) {
        if (!GalacticNetwork.CHANNEL.isActive(player.connection.getConnection())) {
            return;
        }
        String[] abilities = faction.equals("republic")
                ? new String[]{"light_push", "light_pull", "light_leap"}
                : faction.equals("nightsister")
                ? new String[]{"dark_push", "dark_dash", "dark_choke"}
                : new String[]{"", "", ""};
        int[] cooldowns = new int[3];
        for (int index = 0; index < abilities.length; index++) {
            cooldowns[index] = (int) Math.max(0L,
                    Math.min(Integer.MAX_VALUE, state.cooldownEnds().getOrDefault(abilities[index], 0L) - gameTime));
        }
        GalacticNetwork.CHANNEL.sendToPlayer(() -> player,
                new ForceHudPayload(state.energy(), cooldowns[0], cooldowns[1], cooldowns[2]));
    }

    private static void apply(
            ServerPlayer player, LivingEntity target,
            LaunchContentDefinitions.ForceAbilityDefinition ability
    ) {
        Vec3 look = player.getLookAngle().normalize();
        switch (ability.effect()) {
            case "push" -> target.push(look.x * 1.45D, 0.35D, look.z * 1.45D);
            case "pull" -> {
                Vec3 direction = player.position().subtract(target.position()).normalize();
                target.push(direction.x * 1.25D, 0.22D, direction.z * 1.25D);
            }
            case "leap" -> player.push(look.x * 0.9D, 0.85D, look.z * 0.9D);
            case "dash" -> player.push(look.x * 1.6D, Math.max(0.05D, look.y * 0.3D), look.z * 1.6D);
            case "choke" -> {
                target.addEffect(new MobEffectInstance(
                        MobEffects.LEVITATION, ability.durationTicks(), 0), player);
                target.addEffect(new MobEffectInstance(
                        MobEffects.WEAKNESS, ability.durationTicks(), 1), player);
            }
            default -> throw new IllegalStateException("Missing Force effect " + ability.effect());
        }
    }

    private static LivingEntity target(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        return player.level().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0D),
                        entity -> entity != player && entity.isAlive()
                                && entity.distanceToSqr(player) <= range * range
                                && canAffect(player, entity))
                .stream()
                .filter(entity -> {
                    Vec3 direction = entity.getEyePosition().subtract(eye);
                    return direction.lengthSqr() > 0.01D
                            && direction.normalize().dot(look) >= 0.94D;
                })
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
    }

    private static boolean canAffect(ServerPlayer player, LivingEntity target) {
        if (!(target instanceof GalacticRecruitEntity recruit)) {
            return true;
        }
        FactionRelation relation = recruit.factionRelationTo(player);
        return !recruit.isOwnedBy(player)
                && relation != FactionRelation.SAME
                && relation != FactionRelation.ALLY;
    }

    private static boolean fail(ServerPlayer player, String reason) {
        player.sendOverlayMessage(Component.translatable(
                "message.galacticwars.force.failed", reason));
        return false;
    }

    private static boolean requiresTarget(String effect) {
        return effect.equals("push") || effect.equals("pull") || effect.equals("choke");
    }

    private static String abilityFor(String faction, int slot) {
        if (faction.equals("republic")) {
            return new String[]{"light_push", "light_pull", "light_leap"}[slot];
        }
        if (faction.equals("nightsister")) {
            return new String[]{"dark_push", "dark_dash", "dark_choke"}[slot];
        }
        return "";
    }

    private static String path(String id) {
        int separator = id.indexOf(':');
        return separator < 0 ? id : id.substring(separator + 1);
    }
}
