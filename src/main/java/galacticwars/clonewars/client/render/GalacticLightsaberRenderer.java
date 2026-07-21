package galacticwars.clonewars.client.render;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.item.LightsaberItem;
import net.minecraft.resources.Identifier;

/** Renders a color-specific era-authentic hilt with the shared long-blade contract. */
public final class GalacticLightsaberRenderer extends GeoItemRenderer<LightsaberItem> {
    public GalacticLightsaberRenderer(LightsaberItem item) {
        super(new DefaultedItemGeoModel<LightsaberItem>(
                Identifier.fromNamespaceAndPath(
                        GalacticWars.MODID, "lightsaber/" + item.colorId())));
        this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.useAlternateGuiLighting();
    }
}
