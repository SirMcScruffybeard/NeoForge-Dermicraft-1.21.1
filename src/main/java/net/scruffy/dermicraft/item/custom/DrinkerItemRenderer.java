package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class DrinkerItemRenderer extends GeoItemRenderer<DrinkerItem> {
    // DRINKER is wide enough that FIXED display (item frames, and the Workbench's item preview --
    // see WorkbenchItemDisplayLayer) clips through neighboring geometry at full width. Only
    // squeezed on that axis, and only for FIXED, so held/GUI/ground rendering stay untouched.
    private static final float FIXED_X_SCALE = 0.6F;

    public DrinkerItemRenderer() {
        super(new DrinkerItemModel());
        addRenderLayer(new DrinkerGlowLayer(this));
        addRenderLayer(new DrinkerScreenGlowLayer(this));
        addRenderLayer(new DrinkerGaugeGlowLayer(this));
    }

    /** Exposes GeckoLib's protected render perspective, which is only reachable from a subclass.
     * Layers use it to tell a held DRINKER from one drawn in a GUI slot. */
    public ItemDisplayContext currentPerspective() {
        return this.renderPerspective;
    }

    @Override
    public void preRender(PoseStack poseStack, DrinkerItem animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        if (!isReRender && this.renderPerspective == ItemDisplayContext.FIXED) {
            poseStack.scale(FIXED_X_SCALE, 1f, 1f);
        }
    }
}
