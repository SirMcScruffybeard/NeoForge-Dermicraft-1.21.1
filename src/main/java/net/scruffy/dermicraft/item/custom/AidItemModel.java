package net.scruffy.dermicraft.item.custom;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.AidModeData;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

/**
 * Bone-visibility swap driven by real {@link AidModeData} state (see {@link AidItem}) -- each
 * mode's top-level bone (which owns all of that mode's geometry as children with no cubes of
 * their own) is shown only while its mode is active, hidden otherwise. Mirrors SunderItemModel's
 * getBone/setHidden pattern.
 */
public class AidItemModel extends GeoModel<AidItem> {
    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(AidItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "geo/item/aid.geo.json");
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(AidItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/aid/aid.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AidItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "animations/item/aid.animation.json");
    }

    @Override
    public void setCustomAnimations(AidItem animatable, long instanceId, AnimationState<AidItem> state) {
        super.setCustomAnimations(animatable, instanceId, state);

        ItemStack stack = state.getData(DataTickets.ITEMSTACK);
        AidModeData.Mode mode = stack != null ? AidItem.modeData(stack).modeEnum() : AidModeData.Mode.FORCEPS;

        getBone("forceps").ifPresent(bone -> bone.setHidden(mode != AidModeData.Mode.FORCEPS));
        getBone("scalpel").ifPresent(bone -> bone.setHidden(mode != AidModeData.Mode.SCALPEL));
        getBone("suture").ifPresent(bone -> bone.setHidden(mode != AidModeData.Mode.SUTURE));
        getBone("syringe").ifPresent(bone -> bone.setHidden(mode != AidModeData.Mode.SYRINGE));
    }
}
