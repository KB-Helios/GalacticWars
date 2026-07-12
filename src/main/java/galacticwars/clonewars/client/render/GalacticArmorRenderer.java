package galacticwars.clonewars.client.render;

import com.geckolib.renderer.GeoArmorRenderer;
import galacticwars.clonewars.item.GalacticArmorItem;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** Equipped-armor renderer shared by every piece in one visual family. */
public final class GalacticArmorRenderer
        extends GeoArmorRenderer<GalacticArmorItem, HumanoidRenderState> {
    public GalacticArmorRenderer(String familyId) {
        super(new GalacticArmorModel(familyId));
    }
}
