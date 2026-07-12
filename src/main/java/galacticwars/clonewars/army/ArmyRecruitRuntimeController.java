package galacticwars.clonewars.army;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import galacticwars.clonewars.combat.BlasterHeatPolicy;
import galacticwars.clonewars.combat.BlasterItem;
import galacticwars.clonewars.combat.FactionRangedWeaponService;
import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.registry.ModItems;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.recruitment.RecruitDuty;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * The only runtime owner of grouped-soldier navigation and combat targets.
 * Persistence remains authoritative; this controller only realizes the current
 * SavedData order for a loaded recruit.
 */
public final class ArmyRecruitRuntimeController {
    private static final int BEHAVIOR_INTERVAL = 10;
    private static final int TARGET_INTERVAL = 20;

    private UUID selectedTargetId;
    private int ticksUntilNextAttack;
    private BlasterHeatPolicy.BlasterHeatState blasterHeat = BlasterHeatPolicy.BlasterHeatState.ready();
    private boolean ownsRuntimeControl;

    public void tick(GalacticRecruitEntity recruit, ServerLevel level) {
        ticksUntilNextAttack = Math.max(0, ticksUntilNextAttack - 1);
        blasterHeat = BlasterHeatPolicy.tick(blasterHeat);
        if (!recruit.isTame()
                || recruit.getRecruitDuty() == RecruitDuty.WORKER) {
            releaseRuntimeControl(recruit);
            return;
        }
        if (Math.floorMod(recruit.getUUID().hashCode(), BEHAVIOR_INTERVAL)
                != Math.floorMod(recruit.tickCount, BEHAVIOR_INTERVAL)) {
            return;
        }
        UUID groupId = recruit.getArmyGroupId();
        if (groupId == null) {
            releaseRuntimeControl(recruit);
            return;
        }
        KingdomSavedData data = KingdomSavedData.get(level);
        ArmyGroupRecord group = data.armyGroup(groupId).orElse(null);
        if (group == null || group.simulation().lifecycleState() != ArmyGroupLifecycleState.LIVE) {
            releaseRuntimeControl(recruit);
            return;
        }
        group = advancePatrolWaypoint(recruit, level, data, group);
        ownsRuntimeControl = true;

        LivingEntity owner = living(level.getEntity(group.ownerId()));
        ArmyPosition self = position(recruit);
        ArmyPosition ownerPosition = owner == null ? self : position(owner);
        int followRange = Math.max(4, (int) Math.round(recruit.getAttributeValue(Attributes.FOLLOW_RANGE)));

        if (Math.floorMod(recruit.getUUID().hashCode(), TARGET_INTERVAL)
                == Math.floorMod(recruit.tickCount, TARGET_INTERVAL)) {
            selectedTargetId = selectTarget(recruit, level, group, self, followRange);
        }
        LivingEntity selectedTarget = living(selectedTargetId == null ? null : level.getEntity(selectedTargetId));
        if (!validTarget(recruit, group, selectedTarget)) {
            selectedTarget = null;
            selectedTargetId = null;
        }

        MemberOrder memberOrder = memberOrder(group, recruit, ownerPosition);
        ArmyCommand memberCommand = memberOrder.command();
        RecruitState state = new RecruitState(recruit.getUUID(), group.ownerId(), group.id(), memberCommand);
        UUID visibleThreat = selectedTarget == null ? null : selectedTarget.getUUID();
        boolean commandTargetAlive = memberCommand.targetEntityId() != null
                && living(level.getEntity(memberCommand.targetEntityId())) != null;
        ArmyBehaviorDecision behavior = ArmyBehaviorPlanner.plan(state, new ArmyBehaviorContext(
                self, memberOrder.behaviorAnchor(), visibleThreat, commandTargetAlive, followRange));

        if (selectedTarget != null && group.order().type() != ArmyCommandType.CLEAR_TARGET) {
            behavior = ArmyBehaviorDecision.attack(selectedTarget.getUUID(), "planner_selected_target");
        }
        ArmyPosition fallback = commanderOrAnchor(level, group);
        ArmyTacticalDecision tactical = ArmyTacticalPlanner.plan(behavior, recruit.getRecruitVitals(), fallback);
        apply(recruit, level, group, tactical);
    }

