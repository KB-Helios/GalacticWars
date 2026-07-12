package galacticwars.clonewars.client.render;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.item.GalacticArmorItem;
import net.minecraft.resources.Identifier;

/** Resolves one of the project-owned armor families without hard-coding a renderer per item. */
public final class GalacticArmorModel extends GeoModel<GalacticArmorItem> {
    private final Identifier model;
    private final Identifier texture;
    private final Identifier animation;

    public GalacticArmorModel(String familyId) {
        this.model = Identifier.fromNamespaceAndPath(GalacticWars.MODID, "armor/" + familyId);
        this.texture = Identifier.fromNamespaceAndPath(
                GalacticWars.MODID, "textures/armor/" + familyId + ".png");
        this.animation = Identifier.fromNamespaceAndPath(GalacticWars.MODID, "armor/" + familyId);
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return this.model;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return this.texture;
    }

    @Override
    public Identifier getAnimationResource(GalacticArmorItem animatable) {
        return this.animation;
    }
}
