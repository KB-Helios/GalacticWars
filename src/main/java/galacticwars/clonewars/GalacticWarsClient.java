package galacticwars.clonewars;

import galacticwars.clonewars.client.gui.RecruitCommandScreen;
import galacticwars.clonewars.client.gui.CommandCenterNavigationScreen;
import galacticwars.clonewars.client.gui.FactionSelectionScreen;
import galacticwars.clonewars.client.gui.BlasterHeatHud;
import galacticwars.clonewars.client.gui.GalacticWarsConfigScreen;
import galacticwars.clonewars.client.gui.MerchantTradeScreen;
import galacticwars.clonewars.client.gui.CommandCenterOperationsScreen;
import galacticwars.clonewars.client.render.GalacticRecruitRenderer;
import galacticwars.clonewars.client.render.LightsaberClientExtensions;
import galacticwars.clonewars.client.render.GalacticVehicleRenderer;
import galacticwars.clonewars.registry.ModEntityTypes;
import galacticwars.clonewars.registry.ModItems;
import galacticwars.clonewars.registry.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = GalacticWars.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = GalacticWars.MODID, value = Dist.CLIENT)
public class GalacticWarsClient {
    public GalacticWarsClient(ModContainer container) {
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (ignored, parent) -> GalacticWarsConfigScreen.create(parent));
        NeoForge.EVENT_BUS.addListener(galacticwars.clonewars.client.ForceKeyMappings::tick);
    }

    @SubscribeEvent
    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        galacticwars.clonewars.client.ForceKeyMappings.register(event);
    }

    @SubscribeEvent
    static void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(galacticwars.clonewars.network.ForceHudPayload.TYPE,
                (payload, context) -> galacticwars.clonewars.client.ForceClientState.update(payload));
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        GalacticWars.LOGGER.info("Galactic Wars: Clone Wars client foundation loaded.");
    }

    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                ModEntityTypes.BLASTER_BOLT.get(),
                context -> new ThrownItemRenderer<>(context, 1.0F, true));
        ModEntityTypes.recruits().forEach(holder -> event.registerEntityRenderer(
                holder.get(), context -> new GalacticRecruitRenderer<>(
                        context, holder.get())));
        ModEntityTypes.vehicles().forEach(holder -> event.registerEntityRenderer(
                holder.get(), context -> new GalacticVehicleRenderer<>(context, holder.get())));
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
        event.register(ModMenuTypes.MERCHANT_TRADE.get(), MerchantTradeScreen::new);
        event.register(ModMenuTypes.COMMAND_CENTER_OPERATIONS.get(), CommandCenterOperationsScreen::new);
    }

    @SubscribeEvent
    static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                Identifier.fromNamespaceAndPath(GalacticWars.MODID, "blaster_heat"),
                (graphics, deltaTracker) -> BlasterHeatHud.render(graphics));
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                Identifier.fromNamespaceAndPath(GalacticWars.MODID, "force_hud"),
                (graphics, deltaTracker) -> galacticwars.clonewars.client.gui.ForceHud.render(graphics));
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                Identifier.fromNamespaceAndPath(GalacticWars.MODID, "vehicle_hud"),
                (graphics, deltaTracker) -> galacticwars.clonewars.client.gui.VehicleHud.render(graphics));
    }
}
