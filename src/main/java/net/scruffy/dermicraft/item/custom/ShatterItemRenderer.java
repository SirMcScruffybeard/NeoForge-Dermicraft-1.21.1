package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.property.ShatterHeadProperties;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Tints the piston's "face" and "tail" bones with the mounted head's material colour -- same shared
 * tint across both bones (see the Shatter design notes' piston-tint entry), same grayscale-texture-
 * plus-vertex-colour-multiply technique as {@link SunderItemRenderer}'s chain tint. "piston" (the
 * untinted top bone) is deliberately not touched here.
 *
 * <p>Bone and Diamond's dedicated look comes from {@link ShatterHeadTextureLayer}, a separate overlay
 * pass, not from swapping this renderer's base texture -- their hand-painted textures only cover the
 * face/tail region (transparent everywhere else), so the base {@code shatter.png} still shows through
 * for the rest of the tool. Their {@link ShatterHeadProperties#tint} stays white (a no-op) here since
 * the overlay, not this tint pass, is what actually colors their face/tail.
 */
public class ShatterItemRenderer extends GeoItemRenderer<ShatterItem> {

    private static final String FACE_BONE = "face";
    private static final String TAIL_BONE = "tail";

    public ShatterItemRenderer() {
        super(new ShatterItemModel());
        addRenderLayer(new ShatterHeadTextureLayer(this));
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ShatterItem animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay, int colour) {
        if (FACE_BONE.equals(bone.getName()) || TAIL_BONE.equals(bone.getName())) {
            ItemStack stack = getCurrentItemStack();
            if (stack != null) {
                ShatterHeadProperties head = ShatterItem.headProperties(stack);
                // No head mounted -- leaving the colour alone is defensive; there's no decided
                // "bare piston" visual treatment yet (see the Shatter design notes' open questions).
                if (head != null) {
                    colour = 0xFF000000 | head.tint().getValue();
                }
            }
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }
}
