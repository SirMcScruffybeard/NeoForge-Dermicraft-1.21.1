package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.property.ChainProperties;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SunderItemRenderer extends GeoItemRenderer<SunderItem> {

    /** Parent of both chain variants (see sunder.geo.json: chains -> idle -> tooth*, and
     * chains -> running). Tinting here is enough for every chain cube -- GeckoLib passes the colour
     * down through {@code renderChildBones}, so the whole subtree inherits it from one check. */
    private static final String CHAIN_ROOT_BONE = "chains";

    public SunderItemRenderer() {
        super(new SunderItemModel());
        addRenderLayer(new SunderGlowLayer(this));
        addRenderLayer(new KnowledgeChainGlowLayer(this));
    }

    /**
     * Tints only the chain bones with the mounted chain material's colour, leaving the body drawing
     * untinted from the same shared texture.
     *
     * <p>This is why Sunder needs no separate chain texture file: GeckoLib's base render pass binds
     * one texture per model, but the per-bone {@code colour} it threads through here is a plain
     * vertex-colour multiply against whatever the texture sampled. Since the chain pixels are
     * authored grayscale, multiplying them by the material tint produces the material's colour,
     * while the body bones -- never reassigned -- keep the incoming colour untouched.
     *
     * <p>Requires the chain pixels stay grayscale. If they're ever painted with real hue, the
     * multiply will compound with it rather than replace it, and the chain will need pulling into
     * its own texture after all.
     */
    @Override
    public void renderRecursively(PoseStack poseStack, SunderItem animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay, int colour) {
        if (CHAIN_ROOT_BONE.equals(bone.getName())) {
            ItemStack stack = getCurrentItemStack();
            if (stack != null) {
                ChainProperties chain = SunderItem.chainProperties(stack);
                // No chain mounted -- the model hides these bones anyway (see SunderItemModel), so
                // leaving the colour alone is purely defensive rather than a visible case.
                if (chain != null) {
                    colour = 0xFF000000 | chain.tint().getValue();
                }
            }
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }
}
