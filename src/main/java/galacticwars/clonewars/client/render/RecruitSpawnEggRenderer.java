package galacticwars.clonewars.client.render;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.entity.RecruitSpawnEggItem;
import net.minecraft.resources.Identifier;

/** Renders a recruit egg as the shared animated capsule with its unit-specific material atlas. */
public final class RecruitSpawnEggRenderer extends GeoItemRenderer<RecruitSpawnEggItem> {
    public RecruitSpawnEggRenderer(RecruitSpawnEggItem item) {
        super(new DefaultedItemGeoModel<RecruitSpawnEggItem>(
                Identifier.fromNamespaceAndPath(GalacticWars.MODID, "spawn_capsule"))
                .withAltTexture(Identifier.fromNamespaceAndPath(
                        GalacticWars.MODID, "spawn_capsule/" + item.visualId())));
        this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.useAlternateGuiLighting();
    }
}
