package galacticwars.clonewars.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import galacticwars.clonewars.client.render.GalacticLightsaberRenderer;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.world.item.Item;

/** A full GeckoLib-rendered lightsaber with a color-specific emissive material atlas. */
public final class LightsaberItem extends Item implements GeoItem {
    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.lightsaber.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String colorId;

    public LightsaberItem(String colorId, Properties properties) {
        super(properties);
        this.colorId = Objects.requireNonNull(colorId, "colorId");
        GeoItem.registerSyncedAnimatable(this);
    }

    public String colorId() {
        return this.colorId;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<?> renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new GalacticLightsaberRenderer(LightsaberItem.this);
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("lightsaber", 2, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
