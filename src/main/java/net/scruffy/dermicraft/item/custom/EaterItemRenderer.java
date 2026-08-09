package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.scruffy.dermicraft.main.Dermicraft;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class EaterItemRenderer extends GeoItemRenderer<EaterItem> {
    // Same rationale as DrinkerItemRenderer's own FIXED_X_SCALE -- EATER is wide enough to clip
    // through neighboring geometry in FIXED display (item frames, Workbench item preview) at full
    // width. Only squeezed on that axis, and only for FIXED.
    private static final float FIXED_X_SCALE = 0.6F;

    public EaterItemRenderer() {
        super(new EaterItemModel());

        addRenderLayer(new EaterGlowLayer(this));

        // One glow layer per screen bone -- see EaterScreenGlowLayer's javadoc for why this can't
        // be a single mutually-exclusive layer the way the mode lights above are.
        for (int i = 1; i <= EaterItem.SLOT_COUNT; i++) {
            ResourceLocation lit = ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID,
                    "textures/item/eater/eater_screen_" + i + ".png");
            addRenderLayer(new EaterScreenGlowLayer(this, i - 1, lit));
        }

        // Added last so the live item render draws on top of the lit screen backdrop, not under it.
        addRenderLayer(new EaterItemDisplayLayer(this));
    }

    public ItemDisplayContext currentPerspective() {
        return this.renderPerspective;
    }

    @Override
    public void preRender(PoseStack poseStack, EaterItem animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        if (!isReRender && this.renderPerspective == ItemDisplayContext.FIXED) {
            poseStack.scale(FIXED_X_SCALE, 1f, 1f);
        }
    }
}
