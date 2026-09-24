package net.scruffy.dermicraft.renderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.scruffy.dermicraft.block.entity.custom.DockBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class DockBlockEntityRenderer extends GeoBlockRenderer<DockBlockEntity> {

    // Context is unused -- only present so this constructor matches the BlockEntityRendererProvider
    // functional interface event.registerBlockEntityRenderer expects.
    public DockBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new DockGeoModel());
    }
}
