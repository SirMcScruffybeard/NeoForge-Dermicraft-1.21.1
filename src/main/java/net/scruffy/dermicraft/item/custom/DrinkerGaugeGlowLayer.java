package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Fill-amount gauge: 3 discrete steps (below-half, half, full), emissive green. Empty renders
 * nothing at all -- same skip-when-off approach as {@link DrinkerScreenGlowLayer}, not the
 * always-show-something approach {@link DrinkerGlowLayer} uses for the mode lights, since here
 * "empty" means the base texture's unlit gauge art shows through rather than a 4th texture.
 *
 * <p>TEMP: reads {@link DrinkerItem#DEMO_GAUGE_STEP} for now, purely to validate the three gauge
 * textures in-game. Needs to switch to real buffer-amount state once DRINKER's actual fluid
 * buffer is built.
 */
public class DrinkerGaugeGlowLayer extends AutoGlowingGeoLayer<DrinkerItem> {

    private static final ResourceLocation GAUGE_1 =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/drinker/drinker_gauge_1.png");
    private static final ResourceLocation GAUGE_2 =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/drinker/drinker_gauge_2.png");
    private static final ResourceLocation GAUGE_3 =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/drinker/drinker_gauge_3.png");

    public DrinkerGaugeGlowLayer(GeoRenderer<DrinkerItem> renderer) {
        super(renderer);
    }

    @Override
    protected RenderType getRenderType(DrinkerItem animatable, MultiBufferSource bufferSource) {
        ResourceLocation gauge = switch (DrinkerItem.DEMO_GAUGE_STEP) {
            case 2 -> GAUGE_2;
            case 3 -> GAUGE_3;
            default -> GAUGE_1;
        };
        return RenderType.entityTranslucentEmissive(gauge);
    }

    @Override
    public void render(PoseStack poseStack, DrinkerItem animatable, BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (DrinkerItem.DEMO_GAUGE_STEP <= 0) return;
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
    }
}
