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
import net.scruffy.dermicraft.block.entity.custom.DroolingCauldronBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.DroolingMachineBlockEntity;


// Credits to TurtyWurty
// Under MIT-License: https://github.com/DaRealTurtyWurty/1.20-Tutorial-Mod?tab=MIT-1-ov-file#readme
// Generalized (2026-08-20) to the shared Drooling-family base so Drooling Crucible can reuse this
// same renderer instead of a near-duplicate class -- nothing here ever depended on which specific
// fluid or machine, only on getFluid()/getTank(), both on DroolingMachineBlockEntity itself.
public class DroolingCauldronBlockEntityRenderer extends TankBlockEntityRenderer implements BlockEntityRenderer<DroolingMachineBlockEntity<?>> {

    /** Tiny outward offset from the block's own outer faces (0..1) -- avoids z-fighting against the
     * model's own opaque geometry, which sits exactly at those bounds. */
    private static final float OVERLAY_INSET = -0.002f;
    private static final float OVERLAY_OUTSET = 1.002f;

    /** Translucent red/orange, per direction ("red or orange since we're talking about evolving to
     * high temperatures") -- alpha kept low so it reads as a creeping tint over the block's own
     * texture, not a solid repaint. Packed ARGB: alpha 90, R 255, G 70, B 20. */
    private static final int EVOLUTION_TINT = (90 << 24) | (255 << 16) | (70 << 8) | 20;

    public DroolingCauldronBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DroolingMachineBlockEntity<?> pBlockEntity, float partialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int packedOverlay) {
        Level level = pBlockEntity.getLevel();
        if (level == null) return;

        FluidStack fluidStack = pBlockEntity.getFluid();
        if (!fluidStack.isEmpty()) {
            renderFluidPool(pBlockEntity, fluidStack, level, pPoseStack, pBuffer, pPackedLight);
        }

        // Evolution overlay is Drooling Cauldron-only -- Crucible is already the end state, it never
        // has anything to creep toward. Independent of whether the tank currently has fluid in it
        // (the halt-while-draining dead period still counts as "evolving" from the player's POV).
        if (pBlockEntity instanceof DroolingCauldronBlockEntity cauldron) {
            float progress = cauldron.getEvolutionProgressFraction();
            if (progress > 0) {
                renderEvolutionOverlay(progress, pPoseStack, pBuffer, pPackedLight);
            }
        }
    }

    private void renderFluidPool(DroolingMachineBlockEntity<?> pBlockEntity, FluidStack fluidStack, Level level,
                                  PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        BlockPos pos = pBlockEntity.getBlockPos();

        IClientFluidTypeExtensions fluidTypeExtensions = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        ResourceLocation stillTexture = fluidTypeExtensions.getStillTexture(fluidStack);

        FluidState state = fluidStack.getFluid().defaultFluidState();

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
        int tintColor = fluidTypeExtensions.getTintColor(state, level, pos);

        float height = (((float) pBlockEntity.getTank(null).getFluidInTank(0).getAmount() / pBlockEntity.getTank(null).getTankCapacity(0)) * 0.625f) + 0.25f;

        VertexConsumer builder = pBuffer.getBuffer(ItemBlockRenderTypes.getRenderLayer(state));

        drawDefaultTop(builder, pPoseStack, height, sprite, pPackedLight, tintColor);
        drawDefaultBottom(builder, pPoseStack, height, sprite, pPackedLight, tintColor);
        drawDefaultSides(builder, pPoseStack, height, sprite, pPackedLight, tintColor);
    }

    /**
     * A creeping tint over the Cauldron's own outer surface, scaled by evolution progress --
     * dermicraft-machine-notes.md's Evolution Module design. Deliberately new geometry, not a reuse
     * of {@link #drawDefaultTop}/{@link #drawDefaultBottom}/{@link #drawDefaultSides}: those draw the
     * INSET bowl cavity the fluid pool sits in (0.1-0.9), shaped for a liquid recessed into a
     * container. This needs the block's actual outer shell (0-1) instead -- close enough with a
     * simple 6-face box rather than hugging the model's real (non-cuboid) silhouette exactly; a
     * slightly-oversized glow reads fine without needing to match the frame/corner-post geometry
     * exactly.
     *
     * <p>Reuses the fluid's own still texture (fetched fresh here, not passed in, since this must
     * still render during the drained-tank part of the halt-while-draining period) so the creep
     * reads as "heat/energy licking up the outside" rather than a flat color card, with the
     * texture's own animation for a simmering effect at no extra cost.
     */
    private void renderEvolutionOverlay(float progressFraction, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ResourceLocation lavaStill = IClientFluidTypeExtensions.of(Fluids.LAVA).getStillTexture();
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(lavaStill);

        VertexConsumer builder = buffer.getBuffer(RenderType.translucent());
        float height = progressFraction; // full-height creep at 100%, not clipped to any inset max
        float lo = OVERLAY_INSET;
        float hi = OVERLAY_OUTSET;

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

        // South wall. The -1.8 translate.z here was NOT a transform that stays valid for any quad
        // bound -- it was tuned specifically for drawDefaultSides' own 0.9 literal (working out to
        // world z = 1.8 - 0.9 = 0.9, i.e. translate.z = -(2 * 0.9)). Copying it unchanged while
        // swapping the quad's own bound to `hi` left the two out of sync: this wall was landing at
        // world z = 1.8 - hi = 0.798, well short of the other three faces' outward bound at `hi`.
        // Correct in general: translate.z = -(2 * hi), which resolves to world z = hi for whatever
        // hi actually is.
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
}
