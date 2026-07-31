package net.scruffy.dermicraft.item.custom;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class EaterItemRenderer extends GeoItemRenderer<EaterItem> {
    public EaterItemRenderer() {
        super(new EaterItemModel());

        addRenderLayer(new EaterGlowLayer(this));

        // One glow layer per screen bone -- see EaterScreenGlowLayer's javadoc for why this can't
        // be a single mutually-exclusive layer the way the mode lights above are.
        for (int i = 1; i <= EaterItem.SLOT_COUNT; i++) {
            ResourceLocation lit = ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID,
                    "textures/item/eater/eater_screen_" + i + ".png");
            addRenderLayer(new EaterScreenGlowLayer(this, i - 1, lit));
        }

        // Added last so the live item render draws on top of the lit screen backdrop, not under it.
        addRenderLayer(new EaterItemDisplayLayer(this));
    }

    public ItemDisplayContext currentPerspective() {
        return this.renderPerspective;
    }
}
