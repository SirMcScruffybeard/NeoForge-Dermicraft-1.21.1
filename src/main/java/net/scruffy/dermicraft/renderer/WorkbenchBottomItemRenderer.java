package net.scruffy.dermicraft.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.WorkbenchBlockEntity;

/**
 * Item-form (inventory/hand/ground) renderer for Workbench's bottom half. A plain auto-generated
 * BlockItem is not a GeoItem, so GeckoLib's own automatic item-render hook
 * (BlockEntityWithoutLevelRendererMixin, which only fires for GeoItem-implementing items) never
 * fires for it -- without this, the icon renders as nothing at all. Standard pattern for a
 * GeckoLib block that has no item-side GeoAnimatable of its own: feed a throwaway, never-placed
 * block entity into the exact same GeoBlockRenderer used in-world.
 */
public class WorkbenchBottomItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final WorkbenchBlockEntity dummy =
            new WorkbenchBlockEntity(BlockPos.ZERO, ModBlocks.WORKBENCH.get().defaultBlockState());
    private final WorkbenchBottomBlockEntityRenderer renderer = new WorkbenchBottomBlockEntityRenderer(null);

    public WorkbenchBottomItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderer.render(dummy, 0, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
