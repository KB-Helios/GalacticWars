package galacticwars.clonewars.vehicle;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import galacticwars.clonewars.progression.LaunchContentCatalog;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.registry.ModItems;
import java.util.Optional;
import java.util.UUID;
import java.util.LinkedHashSet;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import galacticwars.clonewars.combat.BlasterBoltEntity;

/** Server-driven vehicle shared by the five launch chassis. */
public final class GalacticVehicleEntity extends Entity implements GeoEntity {
    private static final int DURACRETE_REPAIR_AMOUNT = 12;
    private static final EntityDataAccessor<String> OWNER = SynchedEntityData.defineId(
            GalacticVehicleEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> FACTION = SynchedEntityData.defineId(
            GalacticVehicleEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(
            GalacticVehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HEALTH = SynchedEntityData.defineId(
            GalacticVehicleEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.vehicle.idle");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final LinkedHashSet<UUID> processedInputIds = new LinkedHashSet<>();
    private float inputForward;
    private float inputStrafe;
    private boolean inputAscend;
    private boolean inputDescend;
    private boolean inputFire;
    private UUID firingPassengerId;
    private long inputExpiresGameTime;
    private long weaponReadyGameTime;

    public GalacticVehicleEntity(EntityType<? extends GalacticVehicleEntity> type, Level level) {
        super(type, level);
    }

    public void deploy(UUID ownerId, String factionId) {
        entityData.set(OWNER, ownerId.toString());
        entityData.set(FACTION, factionId);
        entityData.set(FUEL, fuelCapacity());
        entityData.set(HEALTH, maxVehicleHealth());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, "");
        builder.define(FACTION, "");
        builder.define(FUEL, 0);
        builder.define(HEALTH, 1);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.storeNullable("Owner", UUIDUtil.CODEC, ownerId().orElse(null));
        output.putString("Faction", factionId());
        output.putInt("Fuel", fuel());
        output.putInt("Health", health());
        output.putInt("VehicleDataVersion", 1);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        entityData.set(OWNER, input.read("Owner", UUIDUtil.CODEC).map(UUID::toString).orElse(""));
        entityData.set(FACTION, input.getStringOr("Faction", ""));
        entityData.set(FUEL, Math.max(0, Math.min(fuelCapacity(), input.getIntOr("Fuel", 0))));
        entityData.set(HEALTH, Math.max(0, Math.min(maxVehicleHealth(), input.getIntOr("Health", 1))));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (health() <= 0) {
            destroyVehicle();
            return;
        }
        Entity controlling = getFirstPassenger();
        if (!(controlling instanceof LivingEntity driver) || fuel() <= 0) {
            setDeltaMovement(getDeltaMovement().scale(0.75D).add(0.0D, -0.04D, 0.0D));
            move(MoverType.SELF, getDeltaMovement());
            return;
        }
        setYRot(driver.getYRot());
        boolean inputCurrent = level().getGameTime() <= inputExpiresGameTime;
        float forward = inputCurrent ? inputForward : 0.0F;
        float strafe = inputCurrent ? inputStrafe : 0.0F;
        Vec3 facing = Vec3.directionFromRotation(0.0F, getYRot());
        Vec3 sideways = new Vec3(-facing.z, 0.0D, facing.x);
        var definition = definition();
        double speed = definition == null ? 0.18D : definition.maxSpeed();
        double strafeMultiplier = definition == null ? 0.65D : definition.strafeMultiplier();
        Vec3 motion = facing.scale(forward * speed)
                .add(sideways.scale(strafe * speed * strafeMultiplier));
        if (movement().equals("flight")) {
            double vertical = definition == null ? 0.28D : definition.verticalSpeed();
            motion = motion.add(0.0D, inputAscend ? vertical : 0.0D, 0.0D);
            if (inputDescend) motion = motion.add(0.0D, -vertical, 0.0D);
        } else if (!onGround()) {
            motion = motion.add(0.0D, movement().equals("hover") ? -0.02D : -0.08D, 0.0D);
        }
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
        if (horizontalCollision && motion.horizontalDistanceSqr() > 0.08D) {
            damageVehicle(1);
            if (isRemoved()) return;
        }
        int fuelRate = definition == null ? 5 : definition.fuelRateTicks();
        if (motion.horizontalDistanceSqr() > 0.0001D && tickCount % fuelRate == 0) {
            entityData.set(FUEL, Math.max(0, fuel() - 1));
        }
        LivingEntity weaponOperator = driver;
        if (firingPassengerId != null && level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(firingPassengerId) instanceof LivingEntity passenger
                && passenger.getVehicle() == this) {
            weaponOperator = passenger;
        }
        if (inputFire) fireWeapon(weaponOperator);
        inputFire = false;
        firingPassengerId = null;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.ENERGY_CELL.get()) && fuel() < fuelCapacity()) {
            if (!canBoard(player)) return InteractionResult.FAIL;
            if (!level().isClientSide()) {
                entityData.set(FUEL, Math.min(fuelCapacity(), fuel() + 250));
                if (!player.hasInfiniteMaterials()) held.shrink(1);
                player.sendOverlayMessage(Component.translatable(
                        "message.galacticwars.vehicle.refueled", fuel(), fuelCapacity()));
            }
            return InteractionResult.SUCCESS;
        }
        if (held.is(ModItems.DURACRETE.get()) && health() < maxVehicleHealth()) {
            if (!canBoard(player)) return InteractionResult.FAIL;
            if (!level().isClientSide()) {
                entityData.set(HEALTH, Math.min(
                        maxVehicleHealth(), health() + DURACRETE_REPAIR_AMOUNT));
                if (!player.hasInfiniteMaterials()) held.shrink(1);
                player.sendOverlayMessage(Component.translatable(
                        "message.galacticwars.vehicle.repaired", health(), maxVehicleHealth()));
            }
            return InteractionResult.SUCCESS;
        }
        if (!canBoard(player)) return InteractionResult.FAIL;
        if (!level().isClientSide()) player.startRiding(this);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player && canBoard(player)) {
            return false;
        }
        damageVehicle(Math.max(1, (int) Math.ceil(amount)));
        return true;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < seatCount();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    public Optional<UUID> ownerId() {
        try {
            return entityData.get(OWNER).isEmpty()
                    ? Optional.empty() : Optional.of(UUID.fromString(entityData.get(OWNER)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
    public String factionId() { return entityData.get(FACTION); }
    public int fuel() { return entityData.get(FUEL); }
    public int health() { return entityData.get(HEALTH); }
    public int maxHealthForHud() { return maxVehicleHealth(); }
    public int fuelCapacityForHud() { return fuelCapacity(); }
    public String vehicleId() { return BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath(); }

    public void damageVehicle(int amount) {
        if (amount <= 0 || isRemoved()) return;
        entityData.set(HEALTH, Math.max(0, health() - amount));
        if (health() <= 0 && !level().isClientSide()) {
            destroyVehicle();
        }
    }

    private void destroyVehicle() {
        ejectPassengers();
        discard();
    }

    public boolean applyInput(
            ServerPlayer player, UUID replayId, float forward, float strafe,
            boolean ascend, boolean descend, boolean fire
    ) {
        int seat = getPassengers().indexOf(player);
        String role = seatRole(seat);
        boolean driver = role.equals("driver") && canBoard(player);
        boolean gunner = role.equals("gunner");
        if ((!driver && !gunner) || !Float.isFinite(forward) || !Float.isFinite(strafe)
                || Math.abs(forward) > 1.01F || Math.abs(strafe) > 1.01F
                || !processedInputIds.add(replayId)) return false;
        while (processedInputIds.size() > 64) processedInputIds.remove(processedInputIds.iterator().next());
        if (driver) {
            inputForward = forward;
            inputStrafe = strafe;
            inputAscend = ascend;
            inputDescend = descend;
            inputExpiresGameTime = level().getGameTime() + 5L;
        }
        boolean driverCanFire = definition() == null || !definition().seatRoles().contains("gunner");
        if (fire && (gunner || driverCanFire)) {
            inputFire = true;
            firingPassengerId = player.getUUID();
        }
        return true;
    }

    private void fireWeapon(LivingEntity driver) {
        if (!(level() instanceof ServerLevel serverLevel)
                || level().getGameTime() < weaponReadyGameTime || fuel() < 5) return;
        Vec3 direction = driver.getLookAngle();
        String weapon = definition() == null ? "light_blaster" : definition().weaponEffect();
        double damage = weapon.equals("heavy_blaster") || weapon.equals("gunship_blaster") ? 10.0D
                : weapon.equals("medium_blaster") ? 8.0D : 6.0D;
        BlasterBoltEntity bolt = new BlasterBoltEntity(
                serverLevel, driver, mountedWeaponItem(), damage);
        bolt.setPos(driver.getEyePosition().add(direction.scale(1.2D)));
        bolt.shoot(direction.x, direction.y, direction.z, 3.8F, 0.25F);
        if (serverLevel.addFreshEntity(bolt)) {
            entityData.set(FUEL, fuel() - 5);
            weaponReadyGameTime = level().getGameTime() + (weapon.equals("heavy_blaster") ? 16L : 8L);
        }
    }

    private ItemStack mountedWeaponItem() {
        return new ItemStack(switch (factionId()) {
            case "galacticwars:separatist" -> ModItems.E5_BLASTER.get();
            case "galacticwars:mandalorian" -> ModItems.WESTAR_BLASTER.get();
            case "galacticwars:hutt_cartel" -> ModItems.SCATTER_BLASTER.get();
            default -> ModItems.DC15_BLASTER.get();
        });
    }

    private boolean canBoard(Player player) {
        if (ownerId().filter(player.getUUID()::equals).isPresent()) return true;
        if (!(level() instanceof ServerLevel serverLevel)) return true;
        String playerFaction = ProgressionSavedData.get(serverLevel).state(player.getUUID()).factionId();
        if (!playerFaction.isEmpty() && playerFaction.equals(factionId())) return true;
        var kingdoms = galacticwars.clonewars.kingdom.KingdomSavedData.get(serverLevel);
        var ownerKingdom = ownerId().flatMap(kingdoms::kingdomForPlayer).orElse(null);
        var playerKingdom = kingdoms.kingdomForPlayer(player.getUUID()).orElse(null);
        return ownerKingdom != null && playerKingdom != null
                && kingdoms.effectiveRelation(ownerKingdom.id(), playerKingdom.id(), serverLevel.getGameTime())
                == galacticwars.clonewars.kingdom.KingdomRelation.ALLY;
    }

    public boolean canTransferPassenger(ServerPlayer player) {
        return canBoard(player);
    }

    private int seatCount() {
        var definition = definition();
        return definition == null ? 1 : definition.seats();
    }

    private String seatRole(int seat) {
        var definition = definition();
        return definition == null || seat < 0 || seat >= definition.seatRoles().size()
                ? "passenger" : definition.seatRoles().get(seat);
    }

    private int fuelCapacity() {
        var definition = LaunchContentCatalog.data().vehicles().get(vehicleId());
        return definition == null ? 1000 : definition.fuelCapacity();
    }

    private int maxVehicleHealth() {
        var definition = LaunchContentCatalog.data().vehicles().get(vehicleId());
        return definition == null ? 40 : definition.maxHealth();
    }

    private String movement() {
        var definition = definition();
        return definition == null ? "hover" : definition.movement();
    }

    private galacticwars.clonewars.data.LaunchContentDefinitions.VehicleDefinition definition() {
        return LaunchContentCatalog.data().vehicles().get(vehicleId());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("vehicle", 5, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
