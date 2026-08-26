package net.scruffy.dermicraft.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.scruffy.dermicraft.component.DrinkerModeData;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.interfaces.IGadget;
import net.scruffy.dermicraft.interfaces.IHaveFluidData;
import net.scruffy.dermicraft.interfaces.IHaveItemData;
import net.scruffy.dermicraft.interfaces.IHaveModules;
import net.scruffy.dermicraft.interfaces.IWorkbenchSwappable;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.scruffy.dermicraft.screen.custom.scrench.ScrenchMenu;
import net.scruffy.dermicraft.util.ModFluidUtil;
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
public class DrinkerItem extends Item implements GeoItem, IHaveFluidData, IGadget, IHaveModules, IWorkbenchSwappable {

    /** One fluid source block's worth -- the buffer holds exactly one atomic pickup. */
    public static final int CAPACITY = 1000;

    /** Real, current capacity: {@link #CAPACITY} plus whatever Capacity Module(s) are installed --
     * see {@code IHaveModules#capacityBonus}. Every fill/drain/siphon check reads this, never the
     * bare constant, so an installed Capacity Module actually widens what a single siphon gulp can
     * hold, not just the buffer's nominal size. */
    public static int effectiveCapacity(ItemStack drinkerStack) {
        return CAPACITY + IHaveModules.capacityBonus(drinkerStack, ModDataComponentTypes.DRINKER_MODULE_DATA.get(), MODULE_SLOT_COUNT);
    }

    /** Gadget health, expressed as vanilla durability -- see {@link IGadget}. Registered via
     * {@code Item.Properties#durability}, which is the single source of truth for max HP. */
    public static final int MAX_HP = 10;

    /** mB added to the ghost buffer per tick while locked on. 200 ticks (10s) for a full block. */
    private static final int SIPHON_RATE = 5;
    /** mB lost per tick while off target. Deliberately equal to {@link #SIPHON_RATE}: time lost
     * exactly equals time needed to win it back, so a momentary flick off target is cheap and
     * "costs only time" is literally true. Raise it to punish losing target harder. */
    private static final int DRAIN_RATE = 5;

    /** Effectively "until released" -- the same trick bows and shields use for a held pose. */
    private static final int HELD_INDEFINITELY = 72000;

    /**
     * Ticks of dead air after a siphon completes, so a still-held trigger doesn't immediately do
     * something else.
     *
     * <p>Vanilla re-fires use the instant {@code isUsingItem()} goes false while the button is
     * down. Completing a siphon removes the source block, so that re-fire finds no target and falls
     * straight through to the mode cycle -- the player finishes a ten-second draw and their mode
     * silently changes. This swallows the re-fire.
     */
    private static final int POST_SIPHON_LOCKOUT_TICKS = 20;

    /** Minimum ticks after arming before a confirm counts -- blocks accidental double-clicks. */
    private static final long MIN_CONFIRM_DELAY_TICKS = 20;
    /** Total ticks the Disposal confirm window stays open. */
    private static final long ARM_WINDOW_TICKS = 60;

    /** Module loadout size -- see dermicraft-progression-notes.md, step 3. 1 general-purpose slot
     * (deliberately smaller than Eater's 3 -- Drinker's Module catalog is Safety-only for now, no
     * mouthpiece-style specialties competing for the budget). Backed by
     * {@link ModDataComponentTypes#DRINKER_MODULE_DATA}, Drinker's own distinct component (never
     * Eater's {@code MODULE_DATA} -- see that field's javadoc). */
    public static final int MODULE_SLOT_COUNT = 1;
    /** Exactly one Module per slot -- these aren't stackable resources. */
    public static final int MODULE_SLOT_CAPACITY = IHaveModules.DEFAULT_MODULE_SLOT_CAPACITY;

    /** Same field-swap cost shape as Eater's own Module slot -- see that class's identical constant
     * for the reasoning (a harvesting/utility gadget doesn't carry Sunder's weapon-mid-combat
     * stakes, so a brief "can't use again yet" cooldown is the right-sized cost, not a movement
     * penalty). */
    private static final int SWAP_RECALIBRATION_COOLDOWN_TICKS = 40;