    private static ArmyGroupRecord advancePatrolWaypoint(
            GalacticRecruitEntity recruit,
            ServerLevel level,
            KingdomSavedData data,
            ArmyGroupRecord group
    ) {
        if (group.order().type() != ArmyCommandType.PATROL_ROUTE
                || group.commanderId().filter(recruit.getUUID()::equals).isEmpty()
                || group.patrolRoute().size() < 2
                || group.patrolRoute().stream().anyMatch(
                        waypoint -> !waypoint.dimensionId().equals(level.dimension().identifier().toString()))) {
            return group;
        }
        ArmyLocation activeWaypoint = group.order().targetPosition().orElse(null);
        int activeIndex = group.patrolRoute().indexOf(activeWaypoint);
        if (activeIndex < 0) {
            return replacePatrolTarget(data, group, group.patrolRoute().getFirst());
        }
        ArmyPatrolRoute route = new ArmyPatrolRoute(
                group.patrolRoute().stream().map(ArmyLocation::blockPosition).toList(),
                ArmyPatrolMode.LOOP,
                2,
                0);
        ArmyPatrolDecision decision = ArmyPatrolPlanner.advance(
                route,
                new ArmyPatrolState(activeIndex, 1, 0),
                position(recruit));
        if (decision.nextState().waypointIndex() == activeIndex) {
            return group;
        }
        return replacePatrolTarget(
                data,
                group,
                group.patrolRoute().get(decision.nextState().waypointIndex()));
    }

    private static ArmyGroupRecord replacePatrolTarget(
            KingdomSavedData data,
            ArmyGroupRecord group,
            ArmyLocation waypoint
    ) {
        ArmyGroupOrder nextOrder = new ArmyGroupOrder(
                ArmyCommandType.PATROL_ROUTE,
                Optional.of(waypoint),
                Optional.empty(),
                group.order().formation(),
                group.order().spacing());
        ArmyGroupRecord updated = group.withOrder(nextOrder);
        if (data.replaceArmyGroup(updated, group.simulation().revision())) {
            return updated;
        }
        return data.armyGroup(group.id()).orElse(group);
    }

    private UUID selectTarget(
            GalacticRecruitEntity recruit,
            ServerLevel level,
            ArmyGroupRecord group,
            ArmyPosition origin,
            int maxRange
    ) {
        ArmyCommandType command = group.order().type();
        LivingEntity explicit = group.order().targetEntityId()
                .map(level::getEntity)
                .map(ArmyRecruitRuntimeController::living)
                .orElse(null);
        if (validTarget(recruit, group, explicit)) {
            return explicit.getUUID();
        }

        if (command == ArmyCommandType.PROTECT_OWNER) {
            LivingEntity owner = living(level.getEntity(group.ownerId()));
            LivingEntity ownerAttacker = owner == null ? null : owner.getLastHurtByMob();
            if (validTarget(recruit, group, ownerAttacker, true)
                    && recruit.distanceToSqr(ownerAttacker) <= (double) maxRange * maxRange) {
                return ownerAttacker.getUUID();
            }
        }

        FactionId ownFaction = FactionId.of(recruit.getRecruitFactionId());
        List<ArmyTargetCandidate> candidates = new ArrayList<>();
        AABB scanBox = recruit.getBoundingBox().inflate(maxRange);
        for (LivingEntity candidate : level.getEntitiesOfClass(
                LivingEntity.class, scanBox, entity -> entity != recruit && entity.isAlive())) {
            ArmyTargetCandidate mapped = candidate(recruit, group, candidate);
            if (mapped != null) {
                candidates.add(mapped);
            }
        }
        ArmyEngagementStance stance = switch (command) {
            case ATTACK_TARGET -> ArmyEngagementStance.AGGRESSIVE;
            case PROTECT_OWNER -> ArmyEngagementStance.DEFENSIVE;
            default -> ArmyEngagementStance.PASSIVE;
        };
        ArmyEngagementDecision engagement = ArmyEngagementPlanner.plan(
                        stance,
                        ownFaction,
                        origin,
                        candidates,
                        GameplayDataManager.snapshot().factions(),
                        maxRange);
        return Optional.ofNullable(engagement.targetSelection())
                .map(ArmyTargetSelection::targetId)
                .orElseGet(() -> retaliatoryTarget(recruit, group));
    }

