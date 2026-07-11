package galacticwars.clonewars.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.EntityType;

public class GalacticRecruitRenderer<R extends EntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<GalacticRecruitEntity, R> {
    public GalacticRecruitRenderer(
            EntityRendererProvider.Context context,
            EntityType<GalacticRecruitEntity> entityType
    ) {
        super(context, entityType);
        this.withRenderLayer(new ItemInHandGeoLayer<>(context, this));
        this.withScale(1.0F);
    }
}
