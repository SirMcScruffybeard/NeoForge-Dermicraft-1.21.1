package net.scruffy.dermicraft.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.block.entity.custom.WorkbenchBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.WorkbenchTopBlockEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Floats the paired bottom's Storage slot 0 item under the top half's "item_display" bone -- a
 * static, unparented bone sitting at the model's bottom-center (see workbench_top.geo.json), so it
 * doesn't swing along with "main"'s open/close animation. Only rendered while open, and only once
 * the "activate" swing has actually finished (see WorkbenchTopBlockEntity#isPastActivateAnimation)
 * rather than popping in mid-animation, so players who've walked up and opened the GUI can see
 * what's in the first storage slot without scrolling into the strip.
 */
public class WorkbenchItemDisplayLayer extends BlockAndItemGeoLayer<WorkbenchTopBlockEntity> {

    private static final String BONE_NAME = "item_display";
    // Blockbench's 16-units-per-block convention against a default item render's full-block
    // footprint -- shrunk down so the item reads as a preview, not a floating full-size block.
    private static final float ITEM_SCALE = 0.4F;

    // Tunable knobs -- adjust these rather than the bone pivot, since the pivot only controls
    // where "item_display" sits, not the item render's own alignment within it. Axes are the usual
    // Minecraft world convention: X = east(+)/west(-), Y = up(+)/down(-), Z = south(+)/north(-).
    // Rotation is in degrees around each axis (X = pitch/tips forward-back, Y = yaw/spins in place,
    // Z = roll/tips side-to-side).
    private static final float OFFSET_X = 0.0F;
    private static final float OFFSET_Y = 0.575F;
    private static final float OFFSET_Z = 0.5625F;
    private static final float ROTATION_X = 0.0F;
    private static final float ROTATION_Y = 0.0F;
    private static final float ROTATION_Z = 0.0F;

    public WorkbenchItemDisplayLayer(GeoRenderer<WorkbenchTopBlockEntity> renderer) {
        super(renderer);
    }

    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, WorkbenchTopBlockEntity animatable) {
        if (!bone.getName().equals(BONE_NAME) || !animatable.isOpen()) return null;
        // Waits out the activate animation's own length before appearing -- see
        // WorkbenchTopBlockEntity#isPastActivateAnimation -- instead of popping in mid-swing.
        if (!animatable.isPastActivateAnimation()) return null;

        BlockPos belowPos = animatable.getBlockPos().below();
        if (animatable.getLevel() == null
                || !(animatable.getLevel().getBlockEntity(belowPos) instanceof WorkbenchBlockEntity bottom)) {
            return null;
        }

        ItemStack stack = bottom.STORAGE.getStackInSlot(0);
        return stack.isEmpty() ? null : stack;
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, WorkbenchTopBlockEntity animatable) {
        return ItemDisplayContext.FIXED;
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, WorkbenchTopBlockEntity animatable,
                                      MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(OFFSET_X, OFFSET_Y, OFFSET_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(ROTATION_X));
        poseStack.mulPose(Axis.YP.rotationDegrees(ROTATION_Y));
        poseStack.mulPose(Axis.ZP.rotationDegrees(ROTATION_Z));
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
