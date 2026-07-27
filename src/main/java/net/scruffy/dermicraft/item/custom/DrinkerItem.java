package net.scruffy.dermicraft.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Start the sustained-use state immediately on click -- all siphon logic keys off this,
        // never off an animation finishing, so the deliberately-long activate clip can't gate it.
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    ////////////////////Siphon\\\\\\\\\\\\\\\\\\\\

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
        // Note the short-circuit: tryAccumulate only runs while the trigger is held, and reports
        // whether it actually fed the ghost buffer this tick (false when off target or no room).
        boolean accumulated = holdingTrigger && tryAccumulate(level, player, stack);
        if (!accumulated) drainGhost(stack);

        // Gated on actually drawing fluid, not merely holding the trigger -- the bladder shouldn't
        // inflate while aimed at nothing. Tradeoff: sweeping off target mid-siphon does bounce the
        // model between activate/deactivate; the controller's transition blending softens it.
        if (stack.getOrDefault(ModDataComponentTypes.DRINKER_SIPHONING.get(), false) != accumulated) {
            stack.set(ModDataComponentTypes.DRINKER_SIPHONING.get(), accumulated);
        }
    }

    /** @return whether the ghost buffer actually advanced this tick. */
    private boolean tryAccumulate(Level level, Player player, ItemStack stack) {
        // SOURCE_ONLY rather than the crosshair's usual fluid-blind raycast -- same approach
        // BucketItem takes, since fluids aren't normal interaction targets.
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return false;

        BlockPos pos = hit.getBlockPos();
        BlockState blockState = level.getBlockState(pos);
        FluidState fluidState = blockState.getFluidState();
        if (fluidState.isEmpty() || !fluidState.isSource()) return false;
        if (!(blockState.getBlock() instanceof BucketPickup)) return false;

        FluidStack source = new FluidStack(fluidState.getType(), CAPACITY);

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
                    FluidData.createData(new FluidStack(fluidState.getType(), progress)));
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
