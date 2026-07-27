package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Fill-amount gauge: 3 discrete steps (below-half, half, full), emissive green, read off the real
 * fluid buffer. Empty renders nothing at all -- same skip-when-off approach as
 * {@link DrinkerScreenGlowLayer}, not the always-show-something approach {@link DrinkerGlowLayer}
 * uses for the mode lights, since here "empty" means the base texture's unlit gauge art shows
 * through rather than a 4th texture.
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
        int amount = bufferedAmount();
        ResourceLocation gauge = amount >= DrinkerItem.CAPACITY ? GAUGE_3
                : amount >= DrinkerItem.CAPACITY / 2 ? GAUGE_2
                : GAUGE_1;
        return RenderType.entityTranslucentEmissive(gauge);
    }

    @Override
    public void render(PoseStack poseStack, DrinkerItem animatable, BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (bufferedAmount() <= 0) return;
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
    }

    /** Reads the rendered stack's own buffer, so each DRINKER shows its own level rather than a
     * shared global -- unlike the target-scan screen, which is inherently about the holder's aim. */
    private int bufferedAmount() {
        if (!(getRenderer() instanceof GeoItemRenderer<?> itemRenderer)) return 0;
        ItemStack stack = itemRenderer.getCurrentItemStack();
        if (stack == null || stack.isEmpty()) return 0;

        FluidStack fluid = stack.getOrDefault(ModDataComponentTypes.FLUID_DATA.get(), FluidData.EMPTY).getFluidStack();
        return fluid.getAmount();
    }
}
