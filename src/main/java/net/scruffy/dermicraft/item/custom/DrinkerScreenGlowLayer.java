package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.event.DrinkerTargetScanner;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
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
        if (!DrinkerTargetScanner.hasValidTarget() || !isHeldStack()) return;
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
    }

    /** Reference-equality against the player's hands -- the held stack is the same object the
     * renderer was handed, the same check {@code SippingItem.inventoryTick} already relies on. */
    private boolean isHeldStack() {
        if (!(getRenderer() instanceof GeoItemRenderer<?> itemRenderer)) return false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;

        ItemStack rendered = itemRenderer.getCurrentItemStack();
        return player.getMainHandItem() == rendered || player.getOffhandItem() == rendered;
    }
}
