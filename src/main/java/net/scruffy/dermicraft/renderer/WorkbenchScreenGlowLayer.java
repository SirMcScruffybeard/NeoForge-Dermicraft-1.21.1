package net.scruffy.dermicraft.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.block.entity.custom.WorkbenchBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Screen-on glow for the Workbench's "screen" bone -- same shape as
 * {@link net.scruffy.dermicraft.item.custom.DrinkerScreenGlowLayer}: a single pre-authored "on"
 * texture rendered emissive on top of the base model, with no separate "off" texture needed since
 * the base art already shows the screen blank/unlit. Gated to WorkbenchBlockEntity#isOpen() so it
 * only lights up for the activate/active window, not while deactivating.
 */
public class WorkbenchScreenGlowLayer extends AutoGlowingGeoLayer<WorkbenchBlockEntity> {

    private static final ResourceLocation SCREEN_ON = ResourceLocation.fromNamespaceAndPath(
            Dermicraft.MOD_ID, "textures/block/workbench/workbench_bottom_active.png");

    public WorkbenchScreenGlowLayer(GeoRenderer<WorkbenchBlockEntity> renderer) {
        super(renderer);
    }

    @Override
    protected RenderType getRenderType(WorkbenchBlockEntity animatable, MultiBufferSource bufferSource) {
        return RenderType.entityTranslucentEmissive(SCREEN_ON);
    }

    @Override
    public void render(PoseStack poseStack, WorkbenchBlockEntity animatable, BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!animatable.isOpen()) return;
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
    }
}