    private ArmyTargetCandidate candidate(
            GalacticRecruitEntity recruit,
            ArmyGroupRecord group,
            LivingEntity candidate
    ) {
        if (candidate instanceof GalacticRecruitEntity other) {
            if (other.getRecruitDuty() == RecruitDuty.WORKER
                    || group.contains(other.getUUID())
                    || sameOwner(recruit, other)
                    || recruit.factionRelationTo(other)
                            != galacticwars.clonewars.faction.FactionRelation.ENEMY) {
                return null;
            }
            int threat = (int) Math.round(Math.min(100.0D,
                    other.getAttributeValue(Attributes.ATTACK_DAMAGE) * 5.0D));
            LivingEntity target = other.getTarget();
            return new ArmyTargetCandidate(
                    other.getUUID(),
                    FactionId.of(other.getRecruitFactionId()),
                    position(other),
                    target != null && target.getUUID().equals(group.ownerId()),
                    target != null && group.contains(target.getUUID()),
                    Math.max(0, threat),
                    Optional.of(recruit.factionRelationTo(other)));
        }
        return null;
    }

    private static UUID retaliatoryTarget(GalacticRecruitEntity recruit, ArmyGroupRecord group) {
        LivingEntity attacker = recruit.getLastHurtByMob();
        return validTarget(recruit, group, attacker) ? attacker.getUUID() : null;
    }

    private static MemberOrder memberOrder(
            ArmyGroupRecord group,
            GalacticRecruitEntity recruit,
            ArmyPosition ownerPosition
    ) {
        if (group.commanderId().filter(recruit.getUUID()::equals).isPresent()) {
            return new MemberOrder(group.order().toCommand(group.ownerId(), group.id()), ownerPosition);
        }
        List<ArmyGroupOrderAssignment> assignments = ArmyGroupOrderPlanner.plan(
                group.plannerState(), group.order().formation(), group.order().spacing(), ownerPosition);
        Optional<ArmyGroupOrderAssignment> assigned = assignments.stream()
                .filter(assignment -> assignment.recruitId().equals(recruit.getUUID()))
                .findFirst();
        if (assigned.isPresent()) {
            ArmyGroupOrderAssignment assignment = assigned.orElseThrow();
            ArmyPosition behaviorAnchor = assignment.assignedPosition() == null
                    ? ownerPosition
                    : assignment.assignedPosition();
            return new MemberOrder(assignment.command(), behaviorAnchor);
        }
        return new MemberOrder(group.order().toCommand(group.ownerId(), group.id()), ownerPosition);
    }

    private void apply(
            GalacticRecruitEntity recruit,
            ServerLevel level,
            ArmyGroupRecord group,
            ArmyTacticalDecision tactical
    ) {
        if (tactical.intent() != ArmyTacticalIntent.EXECUTE_ORDER) {
            recruit.setTarget(null);
            recruit.setAggressive(false);
            move(recruit, tactical.tacticalTarget(), tactical.intent() == ArmyTacticalIntent.HOLD_POSITION ? 0.8D : 1.05D);
            return;
        }
        ArmyBehaviorDecision behavior = tactical.behaviorDecision();
        switch (behavior.intent()) {
            case ATTACK_TARGET -> {
                LivingEntity target = living(level.getEntity(behavior.attackTargetId()));
                if (!validTarget(recruit, group, target)) {
                    recruit.setTarget(null);
                    recruit.setAggressive(false);
                    recruit.getNavigation().stop();
                    break;
                }
                recruit.setTarget(target);
                recruit.setAggressive(true);
                recruit.getLookControl().setLookAt(target, 30.0F, 30.0F);
                ItemStack weapon = recruit.getMainHandItem();
                if (FactionRangedWeaponService.supportsRecruitRangedCombat(weapon)
                        && recruit.distanceToSqr(target) <= recruit.getAttributeValue(Attributes.FOLLOW_RANGE)
                                * recruit.getAttributeValue(Attributes.FOLLOW_RANGE)
                        && recruit.getSensing().hasLineOfSight(target)) {
                    recruit.getNavigation().stop();
                    if (weapon.getItem() instanceof BlasterItem blaster
                            && BlasterHeatPolicy.canFire(blasterHeat)) {
                        blaster.fireAt(level, recruit, target, weapon);
                        blasterHeat = BlasterHeatPolicy.afterShot(blasterHeat);
                    } else if (weapon.is(ModItems.NIGHTSISTER_BOW.get())
                            && ticksUntilNextAttack == 0) {
                        FactionRangedWeaponService.fireNightsisterBow(level, recruit, target, weapon);
                        ticksUntilNextAttack = 24;
                    }
                } else if (recruit.isWithinMeleeAttackRange(target)
                        && recruit.getSensing().hasLineOfSight(target)) {
                    recruit.getNavigation().stop();
                    if (ticksUntilNextAttack == 0) {
                        recruit.swing(InteractionHand.MAIN_HAND);
                        recruit.doHurtTarget(level, target);
                        ticksUntilNextAttack = 20;
                    }
                } else {
                    move(recruit, position(target), 1.05D);
                }
            }
            case MOVE_TO_POSITION, FOLLOW_OWNER, PROTECT_OWNER -> {
                recruit.setTarget(null);
                recruit.setAggressive(false);
                move(recruit, behavior.moveTarget(), 1.0D);
            }
            case HOLD_POSITION -> {
                recruit.setTarget(null);
                recruit.setAggressive(false);
                if (distanceSquared(position(recruit), behavior.moveTarget()) > 4L) {
                    move(recruit, behavior.moveTarget(), 0.9D);
                } else {
                    recruit.getNavigation().stop();
                }
            }
            case IDLE -> {
                recruit.setTarget(null);
                recruit.setAggressive(false);
                recruit.getNavigation().stop();
            }
        }
    }

