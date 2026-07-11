package middleearth.lotr.warmod.registry;

import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KingdomWarsMiddleEarth.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MIDDLE_EARTH =
            CREATIVE_MODE_TABS.register("middle_earth", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.kingdomwarsmiddleearth.middle_earth"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.MITHRIL_INGOT.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.MIDDLE_EARTH_STONE.get());
                        output.accept(ModItems.MITHRIL_ORE.get());
                        output.accept(ModItems.MALLORN_LOG.get());
                        output.accept(ModItems.MALLORN_PLANKS.get());
                        output.accept(ModItems.MALLORN_LEAVES.get());
                        output.accept(ModItems.MALLORN_SAPLING.get());
                        output.accept(ModItems.KINGDOM_HALL.get());
                        output.accept(ModItems.MITHRIL_INGOT.get());
                        output.accept(ModItems.RAW_MITHRIL.get());
                        output.accept(ModItems.GONDOR_STEEL_INGOT.get());
                        output.accept(ModItems.ROHAN_HORSEHAIR.get());
                        output.accept(ModItems.ROHAN_REINFORCED_IRON_INGOT.get());
                        output.accept(ModItems.MORDOR_IRON_SHARD.get());
                        output.accept(ModItems.MORDOR_IRON_INGOT.get());
                        output.accept(ModItems.MALLORN_WEAVE.get());
                        ModItems.FACTION_EQUIPMENT.values().forEach(item -> output.accept(item.get()));
                        output.accept(ModItems.GONDOR_FACTION_TOKEN.get());
                        output.accept(ModItems.ROHAN_FACTION_TOKEN.get());
                        output.accept(ModItems.MORDOR_FACTION_TOKEN.get());
                        output.accept(ModItems.DWARF_FACTION_TOKEN.get());
                        output.accept(ModItems.ELF_FACTION_TOKEN.get());
                        output.accept(ModItems.GONDOR_RECRUIT_SPAWN_EGG.get());
                        output.accept(ModItems.ROHAN_RECRUIT_SPAWN_EGG.get());
                        output.accept(ModItems.MORDOR_ORC_RECRUIT_SPAWN_EGG.get());
                        output.accept(ModItems.DWARF_RECRUIT_SPAWN_EGG.get());
                        output.accept(ModItems.ELF_RECRUIT_SPAWN_EGG.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
