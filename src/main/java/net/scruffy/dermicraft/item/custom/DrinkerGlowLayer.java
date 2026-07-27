package net.scruffy.dermicraft.item.custom;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Storage/Transfer/Disposal lit textures are mutually exclusive, same approach as SIPPING's
 * {@link SippingGlowLayer} -- points directly at whichever full lit variant matches the current
 * mode rather than the default "<texture>_glowmask" convention.
 *
 * <p>TEMP: reads {@link DrinkerItem#DEMO_MODE} for now, purely to validate the three lit textures
 * in-game. Needs to switch to real per-stack mode state once DRINKER's actual mode logic is built.
 */
public class DrinkerGlowLayer extends AutoGlowingGeoLayer<DrinkerItem> {

    private static final ResourceLocation STORAGE_LIT =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/drinker/drinker_storage.png");
    private static final ResourceLocation TRANSFER_LIT =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/drinker/drinker_transfer.png");
    private static final ResourceLocation DISPOSAL_LIT =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/drinker/drinker_disposal.png");

    public DrinkerGlowLayer(GeoRenderer<DrinkerItem> renderer) {
        super(renderer);
    }

    @Override
    protected RenderType getRenderType(DrinkerItem animatable, MultiBufferSource bufferSource) {
        ResourceLocation lit = switch (DrinkerItem.DEMO_MODE) {
            case 1 -> TRANSFER_LIT;
            case 2 -> DISPOSAL_LIT;
            default -> STORAGE_LIT;
        };
        return RenderType.entityTranslucentEmissive(lit);
    }
}
