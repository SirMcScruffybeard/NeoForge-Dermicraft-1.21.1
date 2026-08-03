package net.scruffy.dermicraft.item.custom;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.component.SunderModeData;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

/**
 * Chain bone-visibility swap driven by real SunderModeData state (see SunderItem) -- idle bone
 * shows for IDLE/ARM_DELAY (nothing's visibly changed yet), running bone shows from ACTIVE through
 * UNREVVING (stays until the wind-down's transition blend finishes, matching the design notes'
 * "chain swaps back after the reverse animation ends" rule).
 */
public class SunderItemModel extends GeoModel<SunderItem> {
    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(SunderItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "geo/item/sunder.geo.json");
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(SunderItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/sunder/sunder.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SunderItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "animations/item/sunder.animation.json");
    }

    @Override
    public void setCustomAnimations(SunderItem animatable, long instanceId, AnimationState<SunderItem> state) {
        super.setCustomAnimations(animatable, instanceId, state);

        ItemStack stack = state.getData(DataTickets.ITEMSTACK);
        SunderModeData mode = stack != null
                ? stack.getOrDefault(ModDataComponentTypes.SUNDER_MODE_DATA.get(), SunderModeData.DEFAULT)
                : SunderModeData.DEFAULT;

        // No chain mounted (never installed, or broken from wear) -- hide BOTH variants so the bar
        // renders bare, matching the decided "the chain disappears off the model" behavior.
        if (stack == null || !SunderItem.hasMountedChain(stack)) {
            getBone("idle").ifPresent(bone -> bone.setHidden(true));
            getBone("running").ifPresent(bone -> bone.setHidden(true));
            return;
        }

        boolean showRunning = switch (mode.stateEnum()) {
            case ACTIVE, RELEASE_DELAY, UNREVVING -> true;
            case IDLE, ARM_DELAY -> false;
        };

        getBone("idle").ifPresent(bone -> bone.setHidden(showRunning));
        getBone("running").ifPresent(bone -> bone.setHidden(!showRunning));
    }
}
