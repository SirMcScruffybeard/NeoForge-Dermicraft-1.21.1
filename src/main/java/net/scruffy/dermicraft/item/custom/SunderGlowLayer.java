package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.component.SunderModeData;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.Color;

/**
 * Heat glow on the running chain -- fades in as it revs up, holds while active, fades out on
 * wind-down. Reuses {@code SunderItem.UNREV_TICKS}' duration (5) for both fades rather than
 * inventing new numbers, since that's already the rev-up/rev-down animation length -- the glow
 * reaching full strength lines up with the rev-up animation finishing, and fading out lines up
 * with the wind-down blend.
 *
 * <p>Overrides {@code render(...)} itself, not just {@code getRenderType}, because scaling alpha
 * requires replacing the colour {@link AutoGlowingGeoLayer}'s default render passes to
 * {@code reRender} -- there's no hook to intercept just that value on the default path. The body
 * here mirrors what the default implementation does (confirmed by decompiling 4.8.4's actual
 * class, not assumed from documentation), substituting a computed alpha for the renderer's normal
 * full-strength one.
 */
public class SunderGlowLayer extends AutoGlowingGeoLayer<SunderItem> {

    private static final ResourceLocation RUNNING_CHAIN_GLOW = ResourceLocation.fromNamespaceAndPath(
            Dermicraft.MOD_ID, "textures/item/sunder/sunder_running_chain_glow_layer.png");

    private static final float FADE_TICKS = 5.0F;

    public SunderGlowLayer(GeoRenderer<SunderItem> renderer) {
        super(renderer);
    }

    @Override
    protected RenderType getRenderType(SunderItem animatable, MultiBufferSource bufferSource) {
        return RenderType.entityTranslucentEmissive(RUNNING_CHAIN_GLOW);
    }

    @Override
    public void render(PoseStack poseStack, SunderItem animatable, BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                        int packedLight, int packedOverlay) {
        RenderType glowRenderType = getRenderType(animatable, bufferSource);
        if (glowRenderType == null) return;

        float alphaRatio = glowAlphaRatio(partialTick);
        if (alphaRatio <= 0.0F) return;

        Color renderColor = getRenderer().getRenderColor(animatable, partialTick, packedLight);
        int scaledArgb = (Math.round(0xFF * alphaRatio) << 24) | (renderColor.argbInt() & 0x00FFFFFF);

        // A fresh VertexConsumer bound to the GLOW's render type -- reusing the `buffer` parameter
        // passed into this method would write the glow's geometry into the base pass's buffer
        // instead (wrong texture/render type entirely), which is what made the texture disappear.
        VertexConsumer glowBuffer = bufferSource.getBuffer(glowRenderType);

        // Fullbright (0xF000F0) instead of the real packedLight -- same fixed value the default
        // AutoGlowingGeoLayer render pass uses, so the glow ignores world lighting like any other
        // emissive effect. Only getRenderColor above uses the real light level, matching the
        // decompiled default's own split between the two.
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glowRenderType, glowBuffer,
                partialTick, 0xF000F0, packedOverlay, scaledArgb);
    }

    /** 0 outside ACTIVE/RELEASE_DELAY/UNREVVING (matches SunderItemModel's own running-bone
     * visibility, so this never has to duplicate that decision) -- ramps 0->1 through the rev-up,
     * holds at 1 while engaged, ramps 1->0 through the wind-down. {@code partialTick} smooths the
     * ramp between ticks rather than stepping once per tick. */
    private float glowAlphaRatio(float partialTick) {
        ItemStack stack = getRenderer() instanceof GeoItemRenderer<?> itemRenderer
                ? itemRenderer.getCurrentItemStack() : ItemStack.EMPTY;
        if (stack.isEmpty()) return 0.0F;

        SunderModeData mode = stack.getOrDefault(ModDataComponentTypes.SUNDER_MODE_DATA.get(), SunderModeData.DEFAULT);
        Level level = Minecraft.getInstance().level;
        long now = level != null ? level.getGameTime() : 0L;
        float elapsed = (now - mode.since()) + partialTick;

        return switch (mode.stateEnum()) {
            // ACTIVE covers both the rev_up_down one-shot AND the running hold loop it settles
            // into (see the controller in SunderItem) -- the glow is meant to fade in during the
            // HOLD specifically, not the mechanical rev-up, so elapsed time spent still playing
            // rev_up_down (its first SunderItem.UNREV_TICKS) doesn't count toward the fade at all.
            case ACTIVE -> Mth.clamp((elapsed - SunderItem.UNREV_TICKS) / FADE_TICKS, 0.0F, 1.0F);
            case RELEASE_DELAY -> 1.0F;
            case UNREVVING -> Mth.clamp(1.0F - elapsed / FADE_TICKS, 0.0F, 1.0F);
            case IDLE, ARM_DELAY -> 0.0F;
        };
    }
}
