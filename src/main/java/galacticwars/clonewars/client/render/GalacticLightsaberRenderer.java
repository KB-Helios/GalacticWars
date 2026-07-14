package galacticwars.clonewars.client.render;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.item.LightsaberItem;
import net.minecraft.resources.Identifier;

/** Renders the shared long-blade model with the item's color atlas and full-bright glowmask. */
public final class GalacticLightsaberRenderer extends GeoItemRenderer<LightsaberItem> {
    public GalacticLightsaberRenderer(LightsaberItem item) {
        super(new DefaultedItemGeoModel<LightsaberItem>(
                Identifier.fromNamespaceAndPath(GalacticWars.MODID, "lightsaber"))
                .withAltTexture(Identifier.fromNamespaceAndPath(
                        GalacticWars.MODID, "lightsaber/" + item.colorId())));
        this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.useAlternateGuiLighting();
    }
}
