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
 * Knowledge Head's own signature glow -- always on while mounted, unlike the Chain's idle-only
 * version (Shatter has no rev/mode state to gate against in the first place). Gated to the
 * Knowledge head specifically -- every other material renders with no layer at all, same "no-op
 * when not applicable" shape {@link ShatterHeadTextureLayer} uses for its own per-material overlay.
 *
 * <p>Uses the same "re-render the full baked model bound to an emissive render type" technique as
 * {@link SunderGlowLayer}/{@link KnowledgeChainGlowLayer} -- flat on/off, no fade.
 */
public class KnowledgeHeadGlowLayer extends GeoRenderLayer<ShatterItem> {

    private static final ResourceLocation KNOWLEDGE_GLOW = ResourceLocation.fromNamespaceAndPath(
            Dermicraft.MOD_ID, "textures/item/shatter/shatter_knowledge.png");

    public KnowledgeHeadGlowLayer(GeoRenderer<ShatterItem> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, ShatterItem animatable, BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                        int packedLight, int packedOverlay) {
        if (!isKnowledgeHeadMounted()) return;

        RenderType glowRenderType = RenderType.entityTranslucentEmissive(KNOWLEDGE_GLOW);
        VertexConsumer glowBuffer = bufferSource.getBuffer(glowRenderType);

        // Fullbright (0xF000F0), same fixed value SunderGlowLayer's own glow pass uses, so this
        // ignores world lighting like any other emissive effect.
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glowRenderType, glowBuffer,
                partialTick, 0xF000F0, packedOverlay, -1);
    }

    private boolean isKnowledgeHeadMounted() {
        ItemStack stack = getRenderer() instanceof GeoItemRenderer<?> itemRenderer
                ? itemRenderer.getCurrentItemStack() : ItemStack.EMPTY;
        if (stack.isEmpty()) return false;

        ItemStack head = ShatterItem.mountedHead(stack).itemStack();
        return head.getItem() == ModItems.KNOWLEDGE_SHATTER_HEAD.get();
    }
}
