package galacticwars.clonewars.client.render;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.recruitment.RecruitDuty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

/** Default recruit model with a synchronized, duty-sensitive texture selection. */
public final class GalacticRecruitGeoModel extends DefaultedEntityGeoModel<GalacticRecruitEntity> {
    public static final DataTicket<RecruitDuty> RECRUIT_DUTY = DataTickets.create(
            "galacticwars:recruit_duty", RecruitDuty.class);

    private final EntityType<GalacticRecruitEntity> entityType;

    public GalacticRecruitGeoModel(EntityType<GalacticRecruitEntity> entityType) {
        super(entityType);
        this.entityType = entityType;
    }

    @Override
    public void addAdditionalStateData(
            GalacticRecruitEntity animatable,
            Object relatedObject,
            GeoRenderState renderState
    ) {
        super.addAdditionalStateData(animatable, relatedObject, renderState);
        RecruitDuty duty = animatable == null ? RecruitDuty.SOLDIER : animatable.getRecruitDuty();
        renderState.addGeckolibData(RECRUIT_DUTY, duty);
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        RecruitDuty duty = renderState.getOrDefaultGeckolibData(RECRUIT_DUTY, RecruitDuty.SOLDIER);
        return RecruitVisualProfileCatalog.textureResource(this.entityType, duty);
    }
}
