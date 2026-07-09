package middleearth.lotr.warmod.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import middleearth.lotr.warmod.entity.ai.RecruitMoveToCommandGoal;
import middleearth.lotr.warmod.entity.ai.RecruitCompanionGoal;
import middleearth.lotr.warmod.entity.ai.RecruitWorkerGoal;
import middleearth.lotr.warmod.menu.RecruitCommandMenu;
import middleearth.lotr.warmod.menu.RecruitCommandMenuProvider;
import middleearth.lotr.warmod.recruitment.RecruitmentAction;
import middleearth.lotr.warmod.registry.ModBlocks;
import middleearth.lotr.warmod.settlement.BaseBlockPlacement;
import middleearth.lotr.warmod.settlement.KingdomBaseBlueprint;
import middleearth.lotr.warmod.settlement.KingdomBaseBuildAction;
import middleearth.lotr.warmod.settlement.KingdomBaseBuildDecision;
import middleearth.lotr.warmod.settlement.KingdomBaseBuildPlanner;
import middleearth.lotr.warmod.settlement.KingdomSettlementPlanner;
import middleearth.lotr.warmod.settlement.KingdomSettlementState;
import middleearth.lotr.warmod.settlement.KingdomWorkOrder;
import middleearth.lotr.warmod.workforce.ResourceInventory;
import middleearth.lotr.warmod.workforce.WorkAreaType;
import middleearth.lotr.warmod.workforce.WorkerLogisticsDecision;
import middleearth.lotr.warmod.workforce.WorkerLogisticsPlanner;
import middleearth.lotr.warmod.workforce.WorkerLogisticsRoute;
import middleearth.lotr.warmod.workforce.WorkerProfession;
import middleearth.lotr.warmod.workforce.WorkerProfessionCatalog;
import middleearth.lotr.warmod.workforce.WorkerProfessionDefinition;
import middleearth.lotr.warmod.workforce.WorkerResourceAction;
import middleearth.lotr.warmod.workforce.WorkerResourceDecision;
import middleearth.lotr.warmod.workforce.WorkerResourcePlanner;
import middleearth.lotr.warmod.workforce.WorkerTaskDecision;
import middleearth.lotr.warmod.workforce.WorkerTaskPlanner;
import middleearth.lotr.warmod.workforce.WorkerWorksite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MiddleEarthRecruitEntity extends TamableAnimal implements GeoEntity {
    public static final int HIRE_COST_EMERALDS = 25;
    private static final int DEFAULT_WORK_RADIUS = 8;
    private static final int MIN_WORK_RADIUS = 2;
    private static final int MAX_WORK_RADIUS = 32;
    private static final int WORK_RADIUS_STEP = 2;
    private static final List<String> MERCHANT_TRADE_GOODS = List.of(
            "minecraft:wheat",
            "minecraft:cod",
            "minecraft:leather",
            "minecraft:bread",
            "minecraft:oak_log",
            "minecraft:cobblestone");

    private static final EntityDataAccessor<Integer> DATA_COMMAND =
            SynchedEntityData.defineId(MiddleEarthRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WORKER_PROFESSION =
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

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable BlockPos moveTarget;
    private @Nullable BlockPos workTarget;
    private @Nullable BlockPos storageTarget;
    private @Nullable BlockPos baseTarget;
    private ResourceInventory carriedResources = ResourceInventory.empty();
    private ResourceInventory storageResources = ResourceInventory.empty();
    private int starterBaseCompletedBlocks;
    private int workRadius = DEFAULT_WORK_RADIUS;

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
        entityData.define(DATA_WORK_TARGET, Optional.empty());
        entityData.define(DATA_STORAGE_TARGET, Optional.empty());
        entityData.define(DATA_BASE_TARGET, Optional.empty());
        entityData.define(DATA_STORAGE_RESOURCE_COUNT, 0);
        entityData.define(DATA_CARRIED_RESOURCE_COUNT, 0);
        entityData.define(DATA_BASE_PROGRESS, 0);
        entityData.define(DATA_WORK_RADIUS, DEFAULT_WORK_RADIUS);
        entityData.define(DATA_RESOURCE_ACTION, "");
        entityData.define(DATA_RESOURCE_ITEM, "");
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
        this.getWorkerProfession().ifPresent(profession -> output.putString("WorkerProfession", profession.id()));
        output.putString("WorkerCarriedResources", encodeResources(this.carriedResources));
        output.putString("WorkerStorageResources", encodeResources(this.storageResources));
        output.putInt("StarterBaseCompletedBlocks", this.starterBaseCompletedBlocks);
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
        WorkerProfession.byId(input.getStringOr("WorkerProfession", ""))
                .ifPresentOrElse(this::setWorkerProfession, () -> this.entityData.set(DATA_WORKER_PROFESSION, -1));
        this.carriedResources = decodeResources(input.getStringOr("WorkerCarriedResources", ""));
        this.storageResources = decodeResources(input.getStringOr("WorkerStorageResources", ""));
        this.starterBaseCompletedBlocks = Math.max(0, input.getIntOr("StarterBaseCompletedBlocks", 0));
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
                : this.storageResources.totalCount();
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
        if (displayedBaseTarget != null) {
            lines.add(Component.translatable(
                    "screen.kingdomwarsmiddleearth.recruit.status.base_progress",
                    displayedBaseProgress,
                    KingdomBaseBlueprint.starterKeep().placements().size()));
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
        if (buttonId == RecruitCommandMenu.BUTTON_HIRE) {
            return this.tryHire(player);
        }
        if (!this.isOwnedBy(player)) {
            player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.not_owner"));
            return false;
        }
        Optional<WorkerProfession> profession = WorkerProfessionCatalog.professionForButton(buttonId);
        if (profession.isPresent()) {
            this.setWorkerProfession(profession.get());
            this.resumeWorkAfterProfessionAssignment();
            player.sendSystemMessage(Component.translatable(
                    "message.kingdomwarsmiddleearth.recruit.profession",
                    Component.translatable(profession.get().translationKey())));
            return true;
        }

        return switch (buttonId) {
            case RecruitCommandMenu.BUTTON_FOLLOW -> {
                this.setRecruitCommand(RecruitmentAction.FOLLOW_OWNER);
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.follow"));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_HOLD -> {
                this.moveTarget = this.blockPosition();
                this.setRecruitCommand(RecruitmentAction.HOLD_POSITION);
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.hold"));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_MOVE -> {
                this.moveTarget = player.blockPosition();
                this.setRecruitCommand(RecruitmentAction.MOVE_TO_POSITION);
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.move"));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_SET_WORKSITE -> {
                if (this.getWorkerProfession().isEmpty()) {
                    player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.worksite.missing_profession"));
                    yield false;
                }
                this.setWorkTarget(player.blockPosition());
                this.moveTarget = this.workTarget;
                this.setRecruitCommand(RecruitmentAction.WORK_AT_SITE);
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
                this.moveTarget = this.workTarget;
                this.setRecruitCommand(RecruitmentAction.WORK_AT_SITE);
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.worksite.return"));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_CLEAR_WORKSITE -> {
                this.setWorkTarget(null);
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
                this.setStorageTarget(player.blockPosition());
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.storage.set"));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_BUILD_STARTER_KEEP -> {
                this.setWorkerProfession(WorkerProfession.BUILDER);
                this.setBaseTarget(player.blockPosition());
                this.setWorkTarget(this.baseTarget);
                this.moveTarget = this.baseTarget;
                this.setRecruitCommand(RecruitmentAction.WORK_AT_SITE);
                KingdomBaseBuildDecision buildDecision = this.planStarterBaseBuild().orElseThrow();
                WorkerResourceDecision resourceDecision = this.planResourceDecision().orElseThrow();
                player.sendSystemMessage(Component.translatable(
                        "message.kingdomwarsmiddleearth.recruit.base.starter_keep",
                        Component.translatable("screen.kingdomwarsmiddleearth.recruit.basebuild."
                                + buildDecision.action().name().toLowerCase()),
                        Component.translatable("screen.kingdomwarsmiddleearth.recruit.workaction."
                                + resourceDecision.action().name().toLowerCase())));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_PROTECT -> {
                this.setRecruitCommand(RecruitmentAction.PROTECT_OWNER);
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.protect"));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_ATTACK -> {
                this.setRecruitCommand(RecruitmentAction.ATTACK_TARGET);
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.attack"));
                yield true;
            }
            case RecruitCommandMenu.BUTTON_CLEAR -> {
                this.setTarget(null);
                this.setRecruitCommand(RecruitmentAction.CLEAR_TARGET);
                player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.clear"));
                yield true;
            }
            default -> false;
        };
    }

    public boolean shouldMoveToCommandTarget() {
        return this.isTame()
                && (this.getRecruitCommand() == RecruitmentAction.MOVE_TO_POSITION
                || this.getRecruitCommand() == RecruitmentAction.WORK_AT_SITE)
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
        this.applyWorkerEquipment(profession);
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
                    this.carriedResources,
                    this.storageResources,
                    128);
        });
    }

    public Optional<WorkerLogisticsDecision> planCourierLogistics() {
        if (this.getWorkerProfession().filter(profession -> profession == WorkerProfession.COURIER).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(WorkerLogisticsPlanner.planAnyAvailableSupply(
                this.createCourierRoute().orElse(null),
                this.carriedResources,
                this.storageResources,
                128));
    }

    public Optional<KingdomBaseBuildDecision> planStarterBaseBuild() {
        if (this.baseTarget == null) {
            return Optional.empty();
        }
        return Optional.of(KingdomBaseBuildPlanner.planNext(
                KingdomBaseBlueprint.starterKeep(),
                this.storageResources,
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
                        this.storageResources,
                        workers,
                        population,
                        Math.max(1, population + 1),
                        this.starterBaseCompletedBlocks,
                        true),
                KingdomBaseBlueprint.starterKeep()));
    }

    public boolean shouldRunWorkerCycle() {
        return this.isTame()
                && this.getRecruitCommand() == RecruitmentAction.WORK_AT_SITE
                && this.getWorkerProfession().isPresent()
                && this.workTarget != null;
    }

    public boolean performWorkerCycle() {
        if (!this.shouldRunWorkerCycle() || this.level().isClientSide()) {
            return false;
        }
        if (this.getWorkerProfession().orElseThrow() == WorkerProfession.BUILDER && this.baseTarget != null) {
            KingdomBaseBuildDecision buildDecision = this.planStarterBaseBuild().orElseThrow();
            if (buildDecision.action() == KingdomBaseBuildAction.PLACE_BLOCK) {
                return this.completeNextStarterBaseBlock(buildDecision);
            }
            if (buildDecision.action() == KingdomBaseBuildAction.GATHER_SUPPLIES) {
                this.carriedResources = this.carriedResources.withAdded(buildDecision.itemId(), 1);
                this.depositCarriedResources();
                this.syncRecruitStatusState();
                return true;
            }
            return false;
        }
        if (this.getWorkerProfession().orElseThrow() == WorkerProfession.COURIER) {
            WorkerLogisticsDecision decision = this.planCourierLogistics().orElseThrow();
            if (decision.action() == WorkerResourceAction.WITHDRAW_FROM_STORAGE) {
                this.storageResources = this.storageResources.withRemoved(decision.itemId(), decision.quantity());
                this.carriedResources = this.carriedResources.withAdded(decision.itemId(), decision.quantity());
                this.syncRecruitStatusState();
                return true;
            }
            if (decision.action() == WorkerResourceAction.DELIVER_TO_WORKSITE) {
                this.carriedResources = this.carriedResources.withRemoved(decision.itemId(), decision.quantity());
                this.syncRecruitStatusState();
                return true;
            }
            if (decision.action() == WorkerResourceAction.DEPOSIT_TO_STORAGE) {
                this.depositCarriedResources();
                this.syncRecruitStatusState();
                return true;
            }
            return false;
        }

        WorkerResourceDecision decision = this.planResourceDecision().orElseThrow();
        if (decision.action() == WorkerResourceAction.GATHER_RESOURCE) {
            WorkerProfession profession = this.getWorkerProfession().orElseThrow();
            if (this.tryPerformProfessionWork(profession, decision)) {
                return true;
            }
            if (requiresConcreteProfessionWork(profession)) {
                return false;
            }
            this.carriedResources = this.carriedResources.withAdded(decision.itemId(), decision.quantity());
            this.syncRecruitStatusState();
            return true;
        }
        if (decision.action() == WorkerResourceAction.DEPOSIT_TO_STORAGE) {
            this.depositCarriedResources();
            this.syncRecruitStatusState();
            return true;
        }
        return false;
    }

    private boolean completeNextStarterBaseBlock(KingdomBaseBuildDecision buildDecision) {
        BaseBlockPlacement placement = buildDecision.placement();
        if (placement == null || this.baseTarget == null) {
            return false;
        }
        Optional<Block> block = blockForPlacement(placement.blockId());
        if (block.isEmpty()) {
            return false;
        }
        BlockPos placeAt = this.baseTarget.offset(placement.x(), placement.y(), placement.z());
        BlockState state = this.level().getBlockState(placeAt);
        if (!state.isAir() && !state.canBeReplaced()) {
            return false;
        }
        this.level().setBlock(placeAt, block.get().defaultBlockState(), 3);
        this.storageResources = this.storageResources.withRemoved(placement.itemId(), 1);
        this.starterBaseCompletedBlocks++;
        this.syncRecruitStatusState();
        return true;
    }

    private boolean tryPerformProfessionWork(WorkerProfession profession, WorkerResourceDecision decision) {
        return switch (profession) {
            case FISHERMAN -> this.tryFishAtWorksite(decision);
            case ANIMAL_FARMER -> this.tryGatherFromAnimalPen(decision);
            case COOK -> this.tryCookFromStorage();
            case MERCHANT -> this.tryTradeGoodsForEmeralds();
            default -> this.tryHarvestWorksiteResource(profession, decision);
        };
    }

    private boolean tryFishAtWorksite(WorkerResourceDecision decision) {
        if (this.findWaterTarget().isEmpty()) {
            return false;
        }
        this.carriedResources = this.carriedResources.withAdded(decision.itemId(), decision.quantity());
        this.syncRecruitStatusState();
        return true;
    }

    private boolean tryGatherFromAnimalPen(WorkerResourceDecision decision) {
        if (this.findNearbyAnimal().isEmpty()) {
            return false;
        }
        this.carriedResources = this.carriedResources.withAdded(decision.itemId(), decision.quantity());
        this.syncRecruitStatusState();
        return true;
    }

    private boolean tryCookFromStorage() {
        if (!this.storageResources.hasAtLeast("minecraft:wheat", 3)) {
            return false;
        }
        this.storageResources = this.storageResources.withRemoved("minecraft:wheat", 3);
        this.carriedResources = this.carriedResources.withAdded("minecraft:bread", 1);
        this.syncRecruitStatusState();
        return true;
    }

    private boolean tryTradeGoodsForEmeralds() {
        for (String itemId : MERCHANT_TRADE_GOODS) {
            if (this.storageResources.count(itemId) > 0) {
                this.storageResources = this.storageResources.withRemoved(itemId, 1);
                this.carriedResources = this.carriedResources.withAdded("minecraft:emerald", 1);
                this.syncRecruitStatusState();
                return true;
            }
        }
        return false;
    }

    private boolean tryHarvestWorksiteResource(WorkerProfession profession, WorkerResourceDecision decision) {
        Optional<BlockPos> target = this.findHarvestTarget(profession);
        if (target.isEmpty()) {
            return false;
        }

        BlockPos harvestAt = target.get();
        BlockState state = this.level().getBlockState(harvestAt);
        if (profession == WorkerProfession.FARMER && state.getBlock() instanceof CropBlock cropBlock) {
            this.level().destroyBlock(harvestAt, true, this, 16);
            this.level().setBlock(harvestAt, cropBlock.getStateForAge(0), 3);
        } else {
            this.level().destroyBlock(harvestAt, true, this, 16);
        }
        this.carriedResources = this.carriedResources.withAdded(decision.itemId(), decision.quantity());
        this.syncRecruitStatusState();
        return true;
    }

    private Optional<BlockPos> findHarvestTarget(WorkerProfession profession) {
        if (this.workTarget == null) {
            return Optional.empty();
        }
        int radius = this.worksiteScanRadius();
        BlockPos min = this.workTarget.offset(-radius, -1, -radius);
        BlockPos max = this.workTarget.offset(radius, 3, radius);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = this.level().getBlockState(pos);
            if (isHarvestableFor(profession, state)) {
                return Optional.of(pos.immutable());
            }
        }
        return Optional.empty();
    }

    private Optional<BlockPos> findWaterTarget() {
        if (this.workTarget == null) {
            return Optional.empty();
        }
        int radius = this.worksiteScanRadius();
        BlockPos min = this.workTarget.offset(-radius, -1, -radius);
        BlockPos max = this.workTarget.offset(radius, 1, radius);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (this.level().getBlockState(pos).getBlock() == Blocks.WATER) {
                return Optional.of(pos.immutable());
            }
        }
        return Optional.empty();
    }

    private Optional<Animal> findNearbyAnimal() {
        if (this.workTarget == null) {
            return Optional.empty();
        }
        int radius = this.worksiteScanRadius();
        BlockPos min = this.workTarget.offset(-radius, -1, -radius);
        BlockPos max = this.workTarget.offset(radius, 3, radius);
        AABB animalPen = new AABB(
                min.getX(),
                min.getY(),
                min.getZ(),
                max.getX() + 1.0,
                max.getY() + 1.0,
                max.getZ() + 1.0);
        return this.level().getEntitiesOfClass(Animal.class, animalPen, Animal::isAlive).stream().findFirst();
    }

    private int worksiteScanRadius() {
        return Math.max(MIN_WORK_RADIUS, Math.min(MAX_WORK_RADIUS, this.workRadius));
    }

    private static boolean isHarvestableFor(WorkerProfession profession, BlockState state) {
        Block block = state.getBlock();
        return switch (profession) {
            case LUMBERJACK -> block == Blocks.OAK_LOG || block == ModBlocks.MALLORN_LOG.get();
            case MINER -> block == Blocks.STONE
                    || block == Blocks.COBBLESTONE
                    || block == ModBlocks.MIDDLE_EARTH_STONE.get()
                    || block == ModBlocks.MITHRIL_ORE.get();
            case FARMER -> block == Blocks.WHEAT
                    && block instanceof CropBlock cropBlock
                    && cropBlock.isMaxAge(state);
            default -> false;
        };
    }

    private static boolean requiresConcreteProfessionWork(WorkerProfession profession) {
        return profession == WorkerProfession.FISHERMAN
                || profession == WorkerProfession.ANIMAL_FARMER
                || profession == WorkerProfession.COOK
                || profession == WorkerProfession.MERCHANT;
    }

    private void depositCarriedResources() {
        for (Map.Entry<String, Integer> entry : this.carriedResources.resources().entrySet()) {
            this.storageResources = this.storageResources.withAdded(entry.getKey(), entry.getValue());
        }
        this.carriedResources = ResourceInventory.empty();
        this.syncRecruitStatusState();
    }

    private boolean tryHire(ServerPlayer player) {
        if (this.isTame()) {
            player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.already_hired"));
            return false;
        }
        if (!player.hasInfiniteMaterials() && emeraldCount(player) < HIRE_COST_EMERALDS) {
            player.sendSystemMessage(Component.translatable(
                    "message.kingdomwarsmiddleearth.recruit.need_emeralds",
                    HIRE_COST_EMERALDS));
            return false;
        }
        if (!player.hasInfiniteMaterials()) {
            player.getInventory().clearOrCountMatchingItems(
                    stack -> stack.is(Items.EMERALD),
                    HIRE_COST_EMERALDS,
                    new SimpleContainer(0));
        }

        this.tame(player);
        this.setRecruitCommand(RecruitmentAction.FOLLOW_OWNER);
        this.setTarget(null);
        this.navigation.stop();
        this.level().broadcastEntityEvent(this, (byte) 7);
        player.sendSystemMessage(Component.translatable("message.kingdomwarsmiddleearth.recruit.hired"));
        return true;
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
        this.entityData.set(DATA_STORAGE_RESOURCE_COUNT, this.storageResources.totalCount());
        this.entityData.set(DATA_CARRIED_RESOURCE_COUNT, this.carriedResources.totalCount());
        this.entityData.set(DATA_BASE_PROGRESS, this.starterBaseCompletedBlocks);
        this.entityData.set(DATA_WORK_RADIUS, this.workRadius);
        Optional<WorkerResourceDecision> decision = this.planResourceDecision();
        this.entityData.set(DATA_RESOURCE_ACTION, decision
                .map(value -> value.action().name().toLowerCase())
                .orElse(""));
        this.entityData.set(DATA_RESOURCE_ITEM, decision
                .map(WorkerResourceDecision::itemId)
                .orElse(""));
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
        return this.planResourceDecision()
                .map(decision -> new ResourceStatus(decision.action().name().toLowerCase(), decision.itemId()));
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

    private static int emeraldCount(ServerPlayer player) {
        return player.getInventory().clearOrCountMatchingItems(
                stack -> stack.is(Items.EMERALD),
                0,
                new SimpleContainer(0));
    }

    private static RecruitmentAction parseCommand(String value) {
        try {
            return RecruitmentAction.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return RecruitmentAction.FOLLOW_OWNER;
        }
    }

    private static String encodeResources(ResourceInventory inventory) {
        StringBuilder encoded = new StringBuilder();
        for (Map.Entry<String, Integer> entry : inventory.resources().entrySet()) {
            if (!encoded.isEmpty()) {
                encoded.append(',');
            }
            encoded.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return encoded.toString();
    }

    private static ResourceInventory decodeResources(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return ResourceInventory.empty();
        }
        LinkedHashMap<String, Integer> resources = new LinkedHashMap<>();
        for (String part : encoded.split(",")) {
            String[] pieces = part.split("=", 2);
            if (pieces.length == 2) {
                String itemId = pieces[0].trim();
                if (dropMalformedResourceEntry(itemId)) {
                    continue;
                }
                try {
                    resources.put(itemId, Math.max(0, Integer.parseInt(pieces[1].trim())));
                } catch (NumberFormatException ignored) {
                    resources.remove(itemId);
                }
            }
        }
        try {
            return new ResourceInventory(resources);
        } catch (RuntimeException ignored) {
            return ResourceInventory.empty();
        }
    }

    private static boolean dropMalformedResourceEntry(String itemId) {
        return itemId == null || itemId.isBlank();
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
            default -> Optional.empty();
        };
    }

    private record ResourceStatus(String action, String itemId) {
    }
}
