package net.scruffy.dermicraft.item.custom;

import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class AidItemRenderer extends GeoItemRenderer<AidItem> {
    public AidItemRenderer() {
        super(new AidItemModel());
        addRenderLayer(new AidScreenGlowLayer(this));
    }

    /** Exposes GeckoLib's protected render perspective -- {@link AidScreenGlowLayer} uses it to tell
     * a held A.I.D. from one drawn in a GUI slot, same as DrinkerItemRenderer's own accessor. */
    public ItemDisplayContext currentPerspective() {
        return this.renderPerspective;
    }
}
