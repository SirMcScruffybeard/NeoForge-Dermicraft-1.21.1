package net.scruffy.dermicraft.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.block.entity.custom.WorkbenchBlockEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Floats the Mod page's working-item (WORK_ITEM slot 0 -- the gadget currently being serviced) on
 * the table surface, under the bottom half's own "item_display" bone -- a static, unparented bone
 * sitting at the model's top-center (see workbench_bottom.geo.json), clear of the "screen" bone's
 * corner. Only rendered while open -- see WorkbenchBlockEntity#isOpen -- same rationale as
 * WorkbenchItemDisplayLayer on the top half.
 */
public class WorkbenchWorkItemDisplayLayer extends BlockAndItemGeoLayer<WorkbenchBlockEntity> {

    private static final String BONE_NAME = "item_display";
    private static final float ITEM_SCALE = 0.4F;

    // Tunable knobs -- see WorkbenchItemDisplayLayer's identical set for the axis/rotation
    // convention (X = east(+)/west(-), Y = up(+)/down(-), Z = south(+)/north(-); rotation in
    // degrees, X = pitch, Y = yaw, Z = roll).
    private static final float OFFSET_X = 0.0F;
    private static final float OFFSET_Y = 0.0F;
    private static final float OFFSET_Z = 0.0F;
    private static final float ROTATION_X = 90.0F;
    private static final float ROTATION_Y = 0.0F;
    private static final float ROTATION_Z = 0.0F;

    public WorkbenchWorkItemDisplayLayer(GeoRenderer<WorkbenchBlockEntity> renderer) {
        super(renderer);
    }

    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, WorkbenchBlockEntity animatable) {
        if (!bone.getName().equals(BONE_NAME) || !animatable.isOpen()) return null;

        ItemStack stack = animatable.WORK_ITEM.getStackInSlot(0);
        return stack.isEmpty() ? null : stack;
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, WorkbenchBlockEntity animatable) {
        return ItemDisplayContext.FIXED;
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, WorkbenchBlockEntity animatable,
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
