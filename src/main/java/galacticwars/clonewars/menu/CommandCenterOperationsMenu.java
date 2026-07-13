package galacticwars.clonewars.menu;

import galacticwars.clonewars.army.ArmyLocation;
import galacticwars.clonewars.army.ArmyFormation;
import galacticwars.clonewars.army.ArmyGroupOrder;
import galacticwars.clonewars.kingdom.KingdomMemberRole;
import galacticwars.clonewars.kingdom.KingdomRelation;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.kingdom.SettlementRecord;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.registry.ModItems;
import galacticwars.clonewars.registry.ModMenuTypes;
import galacticwars.clonewars.settlement.CommandCenterBlockEntity;
import galacticwars.clonewars.vehicle.VehicleFabricationService;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
public final class CommandCenterOperationsMenu extends AbstractContainerMenu {
    public static final int STORAGE = 0;
    public static final int CLAIM_REWARDS = 1;
    public static final int FABRICATE_FIRST = 10;
    public static final int MERGE_SQUADS = 20;
    public static final int PATROL_SQUAD = 21;
    public static final int CREATE_SQUAD = 22;
    public static final int SPLIT_SQUAD = 23;
    public static final int CONFIGURE_SQUAD = 24;
    public static final int CYCLE_FORMATION = 25;
    public static final int SUPPLY_SQUAD = 26;
    public static final int FOLLOW_SQUAD = 27;
    public static final int REGISTER_OUTPOST = 30;
    public static final int INVITE_NEAREST = 31;
    public static final int ACCEPT_INVITE = 32;
    public static final int REJECT_INVITE = 35;
    public static final int CYCLE_MEMBER_ROLE = 33;
    public static final int REMOVE_MEMBER = 34;
    public static final int PROPOSE_ALLIANCE = 40;
    public static final int ACCEPT_DIPLOMACY = 41;
    public static final int REJECT_DIPLOMACY = 44;
    public static final int DECLARE_HOSTILITY = 42;
    public static final int TOGGLE_EMBARGO = 43;
    private static final List<String> VEHICLES = List.of(
            "barc_speeder", "at_rt", "stap", "aat", "laat_gunship");
    private final BlockPos hallPos;
    private final LinkedHashSet<UUID> processedActionIds = new LinkedHashSet<>();

