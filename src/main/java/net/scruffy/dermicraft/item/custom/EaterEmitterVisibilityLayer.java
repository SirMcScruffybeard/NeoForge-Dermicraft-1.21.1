package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Hides the modeled {@code emitter} bone unless a Beam Module is installed -- the beam itself is
 * drawn entirely separately (see {@link EaterBeamRenderer}), but the housing it visually comes from
 * shouldn't be there on an Eater that can't actually beam-mine. Runs every render via
 * {@link #preRender}, not an animation clip: this is a structural on/off, not something that needs
 * to blend/transition.
 */
public class EaterEmitterVisibilityLayer extends GeoRenderLayer<EaterItem> {

    private static final String EMITTER_BONE = "emitter";

    public EaterEmitterVisibilityLayer(GeoRenderer<EaterItem> renderer) {
        super(renderer);
    }

    @Override
    public void preRender(PoseStack poseStack, EaterItem animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType,
                           MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick,
                           int packedLight, int packedOverlay) {
        if (!(getRenderer() instanceof GeoItemRenderer<?> itemRenderer)) return;
        ItemStack stack = itemRenderer.getCurrentItemStack();
        boolean hasBeam = stack != null && !stack.isEmpty() && EaterItem.hasModule(stack, ModTags.Items.MODULE_BEAM);

        bakedModel.getBone(EMITTER_BONE).ifPresent(bone -> bone.setHidden(!hasBeam));
    }
}