    /**
     * Tier 1 base, plus whatever hazard kinds any currently-installed Safety Module grants -- see
     * {@link IHaveModules#installedHazardProfile}. Drinker has no permanent per-hazard tier of its
     * own yet (same open question as Eater's), so {@link HazardProfile#TIER_1} is the base every
     * Safety Module's grant unions onto.
     *
     * <p>Checked explicitly rather than leaning on the buffer's own gated handler. That shortcut
     * worked while every route ended in the buffer, but Disposal voids fluid without touching it
     * and Transfer can push into arbitrary third-party containers whose gating is not ours -- so
     * hazard tolerance has to be a property of the DRINKER itself, applied before any routing.
     * Public (not the previous field's {@code private}) so {@code DrinkerTargetScanner}'s readout
     * can check the exact same profile the real siphon uses, rather than a second hardcoded copy.
     */
    public static HazardProfile installedHazardProfile(ItemStack stack) {
        return IHaveModules.installedHazardProfile(stack, ModDataComponentTypes.DRINKER_MODULE_DATA.get(),
                MODULE_SLOT_COUNT, HazardProfile.TIER_1);
    }

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

        // Same Scrench-pairing deferral as use() below -- without this, aiming DRINKER at a real
        // siphon target (a machine/tank face) while a Scrench sits in the other hand would let
        // this intercept fire first, making the swap panel unreachable in exactly the moment a
        // player most wants it: about to touch a hazardous fluid and wanting to check/swap a
        // Safety Module first. PASS here falls through to ordinary use() handling, per this
        // method's own javadoc, which is where the actual Scrench-open check lives.
        InteractionHand otherHand = context.getHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (player.getItemInHand(otherHand).getItem() instanceof ScrenchItem) return InteractionResult.PASS;

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

        // Checks the other hand for a paired Scrench first, deferring to the Module-swap GUI if
        // found -- same pattern as EaterItem's/SunderItem's own matching check (see EaterItem for
        // why this has to happen before any of DRINKER's own click handling below, not after).
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (player.getItemInHand(otherHand).getItem() instanceof ScrenchItem) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                ScrenchMenu.open(serverPlayer, hand);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

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
        if (bufferContents(stack).isEmpty()) {
            player.displayClientMessage(Component.translatable("tooltip.dermicraft.drinker.nothing_held"), true);
            return;
        }

