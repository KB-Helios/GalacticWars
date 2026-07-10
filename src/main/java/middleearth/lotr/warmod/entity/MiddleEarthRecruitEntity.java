package middleearth.lotr.warmod.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import middleearth.lotr.warmod.entity.ai.RecruitMoveToCommandGoal;
import middleearth.lotr.warmod.entity.ai.RecruitCompanionGoal;
import middleearth.lotr.warmod.entity.ai.RecruitWorkerGoal;
import middleearth.lotr.warmod.kingdom.KingdomRecord;
import middleearth.lotr.warmod.kingdom.KingdomSavedData;
import middleearth.lotr.warmod.kingdom.BuildProject;
import middleearth.lotr.warmod.kingdom.CommanderPolicy;
import middleearth.lotr.warmod.kingdom.RecruitmentCampaign;
import middleearth.lotr.warmod.kingdom.RecruitmentCampaignDecision;
import middleearth.lotr.warmod.kingdom.RecruitmentEligibility;
import middleearth.lotr.warmod.kingdom.RecruitmentService;
import middleearth.lotr.warmod.menu.RecruitCommandMenu;
import middleearth.lotr.warmod.menu.RecruitCommandMenuProvider;
import middleearth.lotr.warmod.recruitment.RecruitmentAction;
import middleearth.lotr.warmod.recruitment.RecruitDuty;
import middleearth.lotr.warmod.recruitment.RecruitmentPaymentService;
import middleearth.lotr.warmod.registry.ModBlocks;
import middleearth.lotr.warmod.registry.ModBlockTags;
import middleearth.lotr.warmod.registry.ModEntityTypes;
import middleearth.lotr.warmod.settlement.BaseBlockPlacement;
import middleearth.lotr.warmod.settlement.KingdomBaseBlueprint;
import middleearth.lotr.warmod.settlement.KingdomBaseBuildAction;
import middleearth.lotr.warmod.settlement.KingdomBaseBuildDecision;
import middleearth.lotr.warmod.settlement.KingdomBaseBuildPlanner;
import middleearth.lotr.warmod.settlement.KingdomSettlementPlanner;
import middleearth.lotr.warmod.settlement.KingdomSettlementState;
import middleearth.lotr.warmod.settlement.KingdomWorkOrder;
import middleearth.lotr.warmod.settlement.KingdomHallBlockEntity;
import middleearth.lotr.warmod.workforce.ResourceInventory;
import middleearth.lotr.warmod.workforce.WorkAreaType;
import middleearth.lotr.warmod.workforce.WorkerLogisticsDecision;
import middleearth.lotr.warmod.workforce.WorkerLogisticsPlanner;
import middleearth.lotr.warmod.workforce.WorkerLogisticsRoute;
import middleearth.lotr.warmod.workforce.WorkerProfession;
import middleearth.lotr.warmod.workforce.WorkerProfessionCatalog;
import middleearth.lotr.warmod.workforce.WorkerProfessionDefinition;
import middleearth.lotr.warmod.workforce.WorkerAssignment;
import middleearth.lotr.warmod.workforce.WorkerContractService;
import middleearth.lotr.warmod.workforce.WorkerPhase;
import middleearth.lotr.warmod.workforce.WorkerResourceAction;
import middleearth.lotr.warmod.workforce.WorkerResourceDecision;
import middleearth.lotr.warmod.workforce.WorkerResourcePlanner;
import middleearth.lotr.warmod.workforce.WorkerTaskDecision;
import middleearth.lotr.warmod.workforce.WorkerTaskPlanner;
import middleearth.lotr.warmod.workforce.WorkerStatus;
import middleearth.lotr.warmod.workforce.WorkerWorksite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class MiddleEarthRecruitEntity extends TamableAnimal implements GeoEntity {
    public static final int HIRE_COST_EMERALDS = 25;
    private static final int DEFAULT_WORK_RADIUS = 8;
    private static final int MIN_WORK_RADIUS = 2;
    private static final int MAX_WORK_RADIUS = 32;
    private static final int WORK_RADIUS_STEP = 2;
    private static final EntityDataAccessor<Integer> DATA_COMMAND =
            SynchedEntityData.defineId(MiddleEarthRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WORKER_PROFESSION =
            SynchedEntityData.defineId(MiddleEarthRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RECRUIT_DUTY =
            SynchedEntityData.defineId(MiddleEarthRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_WORK_TARGET =
            SynchedEntityData.defineId(MiddleEarthRecruitEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_STORAGE_TARGET =
            SynchedEntityData.defineId(MiddleEarthRecruitEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_BASE_TARGET =
            SynchedEntityData.defineId(MiddleEarthRecruitEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> DATA_STORAGE_RESOURCE_COUNT =
            SynchedEntityData.defineId(MiddleEarthRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CARRIED_RESOURCE_COUNT =
            SynchedEntityData.defineId(MiddleEarthRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BASE_PROGRESS =
            SynchedEntityData.defineId(MiddleEarthRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WORK_RADIUS =
            SynchedEntityData.defineId(MiddleEarthRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_RESOURCE_ACTION =
            SynchedEntityData.defineId(MiddleEarthRecruitEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_RESOURCE_ITEM =
            SynchedEntityData.defineId(MiddleEarthRecruitEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_WORKER_PHASE =
            SynchedEntityData.defineId(MiddleEarthRecruitEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_WORKER_REASON =
            SynchedEntityData.defineId(MiddleEarthRecruitEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_ACTIVE_WORK_TARGET =
            SynchedEntityData.defineId(MiddleEarthRecruitEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<String> DATA_SELECTED_BLUEPRINT =
            SynchedEntityData.defineId(MiddleEarthRecruitEntity.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable BlockPos moveTarget;
    private @Nullable BlockPos workTarget;
    private @Nullable BlockPos storageTarget;
    private @Nullable BlockPos baseTarget;
    private int starterBaseCompletedBlocks;
    private String selectedBlueprintId = KingdomBaseBlueprint.starterKeep().id();
    private int workRadius = DEFAULT_WORK_RADIUS;
    private @Nullable UUID kingdomId;
    private @Nullable UUID settlementId;
    private @Nullable UUID armyGroupId;
    private @Nullable UUID workOrderId;
    private NonNullList<ItemStack> workerInventory = NonNullList.withSize(9, ItemStack.EMPTY);
    private WorkerPhase workerPhase = WorkerPhase.ACQUIRE_ORDER;
    private String workerReason = "ready";
    private @Nullable BlockPos activeWorkTarget;
    private int workerCooldownTicks;
    private int workerNavigationFailures;
    private int workerScanCursor;
    private @Nullable BlockPos blacklistedWorkTarget;
    private int blacklistedWorkTargetTicks;
    private long lastCommanderCampaignGameTime;
    private boolean deathResourcesReleased;

    public MiddleEarthRecruitEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.05, true));
        this.goalSelector.addGoal(2, new RecruitMoveToCommandGoal(this, 1.0));
        this.goalSelector.addGoal(3, new RecruitCompanionGoal(this, 1.0));
        this.goalSelector.addGoal(4, new RecruitWorkerGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<Monster>(
                this,
                Monster.class,
                10,
                true,
                false,
                (target, level) -> this.getRecruitCommand() == RecruitmentAction.ATTACK_TARGET));
        this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_COMMAND, RecruitmentAction.FOLLOW_OWNER.ordinal());
        entityData.define(DATA_WORKER_PROFESSION, -1);
        entityData.define(DATA_RECRUIT_DUTY, RecruitDuty.SOLDIER.ordinal());
        entityData.define(DATA_WORK_TARGET, Optional.empty());
        entityData.define(DATA_STORAGE_TARGET, Optional.empty());
        entityData.define(DATA_BASE_TARGET, Optional.empty());
        entityData.define(DATA_STORAGE_RESOURCE_COUNT, 0);
        entityData.define(DATA_CARRIED_RESOURCE_COUNT, 0);
        entityData.define(DATA_BASE_PROGRESS, 0);
        entityData.define(DATA_WORK_RADIUS, DEFAULT_WORK_RADIUS);
        entityData.define(DATA_RESOURCE_ACTION, "");
        entityData.define(DATA_RESOURCE_ITEM, "");
        entityData.define(DATA_WORKER_PHASE, WorkerPhase.ACQUIRE_ORDER.id());
        entityData.define(DATA_WORKER_REASON, "ready");
        entityData.define(DATA_ACTIVE_WORK_TARGET, Optional.empty());
        entityData.define(DATA_SELECTED_BLUEPRINT, KingdomBaseBlueprint.starterKeep().id());
    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkRunIdleController());
        controllers.add(DefaultAnimations.genericAttackAnimation(DefaultAnimations.ATTACK_SWING));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("RecruitCommand", this.getRecruitCommand().name());
        output.putString("RecruitDuty", this.getRecruitDuty().id());
        this.getWorkerProfession().ifPresent(profession -> output.putString("WorkerProfession", profession.id()));
        output.putInt("RecruitDataVersion", 3);
        output.storeNullable("KingdomId", UUIDUtil.CODEC, this.kingdomId);
        output.storeNullable("SettlementId", UUIDUtil.CODEC, this.settlementId);
        output.storeNullable("ArmyGroupId", UUIDUtil.CODEC, this.armyGroupId);
        output.storeNullable("WorkOrderId", UUIDUtil.CODEC, this.workOrderId);
        ValueOutput workerInventoryOutput = output.child("WorkerInventory");
        ContainerHelper.saveAllItems(workerInventoryOutput, this.workerInventory);
        output.putString("WorkerPhase", this.workerPhase.id());
        output.putString("WorkerReason", this.workerReason);
        output.putInt("WorkerCooldown", this.workerCooldownTicks);
        output.putLong("LastCommanderCampaignGameTime", this.lastCommanderCampaignGameTime);
        if (this.activeWorkTarget != null) {
            output.putInt("ActiveWorkTargetX", this.activeWorkTarget.getX());
            output.putInt("ActiveWorkTargetY", this.activeWorkTarget.getY());
            output.putInt("ActiveWorkTargetZ", this.activeWorkTarget.getZ());
        }
        output.putInt("BlacklistedWorkTargetTicks", this.blacklistedWorkTargetTicks);
        if (this.blacklistedWorkTarget != null) {
            output.putInt("BlacklistedWorkTargetX", this.blacklistedWorkTarget.getX());
            output.putInt("BlacklistedWorkTargetY", this.blacklistedWorkTarget.getY());
            output.putInt("BlacklistedWorkTargetZ", this.blacklistedWorkTarget.getZ());
        }
        output.putInt("StarterBaseCompletedBlocks", this.starterBaseCompletedBlocks);
        output.putString("SelectedBlueprint", this.selectedBlueprintId);
        output.putInt("WorkRadius", this.workRadius);
        if (this.workTarget != null) {
            output.putInt("WorkTargetX", this.workTarget.getX());
            output.putInt("WorkTargetY", this.workTarget.getY());
            output.putInt("WorkTargetZ", this.workTarget.getZ());
        }
        if (this.storageTarget != null) {
            output.putInt("StorageTargetX", this.storageTarget.getX());
            output.putInt("StorageTargetY", this.storageTarget.getY());
            output.putInt("StorageTargetZ", this.storageTarget.getZ());
        }
        if (this.baseTarget != null) {
            output.putInt("BaseTargetX", this.baseTarget.getX());
            output.putInt("BaseTargetY", this.baseTarget.getY());
            output.putInt("BaseTargetZ", this.baseTarget.getZ());
        }
        if (this.moveTarget != null) {
            output.putInt("MoveTargetX", this.moveTarget.getX());
            output.putInt("MoveTargetY", this.moveTarget.getY());
            output.putInt("MoveTargetZ", this.moveTarget.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setRecruitCommand(parseCommand(input.getStringOr("RecruitCommand", RecruitmentAction.FOLLOW_OWNER.name())));
        this.setRecruitDuty(RecruitDuty.byId(input.getStringOr("RecruitDuty", RecruitDuty.SOLDIER.id())));
        WorkerProfession.byId(input.getStringOr("WorkerProfession", ""))
                .ifPresentOrElse(this::setWorkerProfession, () -> this.entityData.set(DATA_WORKER_PROFESSION, -1));
        int dataVersion = input.getIntOr("RecruitDataVersion", 0);
        if (dataVersion < 3 && (!input.getStringOr("WorkerCarriedResources", "").isBlank()
                || !input.getStringOr("WorkerStorageResources", "").isBlank())) {
            KingdomWarsMiddleEarth.LOGGER.warn(
                    "Reset legacy synthetic worker resource counters for recruit {} during schema migration",
                    this.getUUID());
        }
        this.starterBaseCompletedBlocks = dataVersion >= 2
                ? Math.max(0, input.getIntOr("StarterBaseCompletedBlocks", 0))
                : 0;
        this.selectedBlueprintId = KingdomBaseBlueprint.byId(input.getStringOr(
                        "SelectedBlueprint",
                        KingdomBaseBlueprint.starterKeep().id()))
                .orElseGet(KingdomBaseBlueprint::starterKeep)
                .id();
        this.entityData.set(DATA_SELECTED_BLUEPRINT, this.selectedBlueprintId);
        this.kingdomId = input.read("KingdomId", UUIDUtil.CODEC).orElse(null);
        this.settlementId = input.read("SettlementId", UUIDUtil.CODEC).orElse(null);
        this.armyGroupId = input.read("ArmyGroupId", UUIDUtil.CODEC).orElse(null);
        this.workOrderId = input.read("WorkOrderId", UUIDUtil.CODEC).orElse(null);
        this.workerInventory = NonNullList.withSize(9, ItemStack.EMPTY);
        input.child("WorkerInventory").ifPresent(child -> ContainerHelper.loadAllItems(child, this.workerInventory));
        this.workerPhase = WorkerPhase.byId(input.getStringOr("WorkerPhase", WorkerPhase.ACQUIRE_ORDER.id()));
        this.workerReason = input.getStringOr("WorkerReason", "ready");
        this.workerCooldownTicks = Math.max(0, input.getIntOr("WorkerCooldown", 0));
        this.lastCommanderCampaignGameTime = Math.max(0L, input.getLongOr("LastCommanderCampaignGameTime", 0L));
        if (input.getInt("ActiveWorkTargetX").isPresent()
                && input.getInt("ActiveWorkTargetY").isPresent()
                && input.getInt("ActiveWorkTargetZ").isPresent()) {
            this.activeWorkTarget = new BlockPos(
                    input.getIntOr("ActiveWorkTargetX", this.blockPosition().getX()),
                    input.getIntOr("ActiveWorkTargetY", this.blockPosition().getY()),
                    input.getIntOr("ActiveWorkTargetZ", this.blockPosition().getZ()));
        }
        this.blacklistedWorkTargetTicks = Math.max(0, input.getIntOr("BlacklistedWorkTargetTicks", 0));
        if (this.blacklistedWorkTargetTicks > 0
                && input.getInt("BlacklistedWorkTargetX").isPresent()
                && input.getInt("BlacklistedWorkTargetY").isPresent()
                && input.getInt("BlacklistedWorkTargetZ").isPresent()) {
            this.blacklistedWorkTarget = new BlockPos(
                    input.getIntOr("BlacklistedWorkTargetX", this.blockPosition().getX()),
                    input.getIntOr("BlacklistedWorkTargetY", this.blockPosition().getY()),
                    input.getIntOr("BlacklistedWorkTargetZ", this.blockPosition().getZ()));
        }
        this.setWorkRadius(input.getIntOr("WorkRadius", DEFAULT_WORK_RADIUS));
        if (input.getInt("WorkTargetX").isPresent()
                && input.getInt("WorkTargetY").isPresent()
                && input.getInt("WorkTargetZ").isPresent()) {
            this.setWorkTarget(new BlockPos(
                    input.getIntOr("WorkTargetX", this.blockPosition().getX()),
                    input.getIntOr("WorkTargetY", this.blockPosition().getY()),
                    input.getIntOr("WorkTargetZ", this.blockPosition().getZ())));
        }
        if (input.getInt("StorageTargetX").isPresent()
                && input.getInt("StorageTargetY").isPresent()
                && input.getInt("StorageTargetZ").isPresent()) {
            this.setStorageTarget(new BlockPos(
                    input.getIntOr("StorageTargetX", this.blockPosition().getX()),
                    input.getIntOr("StorageTargetY", this.blockPosition().getY()),
                    input.getIntOr("StorageTargetZ", this.blockPosition().getZ())));
        }
        if (input.getInt("BaseTargetX").isPresent()
                && input.getInt("BaseTargetY").isPresent()
                && input.getInt("BaseTargetZ").isPresent()) {
            this.setBaseTarget(new BlockPos(
                    input.getIntOr("BaseTargetX", this.blockPosition().getX()),
                    input.getIntOr("BaseTargetY", this.blockPosition().getY()),
                    input.getIntOr("BaseTargetZ", this.blockPosition().getZ())));
        }
        if (input.getInt("MoveTargetX").isPresent()
                && input.getInt("MoveTargetY").isPresent()
                && input.getInt("MoveTargetZ").isPresent()) {
            this.moveTarget = new BlockPos(
                    input.getIntOr("MoveTargetX", this.blockPosition().getX()),
                    input.getIntOr("MoveTargetY", this.blockPosition().getY()),
                    input.getIntOr("MoveTargetZ", this.blockPosition().getZ()));
        }
        this.syncRecruitStatusState();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount % 20 == 0) {
            this.migrateLegacyKingdomLink();
            this.tickCommanderCampaign();
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!this.deathResourcesReleased && this.level() instanceof ServerLevel serverLevel) {
            this.deathResourcesReleased = true;
            List<ItemStack> carriedItems = this.workerInventory.stream().map(ItemStack::copy).toList();
            this.workerInventory = NonNullList.withSize(9, ItemStack.EMPTY);
            RecruitLifecycleService.dropCarriedItems(serverLevel, this, carriedItems);
            EntityReference<LivingEntity> owner = this.getOwnerReference();
            if (owner != null) {
                UUID ownerId = owner.getUUID();
                Optional<KingdomRecord> kingdom = KingdomSavedData.get(serverLevel).kingdomForOwner(ownerId);
                boolean commander = this.getRecruitDuty() == RecruitDuty.COMMANDER;
                RecruitLifecycleService.releaseSettlementState(
                        serverLevel, ownerId, this.getUUID(), commander);
                if (commander) {
                    kingdom.flatMap(record -> this.findKingdomHall(serverLevel, record))
                            .ifPresent(hall -> hall.settlePendingCampaignRefunds(serverLevel));
                }
            }
        }
        super.die(damageSource);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new RecruitCommandMenuProvider(this));
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    public RecruitmentAction getRecruitCommand() {
        int command = this.entityData.get(DATA_COMMAND);
        RecruitmentAction[] values = RecruitmentAction.values();
        if (command < 0 || command >= values.length) {
            return RecruitmentAction.FOLLOW_OWNER;
        }
        return values[command];
    }

    public RecruitDuty getRecruitDuty() {
        int duty = this.entityData.get(DATA_RECRUIT_DUTY);
        RecruitDuty[] values = RecruitDuty.values();
        return duty < 0 || duty >= values.length ? RecruitDuty.SOLDIER : values[duty];
    }

    public @Nullable UUID getKingdomId() {
        return kingdomId;
    }

    public @Nullable UUID getSettlementId() {
        return settlementId;
    }

    public Optional<WorkerAssignment> getWorkerAssignment() {
        if (this.workTarget == null || this.getWorkerProfession().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new WorkerAssignment(
                this.getWorkerProfession().orElseThrow(),
                this.level().dimension().identifier().toString(),
                this.workTarget.getX(),
                this.workTarget.getY(),
                this.workTarget.getZ(),
                this.workRadius,
                Optional.ofNullable(this.workOrderId)));
    }

    public WorkerStatus getWorkerStatus() {
        Optional<WorkerStatus.Target> target = Optional.ofNullable(this.activeWorkTarget)
                .map(pos -> new WorkerStatus.Target(
                        this.level().dimension().identifier().toString(),
                        pos.getX(),
                        pos.getY(),
                        pos.getZ()));
        return new WorkerStatus(this.workerPhase, this.workerReason, target);
    }

    public @Nullable BlockPos getMoveTarget() {
        return this.moveTarget;
    }

    public @Nullable BlockPos getWorkTarget() {
        return this.workTarget;
    }

    public @Nullable BlockPos getStorageTarget() {
        return this.storageTarget;
    }

    public @Nullable BlockPos getBaseTarget() {
        return this.baseTarget;
    }

    public List<Component> recruitStatusLines() {
        ArrayList<Component> lines = new ArrayList<>();
        BlockPos displayedWorkTarget = this.displayedTarget(DATA_WORK_TARGET, this.workTarget);
        BlockPos displayedStorageTarget = this.displayedTarget(DATA_STORAGE_TARGET, this.storageTarget);
        BlockPos displayedBaseTarget = this.displayedTarget(DATA_BASE_TARGET, this.baseTarget);
        int displayedStorageCount = this.level().isClientSide()
                ? this.entityData.get(DATA_STORAGE_RESOURCE_COUNT)
                : this.storageItemCount();
        int displayedBaseProgress = this.level().isClientSide()
                ? this.entityData.get(DATA_BASE_PROGRESS)
                : this.starterBaseCompletedBlocks;
        int displayedWorkRadius = this.level().isClientSide()
                ? this.entityData.get(DATA_WORK_RADIUS)
                : this.workRadius;
        lines.add(Component.translatable(
                "screen.kingdomwarsmiddleearth.recruit.status.command",
                Component.literal(this.getRecruitCommand().name().toLowerCase())));
        lines.add(Component.translatable(
                "screen.kingdomwarsmiddleearth.recruit.status.duty",
                Component.literal(this.getRecruitDuty().id())));
        lines.add(Component.translatable(
                "screen.kingdomwarsmiddleearth.recruit.status.profession",
                this.getWorkerProfession()
                        .map(profession -> Component.translatable(profession.translationKey()))
                        .orElseGet(() -> Component.translatable("screen.kingdomwarsmiddleearth.recruit.status.none"))));
        this.displayedResourceStatus().ifPresent(resourceStatus -> lines.add(Component.translatable(
                "screen.kingdomwarsmiddleearth.recruit.status.resource_action",
                Component.translatable("screen.kingdomwarsmiddleearth.recruit.workaction."
                        + resourceStatus.action()),
                resourceStatus.itemId().isBlank() ? Component.literal("-") : Component.literal(resourceStatus.itemId()))));
        lines.add(Component.translatable(
                "screen.kingdomwarsmiddleearth.recruit.status.worksite",
                targetLabel(displayedWorkTarget)));
        lines.add(Component.translatable(
                "screen.kingdomwarsmiddleearth.recruit.status.work_radius",
                displayedWorkRadius));
        lines.add(Component.translatable(
                "screen.kingdomwarsmiddleearth.recruit.status.storage",
                targetLabel(displayedStorageTarget),
                displayedStorageCount));
        lines.add(Component.translatable(
                "screen.kingdomwarsmiddleearth.recruit.status.blueprint",
                Component.literal(this.selectedBlueprint().displayName())));
        if (displayedBaseTarget != null) {
            lines.add(Component.translatable(
                    "screen.kingdomwarsmiddleearth.recruit.status.base_progress",
                    displayedBaseProgress,
                    this.selectedBlueprint().placements().size()));
            this.planKingdomWorkOrder().ifPresent(order -> lines.add(Component.translatable(
                    "screen.kingdomwarsmiddleearth.recruit.status.kingdom_order",
                    Component.translatable("screen.kingdomwarsmiddleearth.recruit.kingdom_order."
                            + order.type().name().toLowerCase()),
                    order.profession() == null
                            ? Component.translatable("screen.kingdomwarsmiddleearth.recruit.status.none")
                            : Component.translatable(order.profession().translationKey()),
                    order.itemId().isBlank() ? Component.literal("-") : Component.literal(order.itemId()))));
        }
        return List.copyOf(lines);
    }

    public boolean handleMenuButton(ServerPlayer player, int buttonId) {
        if (player.level() != this.level()
                || !this.isAlive()
                || player.distanceToSqr(this) > 64.0
                || !RecruitCommandMenu.isSupportedButton(buttonId)) {
            return false;
        }
        if (buttonId == RecruitCommandMenu.BUTTON_HIRE) {
            return this.tryHire(player);
        }
        if (!this.isOwnedBy(player)) {
            sendFeedback(player, Component.translatable("message.kingdomwarsmiddleearth.recruit.not_owner"));
            return false;
        }
        Optional<WorkerProfession> profession = WorkerProfessionCatalog.professionForButton(buttonId);
        if (profession.isPresent()) {
            return this.tryAssignWorkerProfession(player, profession.get());
        }

        return switch (buttonId) {
            case RecruitCommandMenu.BUTTON_PROMOTE_COMMANDER -> this.tryPromoteCommander(player);
            case RecruitCommandMenu.BUTTON_TOGGLE_AUTO_RECRUITMENT -> this.tryToggleAutomaticRecruitment(player);
            case RecruitCommandMenu.BUTTON_START_RECRUITMENT -> this.tryStartCommanderCampaign(player);
            case RecruitCommandMenu.BUTTON_NEXT_BLUEPRINT -> this.cycleSelectedBlueprint(player);
            case RecruitCommandMenu.BUTTON_RETURN_TO_SOLDIER -> this.tryReturnToSoldier(player);
            case RecruitCommandMenu.BUTTON_CANCEL_BUILD -> this.tryCancelBuilding(player);
            case RecruitCommandMenu.BUTTON_FOLLOW -> {
                this.setRecruitCommand(RecruitmentAction.FOLLOW_OWNER);
                this.applyCommanderOrderToLoadedGroup(RecruitmentAction.FOLLOW_OWNER, null);
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.follow"));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_HOLD -> {
                this.moveTarget = this.blockPosition();
                this.setRecruitCommand(RecruitmentAction.HOLD_POSITION);
                this.applyCommanderOrderToLoadedGroup(RecruitmentAction.HOLD_POSITION, null);
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.hold"));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_MOVE -> {
                this.moveTarget = player.blockPosition();
                this.setRecruitCommand(RecruitmentAction.MOVE_TO_POSITION);
                this.applyCommanderOrderToLoadedGroup(RecruitmentAction.MOVE_TO_POSITION, player.blockPosition());
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.move"));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_SET_WORKSITE -> {
                if (this.getWorkerProfession().isEmpty()) {
                    player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.worksite.missing_profession"));
                    yield false;
                }
                Optional<BlockPos> targetedWorksite = targetedBlock(player);
                if (targetedWorksite.isEmpty()) {
                    player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.worksite.invalid_target"));
                    yield false;
                }
                if (!this.isInsideSettlementClaim(targetedWorksite.get())) {
                    player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.worksite.invalid_target"));
                    yield false;
                }
                this.setWorkTarget(targetedWorksite.get());
                this.setRecruitCommand(RecruitmentAction.WORK_AT_SITE);
                this.transitionWorker(WorkerPhase.ACQUIRE_ORDER, "worksite_assigned", null);
                WorkerTaskDecision decision = this.planWorkerTask().orElseThrow();
                player.sendSystemMessage(Component.translatable(
                        "message.kingdomwarsmiddleearth.recruit.worksite.set",
                        Component.translatable("screen.kingdomwarsmiddleearth.recruit.worktask."
                                + decision.taskType().name().toLowerCase())));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_RETURN_WORKSITE -> {
                if (this.workTarget == null) {
                    player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.worksite.missing"));
                    yield false;
                }
                this.setRecruitCommand(RecruitmentAction.WORK_AT_SITE);
                this.transitionWorker(WorkerPhase.ACQUIRE_ORDER, "return_to_worksite", null);
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.worksite.return"));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_CLEAR_WORKSITE -> {
                this.setWorkTarget(null);
                this.pauseWorkerNavigation();
                this.transitionWorker(WorkerPhase.BLOCKED, "worksite_missing", null);
                if (this.getRecruitCommand() == RecruitmentAction.WORK_AT_SITE) {
                    this.setRecruitCommand(RecruitmentAction.FOLLOW_OWNER);
                }
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.worksite.clear"));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_WORK_RADIUS_DECREASE -> {
                this.adjustWorkRadius(-WORK_RADIUS_STEP);
                player.sendSystemMessage(Component.translatable(
                        "message.kingdomwarsmiddleearth.recruit.worksite.radius",
                        this.workRadius));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_WORK_RADIUS_INCREASE -> {
                this.adjustWorkRadius(WORK_RADIUS_STEP);
                player.sendSystemMessage(Component.translatable(
                        "message.kingdomwarsmiddleearth.recruit.worksite.radius",
                        this.workRadius));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_SET_STORAGE -> {
                Optional<BlockPos> targetedStorage = targetedBlock(player);
                if (targetedStorage.isEmpty() || this.findContainer(targetedStorage.get()).isEmpty()) {
                    player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.storage.invalid"));
                    yield false;
                }
                if (!this.isInsideSettlementClaim(targetedStorage.get())) {
                    player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.storage.invalid"));
                    yield false;
                }
                this.setStorageTarget(targetedStorage.get());
                this.transitionWorker(WorkerPhase.ACQUIRE_ORDER, "storage_assigned", null);
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.storage.set"));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_BUILD_STARTER_KEEP -> {
                if (this.getWorkerProfession().filter(value -> value == WorkerProfession.BUILDER).isEmpty()
                        && !this.tryAssignWorkerProfession(player, WorkerProfession.BUILDER)) {
                    yield false;
                }
                Optional<BlockPos> targetedBase = targetedBlock(player);
                if (targetedBase.isEmpty()) {
                    player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.worksite.invalid_target"));
                    yield false;
                }
                if (!this.isInsideSettlementClaim(targetedBase.get())) {
                    player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.worksite.invalid_target"));
                    yield false;
                }
                this.setBaseTarget(targetedBase.get());
                this.setWorkTarget(this.baseTarget);
                this.starterBaseCompletedBlocks = 0;
                this.setRecruitCommand(RecruitmentAction.WORK_AT_SITE);
                this.transitionWorker(WorkerPhase.ACQUIRE_ORDER, "blueprint_queued", null);
                KingdomBaseBuildDecision buildDecision = this.planStarterBaseBuild().orElseThrow();
                WorkerResourceDecision resourceDecision = this.planResourceDecision().orElseThrow();
                player.sendSystemMessage(Component.translatable(
                        "message.kingdomwarsmiddleearth.recruit.base.starter_keep",
                        Component.literal(this.selectedBlueprint().displayName()),
                        Component.translatable("screen.kingdomwarsmiddleearth.recruit.basebuild."
                                + buildDecision.action().name().toLowerCase()),
                        Component.translatable("screen.kingdomwarsmiddleearth.recruit.workaction."
                                + resourceDecision.action().name().toLowerCase())));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_PROTECT -> {
                this.setRecruitCommand(RecruitmentAction.PROTECT_OWNER);
                this.applyCommanderOrderToLoadedGroup(RecruitmentAction.PROTECT_OWNER, null);
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.protect"));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_ATTACK -> {
                this.setRecruitCommand(RecruitmentAction.ATTACK_TARGET);
                this.applyCommanderOrderToLoadedGroup(RecruitmentAction.ATTACK_TARGET, null);
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.attack"));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_CLEAR -> {
                this.setTarget(null);
                this.setRecruitCommand(RecruitmentAction.CLEAR_TARGET);
                this.applyCommanderOrderToLoadedGroup(RecruitmentAction.CLEAR_TARGET, null);
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.clear"));
                yield true;
            }
            default -> false;
        };
    }

    public boolean shouldMoveToCommandTarget() {
        return this.isTame()
                && this.getRecruitCommand() == RecruitmentAction.MOVE_TO_POSITION
                && this.moveTarget != null
                && this.distanceToMoveTargetSqr() > 4.0;
    }

    public double distanceToMoveTargetSqr() {
        if (this.moveTarget == null) {
            return 0.0;
        }
        return this.distanceToSqr(
                this.moveTarget.getX() + 0.5,
                this.moveTarget.getY(),
                this.moveTarget.getZ() + 0.5);
    }

    public boolean shouldUseCompanionAi() {
        RecruitmentAction command = this.getRecruitCommand();
        return this.isTame()
                && !this.isOrderedToSit()
                && (command == RecruitmentAction.FOLLOW_OWNER || command == RecruitmentAction.PROTECT_OWNER)
                && this.getRecruitOwner().isPresent();
    }

    public Optional<LivingEntity> getRecruitOwner() {
        EntityReference<LivingEntity> ownerReference = this.getOwnerReference();
        if (ownerReference == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(EntityReference.getLivingEntity(ownerReference, this.level()));
    }

    public Optional<WorkerProfession> getWorkerProfession() {
        int profession = this.entityData.get(DATA_WORKER_PROFESSION);
        WorkerProfession[] values = WorkerProfession.values();
        if (profession < 0 || profession >= values.length) {
            return Optional.empty();
        }
        return Optional.of(values[profession]);
    }

    public void setWorkerProfession(WorkerProfession profession) {
        this.entityData.set(DATA_WORKER_PROFESSION, profession.ordinal());
        if (this.getRecruitDuty() != RecruitDuty.COMMANDER) {
            this.setRecruitDuty(RecruitDuty.WORKER);
        }
        this.applyWorkerEquipment(profession);
        this.syncRecruitStatusState();
    }

    private void clearWorkerProfession() {
        this.entityData.set(DATA_WORKER_PROFESSION, -1);
        this.setRecruitDuty(RecruitDuty.SOLDIER);
        this.workerPhase = WorkerPhase.ACQUIRE_ORDER;
        this.workerReason = "soldier_duty";
        this.workerCooldownTicks = 0;
        this.activeWorkTarget = null;
        this.blacklistedWorkTarget = null;
        this.blacklistedWorkTargetTicks = 0;
        this.workOrderId = null;
        this.setWorkTarget(null);
        this.setStorageTarget(null);
        this.setBaseTarget(null);
        this.starterBaseCompletedBlocks = 0;
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        this.setRecruitCommand(RecruitmentAction.FOLLOW_OWNER);
        this.syncRecruitStatusState();
    }

    public Optional<WorkerTaskDecision> planWorkerTask() {
        return this.getWorkerProfession()
                .map(profession -> WorkerTaskPlanner.plan(profession, this.createWorksite(profession).orElse(null)));
    }

    public Optional<WorkerResourceDecision> planResourceDecision() {
        return this.getWorkerProfession().map(profession -> {
            if (profession == WorkerProfession.COURIER) {
                return this.planCourierLogistics()
                        .map(WorkerLogisticsDecision::asResourceDecision)
                        .orElseGet(() -> WorkerResourceDecision.idle("missing_courier_route"));
            }
            return WorkerResourcePlanner.plan(
                    profession,
                    this.createWorksite(profession).orElse(null),
                    this.workerInventorySnapshot(),
                    this.storageInventorySnapshot(),
                    128);
        });
    }

    public Optional<WorkerLogisticsDecision> planCourierLogistics() {
        if (this.getWorkerProfession().filter(profession -> profession == WorkerProfession.COURIER).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(WorkerLogisticsPlanner.planAnyAvailableSupply(
                this.createCourierRoute().orElse(null),
                this.workerInventorySnapshot(),
                this.storageInventorySnapshot(),
                128));
    }

    public Optional<KingdomBaseBuildDecision> planStarterBaseBuild() {
        if (this.baseTarget == null) {
            return Optional.empty();
        }
        return Optional.of(KingdomBaseBuildPlanner.planNext(
                this.selectedBlueprint(),
                this.storageInventorySnapshot(),
                this.starterBaseCompletedBlocks));
    }

    public Optional<KingdomWorkOrder> planKingdomWorkOrder() {
        if (this.baseTarget == null) {
            return Optional.empty();
        }
        Map<WorkerProfession, Integer> workers;
        Optional<WorkerProfession> profession = this.getWorkerProfession();
        if (profession.isPresent()) {
            workers = Map.of(profession.get(), 1);
        } else {
            workers = Map.of();
        }
        int population = this.isTame() ? 1 : 0;
        return Optional.of(KingdomSettlementPlanner.planNextWorkOrder(
                new KingdomSettlementState(
                        this.storageInventorySnapshot(),
                        workers,
                        population,
                        Math.max(1, population + 1),
                        this.starterBaseCompletedBlocks,
                        true),
                this.selectedBlueprint()));
    }

    public boolean shouldRunWorkerCycle() {
        return this.isTame()
                && this.getRecruitDuty() == RecruitDuty.WORKER
                && this.getRecruitCommand() == RecruitmentAction.WORK_AT_SITE
                && this.getWorkerProfession().filter(WorkerProfessionCatalog::isEnabled).isPresent()
                && this.workTarget != null
                && this.hasPaidSettlementUpkeep();
    }

    public boolean performWorkerCycle() {
        if (!this.shouldRunWorkerCycle() || this.level().isClientSide()) {
            return false;
        }
        WorkerPhase previousPhase = this.workerPhase;
        String previousReason = this.workerReason;
        this.tickWorkerController();
        return previousPhase != this.workerPhase || !previousReason.equals(this.workerReason);
    }

    public void tickWorkerController() {
        if (!this.shouldRunWorkerCycle() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.getTarget() != null || this.hurtTime > 0) {
            this.workerCooldownTicks = 40;
            this.transitionWorker(WorkerPhase.COOLDOWN, "combat_interrupted", null);
            this.pauseWorkerNavigation();
            return;
        }
        if (this.workerPhase == WorkerPhase.FIND_TARGET
                && Math.floorMod(this.getUUID().hashCode(), 4) != Math.floorMod(this.tickCount, 4)) {
            return;
        }
        if (this.blacklistedWorkTargetTicks > 0 && --this.blacklistedWorkTargetTicks == 0) {
            this.blacklistedWorkTarget = null;
        }

        switch (this.workerPhase) {
            case ACQUIRE_ORDER -> this.tickAcquireOrder();
            case FIND_TARGET -> this.tickFindTarget();
            case NAVIGATE_SOURCE -> this.tickWorkerNavigation(WorkerPhase.INTERACT);
            case INTERACT -> this.tickWorkerInteraction(serverLevel);
            case COLLECT -> this.transitionAfterCollection();
            case NAVIGATE_STORAGE -> this.tickWorkerNavigation(WorkerPhase.DEPOSIT);
            case DEPOSIT -> this.tickWorkerDeposit();
            case COOLDOWN, BLOCKED -> {
                if (this.workerCooldownTicks > 0) {
                    this.workerCooldownTicks--;
                } else {
                    this.transitionWorker(WorkerPhase.ACQUIRE_ORDER, "ready", null);
                }
            }
        }
    }

    public void pauseWorkerNavigation() {
        this.getNavigation().stop();
    }

    private void tickAcquireOrder() {
        WorkerProfession profession = this.getWorkerProfession().orElseThrow();
        if (!this.workerInventoryIsEmpty()
                && profession != WorkerProfession.BUILDER
                && profession != WorkerProfession.COURIER) {
            if (this.storageTarget == null || this.findContainer(this.storageTarget).isEmpty()) {
                this.blockWorker("storage_container_required");
                return;
            }
            this.transitionWorker(WorkerPhase.NAVIGATE_STORAGE, "deposit_inventory", this.storageTarget);
            return;
        }

        switch (profession) {
            case FARMER, MINER -> this.transitionWorker(WorkerPhase.FIND_TARGET, "scan_worksite", null);
            case LUMBERJACK -> {
                if (!this.workerInventoryContains(Items.OAK_SAPLING)) {
                    if (this.storageTarget == null || !this.containerContains(this.storageTarget, Items.OAK_SAPLING)) {
                        this.blockWorker("matching_sapling_required");
                    } else {
                        this.transitionWorker(WorkerPhase.NAVIGATE_SOURCE, "withdraw_sapling", this.storageTarget);
                    }
                } else {
                    this.transitionWorker(WorkerPhase.FIND_TARGET, "scan_worksite", null);
                }
            }
            case BUILDER -> this.acquireBuilderOrder();
            case COURIER -> this.acquireCourierOrder();
            default -> this.blockWorker("profession_not_enabled");
        }
    }

    private void tickFindTarget() {
        if (this.workTarget == null) {
            this.blockWorker("worksite_missing");
            return;
        }
        WorkerProfession profession = this.getWorkerProfession().orElseThrow();
        int radius = this.worksiteScanRadius();
        int side = radius * 2 + 1;
        int layers = 5;
        int totalPositions = side * side * layers;
        int scanBudget = Math.min(128, totalPositions);

        for (int checked = 0; checked < scanBudget; checked++) {
            int index = this.workerScanCursor;
            this.workerScanCursor = (this.workerScanCursor + 1) % totalPositions;
            int xOffset = index % side - radius;
            int zOffset = index / side % side - radius;
            int yOffset = index / (side * side) - 1;
            BlockPos candidate = this.workTarget.offset(xOffset, yOffset, zOffset);
            if (candidate.equals(this.blacklistedWorkTarget)) {
                continue;
            }
            BlockState state = this.level().getBlockState(candidate);
            if (this.isWorkerTarget(profession, state)) {
                this.workerScanCursor = 0;
                this.transitionWorker(WorkerPhase.NAVIGATE_SOURCE, "navigate_work_target", candidate.immutable());
                return;
            }
        }

        if (this.workerScanCursor == 0) {
            this.blockWorker("no_valid_target");
        }
    }

    private void tickWorkerNavigation(WorkerPhase arrivalPhase) {
        if (this.activeWorkTarget == null || !this.level().isLoaded(this.activeWorkTarget)) {
            this.blockWorker("target_unloaded");
            return;
        }
        if (this.distanceToSqr(
                this.activeWorkTarget.getX() + 0.5,
                this.activeWorkTarget.getY() + 0.5,
                this.activeWorkTarget.getZ() + 0.5) <= 4.0) {
            this.workerNavigationFailures = 0;
            this.pauseWorkerNavigation();
            this.transitionWorker(arrivalPhase, this.workerReason, this.activeWorkTarget);
            return;
        }
        if (this.tickCount % 10 == 0) {
            boolean pathStarted = this.getNavigation().moveTo(
                    this.activeWorkTarget.getX() + 0.5,
                    this.activeWorkTarget.getY(),
                    this.activeWorkTarget.getZ() + 0.5,
                    1.0);
            if (!pathStarted) {
                this.workerNavigationFailures++;
                if (this.workerNavigationFailures >= 3) {
                    this.blockWorker("target_unreachable");
                }
            } else {
                this.workerNavigationFailures = 0;
            }
        }
    }

    private void tickWorkerInteraction(ServerLevel level) {
        if (this.activeWorkTarget == null || this.distanceToSqr(
                this.activeWorkTarget.getX() + 0.5,
                this.activeWorkTarget.getY() + 0.5,
                this.activeWorkTarget.getZ() + 0.5) > 4.0) {
            this.blockWorker("interaction_out_of_reach");
            return;
        }
        switch (this.workerReason) {
            case "withdraw_sapling" -> {
                if (this.withdrawSpecificItem(this.activeWorkTarget, Items.OAK_SAPLING, 1)) {
                    this.transitionWorker(WorkerPhase.ACQUIRE_ORDER, "sapling_ready", null);
                } else {
                    this.blockWorker("matching_sapling_required");
                }
            }
            case "withdraw_build_material" -> {
                if (this.withdrawCurrentBuildMaterial()) {
                    this.transitionWorker(WorkerPhase.ACQUIRE_ORDER, "build_material_ready", null);
                } else {
                    this.blockWorker("build_material_missing");
                }
            }
            case "courier_withdraw" -> {
                if (this.withdrawFirstStack(this.activeWorkTarget)) {
                    this.transitionWorker(WorkerPhase.ACQUIRE_ORDER, "courier_loaded", null);
                } else {
                    this.blockWorker("courier_source_empty");
                }
            }
            case "build_place" -> this.placeCurrentBuildBlock();
            case "navigate_work_target" -> this.performGatheringInteraction(level);
            default -> this.blockWorker("unknown_worker_action");
        }
    }

    private void transitionAfterCollection() {
        if (this.workerInventoryIsEmpty()) {
            this.workerCooldownTicks = 20;
            this.transitionWorker(WorkerPhase.COOLDOWN, "work_complete", null);
        } else if (this.storageTarget != null && this.findContainer(this.storageTarget).isPresent()) {
            this.transitionWorker(WorkerPhase.NAVIGATE_STORAGE, "deposit_inventory", this.storageTarget);
        } else {
            this.blockWorker("storage_container_required");
        }
    }

    private void tickWorkerDeposit() {
        if (this.activeWorkTarget == null || !this.insertWorkerInventory(this.activeWorkTarget)) {
            this.blockWorker("storage_full_or_missing");
            return;
        }
        this.workerCooldownTicks = 40;
        this.transitionWorker(WorkerPhase.COOLDOWN, "deposit_complete", null);
    }

    private void performGatheringInteraction(ServerLevel level) {
        WorkerProfession profession = this.getWorkerProfession().orElseThrow();
        BlockPos target = this.activeWorkTarget;
        if (target == null || !this.canModifyWorkerTarget(target)) {
            this.blockWorker("target_outside_claim");
            return;
        }
        BlockState state = level.getBlockState(target);
        if (!this.isWorkerTarget(profession, state)) {
            this.transitionWorker(WorkerPhase.ACQUIRE_ORDER, "target_changed", null);
            return;
        }

        ItemStack tool = this.getMainHandItem();
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(
                state,
                level,
                target,
                level.getBlockEntity(target),
                this,
                tool));
        NonNullList<ItemStack> nextInventory = this.copyWorkerInventory();

        if (profession == WorkerProfession.FARMER) {
            if (!removeOneFromStacks(drops, Items.WHEAT_SEEDS)
                    && !removeOneFromStacks(nextInventory, Items.WHEAT_SEEDS)) {
                this.blockWorker("replant_seed_missing");
                return;
            }
        } else if (profession == WorkerProfession.LUMBERJACK
                && !removeOneFromStacks(nextInventory, Items.OAK_SAPLING)) {
            this.blockWorker("matching_sapling_required");
            return;
        }

        if (!mergeAll(nextInventory, drops)) {
            this.blockWorker("worker_inventory_full");
            return;
        }
        if (!level.destroyBlock(target, false, this, 16)) {
            this.blockWorker("world_change_rejected");
            return;
        }
        if (profession == WorkerProfession.FARMER && state.getBlock() instanceof CropBlock cropBlock) {
            level.setBlock(target, cropBlock.getStateForAge(0), 3);
        } else if (profession == WorkerProfession.LUMBERJACK) {
            level.setBlock(target, Blocks.OAK_SAPLING.defaultBlockState(), 3);
        }
        this.workerInventory = nextInventory;
        if (!tool.isEmpty()) {
            tool.hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
        }
        this.transitionWorker(WorkerPhase.COLLECT, "resource_collected", null);
    }

    private void acquireBuilderOrder() {
        if (this.baseTarget == null) {
            this.blockWorker("base_target_missing");
            return;
        }
        List<BaseBlockPlacement> placements = this.selectedBlueprint().placements();
        while (this.starterBaseCompletedBlocks < placements.size()) {
            BaseBlockPlacement placement = placements.get(this.starterBaseCompletedBlocks);
            Optional<Block> block = blockForPlacement(placement.blockId());
            if (block.isEmpty()) {
                this.blockWorker("unknown_blueprint_block");
                return;
            }
            BlockPos placementPos = this.baseTarget.offset(placement.x(), placement.y(), placement.z());
            BlockState state = this.level().getBlockState(placementPos);
            if (state.is(block.get())) {
                this.starterBaseCompletedBlocks++;
                continue;
            }
            if (!state.isAir() && !state.canBeReplaced()) {
                this.blockWorker("blueprint_position_blocked");
                return;
            }
            if (this.workerInventoryContains(block.get().asItem())) {
                this.transitionWorker(WorkerPhase.NAVIGATE_SOURCE, "build_place", placementPos);
                return;
            }
            if (this.storageTarget == null || !this.containerContains(this.storageTarget, block.get().asItem())) {
                this.blockWorker("build_material_missing");
                return;
            }
            this.transitionWorker(WorkerPhase.NAVIGATE_SOURCE, "withdraw_build_material", this.storageTarget);
            return;
        }
        this.finishCompletedBlueprint();
    }

    private void acquireCourierOrder() {
        if (this.storageTarget == null || this.workTarget == null
                || this.storageTarget.equals(this.workTarget)
                || this.findContainer(this.storageTarget).isEmpty()
                || this.findContainer(this.workTarget).isEmpty()) {
            this.blockWorker("courier_route_requires_two_containers");
            return;
        }
        if (this.workerInventoryIsEmpty()) {
            this.transitionWorker(WorkerPhase.NAVIGATE_SOURCE, "courier_withdraw", this.storageTarget);
        } else {
            this.transitionWorker(WorkerPhase.NAVIGATE_STORAGE, "courier_deliver", this.workTarget);
        }
    }

    private void finishCompletedBlueprint() {
        if (!(this.level() instanceof ServerLevel serverLevel)
                || this.baseTarget == null
                || this.getOwnerReference() == null) {
            this.blockWorker("kingdom_link_missing");
            return;
        }
        KingdomBaseBlueprint blueprint = this.selectedBlueprint();
        ArrayList<Integer> completedPlacements = new ArrayList<>(blueprint.placements().size());
        for (int i = 0; i < blueprint.placements().size(); i++) {
            completedPlacements.add(i);
        }
        BuildProject project = new BuildProject(
                UUID.randomUUID(),
                blueprint.id(),
                serverLevel.dimension().identifier().toString(),
                this.baseTarget.getX(),
                this.baseTarget.getY(),
                this.baseTarget.getZ(),
                0,
                completedPlacements,
                "");
        boolean success = KingdomSavedData.get(serverLevel).completeBuildProject(
                this.getOwnerReference().getUUID(),
                project,
                blueprint.housingReward(),
                blueprint.worksiteType(),
                blueprint.worksiteCapacity());
        if (!success) {
            this.blockWorker("kingdom_link_missing");
            return;
        }
        this.workerCooldownTicks = 100;
        this.setBaseTarget(null);
        this.setWorkTarget(null);
        this.setRecruitCommand(RecruitmentAction.FOLLOW_OWNER);
        this.transitionWorker(WorkerPhase.COOLDOWN, "blueprint_complete", null);
    }

    private void placeCurrentBuildBlock() {
        if (this.baseTarget == null || this.activeWorkTarget == null || !this.canModifyWorkerTarget(this.activeWorkTarget)) {
            this.blockWorker("target_outside_claim");
            return;
        }
        List<BaseBlockPlacement> placements = this.selectedBlueprint().placements();
        if (this.starterBaseCompletedBlocks >= placements.size()) {
            this.transitionWorker(WorkerPhase.ACQUIRE_ORDER, "blueprint_complete", null);
            return;
        }
        BaseBlockPlacement placement = placements.get(this.starterBaseCompletedBlocks);
        Optional<Block> block = blockForPlacement(placement.blockId());
        if (block.isEmpty() || !this.workerInventoryContains(block.get().asItem())) {
            this.blockWorker("build_material_missing");
            return;
        }
        BlockPos expected = this.baseTarget.offset(placement.x(), placement.y(), placement.z());
        if (!expected.equals(this.activeWorkTarget)) {
            this.transitionWorker(WorkerPhase.ACQUIRE_ORDER, "project_advanced", null);
            return;
        }
        BlockState current = this.level().getBlockState(expected);
        if (current.is(block.get())) {
            this.starterBaseCompletedBlocks++;
            this.transitionWorker(WorkerPhase.ACQUIRE_ORDER, "placement_reconciled", null);
            return;
        }
        if (!current.isAir() && !current.canBeReplaced()) {
            this.blockWorker("blueprint_position_blocked");
            return;
        }
        if (!this.level().setBlock(expected, block.get().defaultBlockState(), 3)) {
            this.blockWorker("world_change_rejected");
            return;
        }
        removeOneFromStacks(this.workerInventory, block.get().asItem());
        this.starterBaseCompletedBlocks++;
        this.workerCooldownTicks = 20;
        this.transitionWorker(WorkerPhase.COOLDOWN, "placement_complete", null);
    }

    private boolean withdrawCurrentBuildMaterial() {
        if (this.baseTarget == null || this.activeWorkTarget == null) {
            return false;
        }
        List<BaseBlockPlacement> placements = this.selectedBlueprint().placements();
        if (this.starterBaseCompletedBlocks >= placements.size()) {
            return false;
        }
        return blockForPlacement(placements.get(this.starterBaseCompletedBlocks).blockId())
                .map(block -> this.withdrawSpecificItem(this.activeWorkTarget, block.asItem(), 1))
                .orElse(false);
    }

    private boolean isWorkerTarget(WorkerProfession profession, BlockState state) {
        return switch (profession) {
            case FARMER -> state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMaxAge(state);
            case LUMBERJACK -> state.is(ModBlockTags.WORKER_LOGS);
            case MINER -> state.is(ModBlockTags.WORKER_MINEABLE);
            default -> false;
        };
    }

    private boolean canModifyWorkerTarget(BlockPos target) {
        if (this.workTarget == null || !this.level().isLoaded(target)) {
            return false;
        }
        int radius = this.worksiteScanRadius();
        if (Math.abs(target.getX() - this.workTarget.getX()) > radius
                || Math.abs(target.getZ() - this.workTarget.getZ()) > radius
                || Math.abs(target.getY() - this.workTarget.getY()) > 4) {
            return false;
        }
        return this.isInsideSettlementClaim(target);
    }

    private boolean isInsideSettlementClaim(BlockPos target) {
        if (!(this.level() instanceof ServerLevel serverLevel) || !this.level().isLoaded(target)) {
            return false;
        }
        EntityReference<LivingEntity> owner = this.getOwnerReference();
        if (owner == null) {
            return false;
        }
        KingdomSavedData data = KingdomSavedData.get(serverLevel);
        if (!data.isHallActive(owner.getUUID())) {
            return false;
        }
        return data.kingdomForOwner(owner.getUUID())
                .map(KingdomRecord::settlement)
                .filter(settlement -> settlement.dimensionId().equals(serverLevel.dimension().identifier().toString()))
                .filter(settlement -> Math.abs(target.getX() - settlement.hallX()) <= settlement.claimRadius())
                .filter(settlement -> Math.abs(target.getZ() - settlement.hallZ()) <= settlement.claimRadius())
                .isPresent();
    }

    private Optional<Container> findContainer(BlockPos pos) {
        if (!this.level().isLoaded(pos)) {
            return Optional.empty();
        }
        BlockEntity blockEntity = this.level().getBlockEntity(pos);
        return blockEntity instanceof Container container ? Optional.of(container) : Optional.empty();
    }

    private boolean containerContains(BlockPos pos, net.minecraft.world.item.Item item) {
        return this.findContainer(pos).stream()
                .flatMap(container -> java.util.stream.IntStream.range(0, container.getContainerSize())
                        .mapToObj(container::getItem))
                .anyMatch(stack -> stack.is(item));
    }

    private boolean withdrawSpecificItem(BlockPos pos, net.minecraft.world.item.Item item, int amount) {
        Optional<Container> containerOptional = this.findContainer(pos);
        if (containerOptional.isEmpty()) {
            return false;
        }
        Container container = containerOptional.get();
        int remaining = amount;
        NonNullList<ItemStack> nextInventory = this.copyWorkerInventory();
        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            ItemStack stored = container.getItem(slot);
            if (!stored.is(item)) {
                continue;
            }
            int transfer = Math.min(remaining, stored.getCount());
            ItemStack candidate = stored.copyWithCount(transfer);
            if (!mergeAll(nextInventory, List.of(candidate))) {
                return false;
            }
            remaining -= transfer;
        }
        if (remaining > 0) {
            return false;
        }
        remaining = amount;
        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            ItemStack stored = container.getItem(slot);
            if (stored.is(item)) {
                int transfer = Math.min(remaining, stored.getCount());
                container.removeItem(slot, transfer);
                remaining -= transfer;
            }
        }
        container.setChanged();
        this.workerInventory = nextInventory;
        return true;
    }

    private boolean withdrawFirstStack(BlockPos pos) {
        Optional<Container> containerOptional = this.findContainer(pos);
        if (containerOptional.isEmpty()) {
            return false;
        }
        Container container = containerOptional.get();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stored = container.getItem(slot);
            if (stored.isEmpty()) {
                continue;
            }
            int transfer = Math.min(stored.getCount(), stored.getMaxStackSize());
            NonNullList<ItemStack> nextInventory = this.copyWorkerInventory();
            if (!mergeAll(nextInventory, List.of(stored.copyWithCount(transfer)))) {
                continue;
            }
            container.removeItem(slot, transfer);
            container.setChanged();
            this.workerInventory = nextInventory;
            return true;
        }
        return false;
    }

    private boolean insertWorkerInventory(BlockPos pos) {
        Optional<Container> containerOptional = this.findContainer(pos);
        if (containerOptional.isEmpty()) {
            return false;
        }
        Container container = containerOptional.get();
        NonNullList<ItemStack> simulated = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            simulated.set(slot, container.getItem(slot).copy());
        }
        List<ItemStack> contents = this.workerInventory.stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
        if (!mergeAll(simulated, contents)) {
            return false;
        }
        for (int slot = 0; slot < simulated.size(); slot++) {
            container.setItem(slot, simulated.get(slot));
        }
        container.setChanged();
        this.workerInventory = NonNullList.withSize(9, ItemStack.EMPTY);
        return true;
    }

    private boolean workerInventoryIsEmpty() {
        return this.workerInventory.stream().allMatch(ItemStack::isEmpty);
    }

    private boolean workerInventoryContains(net.minecraft.world.item.Item item) {
        return this.workerInventory.stream().anyMatch(stack -> stack.is(item));
    }

    private NonNullList<ItemStack> copyWorkerInventory() {
        NonNullList<ItemStack> copy = NonNullList.withSize(this.workerInventory.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < this.workerInventory.size(); slot++) {
            copy.set(slot, this.workerInventory.get(slot).copy());
        }
        return copy;
    }

    private static boolean mergeAll(List<ItemStack> inventory, List<ItemStack> incoming) {
        for (ItemStack original : incoming) {
            ItemStack remaining = original.copy();
            for (ItemStack existing : inventory) {
                if (!remaining.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)
                        && existing.getCount() < existing.getMaxStackSize()) {
                    int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                    existing.grow(moved);
                    remaining.shrink(moved);
                }
            }
            for (int slot = 0; slot < inventory.size() && !remaining.isEmpty(); slot++) {
                if (inventory.get(slot).isEmpty()) {
                    int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                    inventory.set(slot, remaining.copyWithCount(moved));
                    remaining.shrink(moved);
                }
            }
            if (!remaining.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean removeOneFromStacks(List<ItemStack> stacks, net.minecraft.world.item.Item item) {
        for (ItemStack stack : stacks) {
            if (stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private void transitionWorker(WorkerPhase phase, String reason, @Nullable BlockPos target) {
        this.workerPhase = phase;
        this.workerReason = reason;
        this.activeWorkTarget = target == null ? null : target.immutable();
        this.entityData.set(DATA_WORKER_PHASE, phase.id());
        this.entityData.set(DATA_WORKER_REASON, reason);
        this.entityData.set(DATA_ACTIVE_WORK_TARGET, Optional.ofNullable(this.activeWorkTarget));
        this.syncRecruitStatusState();
    }

    private void blockWorker(String reason) {
        if (reason.equals("target_unreachable") && this.activeWorkTarget != null) {
            this.blacklistedWorkTarget = this.activeWorkTarget.immutable();
            this.blacklistedWorkTargetTicks = 600;
        }
        this.workerCooldownTicks = 100;
        this.workerNavigationFailures = 0;
        this.pauseWorkerNavigation();
        this.transitionWorker(WorkerPhase.BLOCKED, reason, null);
    }

    private int worksiteScanRadius() {
        return Math.max(MIN_WORK_RADIUS, Math.min(MAX_WORK_RADIUS, this.workRadius));
    }

    private boolean tryHire(ServerPlayer player) {
        if (this.isTame()) {
            sendFeedback(player, Component.translatable("message.kingdomwarsmiddleearth.recruit.already_hired"));
            return false;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        KingdomSavedData kingdomData = KingdomSavedData.get(serverLevel);
        Optional<KingdomRecord> kingdom = kingdomData.kingdomForOwner(player.getUUID());
        if (kingdom.isEmpty()) {
            sendFeedback(player, Component.translatable("message.kingdomwarsmiddleearth.recruit.kingdom_hall_required"));
            return false;
        }
        Optional<KingdomHallBlockEntity> hall = this.findKingdomHall(serverLevel, kingdom.get());
        RecruitmentEligibility eligibility = RecruitmentService.evaluateDirectHire(
                kingdom.get(),
                this.recruitFactionId(),
                HIRE_COST_EMERALDS,
                player.hasInfiniteMaterials()
                        ? Integer.MAX_VALUE
                        : RecruitmentPaymentService.emeraldCount(player),
                hall.map(KingdomHallBlockEntity::upkeepPaid).orElse(false),
                alliedFactionsFor(kingdom.get().factionId()));
        if (!eligibility.accepted()) {
            String translationKey = switch (eligibility.reasonCode()) {
                case "insufficient_funds" -> "message.kingdomwarsmiddleearth.recruit.need_emeralds";
                case "housing_full" -> "message.kingdomwarsmiddleearth.recruit.housing_full";
                case "hostile_faction" -> "message.kingdomwarsmiddleearth.recruit.hostile_faction";
                default -> "message.kingdomwarsmiddleearth.recruit.upkeep_unpaid";
            };
            sendFeedback(player, eligibility.reasonCode().equals("insufficient_funds")
                    ? Component.translatable(translationKey, HIRE_COST_EMERALDS)
                    : Component.translatable(translationKey));
            return false;
        }
        if (!kingdomData.registerRecruit(player.getUUID(), this.getUUID())) {
            sendFeedback(player, Component.translatable("message.kingdomwarsmiddleearth.recruit.housing_full"));
            return false;
        }
        if (!RecruitmentPaymentService.withdrawEmeralds(player, HIRE_COST_EMERALDS)) {
            kingdomData.unregisterRecruit(player.getUUID(), this.getUUID());
            sendFeedback(player, Component.translatable(
                    "message.kingdomwarsmiddleearth.recruit.payment_changed"));
            return false;
        }

        this.tameForContract(player);
        KingdomRecord registeredKingdom = kingdomData.kingdomForOwner(player.getUUID()).orElseThrow();
        this.kingdomId = registeredKingdom.id();
        this.settlementId = registeredKingdom.settlement().id();
        this.setRecruitDuty(RecruitDuty.SOLDIER);
        this.setRecruitCommand(RecruitmentAction.FOLLOW_OWNER);
        this.setTarget(null);
        this.navigation.stop();
        this.level().broadcastEntityEvent(this, (byte) 7);
        sendFeedback(player, Component.translatable("message.kingdomwarsmiddleearth.recruit.hired"));
        return true;
    }

    private boolean tryAssignWorkerProfession(ServerPlayer player, WorkerProfession profession) {
        if (!WorkerProfessionCatalog.isEnabled(profession)) {
            sendFeedback(player, Component.translatable("message.kingdomwarsmiddleearth.recruit.profession.disabled"));
            return false;
        }
        if (this.getRecruitDuty() == RecruitDuty.COMMANDER) {
            sendFeedback(player, Component.translatable("message.kingdomwarsmiddleearth.recruit.commander.worker"));
            return false;
        }
        WorkerProfessionDefinition definition = WorkerProfessionCatalog.definition(profession).orElseThrow();
        int cost = definition.hireCostEmeralds();
        if (this.getWorkerProfession().filter(current -> current == profession).isPresent()) {
            this.resumeWorkAfterProfessionAssignment();
            sendFeedback(player, Component.translatable(
                    "message.kingdomwarsmiddleearth.recruit.profession",
                    Component.translatable(profession.translationKey())));
            return true;
        }
        if (!player.hasInfiniteMaterials() && RecruitmentPaymentService.emeraldCount(player) < cost) {
            sendFeedback(player, Component.translatable(
                    "message.kingdomwarsmiddleearth.recruit.profession.need_emeralds",
                    cost,
                    Component.translatable(profession.translationKey())));
            return false;
        }
        if (!RecruitmentPaymentService.withdrawEmeralds(player, cost)) {
            sendFeedback(player, Component.translatable(
                    "message.kingdomwarsmiddleearth.recruit.payment_changed"));
            return false;
        }
        this.setWorkerProfession(profession);
        this.resumeWorkAfterProfessionAssignment();
        sendFeedback(player, Component.translatable(
                "message.kingdomwarsmiddleearth.recruit.profession.contract",
                Component.translatable(profession.translationKey()),
                cost));
        return true;
    }

    private boolean cycleSelectedBlueprint(ServerPlayer player) {
        KingdomBaseBlueprint current = this.selectedBlueprint();
        if (this.baseTarget != null && this.starterBaseCompletedBlocks < current.placements().size()) {
            player.sendSystemMessage(Component.translatable(
                    "message.kingdomwarsmiddleearth.recruit.base.blueprint_locked"));
            return false;
        }
        List<KingdomBaseBlueprint> blueprints = KingdomBaseBlueprint.all();
        int currentIndex = Math.max(0, blueprints.indexOf(current));
        KingdomBaseBlueprint next = blueprints.get((currentIndex + 1) % blueprints.size());
        this.selectedBlueprintId = next.id();
        this.entityData.set(DATA_SELECTED_BLUEPRINT, this.selectedBlueprintId);
        this.starterBaseCompletedBlocks = 0;
        this.setBaseTarget(null);
        player.sendSystemMessage(Component.translatable(
                "message.kingdomwarsmiddleearth.recruit.base.blueprint",
                Component.literal(next.displayName())));
        return true;
    }

    private boolean tryReturnToSoldier(ServerPlayer player) {
        WorkerContractService.ExitDecision decision = WorkerContractService.evaluateExit(
                this.getRecruitDuty(),
                this.getWorkerProfession().isPresent(),
                this.workerInventoryIsEmpty(),
                this.baseTarget != null);
        if (decision != WorkerContractService.ExitDecision.ACCEPTED) {
            String translationKey = switch (decision) {
                case NOT_WORKER -> "message.kingdomwarsmiddleearth.recruit.soldier.rejected";
                case CARRIED_ITEMS -> "message.kingdomwarsmiddleearth.recruit.soldier.inventory";
                case ACTIVE_BUILD -> "message.kingdomwarsmiddleearth.recruit.soldier.build_active";
                case ACCEPTED -> throw new IllegalStateException("accepted exit handled below");
            };
            sendFeedback(player, Component.translatable(translationKey));
            return false;
        }
        this.pauseWorkerNavigation();
        this.clearWorkerProfession();
        sendFeedback(player, Component.translatable(
                "message.kingdomwarsmiddleearth.recruit.soldier.returned"));
        return true;
    }

    private boolean tryCancelBuilding(ServerPlayer player) {
        if (this.baseTarget == null) {
            sendFeedback(player, Component.translatable(
                    "message.kingdomwarsmiddleearth.recruit.base.cancel_missing"));
            return false;
        }
        this.pauseWorkerNavigation();
        this.setBaseTarget(null);
        this.setWorkTarget(null);
        this.starterBaseCompletedBlocks = 0;
        this.workOrderId = null;
        this.setRecruitCommand(RecruitmentAction.FOLLOW_OWNER);
        this.transitionWorker(WorkerPhase.ACQUIRE_ORDER, "build_cancelled", null);
        sendFeedback(player, Component.translatable(
                "message.kingdomwarsmiddleearth.recruit.base.cancelled"));
        return true;
    }

    private KingdomBaseBlueprint selectedBlueprint() {
        String blueprintId = this.level().isClientSide()
                ? this.entityData.get(DATA_SELECTED_BLUEPRINT)
                : this.selectedBlueprintId;
        return KingdomBaseBlueprint.byId(blueprintId)
                .orElseGet(KingdomBaseBlueprint::starterKeep);
    }

    private boolean tryPromoteCommander(ServerPlayer player) {
        if (this.getRecruitDuty() != RecruitDuty.SOLDIER || this.getWorkerProfession().isPresent()) {
            sendFeedback(player, Component.translatable("message.kingdomwarsmiddleearth.recruit.commander.worker"));
            return false;
        }
        this.migrateLegacyKingdomLink();
        if (!(this.level() instanceof ServerLevel serverLevel)
                || this.kingdomId == null
                || !KingdomSavedData.get(serverLevel).promoteCommander(player.getUUID(), this.getUUID())) {
            sendFeedback(player, Component.translatable("message.kingdomwarsmiddleearth.recruit.commander.rejected"));
            return false;
        }
        this.setRecruitDuty(RecruitDuty.COMMANDER);
        if (this.armyGroupId == null) {
            this.armyGroupId = UUID.randomUUID();
        }
        this.linkLoadedSoldiersToCommander(serverLevel, player.getUUID());
        this.setRecruitCommand(RecruitmentAction.FOLLOW_OWNER);
        sendFeedback(player, Component.translatable("message.kingdomwarsmiddleearth.recruit.commander.promoted"));
        return true;
    }

    private boolean tryToggleAutomaticRecruitment(ServerPlayer player) {
        if (this.getRecruitDuty() != RecruitDuty.COMMANDER || !(this.level() instanceof ServerLevel serverLevel)) {
            player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.commander.rejected"));
            return false;
        }
        KingdomSavedData data = KingdomSavedData.get(serverLevel);
        Optional<KingdomRecord> kingdomOptional = data.kingdomForOwner(player.getUUID());
        if (kingdomOptional.isEmpty()) {
            return false;
        }
        KingdomRecord kingdom = kingdomOptional.get();
        this.linkLoadedSoldiersToCommander(serverLevel, player.getUUID());
        CommanderPolicy current = kingdom.settlement().commanderPolicy();
        CommanderPolicy updated = new CommanderPolicy(
                !current.automaticRecruitment(),
                Math.max(current.targetRecruitCount(), 4),
                current.maximumCampaignSpend(),
                current.minimumTreasuryReserve(),
                current.campaignDelayTicks());
        if (!data.updateCommanderPolicy(player.getUUID(), kingdom.settlement().revision(), updated)) {
            return false;
        }
        player.sendSystemMessage(Component.translatable(
                "message.kingdomwarsmiddleearth.recruit.commander.auto",
                Boolean.toString(updated.automaticRecruitment())));
        return true;
    }

    private boolean tryStartCommanderCampaign(ServerPlayer player) {
        return this.startCommanderCampaign(player);
    }

    private boolean startCommanderCampaign(@Nullable ServerPlayer feedbackPlayer) {
        if (this.getRecruitDuty() != RecruitDuty.COMMANDER || !(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        EntityReference<LivingEntity> owner = this.getOwnerReference();
        if (owner == null) {
            return false;
        }
        UUID ownerId = owner.getUUID();
        KingdomSavedData data = KingdomSavedData.get(serverLevel);
        Optional<KingdomRecord> kingdomOptional = data.kingdomForOwner(ownerId);
        if (kingdomOptional.isEmpty()) {
            return false;
        }
        KingdomRecord kingdom = kingdomOptional.get();
        Optional<KingdomHallBlockEntity> hallOptional = this.findKingdomHall(serverLevel, kingdom);
        if (hallOptional.isEmpty()) {
            this.sendCampaignRejection(feedbackPlayer, "hall_unloaded");
            return false;
        }
        KingdomHallBlockEntity hall = hallOptional.get();
        KingdomRecord evaluatedKingdom = kingdom;
        if (feedbackPlayer != null && !kingdom.settlement().commanderPolicy().automaticRecruitment()) {
            CommanderPolicy currentPolicy = kingdom.settlement().commanderPolicy();
            CommanderPolicy manualPolicy = new CommanderPolicy(
                    true,
                    currentPolicy.targetRecruitCount(),
                    currentPolicy.maximumCampaignSpend(),
                    currentPolicy.minimumTreasuryReserve(),
                    currentPolicy.campaignDelayTicks());
            evaluatedKingdom = kingdom.withSettlement(kingdom.settlement().withCommanderPolicy(manualPolicy));
        }
        RecruitmentCampaignDecision decision = RecruitmentService.evaluateCommanderCampaign(
                evaluatedKingdom,
                this.getUUID(),
                this.recruitUnitId(),
                this.recruitFactionId(),
                "",
                HIRE_COST_EMERALDS,
                hall.treasuryEmeralds(),
                serverLevel.getGameTime(),
                alliedFactionsFor(kingdom.factionId()));
        if (!decision.accepted()) {
            this.sendCampaignRejection(feedbackPlayer, decision.reasonCode());
            return false;
        }
        RecruitmentCampaign campaign = decision.campaign().orElseThrow();
        if (!hall.reserveEmeralds(campaign.reservedCost())) {
            this.sendCampaignRejection(feedbackPlayer, "treasury_changed");
            return false;
        }
        if (!data.beginCampaign(ownerId, decision)) {
            hall.refundEmeralds(campaign.reservedCost());
            this.sendCampaignRejection(feedbackPlayer, "campaign_changed");
            return false;
        }
        if (feedbackPlayer != null) {
            feedbackPlayer.sendSystemMessage(Component.translatable(
                    "message.kingdomwarsmiddleearth.recruit.commander.campaign",
                    campaign.reservedCost()));
        }
        return true;
    }

    private void tickCommanderCampaign() {
        if (this.getRecruitDuty() != RecruitDuty.COMMANDER || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        EntityReference<LivingEntity> owner = this.getOwnerReference();
        if (owner == null) {
            return;
        }
        UUID ownerId = owner.getUUID();
        KingdomSavedData data = KingdomSavedData.get(serverLevel);
        Optional<KingdomRecord> kingdomOptional = data.kingdomForOwner(ownerId);
        if (kingdomOptional.isEmpty()
                || kingdomOptional.get().settlement().commanderId().filter(this.getUUID()::equals).isEmpty()) {
            return;
        }
        KingdomRecord kingdom = kingdomOptional.get();
        long currentGameTime = serverLevel.getGameTime();
        long elapsedGameTime = this.lastCommanderCampaignGameTime == 0L
                ? 20L
                : Math.max(1L, currentGameTime - this.lastCommanderCampaignGameTime);
        this.lastCommanderCampaignGameTime = currentGameTime;
        Optional<RecruitmentCampaign> activeCampaign = kingdom.settlement().recruitmentCampaigns().stream()
                .filter(RecruitmentCampaign::active)
                .findFirst();
        long campaignDelayApplied = 0L;
        if (activeCampaign.isPresent() && elapsedGameTime > 40L) {
            campaignDelayApplied = elapsedGameTime - 20L;
            data.replaceCampaign(ownerId, activeCampaign.get().delay(campaignDelayApplied));
            kingdom = data.kingdomForOwner(ownerId).orElse(kingdom);
            activeCampaign = kingdom.settlement().recruitmentCampaigns().stream()
                    .filter(RecruitmentCampaign::active)
                    .findFirst();
        }
        Optional<KingdomHallBlockEntity> hallOptional = this.findKingdomHall(serverLevel, kingdom);
        if (hallOptional.isEmpty()) {
            long remainingDelay = elapsedGameTime - campaignDelayApplied;
            if (activeCampaign.isPresent() && remainingDelay > 0L) {
                data.replaceCampaign(ownerId, activeCampaign.get().delay(remainingDelay));
            }
            return;
        }
        KingdomHallBlockEntity hall = hallOptional.get();
        if (!hall.chargeDailyUpkeep(serverLevel.getGameTime(), kingdom.settlement().recruitIds().size())) {
            return;
        }
        if (activeCampaign.isPresent()) {
            if (currentGameTime >= activeCampaign.get().readyGameTime()) {
                this.completeCommanderCampaign(serverLevel, kingdom, hall, activeCampaign.get());
            }
            return;
        }
        if (kingdom.settlement().commanderPolicy().automaticRecruitment()) {
            this.startCommanderCampaign(null);
        }
    }

    private void completeCommanderCampaign(
            ServerLevel level,
            KingdomRecord kingdom,
            KingdomHallBlockEntity hall,
            RecruitmentCampaign campaign
    ) {
        KingdomSavedData data = KingdomSavedData.get(level);
        KingdomRecord current = data.kingdomForOwner(kingdom.ownerId()).orElse(kingdom);
        if (!current.settlement().hasHousingSpace()) {
            this.cancelCommanderCampaign(level, current, hall, campaign, "housing_full");
            return;
        }
        Optional<BlockPos> rally = this.findSafeRallyPosition(level, current);
        if (rally.isEmpty()) {
            this.cancelCommanderCampaign(level, current, hall, campaign, "no_safe_rally_position");
            return;
        }

        @SuppressWarnings("unchecked")
        EntityType<? extends MiddleEarthRecruitEntity> recruitType =
                (EntityType<? extends MiddleEarthRecruitEntity>) this.getType();
        MiddleEarthRecruitEntity recruit = recruitType.create(level, EntitySpawnReason.EVENT);
        if (recruit == null) {
            this.cancelCommanderCampaign(level, current, hall, campaign, "spawn_failed");
            return;
        }
        BlockPos spawnPos = rally.get();
        recruit.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        recruit.setOwnerReference(EntityReference.of(kingdom.ownerId()));
        recruit.setTame(true, true);
        recruit.kingdomId = current.id();
        recruit.settlementId = current.settlement().id();
        recruit.armyGroupId = this.armyGroupId;
        recruit.setRecruitDuty(RecruitDuty.SOLDIER);
        recruit.setRecruitCommand(RecruitmentAction.FOLLOW_OWNER);
        if (!level.addFreshEntity(recruit)) {
            this.cancelCommanderCampaign(level, current, hall, campaign, "spawn_rejected");
            return;
        }
        if (!data.registerRecruit(kingdom.ownerId(), recruit.getUUID())) {
            recruit.discard();
            this.cancelCommanderCampaign(level, current, hall, campaign, "housing_changed");
            return;
        }
        data.replaceCampaign(kingdom.ownerId(), campaign.complete());
    }

    private Optional<BlockPos> findSafeRallyPosition(ServerLevel level, KingdomRecord kingdom) {
        BlockPos hall = new BlockPos(
                kingdom.settlement().hallX(),
                kingdom.settlement().hallY(),
                kingdom.settlement().hallZ());
        for (int radius = 2; radius <= 5; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos candidate = hall.offset(x, 1, z);
                    if (level.isLoaded(candidate)
                            && level.getBlockState(candidate).isAir()
                            && level.getBlockState(candidate.above()).isAir()
                            && level.getBlockState(candidate.below()).blocksMotion()) {
                        return Optional.of(candidate.immutable());
                    }
                }
            }
        }
        return Optional.empty();
    }

    private void cancelActiveCommanderCampaign(String reason) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        EntityReference<LivingEntity> owner = this.getOwnerReference();
        if (owner == null) {
            return;
        }
        KingdomSavedData data = KingdomSavedData.get(serverLevel);
        data.kingdomForOwner(owner.getUUID()).ifPresent(kingdom -> {
            kingdom.settlement().recruitmentCampaigns().stream()
                    .filter(RecruitmentCampaign::active)
                    .findFirst()
                    .ifPresent(campaign -> {
                        if (data.replaceCampaign(kingdom.ownerId(), campaign.cancel(reason))) {
                            this.findKingdomHall(serverLevel, kingdom).ifPresent(
                                    hall -> data.applyPendingCampaignRefunds(kingdom.ownerId(), hall::refundEmeralds));
                        }
                    });
        });
    }

    private void cancelCommanderCampaign(
            ServerLevel level,
            KingdomRecord kingdom,
            KingdomHallBlockEntity hall,
            RecruitmentCampaign campaign,
            String reason
    ) {
        KingdomSavedData data = KingdomSavedData.get(level);
        if (data.replaceCampaign(kingdom.ownerId(), campaign.cancel(reason))) {
            data.applyPendingCampaignRefunds(kingdom.ownerId(), hall::refundEmeralds);
        }
    }

    private Optional<KingdomHallBlockEntity> findKingdomHall(ServerLevel level, KingdomRecord kingdom) {
        if (!KingdomSavedData.get(level).isHallActive(kingdom.ownerId())) {
            return Optional.empty();
        }
        if (!kingdom.settlement().dimensionId().equals(level.dimension().identifier().toString())) {
            return Optional.empty();
        }
        BlockPos hallPos = new BlockPos(
                kingdom.settlement().hallX(),
                kingdom.settlement().hallY(),
                kingdom.settlement().hallZ());
        if (!level.isLoaded(hallPos)) {
            return Optional.empty();
        }
        BlockEntity blockEntity = level.getBlockEntity(hallPos);
        return blockEntity instanceof KingdomHallBlockEntity hall ? Optional.of(hall) : Optional.empty();
    }

    private boolean hasPaidSettlementUpkeep() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return true;
        }
        EntityReference<LivingEntity> owner = this.getOwnerReference();
        if (owner == null) {
            return false;
        }
        return KingdomSavedData.get(serverLevel).kingdomForOwner(owner.getUUID())
                .flatMap(kingdom -> this.findKingdomHall(serverLevel, kingdom))
                .map(KingdomHallBlockEntity::upkeepPaid)
                .orElse(false);
    }

    private String recruitFactionId() {
        if (this.getType() == ModEntityTypes.ROHAN_RECRUIT.get()) {
            return "kingdomwarsmiddleearth:rohan";
        }
        if (this.getType() == ModEntityTypes.MORDOR_ORC_RECRUIT.get()) {
            return "kingdomwarsmiddleearth:mordor";
        }
        if (this.getType() == ModEntityTypes.DWARF_RECRUIT.get()) {
            return "kingdomwarsmiddleearth:dwarf";
        }
        if (this.getType() == ModEntityTypes.ELF_RECRUIT.get()) {
            return "kingdomwarsmiddleearth:elf";
        }
        return "kingdomwarsmiddleearth:gondor";
    }

    private String recruitUnitId() {
        return switch (this.recruitFactionId()) {
            case "kingdomwarsmiddleearth:rohan" -> "kingdomwarsmiddleearth:rohan_rider";
            case "kingdomwarsmiddleearth:mordor" -> "kingdomwarsmiddleearth:mordor_orc";
            case "kingdomwarsmiddleearth:dwarf" -> "kingdomwarsmiddleearth:dwarf_warrior";
            case "kingdomwarsmiddleearth:elf" -> "kingdomwarsmiddleearth:elf_guard";
            default -> "kingdomwarsmiddleearth:gondor_soldier";
        };
    }

    private static Set<String> alliedFactionsFor(String factionId) {
        return switch (factionId) {
            case "kingdomwarsmiddleearth:gondor" -> Set.of("kingdomwarsmiddleearth:rohan");
            case "kingdomwarsmiddleearth:rohan" -> Set.of("kingdomwarsmiddleearth:gondor");
            default -> Set.of();
        };
    }

    private void tameForContract(ServerPlayer player) {
        if (player.connection == null) {
            this.setOwnerReference(EntityReference.of(player));
            this.setTame(true, true);
            return;
        }
        this.tame(player);
    }

    private static void sendFeedback(ServerPlayer player, Component message) {
        if (player.connection != null) {
            player.sendSystemMessage(message);
        }
    }

    private void sendCampaignRejection(@Nullable ServerPlayer player, String reason) {
        if (player != null) {
            sendFeedback(player, Component.translatable(
                    "message.kingdomwarsmiddleearth.recruit.commander.campaign_rejected",
                    reason));
        }
    }

    private void setRecruitDuty(RecruitDuty duty) {
        this.entityData.set(DATA_RECRUIT_DUTY, duty.ordinal());
    }

    private void linkLoadedSoldiersToCommander(ServerLevel level, UUID ownerId) {
        if (this.armyGroupId == null) {
            return;
        }
        for (MiddleEarthRecruitEntity recruit : level.getEntitiesOfClass(
                MiddleEarthRecruitEntity.class,
                this.getBoundingBox().inflate(128.0),
                recruit -> recruit != this && recruit.getRecruitDuty() == RecruitDuty.SOLDIER)) {
            EntityReference<LivingEntity> recruitOwner = recruit.getOwnerReference();
            if (recruitOwner != null
                    && ownerId.equals(recruitOwner.getUUID())
                    && (this.kingdomId == null || this.kingdomId.equals(recruit.kingdomId))) {
                recruit.armyGroupId = this.armyGroupId;
            }
        }
    }

    private void applyCommanderOrderToLoadedGroup(RecruitmentAction action, @Nullable BlockPos target) {
        if (this.getRecruitDuty() != RecruitDuty.COMMANDER
                || this.armyGroupId == null
                || !(this.level() instanceof ServerLevel serverLevel)
                || this.getOwnerReference() == null) {
            return;
        }
        UUID ownerId = this.getOwnerReference().getUUID();
        this.linkLoadedSoldiersToCommander(serverLevel, ownerId);
        for (MiddleEarthRecruitEntity recruit : serverLevel.getEntitiesOfClass(
                MiddleEarthRecruitEntity.class,
                this.getBoundingBox().inflate(128.0),
                recruit -> recruit != this
                        && recruit.getRecruitDuty() == RecruitDuty.SOLDIER
                        && this.armyGroupId.equals(recruit.armyGroupId))) {
            EntityReference<LivingEntity> recruitOwner = recruit.getOwnerReference();
            if (recruitOwner == null || !ownerId.equals(recruitOwner.getUUID())) {
                continue;
            }
            recruit.moveTarget = action == RecruitmentAction.HOLD_POSITION
                    ? recruit.blockPosition()
                    : target;
            recruit.setRecruitCommand(action);
        }
    }

    private void migrateLegacyKingdomLink() {
        if (this.kingdomId != null || !this.isTame() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        EntityReference<LivingEntity> owner = this.getOwnerReference();
        if (owner == null) {
            return;
        }
        UUID ownerId = owner.getUUID();
        KingdomSavedData data = KingdomSavedData.get(serverLevel);
        Optional<KingdomRecord> kingdom = data.kingdomForOwner(ownerId);
        if (kingdom.isEmpty()) {
            return;
        }
        if (!kingdom.get().settlement().containsRecruit(this.getUUID())
                && !data.registerRecruit(ownerId, this.getUUID())) {
            return;
        }
        KingdomRecord linked = data.kingdomForOwner(ownerId).orElseThrow();
        this.kingdomId = linked.id();
        this.settlementId = linked.settlement().id();
    }

    private void setRecruitCommand(RecruitmentAction command) {
        this.entityData.set(DATA_COMMAND, command.ordinal());
        this.setOrderedToSit(command == RecruitmentAction.HOLD_POSITION || command == RecruitmentAction.CLEAR_TARGET);
        if (command != RecruitmentAction.MOVE_TO_POSITION
                && command != RecruitmentAction.WORK_AT_SITE
                && command != RecruitmentAction.HOLD_POSITION) {
            this.moveTarget = null;
        }
        if (command != RecruitmentAction.ATTACK_TARGET) {
            this.setTarget(null);
        }
        this.syncRecruitStatusState();
    }

    private void resumeWorkAfterProfessionAssignment() {
        if (this.workTarget != null) {
            this.moveTarget = this.workTarget;
            this.setRecruitCommand(RecruitmentAction.WORK_AT_SITE);
            return;
        }
        this.setRecruitCommand(RecruitmentAction.FOLLOW_OWNER);
    }

    private void setWorkTarget(@Nullable BlockPos target) {
        this.workTarget = target;
        this.entityData.set(DATA_WORK_TARGET, Optional.ofNullable(target));
        this.syncRecruitStatusState();
    }

    private void setStorageTarget(@Nullable BlockPos target) {
        this.storageTarget = target;
        this.entityData.set(DATA_STORAGE_TARGET, Optional.ofNullable(target));
        this.syncRecruitStatusState();
    }

    private void setBaseTarget(@Nullable BlockPos target) {
        this.baseTarget = target;
        this.entityData.set(DATA_BASE_TARGET, Optional.ofNullable(target));
        this.syncRecruitStatusState();
    }

    private void adjustWorkRadius(int delta) {
        this.setWorkRadius(this.workRadius + delta);
    }

    private void setWorkRadius(int radius) {
        this.workRadius = Math.max(MIN_WORK_RADIUS, Math.min(MAX_WORK_RADIUS, radius));
        this.entityData.set(DATA_WORK_RADIUS, this.workRadius);
        this.syncRecruitStatusState();
    }

    private void syncRecruitStatusState() {
        this.entityData.set(DATA_WORK_TARGET, Optional.ofNullable(this.workTarget));
        this.entityData.set(DATA_STORAGE_TARGET, Optional.ofNullable(this.storageTarget));
        this.entityData.set(DATA_BASE_TARGET, Optional.ofNullable(this.baseTarget));
        this.entityData.set(DATA_STORAGE_RESOURCE_COUNT, this.storageItemCount());
        this.entityData.set(DATA_CARRIED_RESOURCE_COUNT,
                this.workerInventory.stream().mapToInt(ItemStack::getCount).sum());
        this.entityData.set(DATA_BASE_PROGRESS, this.starterBaseCompletedBlocks);
        this.entityData.set(DATA_WORK_RADIUS, this.workRadius);
        this.entityData.set(DATA_WORKER_PHASE, this.workerPhase.id());
        this.entityData.set(DATA_WORKER_REASON, this.workerReason);
        this.entityData.set(DATA_ACTIVE_WORK_TARGET, Optional.ofNullable(this.activeWorkTarget));
        this.entityData.set(DATA_SELECTED_BLUEPRINT, this.selectedBlueprintId);
        this.entityData.set(DATA_RESOURCE_ACTION, this.getWorkerProfession().isPresent() ? this.workerPhase.id() : "");
        this.entityData.set(DATA_RESOURCE_ITEM, this.getWorkerProfession().isPresent() ? this.workerReason : "");
    }

    private @Nullable BlockPos displayedTarget(
            EntityDataAccessor<Optional<BlockPos>> accessor,
            @Nullable BlockPos serverValue
    ) {
        if (!this.level().isClientSide()) {
            return serverValue;
        }
        return this.entityData.get(accessor).orElse(null);
    }

    private Optional<ResourceStatus> displayedResourceStatus() {
        if (this.level().isClientSide()) {
            String action = this.entityData.get(DATA_RESOURCE_ACTION);
            if (action.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new ResourceStatus(action, this.entityData.get(DATA_RESOURCE_ITEM)));
        }
        return this.getWorkerProfession().isEmpty()
                ? Optional.empty()
                : Optional.of(new ResourceStatus(this.workerPhase.id(), this.workerReason));
    }

    private int storageItemCount() {
        if (this.storageTarget == null) {
            return 0;
        }
        return this.findContainer(this.storageTarget)
                .map(container -> java.util.stream.IntStream.range(0, container.getContainerSize())
                        .map(slot -> container.getItem(slot).getCount())
                        .sum())
                .orElse(0);
    }

    private ResourceInventory workerInventorySnapshot() {
        return resourceSnapshot(this.workerInventory);
    }

    private ResourceInventory storageInventorySnapshot() {
        if (this.storageTarget == null) {
            return ResourceInventory.empty();
        }
        return this.findContainer(this.storageTarget)
                .map(container -> {
                    ArrayList<ItemStack> stacks = new ArrayList<>(container.getContainerSize());
                    for (int slot = 0; slot < container.getContainerSize(); slot++) {
                        stacks.add(container.getItem(slot));
                    }
                    return resourceSnapshot(stacks);
                })
                .orElseGet(ResourceInventory::empty);
    }

    private static ResourceInventory resourceSnapshot(Iterable<ItemStack> stacks) {
        ResourceInventory resources = ResourceInventory.empty();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                resources = resources.withAdded(
                        BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                        stack.getCount());
            }
        }
        return resources;
    }

    private static Optional<BlockPos> targetedBlock(ServerPlayer player) {
        HitResult hit = player.pick(8.0, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
            return Optional.empty();
        }
        return Optional.of(blockHit.getBlockPos().immutable());
    }

    private Optional<WorkerWorksite> createWorksite(WorkerProfession profession) {
        if (this.workTarget == null) {
            return Optional.empty();
        }
        return WorkerProfessionCatalog.definition(profession)
                .map(WorkerProfessionDefinition::workAreaType)
                .map(area -> new WorkerWorksite(
                        area,
                        this.workTarget.getX(),
                        this.workTarget.getY(),
                        this.workTarget.getZ(),
                        this.workRadius));
    }

    private Optional<WorkerLogisticsRoute> createCourierRoute() {
        if (this.storageTarget == null || this.workTarget == null) {
            return Optional.empty();
        }
        return Optional.of(new WorkerLogisticsRoute(
                new WorkerWorksite(
                        WorkAreaType.STORAGE,
                        this.storageTarget.getX(),
                        this.storageTarget.getY(),
                        this.storageTarget.getZ(),
                        8),
                new WorkerWorksite(
                        WorkAreaType.COURIER_ROUTE,
                        this.workTarget.getX(),
                        this.workTarget.getY(),
                        this.workTarget.getZ(),
                        this.workRadius)));
    }

    private void applyWorkerEquipment(WorkerProfession profession) {
        ItemStack heldItem = switch (profession) {
            case FARMER -> new ItemStack(Items.IRON_HOE);
            case LUMBERJACK -> new ItemStack(Items.IRON_AXE);
            case FISHERMAN -> new ItemStack(Items.FISHING_ROD);
            case ANIMAL_FARMER -> new ItemStack(Items.WHEAT);
            case MINER -> new ItemStack(Items.IRON_PICKAXE);
            case BUILDER -> new ItemStack(Items.BRICKS);
            case COOK -> new ItemStack(Items.BREAD);
            case MERCHANT -> new ItemStack(Items.EMERALD);
            case COURIER -> new ItemStack(Items.CHEST);
        };
        this.setItemSlot(EquipmentSlot.MAINHAND, heldItem);
    }

    private static RecruitmentAction parseCommand(String value) {
        try {
            return RecruitmentAction.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return RecruitmentAction.FOLLOW_OWNER;
        }
    }

    private static Component targetLabel(@Nullable BlockPos target) {
        if (target == null) {
            return Component.translatable("screen.kingdomwarsmiddleearth.recruit.status.none");
        }
        return Component.literal(target.getX() + ", " + target.getY() + ", " + target.getZ());
    }

    private static Optional<Block> blockForPlacement(String blockId) {
        return switch (blockId) {
            case "kingdomwarsmiddleearth:middle_earth_stone" -> Optional.of(ModBlocks.MIDDLE_EARTH_STONE.get());
            case "kingdomwarsmiddleearth:mallorn_log" -> Optional.of(ModBlocks.MALLORN_LOG.get());
            case "minecraft:oak_planks" -> Optional.of(Blocks.OAK_PLANKS);
            case "minecraft:oak_log" -> Optional.of(Blocks.OAK_LOG);
            case "minecraft:dirt" -> Optional.of(Blocks.DIRT);
            case "minecraft:chest" -> Optional.of(Blocks.CHEST);
            default -> Optional.empty();
        };
    }

    private record ResourceStatus(String action, String itemId) {
    }
}
