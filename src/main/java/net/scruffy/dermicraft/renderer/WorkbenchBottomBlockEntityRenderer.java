package net.scruffy.dermicraft.renderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.scruffy.dermicraft.block.entity.custom.WorkbenchBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class WorkbenchBottomBlockEntityRenderer extends GeoBlockRenderer<WorkbenchBlockEntity> {

    // Context is unused -- only present so this constructor matches the BlockEntityRendererProvider
    // functional interface event.registerBlockEntityRenderer expects.
    public WorkbenchBottomBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new WorkbenchBottomGeoModel());
        addRenderLayer(new WorkbenchScreenGlowLayer(this));
        addRenderLayer(new WorkbenchWorkItemDisplayLayer(this));
    }
}
