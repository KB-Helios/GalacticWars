package galacticwars.clonewars;

import galacticwars.clonewars.client.gui.RecruitCommandScreen;
import galacticwars.clonewars.client.gui.CommandCenterNavigationScreen;
import galacticwars.clonewars.client.gui.FactionSelectionScreen;
import galacticwars.clonewars.client.gui.BlasterHeatHud;
import galacticwars.clonewars.client.render.GalacticRecruitRenderer;
import galacticwars.clonewars.client.render.LightsaberClientExtensions;
import galacticwars.clonewars.registry.ModEntityTypes;
import galacticwars.clonewars.registry.ModItems;
import galacticwars.clonewars.registry.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = GalacticWars.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = GalacticWars.MODID, value = Dist.CLIENT)
public class GalacticWarsClient {
    public GalacticWarsClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        GalacticWars.LOGGER.info("Galactic Wars: Clone Wars client foundation loaded.");
    }

    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ModEntityTypes.recruits().forEach(holder -> event.registerEntityRenderer(
                holder.get(), context -> new GalacticRecruitRenderer<>(
                        context, holder.get())));
    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(
                LightsaberClientExtensions.INSTANCE,
                ModItems.BLUE_LIGHTSABER.get(),
                ModItems.GREEN_LIGHTSABER.get(),
                ModItems.RED_LIGHTSABER.get(),
                ModItems.PURPLE_LIGHTSABER.get(),
                ModItems.YELLOW_LIGHTSABER.get(),
                ModItems.WHITE_LIGHTSABER.get());
    }

    @SubscribeEvent
    static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.RECRUIT_COMMAND.get(), RecruitCommandScreen::new);
        event.register(ModMenuTypes.COMMAND_CENTER_NAVIGATION.get(), CommandCenterNavigationScreen::new);
        event.register(ModMenuTypes.FACTION_SELECTION.get(), FactionSelectionScreen::new);
    }

    @SubscribeEvent
    static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                Identifier.fromNamespaceAndPath(GalacticWars.MODID, "blaster_heat"),
                (graphics, deltaTracker) -> BlasterHeatHud.render(graphics));
    }
}
