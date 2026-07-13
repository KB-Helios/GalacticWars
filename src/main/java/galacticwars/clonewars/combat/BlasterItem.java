package galacticwars.clonewars.combat;

import galacticwars.clonewars.registry.ModItems;
import galacticwars.clonewars.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import org.jspecify.annotations.Nullable;

public final class BlasterItem extends Item {
    private final double damage;
    private final float velocity;
    private final float inaccuracy;

    public BlasterItem(Properties properties, double damage, float velocity, float inaccuracy) {
        super(properties.stacksTo(1));
        if (damage <= 0.0D || velocity <= 0.0F || inaccuracy < 0.0F) {
            throw new IllegalArgumentException("Invalid blaster tuning");
        }
        this.damage = damage;
        this.velocity = velocity;
        this.inaccuracy = inaccuracy;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack weapon = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlasterHeatPolicy.BlasterHeatState heat = heat(weapon);
        if (!BlasterHeatPolicy.canFire(heat)) {
            notify(player, Component.translatable(
                    heat.overheatTicks() > 0
                            ? "message.galacticwars.blaster.overheated"
                            : "message.galacticwars.blaster.cooldown"));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.2F);
            return InteractionResult.FAIL;
        }
        if (!player.hasInfiniteMaterials() && !consumeEnergyCell(player)) {
            notify(player, Component.translatable("message.galacticwars.blaster.need_energy_cell"));
            return InteractionResult.FAIL;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.65F,
                1.45F + level.getRandom().nextFloat() * 0.15F);
        ServerLevel serverLevel = (ServerLevel) level;
        BlasterBoltEntity bolt = new BlasterBoltEntity(level, player, weapon, damage);
        bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, velocity, inaccuracy);
        serverLevel.addFreshEntity(bolt);
        weapon.hurtAndBreak(1, player, hand);
        BlasterHeatPolicy.BlasterHeatState updated = BlasterHeatPolicy.afterShot(heat);
        weapon.set(ModDataComponents.BLASTER_HEAT.get(), updated);
        if (updated.overheatTicks() > 0) {
            notify(player, Component.translatable("message.galacticwars.blaster.overheated"));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.75F, 1.6F);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(weapon, BlasterHeatPolicy.SHOT_COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(
            ItemStack itemStack,
            ServerLevel level,
            Entity owner,
            @Nullable EquipmentSlot slot
    ) {
        BlasterHeatPolicy.BlasterHeatState heat = heat(itemStack);
        if (!heat.isReady()) {
            itemStack.set(ModDataComponents.BLASTER_HEAT.get(), BlasterHeatPolicy.tick(heat));
        }
    }

    public void fireAt(ServerLevel level, LivingEntity shooter, LivingEntity target, ItemStack weapon) {
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.CROSSBOW_SHOOT, SoundSource.NEUTRAL, 0.65F,
                1.45F + level.getRandom().nextFloat() * 0.15F);
        BlasterBoltEntity bolt = new BlasterBoltEntity(level, shooter, weapon, damage);
        double targetY = target.getY() + target.getBbHeight() * 0.6D;
        bolt.shoot(target.getX() - shooter.getX(), targetY - bolt.getY(), target.getZ() - shooter.getZ(),
                velocity, inaccuracy);
        level.addFreshEntity(bolt);
    }

    private static boolean consumeEnergyCell(Player player) {
        List<ItemStack> inventory = player.getInventory().getNonEquipmentItems();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.get(slot);
            if (!stack.is(ModItems.ENERGY_CELL.get())) {
                continue;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                inventory.set(slot, ItemStack.EMPTY);
            }
            player.getInventory().setChanged();
            return true;
        }
        return false;
    }

    public static BlasterHeatPolicy.BlasterHeatState heat(ItemStack weapon) {
        return weapon.getOrDefault(
                ModDataComponents.BLASTER_HEAT.get(), BlasterHeatPolicy.BlasterHeatState.ready());
    }

    private static void notify(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message, true);
        } else {
            player.sendSystemMessage(message);
        }
    }
}
