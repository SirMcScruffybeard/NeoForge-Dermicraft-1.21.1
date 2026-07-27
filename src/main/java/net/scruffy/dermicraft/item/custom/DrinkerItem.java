package net.scruffy.dermicraft.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.scruffy.dermicraft.component.DrinkerModeData;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.interfaces.IHaveFluidData;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * D.R.I.N.K.E.R. -- held fluid vacuum. Hold right-click aimed at a fluid source block or a
 * machine/tank face to siphon it.
 *
 * <p><b>Atomic pickup:</b> a world source block is indivisible (same reason a bucket is
 * all-or-nothing), so holding fills a "ghost buffer"
 * ({@link ModDataComponentTypes#DRINKER_SIPHON_PROGRESS}) toward a full {@link #CAPACITY}, and only
 * on reaching it is the block removed and the fluid banked. Coming off target -- by aiming away or
 * letting go -- drains the ghost buffer back down gradually, so a momentary flick of the crosshair
 * costs a little time instead of all progress. A machine tank is divisible, so it skips the ghost
 * buffer entirely and transfers continuously.
 *
 * <p><b>Modes govern newly acquired fluid only, never what is already banked.</b> That separation
 * is the whole point: the cycle passes through Storage between Transfer and Disposal, and if a mode
 * acted on existing contents the moment it was entered, merely stepping past one on the way to
 * another would silently dump or destroy fluid the player never meant to touch. Acting on the
 * current buffer is always a separate, explicit gesture.
 */
public class DrinkerItem extends Item implements GeoItem, IHaveFluidData {

    /** One fluid source block's worth -- the buffer holds exactly one atomic pickup. */
    public static final int CAPACITY = 1000;

    /** mB added to the ghost buffer per tick while locked on. 200 ticks (10s) for a full block. */
    private static final int SIPHON_RATE = 5;
    /** mB lost per tick while off target. Deliberately equal to {@link #SIPHON_RATE}: time lost
     * exactly equals time needed to win it back, so a momentary flick off target is cheap and
     * "costs only time" is literally true. Raise it to punish losing target harder. */
    private static final int DRAIN_RATE = 5;

    /** Effectively "until released" -- the same trick bows and shields use for a held pose. */
    private static final int HELD_INDEFINITELY = 72000;

    /** Minimum ticks after arming before a confirm counts -- blocks accidental double-clicks. */
    private static final long MIN_CONFIRM_DELAY_TICKS = 20;
    /** Total ticks the Disposal confirm window stays open. */
    private static final long ARM_WINDOW_TICKS = 60;

    /**
     * Tier 1: tolerates no hazard at all.
     *
     * <p>Checked explicitly rather than leaning on the buffer's own gated handler. That shortcut
     * worked while every route ended in the buffer, but Disposal voids fluid without touching it
     * and Transfer can push into arbitrary third-party containers whose gating is not ours -- so
     * hazard tolerance has to be a property of the DRINKER itself, applied before any routing.
     */
    private static final HazardProfile PROFILE = HazardProfile.TIER_1;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DrinkerItem(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    ////////////////////Held pose\\\\\\\\\\\\\\\\\\\\

    /**
     * Held normally while siphoning -- the model's own bladder/mouth animation carries the "this is
     * running" read, so it doesn't need a canned vanilla arm pose on top.
     *
     * <p>Explicitly NOT {@link UseAnim#CROSSBOW}, despite that being the look originally wanted:
     * vanilla's first-person renderer switches on the use animation but has no CROSSBOW case at
     * all ({@code ItemInHandRenderer#renderArmWithItem}) -- the two-handed crossbow pose is
     * selected by {@code stack.is(Items.CROSSBOW)} identity checks instead. A modded item
     * returning CROSSBOW therefore matches no case, never gets {@code applyItemArmTransform}, and
     * renders at the untransformed origin (floating above the camera). The residual downward drift
     * while using is cancelled in {@link DrinkerClientExtensions}.
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return HELD_INDEFINITELY;
    }

    /**
     * Only re-equip on an actual item change, never on a component change.
     *
     * <p>The default is {@code !oldStack.equals(newStack)}, which compares components -- and the
     * ghost buffer rewrites {@link ModDataComponentTypes#DRINKER_SIPHON_PROGRESS} every single tick
     * while filling or draining. Vanilla reads that as "the player swapped items" and restarts the
     * equip animation continuously, so the rig visibly thrashes for the whole drain-down.
     */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    ////////////////////Gestures\\\\\\\\\\\\\\\\\\\\

    /**
     * Claims a right-click on a machine/tank face before the block can open its GUI -- the same
     * intercept {@code IdepItem} uses. Returning anything but PASS short-circuits
     * {@code ServerPlayerGameMode#useItemOn} before {@code blockstate.useItemOn}, so the machine
     * screen never opens; PASS leaves ordinary right-click behaviour completely untouched.
     *
     * <p>Re-raycasts rather than trusting {@code context.getClickedPos()}: the context's hit result
     * is fluid-blind, so a water source standing in front of a machine would be missed and the
     * click would open the machine instead of siphoning the water the player was aiming at.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (findSiphonTarget(context.getLevel(), player) == null) return InteractionResult.PASS;

        player.startUsingItem(context.getHand());
        // CONSUME rather than SUCCESS: SUCCESS swings the arm, which fights the deliberately
        // motionless hold (see DrinkerClientExtensions).
        return InteractionResult.CONSUME;
    }

    /**
     * Reached only when nothing claimed the click as a block interaction -- aiming at a bare fluid
     * source (vanilla's crosshair ray ignores fluids, so that reads as a miss), or at nothing
     * useful at all.
     *
     * <p>Crouch state only matters when there is nothing to siphon: a valid target always wins, so
     * the player can never lose a siphon by happening to be sneaking.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (findSiphonTarget(level, player) != null) {
            // Start the sustained-use state immediately on click -- all siphon logic keys off this,
            // never off an animation finishing, so the long activate clip can't gate it.
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        if (!level.isClientSide) {
            if (player.isCrouching()) {
                processBuffer(level, player, stack);
            } else {
                cycleMode(player, stack);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void cycleMode(Player player, ItemStack stack) {
        DrinkerModeData next = modeData(stack).next();
        stack.set(ModDataComponentTypes.DRINKER_MODE_DATA.get(), next);
        player.displayClientMessage(Component.translatable("tooltip.dermicraft.drinker.mode",
                Component.translatable(modeKey(next.mode()))), true);
    }

    /** Acts on fluid already banked -- the only thing that ever does. */
    private void processBuffer(Level level, Player player, ItemStack stack) {
        DrinkerModeData data = modeData(stack);
        switch (data.mode()) {
            case STORAGE -> player.displayClientMessage(
                    Component.translatable("tooltip.dermicraft.drinker.storage_inert"), true);
            case TRANSFER -> transferOut(player, stack);
            case DISPOSAL -> voidBuffer(level, player, stack, data);
        }
    }

    private void transferOut(Player player, ItemStack stack) {
        FluidStack held = bufferContents(stack);
        if (held.isEmpty()) {
            player.displayClientMessage(Component.translatable("tooltip.dermicraft.drinker.nothing_held"), true);
            return;
        }

        int moved = fillContainers(stack, player, held, IFluidHandler.FluidAction.EXECUTE);
        if (moved <= 0) {
            player.displayClientMessage(Component.translatable("tooltip.dermicraft.drinker.no_container")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        drainBuffer(stack, moved);
        player.displayClientMessage(Component.translatable("tooltip.dermicraft.drinker.transferred", moved), true);
    }

    /**
     * Disposal's destructive action, behind the same arm/confirm shape S.I.P.P.I.N.G. uses.
     *
     * <p>Note the confirm guards the ACT of voiding, not the mode switch -- unlike SIPPING, where
     * entering Disposal is itself what destroys. Here modes never touch banked fluid, so switching
     * into Disposal is harmless and needs no confirmation; only this does.
     */
    private void voidBuffer(Level level, Player player, ItemStack stack, DrinkerModeData data) {
        FluidStack held = bufferContents(stack);
        if (held.isEmpty()) {
            player.displayClientMessage(Component.translatable("tooltip.dermicraft.drinker.nothing_held"), true);
            return;
        }

        long now = level.getGameTime();
        if (data.armed()) {
            long elapsed = now - data.armedAtGameTime();
            // Inside the dead zone: swallowed silently, arm state untouched. A second click that
            // fast is far more likely to be a slip than a decision.
            if (elapsed < MIN_CONFIRM_DELAY_TICKS) return;

            if (elapsed <= ARM_WINDOW_TICKS) {
                stack.remove(getDataType());
                stack.set(ModDataComponentTypes.DRINKER_MODE_DATA.get(), data.disarmed());
                player.displayClientMessage(Component.translatable("tooltip.dermicraft.drinker.voided")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
        }

        stack.set(ModDataComponentTypes.DRINKER_MODE_DATA.get(), data.armedAt(now));
        player.displayClientMessage(Component.translatable("tooltip.dermicraft.drinker.void_warning")
                .withStyle(ChatFormatting.RED), true);
    }

    ////////////////////Siphon\\\\\\\\\\\\\\\\\\\\

    /** What the player is aimed at, if it's something DRINKER can pull from. */
    private record Target(BlockPos pos, BlockState blockState, @Nullable IFluidHandler tank) {
        boolean isTank() {
            return tank != null;
        }
    }

    /**
     * One raycast serving both target kinds. SOURCE_ONLY clips fluid sources AND normal blocks, so
     * whichever is physically nearer wins -- that's what makes water in front of a machine siphon
     * instead of opening the machine.
     */
    @Nullable
    private Target findSiphonTarget(Level level, Player player) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return null;

        BlockPos pos = hit.getBlockPos();
        BlockState blockState = level.getBlockState(pos);

        // A face-routed tank takes priority over the raw fluidstate at the same position, matching
        // DrinkerTargetScanner so the readout and the action can never disagree.
        IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, hit.getDirection());
        if (tank != null) return new Target(pos, blockState, tank);

        FluidState fluidState = blockState.getFluidState();
        if (fluidState.isEmpty() || !fluidState.isSource()) return null;
        if (!(blockState.getBlock() instanceof BucketPickup)) return null;

        return new Target(pos, blockState, null);
    }

    /**
     * All siphon state lives here rather than being split across {@code onUseTick}/{@code
     * releaseUsing}: the ghost buffer has to keep draining after the player lets go, and deriving
     * "am I siphoning" from vanilla's use state every tick means release, dropping the item, death,
     * and swapping hands are all handled by the same branch instead of needing separate hooks.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;

        expireArm(stack, level, player);

        boolean holdingTrigger = player.isUsingItem() && player.getUseItem() == stack;
        Draw draw = holdingTrigger ? siphonTick(level, player, stack) : Draw.NOTHING;

        // Centralised so no code path can forget it: unbanked progress decays on every tick that
        // didn't specifically feed it -- including while draining a tank, which abandons whatever
        // source block was part-way done.
        if (draw != Draw.SOURCE) drainGhost(stack);

        boolean drawing = draw != Draw.NOTHING;
        // Gated on actually drawing fluid, not merely holding the trigger -- the bladder shouldn't
        // inflate while aimed at nothing. Tradeoff: sweeping off target mid-siphon does bounce the
        // model between activate/deactivate; the controller's transition blending softens it.
        if (stack.getOrDefault(ModDataComponentTypes.DRINKER_SIPHONING.get(), false) != drawing) {
            stack.set(ModDataComponentTypes.DRINKER_SIPHONING.get(), drawing);
        }
    }

    /** A pending void confirm never outlives its window, nor the player's grip on the item. */
    private void expireArm(ItemStack stack, Level level, Player player) {
        DrinkerModeData data = modeData(stack);
        if (!data.armed()) return;

        boolean stillHeld = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (!stillHeld || level.getGameTime() - data.armedAtGameTime() > ARM_WINDOW_TICKS) {
            stack.set(ModDataComponentTypes.DRINKER_MODE_DATA.get(), data.disarmed());
        }
    }

    /** What, if anything, moved fluid this tick. SOURCE is distinguished because it's the only
     * path that feeds the ghost buffer rather than banking directly. */
    private enum Draw {NOTHING, SOURCE, TANK}

    private Draw siphonTick(Level level, Player player, ItemStack stack) {
        Target target = findSiphonTarget(level, player);
        if (target == null) return Draw.NOTHING;

        if (target.isTank()) {
            // A tank is divisible, unlike a source block, so it transfers continuously and banks as
            // it goes -- no ghost buffer, and nothing to lose by looking away part-way through.
            return drainTank(stack, player, target.tank()) ? Draw.TANK : Draw.NOTHING;
        }
        return accumulateSource(level, player, stack, target) ? Draw.SOURCE : Draw.NOTHING;
    }

    /** Continuous partial transfer out of a machine/tank face. */
    private boolean drainTank(ItemStack stack, Player player, IFluidHandler tank) {
        FluidStack available = tank.drain(SIPHON_RATE, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty() || !PROFILE.accepts(available)) return false;

        int accepted = route(stack, player, available, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return false;

        // Drain by stack, not by amount: on a multi-tank handler drain(int) could pull a different
        // fluid than the one that was just approved.
        FluidStack request = available.copy();
        request.setAmount(accepted);
        FluidStack drained = tank.drain(request, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return false;

        route(stack, player, drained, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    /** @return whether the ghost buffer actually advanced this tick. */
    private boolean accumulateSource(Level level, Player player, ItemStack stack, Target target) {
        BlockState blockState = target.blockState();
        FluidStack source = new FluidStack(blockState.getFluidState().getType(), CAPACITY);
        if (!PROFILE.accepts(source)) return false;

        // Somewhere for a WHOLE source block or nothing -- a partial route would strand fluid that
        // can never be picked up again.
        if (route(stack, player, source, IFluidHandler.FluidAction.SIMULATE) < CAPACITY) return false;

        FluidStack ghost = stack.getOrDefault(ModDataComponentTypes.DRINKER_SIPHON_PROGRESS.get(), FluidData.EMPTY)
                .getFluidStack();

        // Switching fluids mid-siphon discards progress rather than mixing -- the ghost buffer
        // represents one specific pending source block.
        if (!ghost.isEmpty() && !FluidStack.isSameFluidSameComponents(ghost, source)) {
            stack.remove(ModDataComponentTypes.DRINKER_SIPHON_PROGRESS.get());
            return false;
        }

        int progress = Math.min(CAPACITY, ghost.getAmount() + SIPHON_RATE);
        if (progress < CAPACITY) {
            stack.set(ModDataComponentTypes.DRINKER_SIPHON_PROGRESS.get(),
                    FluidData.createData(new FluidStack(source.getFluid(), progress)));
            return true;
        }

        completeSiphon(level, player, stack, target, source);
        return true;
    }

    /**
     * Ghost buffer is full: take the block and route the fluid.
     *
     * <p>Goes through {@link BucketPickup} rather than just clearing the block, so waterlogged
     * blocks give up their water and survive instead of being destroyed. The returned bucket is
     * discarded -- it's only used as vanilla's confirmation that the pickup succeeded.
     */
    private void completeSiphon(Level level, Player player, ItemStack stack, Target target, FluidStack source) {
        BucketPickup pickup = (BucketPickup) target.blockState().getBlock();
        ItemStack picked = pickup.pickupBlock(player, level, target.pos(), target.blockState());
        if (picked.isEmpty()) return;

        route(stack, player, source, IFluidHandler.FluidAction.EXECUTE);
        pickup.getPickupSound(target.blockState()).ifPresent(sound ->
                level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F));

        stack.remove(ModDataComponentTypes.DRINKER_SIPHON_PROGRESS.get());
        player.stopUsingItem();
    }

    /** Bleeds off unbanked progress. See the class javadoc on why this is gradual. */
    private void drainGhost(ItemStack stack) {
        FluidStack ghost = stack.getOrDefault(ModDataComponentTypes.DRINKER_SIPHON_PROGRESS.get(), FluidData.EMPTY)
                .getFluidStack();
        if (ghost.isEmpty()) return;

        int remaining = ghost.getAmount() - DRAIN_RATE;
        if (remaining <= 0) {
            stack.remove(ModDataComponentTypes.DRINKER_SIPHON_PROGRESS.get());
        } else {
            stack.set(ModDataComponentTypes.DRINKER_SIPHON_PROGRESS.get(),
                    FluidData.createData(new FluidStack(ghost.getFluid(), remaining)));
        }
    }

    ////////////////////Routing newly acquired fluid\\\\\\\\\\\\\\\\\\\\

    /**
     * Sends freshly siphoned fluid wherever the current mode says it goes, and reports how much
     * that destination would take. Only ever handles fluid arriving <em>now</em> -- never what is
     * already banked, which is what keeps stepping through a mode from disturbing stored contents.
     */
    public static int route(ItemStack stack, Player player, FluidStack fluid, IFluidHandler.FluidAction action) {
        return switch (modeData(stack).mode()) {
            // A void is bottomless, so it always accepts everything; the hazard check upstream is
            // what stops this becoming a way to destroy fluids DRINKER shouldn't touch.
            case DISPOSAL -> fluid.getAmount();
            case STORAGE -> fillBuffer(stack, fluid, action);
            case TRANSFER -> {
                int intoContainers = fillContainers(stack, player, fluid, action);
                if (intoContainers >= fluid.getAmount()) yield intoContainers;

                // Buffer is the overflow, not the destination -- it catches what the inventory
                // couldn't take so a siphon isn't blocked just because no container fits.
                FluidStack leftover = fluid.copy();
                leftover.setAmount(fluid.getAmount() - intoContainers);
                yield intoContainers + fillBuffer(stack, leftover, action);
            }
        };
    }

    private static int fillBuffer(ItemStack stack, FluidStack fluid, IFluidHandler.FluidAction action) {
        IFluidHandlerItem buffer = stack.getCapability(Capabilities.FluidHandler.ITEM, null);
        return buffer == null ? 0 : buffer.fill(fluid, action);
    }

    private static void drainBuffer(ItemStack stack, int amount) {
        IFluidHandlerItem buffer = stack.getCapability(Capabilities.FluidHandler.ITEM, null);
        if (buffer != null) buffer.drain(amount, IFluidHandler.FluidAction.EXECUTE);
    }

    public static FluidStack bufferContents(ItemStack stack) {
        IFluidHandlerItem buffer = stack.getCapability(Capabilities.FluidHandler.ITEM, null);
        return buffer == null ? FluidStack.EMPTY : buffer.getFluidInTank(0);
    }

    /**
     * Pushes into inventory fluid containers, topping up ones already holding the same fluid before
     * starting a fresh one.
     *
     * <p>Stacked containers are split rather than skipped. A fluid data component belongs to the
     * whole stack, so filling a stack of five empty Flasks in place would fill all five from one
     * container's worth -- see {@link IHaveFluidData#isSingleContainer}. Splitting one off keeps
     * stacked containers usable without that duplication.
     *
     * <p>Only one item is ever taken from a given stack per call, so a simulation reports the room
     * of a single container rather than the whole stack. That understates capacity, which is the
     * safe direction: Transfer falls back to the buffer for whatever the inventory couldn't take.
     *
     * @return total accepted.
     */
    private static int fillContainers(ItemStack self, Player player, FluidStack fluid, IFluidHandler.FluidAction action) {
        int remaining = fluid.getAmount();

        for (ItemStack candidate : orderedContainers(self, player, fluid)) {
            if (remaining <= 0) break;
            if (candidate.isEmpty()) continue;

            FluidStack offer = fluid.copy();
            offer.setAmount(remaining);

            if (candidate.getCount() == 1) {
                IFluidHandlerItem handler = candidate.getCapability(Capabilities.FluidHandler.ITEM, null);
                if (handler == null) continue;
                remaining -= handler.fill(offer, action);
                continue;
            }

            ItemStack single = candidate.copyWithCount(1);
            IFluidHandlerItem handler = single.getCapability(Capabilities.FluidHandler.ITEM, null);
            if (handler == null) continue;

            int accepted = handler.fill(offer, action);
            if (accepted <= 0) continue;
            remaining -= accepted;

            if (action.execute()) {
                candidate.shrink(1);
                // getContainer() is the filled single -- the handler wrote into the copy, not the
                // stack it came from, which is the entire point of splitting.
                IHaveFluidData.giveOrDrop(player, handler.getContainer());
            }
        }
        return fluid.getAmount() - remaining;
    }

    /**
     * Search order: containers already holding this fluid first, then everything else, each group
     * ordered offhand, hotbar, main inventory.
     *
     * <p>Built as a single pre-sorted list rather than two passes on purpose -- two passes over the
     * same candidates would double-count under SIMULATE, since simulating doesn't consume the room
     * it reports.
     */
    private static List<ItemStack> orderedContainers(ItemStack self, Player player, FluidStack fluid) {
        List<ItemStack> sameFluid = new ArrayList<>();
        List<ItemStack> others = new ArrayList<>();

        Inventory inventory = player.getInventory();
        List<ItemStack> scanOrder = new ArrayList<>();
        scanOrder.add(player.getOffhandItem());
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) scanOrder.add(inventory.getItem(slot));
        for (int slot = Inventory.getSelectionSize(); slot < inventory.items.size(); slot++) {
            scanOrder.add(inventory.getItem(slot));
        }

        for (ItemStack candidate : scanOrder) {
            if (candidate == self || candidate.isEmpty()) continue;

            IFluidHandlerItem handler = candidate.getCapability(Capabilities.FluidHandler.ITEM, null);
            if (handler == null) continue;

            FluidStack contained = handler.getFluidInTank(0);
            if (!contained.isEmpty() && FluidStack.isSameFluidSameComponents(contained, fluid)) {
                sameFluid.add(candidate);
            } else {
                others.add(candidate);
            }
        }

        sameFluid.addAll(others);
        return sameFluid;
    }

    ////////////////////Tooltip\\\\\\\\\\\\\\\\\\\\

    /**
     * The model's gauge is only three coarse steps and the mode lights are colour-coded, so neither
     * answers "exactly how much of what am I holding?". This does -- which also makes the buffer
     * observable while testing transfers.
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dermicraft.drinker.mode",
                        Component.translatable(modeKey(modeData(stack).mode())))
                .withStyle(ChatFormatting.GRAY));

        FluidStack held = bufferContents(stack);
        tooltip.add(held.isEmpty()
                ? Component.translatable("tooltip.dermicraft.drinker.tooltip_empty").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("tooltip.dermicraft.drinker.tooltip_holding",
                                Component.translatable(held.getFluid().getFluidType().getDescriptionId()),
                                held.getAmount(), CAPACITY)
                        .withStyle(ChatFormatting.GRAY));
    }

    ////////////////////Helpers\\\\\\\\\\\\\\\\\\\\

    public static DrinkerModeData modeData(ItemStack stack) {
        return stack.getOrDefault(ModDataComponentTypes.DRINKER_MODE_DATA.get(), DrinkerModeData.DEFAULT);
    }

    private static String modeKey(DrinkerModeData.Mode mode) {
        return switch (mode) {
            case DISPOSAL -> "tooltip.dermicraft.drinker.mode.disposal";
            case STORAGE -> "tooltip.dermicraft.drinker.mode.storage";
            case TRANSFER -> "tooltip.dermicraft.drinker.mode.transfer";
        };
    }

    ////////////////////Animation\\\\\\\\\\\\\\\\\\\\

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "Body", 0, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            boolean active = stack != null
                    && stack.getOrDefault(ModDataComponentTypes.DRINKER_SIPHONING.get(), false);

            return state.setAndContinue(active
                    ? RawAnimation.begin().thenPlay("activate").thenLoop("active_hold")
                    : RawAnimation.begin().thenPlay("deactivate").thenLoop("idle"));
        }).transitionLength(4));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
