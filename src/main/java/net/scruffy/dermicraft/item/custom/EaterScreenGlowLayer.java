package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * One instance per screen bone ("1".."4"). Unlike {@link EaterGlowLayer}'s three mutually
 * exclusive mode lights, Eater's four screens can be lit independently and simultaneously -- any
 * subset of the 4 buffer slots can be occupied at once -- so each screen gets its own layer
 * instance rather than one layer picking between textures, and each is skipped entirely (not left
 * dark via texture) when its slot is empty, same "don't render at all" approach
 * {@code DrinkerGaugeGlowLayer} uses for an empty buffer.
 */
public class EaterScreenGlowLayer extends AutoGlowingGeoLayer<EaterItem> {

    private final int slotIndex;
    private final ResourceLocation lit;

    public EaterScreenGlowLayer(GeoRenderer<EaterItem> renderer, int slotIndex, ResourceLocation lit) {
        super(renderer);
        this.slotIndex = slotIndex;
        this.lit = lit;
    }

    @Override
    protected RenderType getRenderType(EaterItem animatable, MultiBufferSource bufferSource) {
        return RenderType.entityTranslucentEmissive(lit);
    }

    @Override
    public void render(PoseStack poseStack, EaterItem animatable, BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!occupied()) return;
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
    }

    private boolean occupied() {
        if (!(getRenderer() instanceof GeoItemRenderer<?> itemRenderer)) return false;
        ItemStack stack = itemRenderer.getCurrentItemStack();
        if (stack == null || stack.isEmpty()) return false;
        return !EaterItem.bufferData(stack).slot(slotIndex).isEmpty();
    }
}
