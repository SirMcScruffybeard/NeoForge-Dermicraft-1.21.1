package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Overlays a dedicated, hand-painted head texture on top of the base render pass, for materials that
 * have one (currently Bone and Diamond) -- these textures only cover the face/tail region (fully
 * transparent everywhere else), so re-rendering the whole model with one bound only actually changes
 * pixels in that region; the rest of the tool keeps showing the base {@code shatter.png} underneath.
 * Same "re-render the full baked model with a different texture bound" technique
 * {@link SunderGlowLayer} uses for its glow pass, just a plain (non-emissive) translucent overlay
 * instead of a fullbright one.
 *
 * <p>No-ops entirely (renders nothing) when the mounted head has no dedicated texture -- Iron/Gold
 * still rely purely on {@link ShatterItemRenderer}'s own tint pass, with nothing here to layer over
 * them.
 */
public class ShatterHeadTextureLayer extends GeoRenderLayer<ShatterItem> {

    private static final ResourceLocation BONE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/shatter/shatter_bone.png");
    private static final ResourceLocation DIAMOND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/shatter/shatter_diamond.png");

    public ShatterHeadTextureLayer(GeoRenderer<ShatterItem> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, ShatterItem animatable, BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                        int packedLight, int packedOverlay) {
        ResourceLocation overlayTexture = overlayTexture();
        if (overlayTexture == null) return;

        RenderType overlayRenderType = RenderType.entityTranslucent(overlayTexture);
        VertexConsumer overlayBuffer = bufferSource.getBuffer(overlayRenderType);

        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, overlayRenderType, overlayBuffer,
                partialTick, packedLight, packedOverlay, -1);
    }

    /** The mounted head's dedicated texture, or {@code null} if it doesn't have one. */
    private ResourceLocation overlayTexture() {
        ItemStack stack = getRenderer() instanceof GeoItemRenderer<?> itemRenderer
                ? itemRenderer.getCurrentItemStack() : null;
        if (stack == null) return null;

        ItemStack head = ShatterItem.mountedHead(stack).itemStack();
        if (head.getItem() == ModItems.BONE_SHATTER_HEAD.get()) return BONE_TEXTURE;
        if (head.getItem() == ModItems.DIAMOND_SHATTER_HEAD.get()) return DIAMOND_TEXTURE;
        return null;
    }
}
