package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Client-side extensions for E.A.T.E.R.: its GeckoLib renderer, plus the same steady-hand
 * transform DRINKER uses while its held-trigger action is active -- see
 * {@link DrinkerClientExtensions}'s javadoc for why vanilla's equip-dip has to be overridden this
 * way rather than through the use animation.
 */
public class EaterClientExtensions implements IClientItemExtensions {

    private static final float REST_X = 0.56F;
    private static final float REST_Y = -0.52F;
    private static final float REST_Z = -0.72F;

    private EaterItemRenderer renderer;

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (this.renderer == null) this.renderer = new EaterItemRenderer();
        return this.renderer;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                                           ItemStack itemInHand, float partialTick, float equipProcess,
                                           float swingProcess) {
        if (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof EaterItem)) return false;

        HumanoidArm usingArm = player.getUsedItemHand() == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        if (arm != usingArm) return false;

        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(side * REST_X, REST_Y, REST_Z);
        return true;
    }
}
