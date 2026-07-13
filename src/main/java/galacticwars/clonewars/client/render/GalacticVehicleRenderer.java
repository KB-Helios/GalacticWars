package galacticwars.clonewars.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import galacticwars.clonewars.vehicle.GalacticVehicleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.EntityType;

public final class GalacticVehicleRenderer<R extends EntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<GalacticVehicleEntity, R> {
    public GalacticVehicleRenderer(
            EntityRendererProvider.Context context,
            EntityType<GalacticVehicleEntity> entityType
    ) {
        super(context, entityType);
    }
}
