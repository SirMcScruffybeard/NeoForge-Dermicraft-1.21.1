package net.scruffy.dermicraft.item.custom;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

public class ShatterItemModel extends GeoModel<ShatterItem> {

    /** The head assembly's own root bone (parent of "face"/"tail" -- see shatter.geo.json) -- hiding
     * it hides the whole head, not just the tint targets, since GeckoLib hides child bones along with
     * a hidden parent. */
    private static final String PISTON_BONE = "piston";

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(ShatterItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "geo/item/shatter.geo.json");
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(ShatterItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/shatter/shatter.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ShatterItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "animations/item/shatter.animation.json");
    }

    /** No head mounted -- hide the piston assembly entirely rather than showing it as a bare/default
     * placeholder head, matching Sunder's own "the chain disappears off the model" no-chain behavior. */
    @Override
    public void setCustomAnimations(ShatterItem animatable, long instanceId, AnimationState<ShatterItem> state) {
        super.setCustomAnimations(animatable, instanceId, state);

        ItemStack stack = state.getData(DataTickets.ITEMSTACK);
        boolean hasHead = stack != null && ShatterItem.hasMountedHead(stack);
        getBone(PISTON_BONE).ifPresent(bone -> bone.setHidden(!hasHead));
    }
}
