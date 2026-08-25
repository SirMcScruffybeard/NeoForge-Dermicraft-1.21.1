package net.scruffy.dermicraft.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.block.entity.custom.SkinTankBlockEntity;


// Credits to TurtyWurty
// Under MIT-License: https://github.com/DaRealTurtyWurty/1.20-Tutorial-Mod?tab=MIT-1-ov-file#readme
public class TankBlockEntityRenderer {

    /** Default outward offset from a block's own outer faces (0..1) -- just enough to avoid
     * z-fighting against the model's own opaque geometry, which sits exactly at those bounds. Reads
     * fine as "outside" Drooling Cauldron's own narrower, non-cuboid silhouette, but is imperceptible
     * against a genuine full cube (Skin Tank, Masticator, Metastasizer) -- those callers pass their
     * own larger margin to {@link #renderEvolutionOverlay(float, PoseStack, MultiBufferSource, int, float)}
     * instead so the creep actually reads as bulging outward rather than fused with the surface. */
    protected static final float EVOLUTION_OVERLAY_MARGIN = 0.002f;

    /** Translucent red/orange, per direction ("red or orange since we're talking about evolving to
     * high temperatures") -- alpha kept low so it reads as a creeping tint over the block's own
     * texture, not a solid repaint. Packed ARGB: alpha 90, R 255, G 70, B 20. */
    protected static final int EVOLUTION_TINT = (90 << 24) | (255 << 16) | (70 << 8) | 20;

    public TankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {

    }

    /**
     * A creeping tint over a block's own outer shell, scaled by evolution progress --
     * dermicraft-machine-notes.md's Evolution Module design. Originally Drooling Cauldron-only, now
     * shared by every {@code IEvolvingMachine} consumer (Masticator, Metastasizer) via
     * {@code EvolutionOverlayBlockEntityRenderer}. Draws the block's actual outer shell (0-1, plus a
     * hair of outward inset/outset) rather than any inset cavity -- close enough with a simple 6-face
     * box rather than hugging each machine's real (non-cuboid) silhouette exactly; a slightly
     * oversized glow reads fine without needing to match frame/corner-post geometry exactly.
     *
     * <p>Reuses lava's own still texture (fetched fresh here, not passed in, so this still renders
     * during any drained/idle period the caller wants to keep counting as "evolving") so the creep
     * reads as "heat/energy licking up the outside" rather than a flat color card, with the texture's
     * own animation for a simmering effect at no extra cost. Independent of whichever fluid the
     * machine is actually evolving toward -- the overlay is always thermal-flavored.
     */
    protected static void renderEvolutionOverlay(float progressFraction, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        renderEvolutionOverlay(progressFraction, poseStack, buffer, packedLight, EVOLUTION_OVERLAY_MARGIN);
    }

