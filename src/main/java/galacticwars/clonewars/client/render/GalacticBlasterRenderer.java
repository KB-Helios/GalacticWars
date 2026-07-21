package galacticwars.clonewars.client.render;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.combat.BlasterItem;
import net.minecraft.resources.Identifier;

/** Shared renderer contract for weapon-specific volumetric blaster resources. */
public final class GalacticBlasterRenderer extends GeoItemRenderer<BlasterItem> {
    public GalacticBlasterRenderer(BlasterItem item) {
        super(new DefaultedItemGeoModel<BlasterItem>(Identifier.fromNamespaceAndPath(
                GalacticWars.MODID, "blaster/" + item.visualId())));
        this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.useAlternateGuiLighting();
    }
}
