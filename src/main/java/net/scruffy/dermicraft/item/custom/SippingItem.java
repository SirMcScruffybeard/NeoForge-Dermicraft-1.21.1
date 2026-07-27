package net.scruffy.dermicraft.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.component.SippingModeData;
import net.scruffy.dermicraft.interfaces.IGadget;
import net.scruffy.dermicraft.interfaces.IHaveFluidData;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * S.I.P.P.I.N.G. -- GUI fill-slot fluid maintenance tool. Storage mode is a flexible 0-1000mB
 * two-way buffer; Disposal mode voids fluid on drain-in with no held state. Switching Storage to
 * Disposal while the buffer is non-empty requires an arm/confirm (see {@link SippingModeData}).
 *
 * <p>Model/animation wiring ({@link SippingItemModel}, {@link SippingItemRenderer},
 * {@link SippingGlowLayer}) was validated separately against demo state; this class now drives
 * those from real per-stack state instead.
 */
public class SippingItem extends Item implements GeoItem, IHaveFluidData, IGadget {

    public static final int CAPACITY = 1000;

    /** Gadget health, expressed as vanilla durability -- see {@link IGadget}. Registered via
     * {@code Item.Properties#durability}, which is the single source of truth for max HP. */
    public static final int MAX_HP = 10;

    /** Minimum ticks after arming before a confirm click counts (blocks accidental double-clicks). */
    private static final long MIN_CONFIRM_DELAY_TICKS = 20;
    /** Total ticks the arm window stays open before auto-cancelling. */
    private static final long ARM_WINDOW_TICKS = 60;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SippingItem(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "Idle", 0, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            SippingModeData mode = stack != null
                    ? stack.getOrDefault(ModDataComponentTypes.SIPPING_MODE_DATA.get(), SippingModeData.DEFAULT)
                    : SippingModeData.DEFAULT;

            String clip = mode.armed() ? "waiting_open" : (mode.disposalMode() ? "disposal_idle" : "storage_idle");
            return state.setAndContinue(RawAnimation.begin().thenLoop(clip));
        }));

        // DELIBERATELY DORMANT -- registered but nothing ever triggers it, so the accordion body
        // never moves. Not dead code left by accident: the "fill" clip is authored and the
        // controller is wired so a future tier can switch it on, but Tier 1 was decided not to show
        // buffer level on the model at all (the tooltip reports contents instead).
        //
        // Reviving it means driving the clip by buffer mB, which needs the controller SEEKED to a
        // position rather than played -- overriding the protected AnimationController#adjustTick.
        // That was never verified against this GeckoLib version and is why it was shelved; treat it
        // as unproven, not as a small change.
        controllers.add(new AnimationController<>(this, "Fill", 0, state -> PlayState.STOP)
                .triggerableAnim("fill", RawAnimation.begin().thenPlay("fill")));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);

        SippingModeData mode = stack.getOrDefault(ModDataComponentTypes.SIPPING_MODE_DATA.get(), SippingModeData.DEFAULT);
        long now = level.getGameTime();

        if (mode.armed()) {
            long elapsed = now - mode.armedAtGameTime();
            if (elapsed < MIN_CONFIRM_DELAY_TICKS) {
                // Inside the dead zone -- silently absorbed, arm state untouched.
                return InteractionResultHolder.sidedSuccess(stack, false);
            }
            if (elapsed <= ARM_WINDOW_TICKS) {
                emptyFluidData(stack);
                stack.set(ModDataComponentTypes.SIPPING_MODE_DATA.get(), new SippingModeData(true, false, 0L));
                player.displayClientMessage(Component.translatable("tooltip.dermicraft.sipping.voided").withStyle(ChatFormatting.RED), true);
            } else {
                // Window already expired -- inventoryTick normally catches this first, handled here defensively.
                stack.set(ModDataComponentTypes.SIPPING_MODE_DATA.get(), new SippingModeData(false, false, 0L));
            }
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        boolean targetDisposal = !mode.disposalMode();
        FluidStack buffered = stack.getOrDefault(getDataType(), net.scruffy.dermicraft.component.FluidData.EMPTY).getFluidStack();
        if (targetDisposal && !buffered.isEmpty()) {
            stack.set(ModDataComponentTypes.SIPPING_MODE_DATA.get(), new SippingModeData(false, true, now));
            player.displayClientMessage(Component.translatable("tooltip.dermicraft.sipping.disposal_warning").withStyle(ChatFormatting.RED), true);
        } else {
            stack.set(ModDataComponentTypes.SIPPING_MODE_DATA.get(), new SippingModeData(targetDisposal, false, 0L));
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) return;

        SippingModeData mode = stack.getOrDefault(ModDataComponentTypes.SIPPING_MODE_DATA.get(), SippingModeData.DEFAULT);
        if (!mode.armed()) return;

        boolean stillHeld = entity instanceof Player player
                && (player.getMainHandItem() == stack || player.getOffhandItem() == stack);
        long elapsed = level.getGameTime() - mode.armedAtGameTime();

        if (!stillHeld || elapsed > ARM_WINDOW_TICKS) {
            stack.set(ModDataComponentTypes.SIPPING_MODE_DATA.get(), new SippingModeData(mode.disposalMode(), false, 0L));
        }
    }

    /**
     * A small, wet thing bursting: slime spatter and a short, high squeal. Same cry as
     * D.R.I.N.K.E.R.'s, pitched well up, so they read as the same creature at very different sizes
     * -- the family resemblance is intentional, the scale difference is the point.
     */
    @Override
    public void onGadgetDeath(ServerLevel level, ItemEntity entity, ItemStack stack) {
        IGadget.deathFlourish(level, entity, ParticleTypes.ITEM_SLIME, 18, 0.14,
                SoundEvents.GHAST_HURT, 0.7F, 1.7F);
        IGadget.deathFlourish(level, entity, ParticleTypes.SMOKE, 6, 0.12,
                SoundEvents.SLIME_SQUISH, 0.8F, 1.4F);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