        int moved = distributeBuffer(player, stack);
        if (moved <= 0) {
            player.displayClientMessage(Component.translatable("tooltip.dermicraft.drinker.no_container")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        FluidStack leftover = bufferContents(stack);
        player.displayClientMessage(leftover.isEmpty()
                ? Component.translatable("tooltip.dermicraft.drinker.transferred", moved)
                : Component.translatable("tooltip.dermicraft.drinker.transferred_partial", moved, leftover.getAmount()),
                true);
    }

    /**
     * Empties as much of the buffer as the player's containers will take: fill one to capacity, move
     * to the next, and so on until either the buffer runs dry or nothing left will accept it.
     * Whatever no container would take simply stays banked -- running out of room is a stopping
     * condition, never a loss.
     *
     * <p><b>The buffer is debited per container, by exactly what that container accepted</b>, rather
     * than by a total tallied across all of them and subtracted at the end. That older shape was
     * only correct while every container's {@code fill()} return agreed exactly with what it
     * actually stored; where the two diverged, the difference was silently destroyed. Paying for
     * each fill as it happens removes the possibility structurally instead of relying on every
     * present and future handler to report itself honestly.
     *
     * @return total mB actually placed into containers.
     */
    private static int distributeBuffer(Player player, ItemStack self) {
        int moved = 0;

        for (ItemStack candidate : orderedContainers(self, player, bufferContents(self))) {
            if (bufferContents(self).isEmpty()) break;

            // Keep going at THIS candidate until it's done rather than taking one and moving on --
            // a stack of five Flasks should fill all five, and a partly-filled container should be
            // topped off, before the search moves to the next one.
            while (!candidate.isEmpty() && !bufferContents(self).isEmpty()) {
                FillResult result = fillOneFromBuffer(player, self, candidate);
                moved += result.moved();
                if (result.moved() <= 0 || result.candidateConsumed()) break;
            }
        }
        return moved;
    }

    /**
     * One container's worth. {@code candidateConsumed} reports that {@code candidate} itself is no
     * longer the stack sitting in the player's inventory -- only true when the handler swapped
     * container identity outright (a vanilla bucket becoming a filled bucket). A stacked container
     * having one item split off does NOT set this: {@code candidate} is the original stack, shrunk
     * in place, still perfectly reusable for the next split. The caller must stop reusing the
     * reference only when this is true; filling a genuinely stale stack would report a fill against
     * something no longer there, creating fluid from nothing.
     */
    private record FillResult(int moved, boolean candidateConsumed) {
        static final FillResult NONE = new FillResult(0, false);
    }

    private static FillResult fillOneFromBuffer(Player player, ItemStack self, ItemStack candidate) {
        FluidStack held = bufferContents(self).copy();
        if (held.isEmpty()) return FillResult.NONE;

        // A fluid data component belongs to the whole stack, so a stacked container has one item
        // split off and filled rather than being filled in place (which would fill every item in
        // it) -- see IHaveFluidData#isSingleContainer.
        boolean stacked = !IHaveFluidData.isSingleContainer(candidate);
        ItemStack target = stacked ? candidate.copyWithCount(1) : candidate;

        IFluidHandlerItem handler = target.getCapability(Capabilities.FluidHandler.ITEM, null);
        if (handler == null || !ModFluidUtil.canHold(handler, held)) return FillResult.NONE;

        int accepted = handler.fill(held, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return FillResult.NONE;

        // Take exactly what this container just agreed to, then hand it that same stack.
        FluidStack request = held.copy();
        request.setAmount(accepted);
        FluidStack drained = drainBuffer(self, request);
        if (drained.isEmpty()) return FillResult.NONE;

        int filled = handler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (filled < drained.getAmount()) {
            // Took less than it promised on the simulate. Bank the difference again immediately --
            // it has already left the buffer at this point, so anything not returned here is gone.
            FluidStack unplaced = drained.copy();
            unplaced.setAmount(drained.getAmount() - filled);
            fillBuffer(self, unplaced, IFluidHandler.FluidAction.EXECUTE);
        }
        if (filled <= 0) return FillResult.NONE;

        if (stacked) {
            candidate.shrink(1);
            // getContainer() is the filled single -- the handler wrote into the copy, not the stack
            // it came from, which is the entire point of splitting.
            IHaveFluidData.giveOrDrop(player, handler.getContainer());
            // NOT consumed: candidate is the original stack, shrunk in place, still a live slot
            // reference -- the caller's while loop keeps splitting off it (its own isEmpty() check
            // is what stops filling a five-Flask stack after the fifth, not this flag).
            return new FillResult(filled, false);
        }

        boolean swapped = handler.getContainer() != candidate;
        if (swapped) ModFluidUtil.writeBackToPlayer(player, candidate, handler.getContainer());
        return new FillResult(filled, swapped);
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
            if (!drainTank(stack, player, target.tank())) return Draw.NOTHING;

            if (level instanceof ServerLevel serverLevel) {
                DrinkerParticles.dripFromMouth(serverLevel, player, tankContents(target.tank()));
            }
            return Draw.TANK;
        }

        if (!accumulateSource(level, player, stack, target)) return Draw.NOTHING;

        if (level instanceof ServerLevel serverLevel) {
            DrinkerParticles.streamFromSource(serverLevel, player,
                    Vec3.atCenterOf(target.pos()),
                    new FluidStack(target.blockState().getFluidState().getType(), CAPACITY));
        }
        return Draw.SOURCE;
    }

    /** First non-empty tank on the face -- matches how DrinkerTargetScanner picks what to report. */
    private static FluidStack tankContents(IFluidHandler tank) {
        for (int i = 0; i < tank.getTanks(); i++) {
            FluidStack inTank = tank.getFluidInTank(i);
            if (!inTank.isEmpty()) return inTank;
        }
        return FluidStack.EMPTY;
    }

    /** Continuous partial transfer out of a machine/tank face. */
    private boolean drainTank(ItemStack stack, Player player, IFluidHandler tank) {
        FluidStack available = tank.drain(SIPHON_RATE, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty() || !installedHazardProfile(stack).accepts(available)) return false;

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
        if (!installedHazardProfile(stack).accepts(source)) return false;

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

        // Uses vanilla's item cooldown rather than a bespoke timer: it gates useItem (so the
        // held-trigger re-fire can't reach the mode cycle) while onItemUseFirst, which runs before
        // the cooldown check, still lets an adjacent tank be drained. It also renders the usual
        // sweep on the hotbar icon, which reads as the rig recovering from swallowing a whole block.
        player.getCooldowns().addCooldown(this, POST_SIPHON_LOCKOUT_TICKS);
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

    /** Drains by stack, not by amount: on a multi-tank buffer, drain(int) could pull a different
     * fluid than the one the caller already got a destination to agree to. */
    private static FluidStack drainBuffer(ItemStack stack, FluidStack request) {
        IFluidHandlerItem buffer = stack.getCapability(Capabilities.FluidHandler.ITEM, null);
        return buffer == null ? FluidStack.EMPTY : buffer.drain(request, IFluidHandler.FluidAction.EXECUTE);
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
                if (handler == null || !ModFluidUtil.canHold(handler, offer)) continue;

                int accepted = handler.fill(offer, action);
                if (accepted <= 0) continue;
                remaining -= accepted;

                // Write the handler's own container back, exactly as the stacked branch below
                // already does -- see ModFluidUtil's "container-swap write-back" section for why.
                if (action.execute()) ModFluidUtil.writeBackToPlayer(player, candidate, handler.getContainer());
                continue;
            }

            ItemStack single = candidate.copyWithCount(1);
            IFluidHandlerItem handler = single.getCapability(Capabilities.FluidHandler.ITEM, null);
            if (handler == null || !ModFluidUtil.canHold(handler, offer)) continue;

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

    ////////////////////IWorkbenchSwappable (Scrench field / Workbench station swap panel)\\\\\\\\\\\\\\\\\\\\

    // Panel layout -- public so ScrenchScreen/WorkbenchScreen can draw the matching background
    // under exactly the slot DrinkerSwapPanel builds, without either screen needing to know
    // DRINKER's internal panel shape beyond this coordinate. Same placeholder-coordinate caveat as
    // EaterItem's own MODULE_SLOT_X/Y: functionally correct regardless of where the art ends up.
    public static final int MODULE_SLOT_X = 8;
    public static final int MODULE_SLOT_Y = 27;

    // Buffer gauge + drain slot -- same tank-above-slot pairing as Sunder's/Shatter's own fuel
    // gauge (TANK_AND_SLOT_TEXTURE, 48px of tank above the slot's own top), just draining instead
    // of filling. Public for the same reason as every other panel constant in this class: the
    // screen needs to draw the matching background under exactly the slot DrinkerSwapPanel builds.
    public static final int DRAIN_SLOT_X = 113;
    public static final int DRAIN_SLOT_Y = 60;
    public static final int TANK_X = DRAIN_SLOT_X;
    public static final int TANK_Y = DRAIN_SLOT_Y - 48;

    @Override
    public SwapPanel openSwapPanel(java.util.function.Supplier<ItemStack> gadgetStackSupplier, Player player, boolean fieldHosted) {
        return new DrinkerSwapPanel(gadgetStackSupplier, fieldHosted);
    }

    /**
     * DRINKER's Module + drain panel. Unlike Eater's, there's no item buffer to expose here
     * alongside the Module slot -- DRINKER's own buffer is the fluid tank ({@link IHaveFluidData}) --
     * but that buffer had no screen presentation of its own before this panel: DRINKER only had
     * mode-cycling right-click and no GUI at all until the Module slot was wired up, which is why
     * the fluid gauge and drain slot are added here rather than a separate screen. Module slot is
     * the same pure live-view shape as Eater's own panel (see that class's identical javadoc for
     * why): reads/writes straight through to {@link ModDataComponentTypes#DRINKER_MODULE_DATA}, so
     * there's nothing to materialize or write back on close. Re-resolves a fresh
     * {@code BulkItemHandler} against {@code gadgetStackSupplier.get()} on every access (see
     * {@link IHaveItemData#liveHandler}), same reasoning as {@code EaterItem.EaterSwapPanel}.
     */
    private final class DrinkerSwapPanel implements SwapPanel {

        private final java.util.function.Supplier<ItemStack> gadgetStackSupplier;
        private final boolean fieldHosted;
        private final IItemHandlerModifiable moduleHandler;
        private boolean moduleSlotChanged = false;

        private DrinkerSwapPanel(java.util.function.Supplier<ItemStack> gadgetStackSupplier, boolean fieldHosted) {
            this.gadgetStackSupplier = gadgetStackSupplier;
            this.fieldHosted = fieldHosted;
            this.moduleHandler = IHaveItemData.liveHandler(() -> new IHaveItemData.BulkItemHandler(gadgetStackSupplier.get(),
                    ModDataComponentTypes.DRINKER_MODULE_DATA.get(), MODULE_SLOT_COUNT, MODULE_SLOT_CAPACITY,
                    candidate -> candidate.is(ModTags.Items.MODULES)));
        }

        @Override
        public List<Slot> slots(int panelX, int panelY, java.util.function.BooleanSupplier active) {
            List<Slot> slots = new ArrayList<>(IHaveModules.buildModuleSlots(moduleHandler, MODULE_SLOT_COUNT,
                    panelX + MODULE_SLOT_X + 1, panelY + MODULE_SLOT_Y + 1, 0, active, () -> moduleSlotChanged = true,
                    slot -> IHaveModules.mayRemoveCapacityModule(gadgetStackSupplier.get(), ModDataComponentTypes.DRINKER_MODULE_DATA.get(),
                            MODULE_SLOT_COUNT, slot, CAPACITY, bufferContents(gadgetStackSupplier.get()).getAmount())));
            slots.add(new DrainSlot(panelX + DRAIN_SLOT_X + 1, panelY + DRAIN_SLOT_Y + 1, active));
            return slots;
        }

        @Override
        public void onClosed(Player player) {
            if (fieldHosted && moduleSlotChanged) {
                player.getCooldowns().addCooldown(DrinkerItem.this, SWAP_RECALIBRATION_COOLDOWN_TICKS);
            }
        }

        /**
         * Drains the buffer immediately into a fluid container placed here, mirroring Sunder's own
         * {@code FuelFillSlot} exactly in shape, just the opposite direction -- source is DRINKER's
         * own buffer capability (read fresh off {@code gadgetStackSupplier.get()}, same live-view
         * rule as the Module slot above) rather than the tank being filled.
         */
        private final class DrainSlot extends Slot {
            private final java.util.function.BooleanSupplier active;

            DrainSlot(int x, int y, java.util.function.BooleanSupplier active) {
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

                IFluidHandlerItem drinkerBuffer = gadgetStackSupplier.get().getCapability(Capabilities.FluidHandler.ITEM, null);
                IFluidHandlerItem containerHandler = held.getCapability(Capabilities.FluidHandler.ITEM, null);
                if (drinkerBuffer == null || containerHandler == null) return;

                // Same destination hazard gate as fillContainers -- see ModFluidUtil#canHold. Without it this
                // slot is a second route around the tier ladder: the buffer may legitimately hold
                // an extreme-heat fluid thanks to a Safety Module, but that says nothing about
                // whether the container placed here can.
                FluidStack buffered = drinkerBuffer.getFluidInTank(0);
                if (buffered.isEmpty() || !ModFluidUtil.canHold(containerHandler, buffered)) return;

                if (net.neoforged.neoforge.fluids.FluidUtil.tryFluidTransfer(containerHandler, drinkerBuffer, Integer.MAX_VALUE, true).isEmpty()) return;
                set(containerHandler.getContainer());
            }
        }
    }

    ////////////////////Gadget health\\\\\\\\\\\\\\\\\\\\

    /**
     * A big, bulky rig dying: heavy smoke and a deep bellow. Deliberately the low end of the family
     * -- S.I.P.P.I.N.G. dies with the same cry pitched up, so the two read as the same kind of
     * creature at different sizes.
     */
    @Override
    public void onGadgetDeath(ServerLevel level, ItemEntity entity, ItemStack stack) {
        IGadget.deathFlourish(level, entity, ParticleTypes.LARGE_SMOKE, 24, 0.18,
                SoundEvents.GHAST_HURT, 0.9F, 0.55F);
        IGadget.deathFlourish(level, entity, ParticleTypes.SMOKE, 12, 0.25,
                SoundEvents.GENERIC_EXTINGUISH_FIRE, 0.5F, 0.7F);
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
                                held.getAmount(), effectiveCapacity(stack))
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
