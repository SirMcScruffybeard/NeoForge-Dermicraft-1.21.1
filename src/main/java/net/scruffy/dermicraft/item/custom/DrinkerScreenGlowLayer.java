package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.event.DrinkerTargetScanner;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * The target-scan "screen" is binary (valid target aimed at or not) with only a single "on"
 * texture authored -- unlike {@link DrinkerGlowLayer}'s mode lights, there's no texture to swap
 * to for "off"; instead this layer simply doesn't render at all when off, letting the base
 * texture's unlit screen art show through.
 *
 * <p>Only ever lights the stack the player is actually HOLDING. The scan state
 * ({@link DrinkerTargetScanner}) is inherently about where the holding player is aiming, so
 * without this guard a DRINKER sitting in an inventory slot would light up alongside the held one.
 */
public class DrinkerScreenGlowLayer extends AutoGlowingGeoLayer<DrinkerItem> {

    private static final ResourceLocation SCREEN_ON =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/drinker/drinker_screen_on.png");

    public DrinkerScreenGlowLayer(GeoRenderer<DrinkerItem> renderer) {
        super(renderer);
    }

    @Override
    protected RenderType getRenderType(DrinkerItem animatable, MultiBufferSource bufferSource) {
        return RenderType.entityTranslucentEmissive(SCREEN_ON);
    }

    @Override
    public void render(PoseStack poseStack, DrinkerItem animatable, BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!DrinkerTargetScanner.hasValidTarget() || !isHeldInHand()) return;
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
    }

    /**
     * Distinguishes a held DRINKER from one drawn in an inventory slot by RENDER PERSPECTIVE, not by
     * comparing stack references against the player's hands.
     *
     * <p>Reference equality looks right but flickers: {@code ItemInHandRenderer} only re-points its
     * cached hand stack at the live one when the two still {@code match()} by components, and the
     * siphon-progress component changes every tick. The renderer therefore hands this layer a stale
     * stack object for most frames, the comparison fails, and the screen blinks in time with the
     * component updates. Perspective is unaffected by any of that.
     */
    private boolean isHeldInHand() {
        if (!(getRenderer() instanceof DrinkerItemRenderer renderer)) return false;
        return switch (renderer.currentPerspective()) {
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND,
                 THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> true;
            default -> false;
        };
    }
}
