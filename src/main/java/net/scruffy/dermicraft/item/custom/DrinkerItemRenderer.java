package net.scruffy.dermicraft.item.custom;

import software.bernie.geckolib.renderer.GeoItemRenderer;

public class DrinkerItemRenderer extends GeoItemRenderer<DrinkerItem> {
    public DrinkerItemRenderer() {
        super(new DrinkerItemModel());
        addRenderLayer(new DrinkerGlowLayer(this));
        addRenderLayer(new DrinkerScreenGlowLayer(this));
        addRenderLayer(new DrinkerGaugeGlowLayer(this));
    }
}