    private static void move(GalacticRecruitEntity recruit, ArmyPosition target, double speed) {
        if (target != null && recruit.level().isLoaded(new net.minecraft.core.BlockPos(target.x(), target.y(), target.z()))) {
            recruit.getNavigation().moveTo(target.x() + 0.5D, target.y(), target.z() + 0.5D, speed);
        }
    }

    private static ArmyPosition commanderOrAnchor(ServerLevel level, ArmyGroupRecord group) {
        LivingEntity commander = group.commanderId().map(level::getEntity)
                .map(ArmyRecruitRuntimeController::living)
                .orElse(null);
        return commander == null ? group.simulation().anchor().blockPosition() : position(commander);
    }

    private static boolean validTarget(
            GalacticRecruitEntity recruit,
            ArmyGroupRecord group,
            LivingEntity target
    ) {
        return validTarget(recruit, group, target, false);
    }

    private static boolean validTarget(
            GalacticRecruitEntity recruit,
            ArmyGroupRecord group,
            LivingEntity target,
            boolean defendingOwner
    ) {
        if (target == null || !target.isAlive() || target == recruit || group.contains(target.getUUID())) {
            return false;
        }
        LivingEntity owner = recruit.getOwner();
        if (owner != null && owner.getUUID().equals(target.getUUID())) {
            return false;
        }
        if (target instanceof Player player) {
            return recruit.canAttackFactionPlayer(player);
        }
        if (target instanceof GalacticRecruitEntity other) {
            if (other.getRecruitDuty() == RecruitDuty.WORKER || sameOwner(recruit, other)) {
                return false;
            }
            return recruit.factionRelationTo(other)
                    == galacticwars.clonewars.faction.FactionRelation.ENEMY;
        }
        if (target instanceof Monster) {
            boolean explicitOrderTarget = group.order().type() == ArmyCommandType.ATTACK_TARGET
                    && group.order().targetEntityId().filter(target.getUUID()::equals).isPresent();
            return defendingOwner || ArmyAttackTargetPolicy.canAttackMonster(
                    explicitOrderTarget,
                    target == recruit.getLastHurtByMob());
        }
        return target == recruit.getLastHurtByMob();
    }

    private record MemberOrder(ArmyCommand command, ArmyPosition behaviorAnchor) {
    }

    private static boolean sameOwner(GalacticRecruitEntity first, GalacticRecruitEntity second) {
        LivingEntity firstOwner = first.getOwner();
        LivingEntity secondOwner = second.getOwner();
        return firstOwner != null && secondOwner != null && firstOwner.getUUID().equals(secondOwner.getUUID());
    }

    private static long distanceSquared(ArmyPosition first, ArmyPosition second) {
        long x = (long) first.x() - second.x();
        long y = (long) first.y() - second.y();
        long z = (long) first.z() - second.z();
        return x * x + y * y + z * z;
    }

    private static ArmyPosition position(Entity entity) {
        return new ArmyPosition(entity.blockPosition().getX(), entity.blockPosition().getY(), entity.blockPosition().getZ());
    }

    private static LivingEntity living(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private void releaseRuntimeControl(GalacticRecruitEntity recruit) {
        if (!ownsRuntimeControl) {
            return;
        }
        recruit.setTarget(null);
        recruit.setAggressive(false);
        recruit.getNavigation().stop();
        selectedTargetId = null;
        ticksUntilNextAttack = 0;
        blasterHeat = BlasterHeatPolicy.BlasterHeatState.ready();
        ownsRuntimeControl = false;
    }
}
