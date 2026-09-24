package net.scruffy.dermicraft.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.DockBlockEntity;

/**
 * Item-form (inventory/hand/ground) renderer for Dock. Same shape as WorkbenchBottomItemRenderer,
 * but pointed at DockIconBlockEntityRenderer/DockIconGeoModel instead of the in-world block
 * renderer, since Dock's item uses its own separate, much smaller model (dock_icon.geo.json) --
 * see DockIconGeoModel's javadoc.
 */
public class DockItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final DockBlockEntity dummy =
            new DockBlockEntity(BlockPos.ZERO, ModBlocks.DOCK.get().defaultBlockState());
    private final DockIconBlockEntityRenderer renderer = new DockIconBlockEntityRenderer(null);

    public DockItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderer.render(dummy, 0, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
