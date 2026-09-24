package net.scruffy.dermicraft.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.scruffy.dermicraft.block.entity.custom.DockBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * A 3-tall model authored in Blockbench for a "builtin/entity" block has to reach its lower layer
 * by going into negative Y (Blockbench has no other way to place geometry below the block it's
 * attached to) -- dock.geo.json's floor plate sits at local y -16..-8 (one full block below the
 * placed BlockPos) and its roof at y 28..32 (near the top of the block above). Left alone, that
 * makes the placed block represent the structure's middle/player layer, with the floor rendering
 * embedded in whatever's below it. Shifting the whole render up by exactly one block instead makes
 * the placed block represent the FLOOR layer -- click the ground, floor sits there, structure grows
 * upward -- matching the vanilla-door-style convention Workbench's bottom/top pair already uses.
 * Compensates in code rather than renumbering every cube in the (already complex, still-evolving)
 * model. DockBlock's own collision VoxelShape is offset by the same +1 block to match.
 */
public class DockBlockEntityRenderer extends GeoBlockRenderer<DockBlockEntity> {

    // Context is unused -- only present so this constructor matches the BlockEntityRendererProvider
    // functional interface event.registerBlockEntityRenderer expects.
    public DockBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new DockGeoModel());
    }

    @Override
    public void render(DockBlockEntity animatable, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0, 1, 0);
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
