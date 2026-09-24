package net.scruffy.dermicraft.renderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.scruffy.dermicraft.block.entity.custom.DockBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Only ever driven directly by DockItemRenderer against a throwaway BlockEntity, never registered
 * as a real BlockEntityRenderer -- see DockGeoModel/DockBlockEntityRenderer for the actual in-world
 * renderer.
 */
public class DockIconBlockEntityRenderer extends GeoBlockRenderer<DockBlockEntity> {

    public DockIconBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new DockIconGeoModel());
    }
}
