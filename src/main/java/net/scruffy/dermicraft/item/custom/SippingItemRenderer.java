package net.scruffy.dermicraft.item.custom;

import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SippingItemRenderer extends GeoItemRenderer<SippingItem> {
    public SippingItemRenderer() {
        super(new SippingItemModel());
        addRenderLayer(new SippingGlowLayer(this));
    }
}
