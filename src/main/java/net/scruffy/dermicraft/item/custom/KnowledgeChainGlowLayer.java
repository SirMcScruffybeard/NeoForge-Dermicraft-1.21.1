package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.component.SunderModeData;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Knowledge Chain's own signature glow -- opposite condition from {@link SunderGlowLayer}'s heat
 * effect: lit only while {@code IDLE} (the chain isn't doing anything else that would already be
 * drawing attention), dark the instant it starts revving. Gated to the Knowledge chain specifically
 * -- every other material renders with no layer at all, same "no-op when not applicable" shape
 * {@link ShatterHeadTextureLayer} uses for its own per-material overlay.
 *
 * <p>Uses the same "re-render the full baked model bound to an emissive render type" technique as
 * {@link SunderGlowLayer}, just without that layer's alpha-fade math -- Knowledge's glow is a flat
 * on/off (idle or not), no rev-up/wind-down animation to track.
 */
public class KnowledgeChainGlowLayer extends GeoRenderLayer<SunderItem> {

    private static final ResourceLocation KNOWLEDGE_IDLE_GLOW = ResourceLocation.fromNamespaceAndPath(
            Dermicraft.MOD_ID, "textures/item/sunder/sunder_knowledge_idle_chain.png");

    public KnowledgeChainGlowLayer(GeoRenderer<SunderItem> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, SunderItem animatable, BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                        int packedLight, int packedOverlay) {
        if (!isKnowledgeChainIdle()) return;

        RenderType glowRenderType = RenderType.entityTranslucentEmissive(KNOWLEDGE_IDLE_GLOW);
        VertexConsumer glowBuffer = bufferSource.getBuffer(glowRenderType);

        // Fullbright (0xF000F0), same fixed value SunderGlowLayer's own glow pass uses, so this
        // ignores world lighting like any other emissive effect.
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glowRenderType, glowBuffer,
                partialTick, 0xF000F0, packedOverlay, -1);
    }

    private boolean isKnowledgeChainIdle() {
        ItemStack stack = getRenderer() instanceof GeoItemRenderer<?> itemRenderer
                ? itemRenderer.getCurrentItemStack() : ItemStack.EMPTY;
        if (stack.isEmpty()) return false;

        ItemStack chain = SunderItem.mountedChain(stack).itemStack();
        if (chain.getItem() != ModItems.KNOWLEDGE_SUNDER_CHAIN.get()) return false;

        SunderModeData mode = stack.getOrDefault(ModDataComponentTypes.SUNDER_MODE_DATA.get(), SunderModeData.DEFAULT);
        return mode.stateEnum() == SunderModeData.State.IDLE;
    }
}
