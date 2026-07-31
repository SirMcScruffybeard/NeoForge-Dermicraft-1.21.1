package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.BulkSlot;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Renders each occupied buffer slot's actual item, floating at its matching screen bone
 * ("1".."4") -- not a baked icon texture, a live {@link ItemStack} render, same renderer used for
 * item entities/item frames. Built on GeckoLib's own {@link BlockAndItemGeoLayer}, which already
 * handles applying the bone's transform before rendering; this only decides which stack belongs
 * at which bone.
 *
 * <p>{@code SCREEN_ITEM_SCALE}: GeckoLib models use Blockbench's 16-units-per-block convention,
 * and the screen cubes are ~0.8 units across (~0.05 of a block) against a default item render's
 * full-block footprint -- hence the small factor. 0.05 confirmed a good fit in-game; nudged down
 * slightly since a full-height item was touching the screen's top/bottom edges at that size.
 */
public class EaterItemDisplayLayer extends BlockAndItemGeoLayer<EaterItem> {

    private static final float SCREEN_ITEM_SCALE = 0.045F;

    public EaterItemDisplayLayer(GeoRenderer<EaterItem> renderer) {
        super(renderer);
    }

    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, EaterItem animatable) {
        int slotIndex = slotIndexForBone(bone.getName());
        if (slotIndex < 0) return null;

        if (!(getRenderer() instanceof GeoItemRenderer<?> itemRenderer)) return null;
        ItemStack stack = itemRenderer.getCurrentItemStack();
        if (stack == null || stack.isEmpty()) return null;

        BulkSlot slot = EaterItem.bufferData(stack).slot(slotIndex);
        return slot.isEmpty() ? null : slot.asDisplayStack();
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, EaterItem animatable) {
        return ItemDisplayContext.FIXED;
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, EaterItem animatable,
                                      MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        // The 3D item models render backwards on these screens otherwise -- flip to face the
        // viewer.
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        poseStack.scale(SCREEN_ITEM_SCALE, SCREEN_ITEM_SCALE, SCREEN_ITEM_SCALE);
        super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static int slotIndexForBone(String boneName) {
        return switch (boneName) {
            case "1" -> 0;
            case "2" -> 1;
            case "3" -> 2;
            case "4" -> 3;
            default -> -1;
        };
    }
}
