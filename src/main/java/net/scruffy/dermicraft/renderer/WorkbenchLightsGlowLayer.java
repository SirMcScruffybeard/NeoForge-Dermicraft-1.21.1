package net.scruffy.dermicraft.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.block.entity.custom.WorkbenchTopBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Light-strip glow for the Workbench top's indicator lights -- same shape as
 * {@link WorkbenchScreenGlowLayer} on the bottom half: a single pre-authored "on" texture rendered
 * emissive over the base model, gated to WorkbenchTopBlockEntity#isOpen() so it only lights up for
 * the activate/active window.
 */
public class WorkbenchLightsGlowLayer extends AutoGlowingGeoLayer<WorkbenchTopBlockEntity> {

    private static final ResourceLocation LIGHTS_ON = ResourceLocation.fromNamespaceAndPath(
            Dermicraft.MOD_ID, "textures/block/workbench/workbench_top_lights.png");

    public WorkbenchLightsGlowLayer(GeoRenderer<WorkbenchTopBlockEntity> renderer) {
        super(renderer);
    }

    @Override
    protected RenderType getRenderType(WorkbenchTopBlockEntity animatable, MultiBufferSource bufferSource) {
        return RenderType.entityTranslucentEmissive(LIGHTS_ON);
    }

    @Override
    public void render(PoseStack poseStack, WorkbenchTopBlockEntity animatable, BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!animatable.isOpen()) return;
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
    }
}
