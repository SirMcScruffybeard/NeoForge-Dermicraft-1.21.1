package net.scruffy.dermicraft.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.interfaces.IEvolvingMachine;

/**
 * Draws Drooling Cauldron's own creeping evolution-progress overlay (see
 * {@link TankBlockEntityRenderer#renderEvolutionOverlay}) for any {@link IEvolvingMachine} that has
 * no visible in-world fluid pool of its own to also render -- Masticator and Metastasizer, unlike
 * Cauldron/Crucible. Generic over the block entity type so one class/registration covers both
 * families instead of a near-duplicate class each.
 */
public class EvolutionOverlayBlockEntityRenderer<T extends BlockEntity & IEvolvingMachine>
        extends TankBlockEntityRenderer implements BlockEntityRenderer<T> {

    /** Masticator/Metastasizer are genuine full cubes (unlike Drooling Cauldron's narrower
     * silhouette), so the overlay needs a real visible margin to read as bulging outward rather than
     * fused with the block's own surface -- see EVOLUTION_OVERLAY_MARGIN's own javadoc. */
    private static final float EVOLUTION_OVERLAY_MARGIN = 0.03f;

    public EvolutionOverlayBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        float progress = blockEntity.getEvolutionProgressFraction();
        if (progress > 0) {
            renderEvolutionOverlay(progress, poseStack, buffer, packedLight, EVOLUTION_OVERLAY_MARGIN);
        }
    }
}
