package net.scruffy.dermicraft.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.component.SippingModeData;
import net.scruffy.dermicraft.hazard.HazardProfile;
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
public class SippingItem extends Item implements GeoItem, IHaveFluidData {

    public static final int CAPACITY = 1000;
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

        // Tier 1 doesn't visually reflect buffer fill level -- may become a higher-tier feature
        // (manual seek of this clip by buffer mB). Registered but never triggered for now.
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

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /**
     * Disposal mode's fluid handler -- bypasses the buffer entirely, consuming anything the
     * hazard profile tolerates the instant it's offered. Never holds or reports contents.
     */
    public static final class DisposalFluidHandler implements IFluidHandlerItem {
        private final ItemStack container;
        private final HazardProfile profile;

        public DisposalFluidHandler(ItemStack stack, HazardProfile profile) {
            this.container = stack;
            this.profile = profile;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return profile.accepts(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !profile.accepts(resource)) return 0;
            return resource.getAmount();
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
