package galacticwars.clonewars.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.combat.BlasterHeatPolicy;
import galacticwars.clonewars.item.CommandTargetSelection;
import galacticwars.clonewars.settlement.ConstructionPlan;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    private static final Codec<BlasterHeatPolicy.BlasterHeatState> BLASTER_HEAT_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("shots_remaining")
                            .forGetter(BlasterHeatPolicy.BlasterHeatState::shotsRemaining),
                    Codec.INT.fieldOf("shot_cooldown_ticks")
                            .forGetter(BlasterHeatPolicy.BlasterHeatState::shotCooldownTicks),
                    Codec.INT.fieldOf("overheat_ticks")
                            .forGetter(BlasterHeatPolicy.BlasterHeatState::overheatTicks)
            ).apply(instance, BlasterHeatPolicy.BlasterHeatState::new));
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, GalacticWars.MODID);

    public static final Supplier<DataComponentType<BlasterHeatPolicy.BlasterHeatState>> BLASTER_HEAT =
            COMPONENTS.registerComponentType("blaster_heat", builder ->
                    builder.persistent(BLASTER_HEAT_CODEC));
    public static final Supplier<DataComponentType<ConstructionPlan>> CONSTRUCTION_PLAN =
            COMPONENTS.registerComponentType("construction_plan", builder ->
                    builder.persistent(ConstructionPlan.CODEC));
    public static final Supplier<DataComponentType<CommandTargetSelection>> COMMAND_TARGET =
            COMPONENTS.registerComponentType("command_target", builder ->
                    builder.persistent(CommandTargetSelection.CODEC));

    private ModDataComponents() {
    }

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}
