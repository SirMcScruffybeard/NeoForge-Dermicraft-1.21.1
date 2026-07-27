package net.scruffy.dermicraft.item.custom;

import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class DrinkerItemRenderer extends GeoItemRenderer<DrinkerItem> {
    public DrinkerItemRenderer() {
        super(new DrinkerItemModel());
        addRenderLayer(new DrinkerGlowLayer(this));
        addRenderLayer(new DrinkerScreenGlowLayer(this));
        addRenderLayer(new DrinkerGaugeGlowLayer(this));
    }

    /** Exposes GeckoLib's protected render perspective, which is only reachable from a subclass.
     * Layers use it to tell a held DRINKER from one drawn in a GUI slot. */
    public ItemDisplayContext currentPerspective() {
        return this.renderPerspective;
    }
}
