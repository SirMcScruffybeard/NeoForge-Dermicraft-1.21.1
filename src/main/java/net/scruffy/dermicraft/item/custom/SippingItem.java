package net.scruffy.dermicraft.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.component.SippingModeData;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.interfaces.IGadget;
import net.scruffy.dermicraft.interfaces.IHaveFluidData;
import net.scruffy.dermicraft.interfaces.IHaveItemData;
import net.scruffy.dermicraft.interfaces.IHaveModules;
import net.scruffy.dermicraft.interfaces.IWorkbenchSwappable;
import net.scruffy.dermicraft.screen.custom.scrench.ScrenchMenu;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * S.I.P.P.I.N.G. -- GUI fill-slot fluid maintenance tool. Storage mode is a flexible 0-1000mB
 * two-way buffer; Disposal mode voids fluid on drain-in with no held state. Switching Storage to
 * Disposal while the buffer is non-empty requires an arm/confirm (see {@link SippingModeData}).
 *
 * <p>Model/animation wiring ({@link SippingItemModel}, {@link SippingItemRenderer},
 * {@link SippingGlowLayer}) was validated separately against demo state; this class now drives
 * those from real per-stack state instead.
 */
public class SippingItem extends Item implements GeoItem, IHaveFluidData, IGadget, IHaveModules, IWorkbenchSwappable {

    public static final int CAPACITY = 1000;

    /** Gadget health, expressed as vanilla durability -- see {@link IGadget}. Registered via
     * {@code Item.Properties#durability}, which is the single source of truth for max HP. */
    public static final int MAX_HP = 10;

    /** Minimum ticks after arming before a confirm click counts (blocks accidental double-clicks). */
    private static final long MIN_CONFIRM_DELAY_TICKS = 20;
    /** Total ticks the arm window stays open before auto-cancelling. */
    private static final long ARM_WINDOW_TICKS = 60;

    /** Module loadout size -- third consumer of the shared Module system, same rationale as
     * Drinker's identical constant: 1 general-purpose slot, Safety-only catalog, no mouthpiece-style
     * specialties competing for the budget. Backed by {@link ModDataComponentTypes#SIPPING_MODULE_DATA}. */
    public static final int MODULE_SLOT_COUNT = 1;
    /** Exactly one Module per slot -- these aren't stackable resources. */
    public static final int MODULE_SLOT_CAPACITY = IHaveModules.DEFAULT_MODULE_SLOT_CAPACITY;

    /** Same field-swap cost shape as Eater's/Drinker's own Module slot -- see those classes'
     * identical constant for the reasoning. */
    private static final int SWAP_RECALIBRATION_COOLDOWN_TICKS = 40;

    /**
     * Tier 1 base, plus whatever hazard kinds any currently-installed Safety Module grants -- see
     * {@link IHaveModules#installedHazardProfile}. Governs BOTH fluid handlers registered for
     * S.I.P.P.I.N.G. in {@code ModBusEvents} (Storage's buffer and Disposal's void), so a Safety
     * Module makes Sipping a valid destination/source for that hazard in either mode, not just one.
     */
    public static HazardProfile installedHazardProfile(ItemStack stack) {
        return IHaveModules.installedHazardProfile(stack, ModDataComponentTypes.SIPPING_MODULE_DATA.get(),
                MODULE_SLOT_COUNT, HazardProfile.TIER_1);
    }

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
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

