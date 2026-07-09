package middleearth.lotr.warmod.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import middleearth.lotr.warmod.entity.MiddleEarthRecruitEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.EntityType;

public class MiddleEarthRecruitRenderer<R extends EntityRenderState>
        extends GeoEntityRenderer<MiddleEarthRecruitEntity, R> {
    public MiddleEarthRecruitRenderer(
            EntityRendererProvider.Context context,
            EntityType<MiddleEarthRecruitEntity> entityType
    ) {
        super(context, entityType);
        this.withScale(1.0F);
    }
}
