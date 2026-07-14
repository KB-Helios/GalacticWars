package galacticwars.clonewars.entity;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import galacticwars.clonewars.client.render.RecruitSpawnEggRenderer;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.SpawnEggItem;

/** Spawn egg rendered as an animated, color-specific GeckoLib recruitment capsule. */
public final class RecruitSpawnEggItem extends SpawnEggItem implements GeoItem {
    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.spawn_capsule.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String visualId;

    public RecruitSpawnEggItem(
            EntityType<GalacticRecruitEntity> recruitType,
            String visualId,
            Properties properties
    ) {
        super(properties.spawnEgg(recruitType));
        this.visualId = Objects.requireNonNull(visualId, "visualId");
        GeoItem.registerSyncedAnimatable(this);
    }

    public String visualId() {
        return this.visualId;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<?> renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new RecruitSpawnEggRenderer(RecruitSpawnEggItem.this);
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("spawn_capsule", 2, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
