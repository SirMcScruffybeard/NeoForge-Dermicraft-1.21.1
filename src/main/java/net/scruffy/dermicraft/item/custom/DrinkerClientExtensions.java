package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Client-side extensions for the D.R.I.N.K.E.R.: its GeckoLib renderer, plus a hand transform that
 * keeps it perfectly still while siphoning.
 *
 * <p>Vanilla lowers any held item while the player is using one -- {@code ItemInHandRenderer#tick}
 * drives {@code mainHandHeight} to 0 whenever {@code isHandsBusy()}, which pushes equip progress to
 * 1, and {@code applyItemArmTransform} then translates the item down by {@code equipProgress * -0.6}.
 * That happens for EVERY {@link net.minecraft.world.item.UseAnim}, including NONE, so it can't be
 * avoided by choosing a different use animation -- the siphon is a 10-second hold, and watching the
 * rig sink out of frame for all of it looked wrong.
 *
 * <p>Returning true from {@link #applyForgeHandTransform} tells vanilla to skip every remaining
 * transform and render immediately, so this reapplies the standard resting offset with the equip-dip
 * term dropped. Only claims the arm actually holding the DRINKER being used -- every other case
 * returns false so normal equip and swing animations are untouched.
 */
public class DrinkerClientExtensions implements IClientItemExtensions {

    /** Vanilla's resting hand offset from {@code ItemInHandRenderer#applyItemArmTransform}, with
     * its {@code equipProgress * -0.6F} vertical term deliberately omitted. */
    private static final float REST_X = 0.56F;
    private static final float REST_Y = -0.52F;
    private static final float REST_Z = -0.72F;

    private DrinkerItemRenderer renderer;

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (this.renderer == null) this.renderer = new DrinkerItemRenderer();
        return this.renderer;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                                           ItemStack itemInHand, float partialTick, float equipProcess,
                                           float swingProcess) {
        if (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof DrinkerItem)) return false;

        // Only steady the arm that's actually siphoning -- a second DRINKER in the other hand should
        // still behave normally.
        HumanoidArm usingArm = player.getUsedItemHand() == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        if (arm != usingArm) return false;

        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(side * REST_X, REST_Y, REST_Z);
        return true;
    }
}