        // Checks the other hand for a paired Scrench first, deferring to the Module-swap GUI if
        // found -- same pattern as Drinker's/Sunder's own matching check, and same reason it has to
        // come before tryHandTransfer below: a Scrench isn't a fluid handler, so tryHandTransfer
        // would just fail through to here anyway, but this makes the intent explicit rather than
        // relying on that fallthrough.
        if (player.getItemInHand(otherHand).getItem() instanceof ScrenchItem) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                ScrenchMenu.open(serverPlayer, hand);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);

        if (tryHandTransfer(player, hand, otherHand)) {
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), false);
        }

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

    /**
     * Bucket-on-cauldron style quick transfer: right-clicking Sipping while the other hand holds
     * any fluid-handler item moves fluid directly between the two, no machine slot needed. Tries
     * draining Sipping's own buffer into the other item first, then the reverse -- whichever
     * direction actually has something to move wins, so Disposal mode (an always-empty source)
     * naturally falls through to accepting fluid from the other item.
     *
     * <p>Handlers aren't guaranteed to mutate the stack we handed them in place -- vanilla's bucket
     * wrapper represents a fill/drain as swapping to a whole different {@code Item} (empty bucket
     * &lt;-&gt; water bucket) and only reflects that on its own {@code getContainer()}, not on the
     * original stack. Our own component-based items already are their own container, so writing
     * both containers back into the player's hands afterward is a no-op for them and the fix for
     * everything else.
     */
    private boolean tryHandTransfer(Player player, InteractionHand hand, InteractionHand otherHand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack otherStack = player.getItemInHand(otherHand);
        if (otherStack.isEmpty()) return false;

        IFluidHandlerItem sippingHandler = stack.getCapability(Capabilities.FluidHandler.ITEM, null);
        IFluidHandlerItem otherHandler = otherStack.getCapability(Capabilities.FluidHandler.ITEM, null);
        if (sippingHandler == null || otherHandler == null) return false;

        FluidStack moved = FluidUtil.tryFluidTransfer(otherHandler, sippingHandler, Integer.MAX_VALUE, true);
        if (moved.isEmpty()) {
            moved = FluidUtil.tryFluidTransfer(sippingHandler, otherHandler, Integer.MAX_VALUE, true);
        }
        if (moved.isEmpty()) return false;

        player.setItemInHand(hand, sippingHandler.getContainer());
        player.setItemInHand(otherHand, otherHandler.getContainer());
        player.displayClientMessage(Component.translatable("tooltip.dermicraft.sipping.transferred", moved.getAmount()), true);
        return true;
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

    ////////////////////IWorkbenchSwappable (Scrench field / Workbench station swap panel)\\\\\\\\\\\\\\\\\\\\

    // Panel layout -- public so ScrenchScreen/WorkbenchScreen can draw the matching background
    // under exactly the slot SippingSwapPanel builds, same convention every other panel constant in
    // this mod follows. Same coordinates as Drinker's own Module slot -- no reason for these to
    // differ between the two single-slot gadgets.
    public static final int MODULE_SLOT_X = 8;
    public static final int MODULE_SLOT_Y = 27;

    // Buffer gauge + fill/drain slot -- same tank-above-slot pairing as Drinker's own (and Sunder's/
    // Shatter's fuel gauge before that), same coordinates as Drinker's DRAIN_SLOT_X/Y since nothing
    // else competes for the space on either panel.
    public static final int FILL_DRAIN_SLOT_X = 113;
    public static final int FILL_DRAIN_SLOT_Y = 60;
    public static final int TANK_X = FILL_DRAIN_SLOT_X;
    public static final int TANK_Y = FILL_DRAIN_SLOT_Y - 48;

    @Override
    public SwapPanel openSwapPanel(java.util.function.Supplier<ItemStack> gadgetStackSupplier, Player player, boolean fieldHosted) {
        return new SippingSwapPanel(gadgetStackSupplier, fieldHosted);
    }

    /**
     * S.I.P.P.I.N.G.'s Module + fill/drain panel. Unlike Drinker's drain-only slot, this one is
     * bidirectional -- same priority order as {@link #tryHandTransfer} (try draining Sipping's own
     * buffer into the placed container first, then the reverse), since Sipping's GUI-side transfer
     * is meant to be the same operation as its hand-to-hand one, just through a slot instead of the
     * other hand. Module slot is the same pure live-view shape as Eater's/Drinker's own panels (see
     * those classes' identical javadoc for why): reads/writes straight through to
     * {@link ModDataComponentTypes#SIPPING_MODULE_DATA}, so there's nothing to materialize or write
     * back on close.
     */
    private final class SippingSwapPanel implements SwapPanel {

        private final java.util.function.Supplier<ItemStack> gadgetStackSupplier;
        private final boolean fieldHosted;
        private final IItemHandlerModifiable moduleHandler;
        private boolean moduleSlotChanged = false;

        private SippingSwapPanel(java.util.function.Supplier<ItemStack> gadgetStackSupplier, boolean fieldHosted) {
            this.gadgetStackSupplier = gadgetStackSupplier;
            this.fieldHosted = fieldHosted;
            this.moduleHandler = IHaveItemData.liveHandler(() -> new IHaveItemData.BulkItemHandler(gadgetStackSupplier.get(),
                    ModDataComponentTypes.SIPPING_MODULE_DATA.get(), MODULE_SLOT_COUNT, MODULE_SLOT_CAPACITY,
                    candidate -> candidate.is(ModTags.Items.MODULES)));
        }

        @Override
        public List<Slot> slots(int panelX, int panelY, java.util.function.BooleanSupplier active) {
            List<Slot> slots = new java.util.ArrayList<>(IHaveModules.buildModuleSlots(moduleHandler, MODULE_SLOT_COUNT,
                    panelX + MODULE_SLOT_X + 1, panelY + MODULE_SLOT_Y + 1, 0, active, () -> moduleSlotChanged = true));
            slots.add(new FillDrainSlot(panelX + FILL_DRAIN_SLOT_X + 1, panelY + FILL_DRAIN_SLOT_Y + 1, active));
            return slots;
        }

        @Override
        public void onClosed(Player player) {
            if (fieldHosted && moduleSlotChanged) {
                player.getCooldowns().addCooldown(SippingItem.this, SWAP_RECALIBRATION_COOLDOWN_TICKS);
            }
        }

        /**
         * Bidirectional: drains Sipping's own buffer into a placed container first, falling back to
         * filling Sipping from the container if the buffer is empty (or the container's already
         * full) -- same priority and same {@code FluidUtil.tryFluidTransfer} mechanics as
         * {@link #tryHandTransfer}, just through this slot instead of the other hand. All-or-nothing
         * either way: {@code tryFluidTransfer} only executes when the destination's own simulate-fill
         * reports it can take the entire amount, so a hazard-gated destination refusing what Sipping
         * offers (or vice versa) simply leaves both sides untouched rather than partially moving.
         */
        private final class FillDrainSlot extends Slot {
            private final java.util.function.BooleanSupplier active;

            FillDrainSlot(int x, int y, java.util.function.BooleanSupplier active) {
                super(new net.minecraft.world.SimpleContainer(1), 0, x, y);
                this.active = active;
            }

            @Override
            public boolean isActive() {
                return active.getAsBoolean();
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getCapability(Capabilities.FluidHandler.ITEM, null) != null;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                ItemStack held = getItem();
                if (held.isEmpty()) return;

                IFluidHandlerItem sippingHandler = gadgetStackSupplier.get().getCapability(Capabilities.FluidHandler.ITEM, null);
                IFluidHandlerItem containerHandler = held.getCapability(Capabilities.FluidHandler.ITEM, null);
                if (sippingHandler == null || containerHandler == null) return;

                FluidStack moved = FluidUtil.tryFluidTransfer(containerHandler, sippingHandler, Integer.MAX_VALUE, true);
                if (moved.isEmpty()) {
                    moved = FluidUtil.tryFluidTransfer(sippingHandler, containerHandler, Integer.MAX_VALUE, true);
                }
                if (moved.isEmpty()) return;

                set(containerHandler.getContainer());
            }
        }
    }
}
