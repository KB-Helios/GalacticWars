package galacticwars.clonewars.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import galacticwars.clonewars.registry.ModDataComponents;
import galacticwars.clonewars.registry.ModItems;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** A server-authored in-world target carried by the physical command marker. */
public record CommandTargetSelection(
        String dimensionId,
        int x,
        int y,
        int z,
        Optional<UUID> entityId
) {
    public static final Codec<CommandTargetSelection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("dimension").forGetter(CommandTargetSelection::dimensionId),
            Codec.INT.fieldOf("x").forGetter(CommandTargetSelection::x),
            Codec.INT.fieldOf("y").forGetter(CommandTargetSelection::y),
            Codec.INT.fieldOf("z").forGetter(CommandTargetSelection::z),
            UUIDUtil.CODEC.optionalFieldOf("entity_id").forGetter(CommandTargetSelection::entityId)
    ).apply(instance, CommandTargetSelection::new));

    public CommandTargetSelection {
        dimensionId = Identifier.parse(Objects.requireNonNull(dimensionId, "dimensionId")).toString();
        entityId = entityId == null ? Optional.empty() : entityId;
    }

    public static CommandTargetSelection block(ServerLevel level, BlockPos pos) {
        return new CommandTargetSelection(
                level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(), Optional.empty());
    }

    public static CommandTargetSelection entity(ServerLevel level, LivingEntity entity) {
        BlockPos pos = entity.blockPosition();
        return new CommandTargetSelection(
                level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(),
                Optional.of(entity.getUUID()));
    }

    public BlockPos blockPos() {
        return new BlockPos(x, y, z);
    }

    public boolean isIn(ServerLevel level) {
        return dimensionId.equals(level.dimension().identifier().toString());
    }

    public Optional<LivingEntity> resolveEntity(ServerLevel level) {
        if (!isIn(level) || entityId.isEmpty()) {
            return Optional.empty();
        }
        UUID targetId = entityId.orElseThrow();
        if (level.getEntity(targetId) instanceof LivingEntity living && living.isAlive()) {
            return Optional.of(living);
        }
        return Optional.empty();
    }

    public static Optional<CommandTargetSelection> fromInventory(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            CommandTargetSelection selection = validSelection(
                    player.getItemInHand(hand), player.level());
            if (selection != null) {
                return Optional.of(selection);
            }
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            CommandTargetSelection selection = validSelection(stack, player.level());
            if (selection != null) {
                return Optional.of(selection);
            }
        }
        return Optional.empty();
    }

    private static CommandTargetSelection validSelection(ItemStack stack, ServerLevel level) {
        if (!stack.is(ModItems.COMMAND_MARKER.get())) {
            return null;
        }
        CommandTargetSelection selection = stack.get(ModDataComponents.COMMAND_TARGET.get());
        return selection != null && selection.isIn(level) ? selection : null;
    }

    public static Optional<BlockPos> blockFromInventory(ServerPlayer player) {
        return fromInventory(player)
                .filter(selection -> selection.entityId().isEmpty())
                .map(CommandTargetSelection::blockPos);
    }

    public static Optional<LivingEntity> entityFromInventory(ServerPlayer player) {
        return fromInventory(player).flatMap(selection -> selection.resolveEntity(player.level()));
    }
}
