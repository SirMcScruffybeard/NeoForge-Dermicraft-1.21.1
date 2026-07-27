package net.scruffy.dermicraft.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Placeholder -- registered only to get the model/texture/animations on screen for a visual check
 * (see [[project_drinker_design]]). Plays idle <-> activate/active_hold/deactivate off a TEMP demo
 * flag; no siphon/mode/hazard/HP game logic yet -- all of that is still pending, same order
 * SIPPING went through.
 */
public class DrinkerItem extends Item implements GeoItem {

    /** TEMP demo state, shared across every stack -- not per-item, not persisted. Cycled by plain
     * right-click, purely to validate the three lit textures ({@link DrinkerGlowLayer}) in-game.
     * 0=storage, 1=transfer, 2=disposal. Remove once real mode state exists (see SippingItem's
     * DEMO_MODE history for the pattern this is following). */
    static int DEMO_MODE = 0;

    /** TEMP demo state -- cycled by offhand right-click, purely to validate the three gauge-step
     * textures ({@link DrinkerGaugeGlowLayer}) in-game. 0=empty (no render), 1=below half,
     * 2=half, 3=full. Remove once real buffer-amount state exists. */
    static int DEMO_GAUGE_STEP = 0;

    /** TEMP demo state -- toggled by offhand sneak-right-click, purely to validate the
     * activate/active_hold/deactivate/idle animation chain in-game. Stands in for the real
     * isUsingItem() siphon-hold state (see the crossbow-pose/timing notes in the design doc) --
     * the real implementation must key game logic off startUsingItem/releaseUsing directly, not
     * this flag or the animation finishing. Remove once the real siphon-hold logic exists. */
    static boolean DEMO_ACTIVE = false;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DrinkerItem(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "Body", 0, state ->
                state.setAndContinue(DEMO_ACTIVE
                        ? RawAnimation.begin().thenPlay("activate").thenLoop("active_hold")
                        : RawAnimation.begin().thenPlay("deactivate").thenLoop("idle")))
                .transitionLength(4));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isCrouching()) {
                DEMO_ACTIVE = !DEMO_ACTIVE;
            } else if (hand == InteractionHand.OFF_HAND) {
                DEMO_GAUGE_STEP = (DEMO_GAUGE_STEP + 1) % 4;
            } else {
                DEMO_MODE = (DEMO_MODE + 1) % 3;
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