    /** Same as {@link #renderEvolutionOverlay(float, PoseStack, MultiBufferSource, int)}, but with an
     * explicit outward margin instead of the default -- see {@link #EVOLUTION_OVERLAY_MARGIN}'s own
     * javadoc for why a genuine full-cube block needs a much larger one to actually read as bulging
     * outward rather than fused with the block's own surface. */
    protected static void renderEvolutionOverlay(float progressFraction, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float margin) {
        ResourceLocation lavaStill = IClientFluidTypeExtensions.of(Fluids.LAVA).getStillTexture();
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(lavaStill);

        VertexConsumer builder = buffer.getBuffer(RenderType.translucent());
        float height = progressFraction; // full-height creep at 100%, not clipped to any inset max
        float lo = -margin;
        float hi = 1f + margin;

        // Every pushPose/mulPose/translate below is copied UNCHANGED from drawDefaultTop/
        // drawDefaultSides -- only the quad's own bound literals differ (lo/hi instead of 0.1/0.9).
        // Those transforms position the rotated local space's origin; the quad's own size within
        // that space is independent of it, so reusing them verbatim with a differently-sized quad
        // still lands each wall on the correct face, just extending further out symmetrically.

        // Top (horizontal surface).
        drawQuad(builder, poseStack, lo, height, lo, hi, height, hi,
                sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), packedLight, EVOLUTION_TINT);
        // North wall -- no transform, matches drawDefaultTop's own second call exactly.
        drawQuad(builder, poseStack, lo, 0, lo, hi, height, lo,
                sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), packedLight, EVOLUTION_TINT);

        // South wall. translate.z = -(2 * hi), which resolves to world z = hi for whatever hi is.
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        poseStack.translate(-1f, 0, -(2 * hi));
        drawQuad(builder, poseStack, lo, 0, hi, hi, height, hi,
                sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), packedLight, EVOLUTION_TINT);
        poseStack.popPose();

        // West wall.
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        poseStack.translate(-1f, 0, 0);
        drawQuad(builder, poseStack, lo, 0, lo, hi, height, lo,
                sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), packedLight, EVOLUTION_TINT);
        poseStack.popPose();

        // East wall.
        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(90));
        poseStack.translate(0, 0, -1f);
        drawQuad(builder, poseStack, lo, 0, lo, hi, height, lo,
                sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), packedLight, EVOLUTION_TINT);
        poseStack.popPose();
    }

    protected static void drawDefaultTop(VertexConsumer builder, PoseStack poseStack, float height, TextureAtlasSprite sprite, int packedLight, int tintColor) {
        drawQuad(builder, poseStack, 0.1f, height, 0.1f, 0.9f, height, 0.9f, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), packedLight, tintColor);
        drawQuad(builder, poseStack, 0.1f, 0, 0.1f, 0.9f, height, 0.1f, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), packedLight, tintColor);
    }

    protected static void drawDefaultBottom(VertexConsumer builder, PoseStack poseStack, float height, TextureAtlasSprite sprite, int packedLight, int tintColor) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.translate(0, -0.9f, -1f);
        drawQuad(builder, poseStack, 0.1f, height, 0.1f, 0.9f, height, 0.9f, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), packedLight, tintColor);
        poseStack.popPose();
    }

    protected static void drawDefaultSides(VertexConsumer builder, PoseStack poseStack, float height, TextureAtlasSprite sprite, int packedLight, int tintColor) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        poseStack.translate(-1f, 0, -1.8f);
        drawQuad(builder, poseStack, 0.1f, 0, 0.9f, 0.9f, height, 0.9f, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), packedLight, tintColor);
        poseStack.popPose();


        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        poseStack.translate(-1f, 0, 0);
        drawQuad(builder, poseStack, 0.1f, 0, 0.1f, 0.9f, height, 0.1f, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), packedLight, tintColor);
        poseStack.popPose();


        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(90));
        poseStack.translate(0, 0, -1f);
        drawQuad(builder, poseStack, 0.1f, 0, 0.1f, 0.9f, height, 0.1f, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), packedLight, tintColor);
        poseStack.popPose();
    }


    protected static void drawVertex(VertexConsumer builder, PoseStack poseStack, float x, float y, float z, float u, float v, int packedLight, int color) {
        builder.addVertex(poseStack.last().pose(), x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setLight(packedLight)
                .setNormal(1, 0, 0);
    }

    protected static void drawQuad(VertexConsumer builder, PoseStack poseStack, float x0, float y0, float z0, float x1, float y1, float z1, float u0, float v0, float u1, float v1, int packedLight, int color) {
        drawVertex(builder, poseStack, x0, y0, z0, u0, v0, packedLight, color);
        drawVertex(builder, poseStack, x0, y1, z1, u0, v1, packedLight, color);
        drawVertex(builder, poseStack, x1, y1, z1, u1, v1, packedLight, color);
        drawVertex(builder, poseStack, x1, y0, z0, u1, v0, packedLight, color);
    }
}
