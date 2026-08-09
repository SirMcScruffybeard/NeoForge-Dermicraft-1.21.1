package net.scruffy.dermicraft.renderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.scruffy.dermicraft.block.entity.custom.WorkbenchTopBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class WorkbenchTopBlockEntityRenderer extends GeoBlockRenderer<WorkbenchTopBlockEntity> {

    // Context is unused -- only present so this constructor matches the BlockEntityRendererProvider
    // functional interface event.registerBlockEntityRenderer expects.
    public WorkbenchTopBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new WorkbenchTopGeoModel());
    }
}
