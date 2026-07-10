package middleearth.lotr.warmod.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import middleearth.lotr.warmod.entity.MiddleEarthRecruitEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.EntityType;

public class MiddleEarthRecruitRenderer<R extends EntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<MiddleEarthRecruitEntity, R> {
    public MiddleEarthRecruitRenderer(
            EntityRendererProvider.Context context,
            EntityType<MiddleEarthRecruitEntity> entityType
    ) {
        super(context, entityType);
        this.withRenderLayer(new ItemInHandGeoLayer<>(this));
        this.withScale(1.0F);
    }
}
