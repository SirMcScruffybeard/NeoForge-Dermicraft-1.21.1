package net.scruffy.dermicraft.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
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
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
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

/**
 * D.R.I.N.K.E.R. -- held fluid vacuum. Hold right-click aimed at a fluid source block to siphon it.
 *
 * <p><b>Atomic pickup:</b> a source block is indivisible (same reason a bucket is all-or-nothing),
 * so holding fills a "ghost buffer" ({@link ModDataComponentTypes#DRINKER_SIPHON_PROGRESS}) toward
 * a full {@link #CAPACITY}, and only on reaching it is the block actually removed and the fluid
 * banked. Coming off target -- by aiming away or letting go -- drains the ghost buffer back down
 * gradually rather than instantly, so a momentary flick of the crosshair costs a little time
 * instead of all progress. Nothing is ever half-banked.
 *
 * <p>Modes (Storage/Transfer/Disposal), machine/tank draining, and the gadget HP mechanic are not
 * built yet -- this is the core siphon only. The buffer currently behaves as Storage mode does.
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
     * renders at the untransformed origin (floating above the camera). If a custom pose is ever
     * wanted, the supported route is NeoForge's {@code IClientItemExtensions#applyForgeHandTransform}.
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

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Reached only when no block claimed the click -- aiming at a bare fluid source (vanilla's
        // crosshair ray ignores fluids, so that reads as a miss) or at nothing at all.
        // Start the sustained-use state immediately on click -- all siphon logic keys off this,
        // never off an animation finishing, so the deliberately-long activate clip can't gate it.
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
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

    /** What, if anything, moved fluid this tick. SOURCE is distinguished because it's the only
     * path that feeds the ghost buffer rather than banking directly. */
    private enum Draw {NOTHING, SOURCE, TANK}

    private Draw siphonTick(Level level, Player player, ItemStack stack) {
        Target target = findSiphonTarget(level, player);
        if (target == null) return Draw.NOTHING;

        if (target.isTank()) {
            // A tank is divisible, unlike a source block, so it transfers continuously and banks as
            // it goes -- no ghost buffer, and nothing to lose by looking away part-way through.
            return drainTank(stack, target.tank()) ? Draw.TANK : Draw.NOTHING;
        }
        return accumulateSource(level, player, stack, target) ? Draw.SOURCE : Draw.NOTHING;
    }

    /** Continuous partial transfer out of a machine/tank face. */
    private boolean drainTank(ItemStack stack, IFluidHandler tank) {
        IFluidHandlerItem buffer = stack.getCapability(Capabilities.FluidHandler.ITEM, null);
        if (buffer == null) return false;

        FluidStack available = tank.drain(SIPHON_RATE, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty()) return false;

        // Hazard gating and capacity both ride on the buffer's own handler, same as the source path.
        int accepted = buffer.fill(available, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return false;

        // Drain by stack, not by amount: on a multi-tank handler drain(int) could pull a different
        // fluid than the one that was just approved.
        FluidStack request = available.copy();
        request.setAmount(accepted);
        FluidStack drained = tank.drain(request, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return false;

        buffer.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    /** @return whether the ghost buffer actually advanced this tick. */
    private boolean accumulateSource(Level level, Player player, ItemStack stack, Target target) {
        BlockPos pos = target.pos();
        BlockState blockState = target.blockState();
        FluidStack source = new FluidStack(blockState.getFluidState().getType(), CAPACITY);

        // Room for a WHOLE source block or nothing -- a partial fill would strand fluid that can
        // never be picked up. Hazard gating rides along for free here: the registered handler is
        // hazard-gated, so it refuses an intolerable fluid outright.
        IFluidHandlerItem buffer = stack.getCapability(Capabilities.FluidHandler.ITEM, null);
        if (buffer == null || buffer.fill(source, IFluidHandler.FluidAction.SIMULATE) < CAPACITY) return false;

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

        completeSiphon(level, player, stack, pos, blockState, source, buffer);
        return true;
    }

    /**
     * Ghost buffer is full: take the block and bank the fluid.
     *
     * <p>Goes through {@link BucketPickup} rather than just clearing the block, so waterlogged
     * blocks give up their water and survive instead of being destroyed. The returned bucket is
     * discarded -- it's only used as vanilla's confirmation that the pickup succeeded.
     */
    private void completeSiphon(Level level, Player player, ItemStack stack, BlockPos pos,
                                BlockState blockState, FluidStack source, IFluidHandlerItem buffer) {
        BucketPickup pickup = (BucketPickup) blockState.getBlock();
        ItemStack picked = pickup.pickupBlock(player, level, pos, blockState);
        if (picked.isEmpty()) return;

        buffer.fill(source, IFluidHandler.FluidAction.EXECUTE);
        pickup.getPickupSound(blockState).ifPresent(sound ->
                level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F));

        // Component writes come after every capability call -- re-fetching a handler across a
        // stack.set() has silently returned a stale one before (see IdepItem.jamClearGeneric).
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
