package net.scruffy.dermicraft.item.custom;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Fixed, always-on heat glow on the running chain -- no per-mode switching needed (unlike
 * {@link SippingGlowLayer}'s Storage/Disposal lights), since Sunder has exactly one glow state.
 *
 * <p>"Only glows while running" isn't checked here at all -- it falls out of the same mechanism
 * that already scopes the base pass: {@code SunderItemModel} hides the {@code running} bone
 * whenever it's not the active variant, and a hidden bone's cubes are skipped by this layer too
 * (same underlying render path as the base pass), so there's nothing to render when idle.
 */
public class SunderGlowLayer extends AutoGlowingGeoLayer<SunderItem> {

    private static final ResourceLocation RUNNING_CHAIN_GLOW = ResourceLocation.fromNamespaceAndPath(
            Dermicraft.MOD_ID, "textures/item/sunder/sunder_running_chain_glow_layer.png");

    public SunderGlowLayer(GeoRenderer<SunderItem> renderer) {
        super(renderer);
    }

    @Override
    protected RenderType getRenderType(SunderItem animatable, MultiBufferSource bufferSource) {
        return RenderType.entityTranslucentEmissive(RUNNING_CHAIN_GLOW);
    }
}