    public CommandCenterOperationsMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }

    CommandCenterOperationsMenu(int id, Inventory inventory, BlockPos hallPos) {
        super(ModMenuTypes.COMMAND_CENTER_OPERATIONS.get(), id);
        this.hallPos = hallPos.immutable();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        return false;
    }

    public boolean handleReplayAction(ServerPlayer player, UUID replayId, int buttonId) {
        if (!processedActionIds.add(replayId)) return false;
        while (processedActionIds.size() > 64) {
            processedActionIds.remove(processedActionIds.iterator().next());
        }
        return executeAction(player, buttonId);
    }

    private boolean executeAction(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(player.level() instanceof ServerLevel level)
                || !(level.getBlockEntity(hallPos) instanceof CommandCenterBlockEntity hall)
                || !stillValid(player)) return false;
        KingdomSavedData data = KingdomSavedData.get(level);
        boolean success;
        String reason = "accepted";
        if (buttonId == STORAGE) {
            serverPlayer.openMenu(hall);
            return true;
        } else if (buttonId == CLAIM_REWARDS) {
            success = ProgressionSavedData.get(level).claimCreditRewards(serverPlayer) > 0;
        } else if (buttonId >= FABRICATE_FIRST && buttonId < FABRICATE_FIRST + VEHICLES.size()) {
            reason = VehicleFabricationService.fabricate(serverPlayer, hall,
                    VEHICLES.get(buttonId - FABRICATE_FIRST));
            success = reason.equals("accepted");
        } else if (buttonId == MERGE_SQUADS) {
            var groups = data.armyGroupsForKingdom(data.kingdomForPlayer(player.getUUID()).orElseThrow().id());
            success = groups.size() >= 2 && data.mergeArmyGroups(
                    player.getUUID(), groups.get(0).id(), groups.get(1).id());
        } else if (buttonId == PATROL_SQUAD) {
            var group = data.kingdomForPlayer(player.getUUID()).stream()
                    .flatMap(kingdom -> data.armyGroupsForKingdom(kingdom.id()).stream()).findFirst().orElse(null);
            ArmyLocation center = new ArmyLocation(level.dimension().identifier().toString(),
                    player.getX(), player.getY(), player.getZ());
            success = group != null && data.startArmyPatrol(player.getUUID(), group.id(), center, List.of(
                    center, new ArmyLocation(center.dimensionId(), center.x() + 16, center.y(), center.z()),
                    new ArmyLocation(center.dimensionId(), center.x() + 16, center.y(), center.z() + 16),
                    new ArmyLocation(center.dimensionId(), center.x(), center.y(), center.z() + 16)));
        } else if (buttonId >= CREATE_SQUAD && buttonId <= FOLLOW_SQUAD) {
            var kingdom = data.kingdomForPlayer(player.getUUID()).orElse(null);
            var groups = kingdom == null ? List.<galacticwars.clonewars.army.ArmyGroupRecord>of()
                    : data.armyGroupsForKingdom(kingdom.id());
            var group = groups.isEmpty() ? null : groups.getFirst();
            ArmyLocation here = new ArmyLocation(level.dimension().identifier().toString(),
                    player.getX(), player.getY(), player.getZ());
            if (buttonId == CREATE_SQUAD) {
                UUID commander = kingdom == null ? null : kingdom.npcRoster().stream()
                        .filter(npc -> npc.serviceBranch()
                                == galacticwars.clonewars.recruitment.NpcServiceBranch.MILITARY)
                        .map(galacticwars.clonewars.kingdom.KingdomNpcRecord::recruitId)
                        .filter(id -> level.getEntity(id) != null)
                        .filter(id -> data.armyGroupForRecruit(id).isEmpty()).findFirst().orElse(null);
                success = commander != null && data.promoteCommander(player.getUUID(), commander)
                        && data.createOrReclaimArmyGroup(player.getUUID(), commander,
                        ArmyFormation.LINE, here, level.getGameTime()).isPresent();
            } else if (buttonId == SPLIT_SQUAD) {
                UUID commander = group == null || group.memberIds().isEmpty()
                        ? null : group.memberIds().getFirst();
                List<UUID> transferred = group == null ? List.of()
                        : group.memberIds().stream().skip(1)
                        .limit(Math.max(0, group.memberIds().size() / 2)).toList();
                success = commander != null && data.splitArmyGroup(player.getUUID(), group.id(), commander,
                        transferred, "Split Squad", here, level.getGameTime()).isPresent();
            } else if (buttonId == CONFIGURE_SQUAD) {
                Optional<UUID> claim = kingdom == null ? Optional.empty()
                        : kingdom.claims().stream().findFirst().map(claimRecord -> claimRecord.id());
                success = group != null && data.configureArmyGroup(player.getUUID(), group.id(),
                        "Squad at " + player.getBlockX() + "," + player.getBlockZ(), here,
                        group.patrolRoute(), claim);
            } else if (buttonId == CYCLE_FORMATION) {
                ArmyFormation formation = group == null ? ArmyFormation.LINE
                        : nextFormation(group.order().formation());
                success = group != null && data.issueArmyOrder(player.getUUID(), group.id(),
                        group.order().withFormation(formation));
            } else if (buttonId == SUPPLY_SQUAD) {
                success = group != null && hasSupplyCell(hall)
                        && data.changeArmySupply(player.getUUID(), group.id(), 16);
                if (success) consumeSupplyCell(hall);
            } else {
                success = group != null && data.issueArmyOrder(player.getUUID(), group.id(),
                        ArmyGroupOrder.follow(group.order().formation()));
            }
        } else if (buttonId == REGISTER_OUTPOST) {
            var progression = ProgressionSavedData.get(level).state(player.getUUID());
            success = progression.hasSubjectPath(ProgressionEventType.BUILDING_COMPLETED, "forward_base")
                    && data.addOutpost(player.getUUID(), SettlementRecord.create(
                    level.dimension().identifier().toString(), player.getBlockX(),
                    player.getBlockY(), player.getBlockZ())).isPresent();
        } else if (buttonId == INVITE_NEAREST) {
            ServerPlayer target = nearestOtherPlayer(serverPlayer);
            success = target != null && data.inviteMember(player.getUUID(), target.getUUID(),
                    KingdomMemberRole.MEMBER, level.getGameTime() + 12000L).isPresent();
        } else if (buttonId == ACCEPT_INVITE || buttonId == REJECT_INVITE) {
            var invite = data.pendingInvites().stream()
                    .filter(candidate -> candidate.targetPlayerId().equals(player.getUUID())).findFirst().orElse(null);
            String faction = ProgressionSavedData.get(level).state(player.getUUID()).factionId();
            success = invite != null && data.respondToInvite(player.getUUID(), invite.id(),
                    buttonId == ACCEPT_INVITE,
                    faction, level.getGameTime());
        } else if (buttonId == CYCLE_MEMBER_ROLE || buttonId == REMOVE_MEMBER) {
            var kingdom = data.kingdomForPlayer(player.getUUID()).orElse(null);
            var member = kingdom == null ? null : kingdom.members().stream()
                    .filter(candidate -> !candidate.playerId().equals(kingdom.ownerId())
                            && !candidate.playerId().equals(player.getUUID())).findFirst().orElse(null);
            success = member != null && (buttonId == REMOVE_MEMBER
                    ? data.removeMember(player.getUUID(), member.playerId())
                    : data.updateMemberRole(player.getUUID(), member.playerId(), nextRole(member.role())));
        } else {
            var own = data.kingdomForPlayer(player.getUUID()).orElse(null);
            var other = data.kingdoms().stream().filter(kingdom -> own != null && !kingdom.id().equals(own.id()))
                    .findFirst().orElse(null);
            if (buttonId == ACCEPT_DIPLOMACY || buttonId == REJECT_DIPLOMACY) {
                var proposal = data.pendingDiplomacy().stream()
                        .filter(candidate -> own != null && candidate.targetKingdomId().equals(own.id()))
                        .findFirst().orElse(null);
                success = proposal != null && data.respondToDiplomacy(player.getUUID(), proposal.id(),
                        buttonId == ACCEPT_DIPLOMACY,
                        level.getGameTime(), 2400L);
            } else if (buttonId == PROPOSE_ALLIANCE) {
                success = other != null && data.proposeAlliance(player.getUUID(), other.id(),
                        level.getGameTime(), 24000L, 12000L).isPresent();
            } else if (buttonId == DECLARE_HOSTILITY) {
                success = other != null && data.setRelation(player.getUUID(), other.id(),
                        KingdomRelation.ENEMY, level.getGameTime(), 2400L);
            } else if (buttonId == TOGGLE_EMBARGO) {
                success = other != null && data.setEmbargo(player.getUUID(), other.id(), true,
                        level.getGameTime(), 2400L);
            } else return false;
        }
        serverPlayer.sendSystemMessage(Component.translatable(
                success ? "message.galacticwars.operations.accepted" : "message.galacticwars.operations.rejected",
                reason));
        return success;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(hallPos) instanceof CommandCenterBlockEntity hall
                && hall.canOpen(player)
                && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(hallPos)) <= 64.0D;
    }

    private static ServerPlayer nearestOtherPlayer(ServerPlayer player) {
        return ((ServerLevel) player.level()).players().stream().filter(other -> other != player)
                .min(java.util.Comparator.comparingDouble((ServerPlayer other) -> player.distanceToSqr(other)))
                .orElse(null);
    }

    private static KingdomMemberRole nextRole(KingdomMemberRole role) {
        return switch (role) {
            case MEMBER -> KingdomMemberRole.BUILDER;
            case BUILDER -> KingdomMemberRole.QUARTERMASTER;
            case QUARTERMASTER -> KingdomMemberRole.OFFICER;
            case OFFICER, OWNER -> KingdomMemberRole.MEMBER;
        };
    }

    private static ArmyFormation nextFormation(ArmyFormation formation) {
        ArmyFormation[] values = ArmyFormation.values();
        return values[(formation.ordinal() + 1) % values.length];
    }

    private static boolean hasSupplyCell(CommandCenterBlockEntity hall) {
        for (int slot = 0; slot < hall.getContainerSize(); slot++) {
            if (hall.getItem(slot).is(ModItems.ENERGY_CELL.get())) return true;
        }
        return false;
    }

    private static void consumeSupplyCell(CommandCenterBlockEntity hall) {
        for (int slot = 0; slot < hall.getContainerSize(); slot++) {
            if (hall.getItem(slot).is(ModItems.ENERGY_CELL.get())) {
                hall.getItem(slot).shrink(1);
                hall.setChanged();
                return;
            }
        }
    }
}
