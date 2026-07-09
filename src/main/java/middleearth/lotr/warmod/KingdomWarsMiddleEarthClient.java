package middleearth.lotr.warmod;

import middleearth.lotr.warmod.client.gui.RecruitCommandScreen;
import middleearth.lotr.warmod.client.render.MiddleEarthRecruitRenderer;
import middleearth.lotr.warmod.registry.ModEntityTypes;
import middleearth.lotr.warmod.registry.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = KingdomWarsMiddleEarth.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = KingdomWarsMiddleEarth.MODID, value = Dist.CLIENT)
public class KingdomWarsMiddleEarthClient {
    public KingdomWarsMiddleEarthClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        KingdomWarsMiddleEarth.LOGGER.info("KingdomWars-Middle-Earth client foundation loaded.");
    }

    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                ModEntityTypes.GONDOR_RECRUIT.get(),
                context -> new MiddleEarthRecruitRenderer<>(context, ModEntityTypes.GONDOR_RECRUIT.get()));
        event.registerEntityRenderer(
                ModEntityTypes.ROHAN_RECRUIT.get(),
                context -> new MiddleEarthRecruitRenderer<>(context, ModEntityTypes.ROHAN_RECRUIT.get()));
        event.registerEntityRenderer(
                ModEntityTypes.MORDOR_ORC_RECRUIT.get(),
                context -> new MiddleEarthRecruitRenderer<>(context, ModEntityTypes.MORDOR_ORC_RECRUIT.get()));
        event.registerEntityRenderer(
                ModEntityTypes.DWARF_RECRUIT.get(),
                context -> new MiddleEarthRecruitRenderer<>(context, ModEntityTypes.DWARF_RECRUIT.get()));
        event.registerEntityRenderer(
                ModEntityTypes.ELF_RECRUIT.get(),
                context -> new MiddleEarthRecruitRenderer<>(context, ModEntityTypes.ELF_RECRUIT.get()));
    }

    @SubscribeEvent
    static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.RECRUIT_COMMAND.get(), RecruitCommandScreen::new);
    }
}
