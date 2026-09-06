package net.scruffy.dermicraft.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.scruffy.dermicraft.block.custom.duct.AbstractInnardsDuctBlock;
import net.scruffy.dermicraft.block.entity.custom.MarredTumorBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.StitchedTumorBlockEntity;
import net.scruffy.dermicraft.component.AidModeData;
import net.scruffy.dermicraft.component.AidPendingModeData;
import net.scruffy.dermicraft.component.HeldItemData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.interfaces.ICollectBlocks;
import net.scruffy.dermicraft.interfaces.ICutStitches;
import net.scruffy.dermicraft.interfaces.IGadget;
import net.scruffy.dermicraft.interfaces.IHarvestParts;
import net.scruffy.dermicraft.interfaces.IHarvestableBlock;
import net.scruffy.dermicraft.interfaces.IHaveFluidData;
import net.scruffy.dermicraft.interfaces.IInject;
import net.scruffy.dermicraft.interfaces.IInjectableBlock;
import net.scruffy.dermicraft.interfaces.ISutableBlock;
import net.scruffy.dermicraft.interfaces.ISuture;
import net.scruffy.dermicraft.screen.custom.aid.AidMenu;
import net.scruffy.dermicraft.util.ModMath;
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
 * A.I.D. -- Adaptive Intervention Device. Mode cycling and every mode's full animation set are
 * wired: Forceps (deploy, idle, retract, grab), Scalpel (deploy, retract, cut), Suture (deploy,
 * retract, idle, sow), and Syringe (deploy, retract, inject) -- see
 * {@code src/docs/aid-gadget-notes.md} for the full design.
 *
 * <p>Tap right-click with no valid target steps through Forceps -> Scalpel -> Suture -> Syringe,
 * single direction, wrapping back to Forceps. Leaving any mode (all four now {@link #hasController})
 * doesn't commit {@link AidModeData} immediately: it fires that mode's {@code retract} trigger and
 * parks the target mode in {@link AidPendingModeData} until the retract clip's real length has
 * elapsed (see {@link #retractTicks}), so the outgoing bone (kept visible by {@code AidItemModel}
 * reading the still-uncommitted current mode) stays on screen for the whole retract instead of
 * popping away under it. No target-priority yet (a valid target should eventually win over cycling,
 * matching Drinker's precedent).
 */
public class AidItem extends Item implements GeoItem, IGadget, ICollectBlocks, IHarvestParts, ISuture, IHaveFluidData, IInject {

    /** Gadget health, expressed as vanilla durability -- see {@link IGadget}. */
    public static final int MAX_HP = 10;

    /** Syringe mode's single-fluid tank -- see the design notes. */
    public static final int FLUID_CAPACITY = 1000;

    /** Matches forceps_retract's authored length (0.5s / 10 ticks) -- see aid.animation.json. */
    private static final int FORCEPS_RETRACT_TICKS = 10;
    /** Matches scalpel_retract's authored length (1s / 20 ticks) -- see aid.animation.json. */
    private static final int SCALPEL_RETRACT_TICKS = 20;
    /** Matches suture_retract's authored length (0.5s / 10 ticks) -- see aid.animation.json. */
    private static final int SUTURE_RETRACT_TICKS = 10;
    /** Matches syringe_retract's authored length (0.5s / 10 ticks) -- see aid.animation.json. */
    private static final int SYRINGE_RETRACT_TICKS = 10;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AidItem(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    ////////////////////Mode cycling\\\\\\\\\\\\\\\\\\\\

    /** Crouch opens the GUI (string slot + syringe fluid gauge, see {@link AidMenu}); standing
     * cycles mode, same crouch/stand split Drinker uses between acting on its buffer and cycling. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isCrouching()) {
                AidMenu.open((ServerPlayer) player, hand);
            } else {
                cycleMode((ServerLevel) level, player, stack);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void cycleMode(ServerLevel level, Player player, ItemStack stack) {
        // Mid-retract already -- swallow the extra tap rather than queuing/racing a second cycle.
        if (stack.has(ModDataComponentTypes.AID_PENDING_MODE_DATA.get())) return;

        AidModeData current = modeData(stack);
        AidModeData.Mode currentMode = current.modeEnum();
        AidModeData.Mode next = current.next().modeEnum();

        if (hasController(currentMode)) {
            triggerAnim(player, GeoItem.getOrAssignId(stack, level), controllerName(currentMode), "retract");
            stack.set(ModDataComponentTypes.AID_PENDING_MODE_DATA.get(),
                    new AidPendingModeData(next.ordinal(), level.getGameTime() + retractTicks(currentMode)));
        } else {
            stack.set(ModDataComponentTypes.AID_MODE_DATA.get(), current.next());
            fireDeploy(next, player, stack, level);
        }

        player.displayClientMessage(Component.translatable("tooltip.dermicraft.aid.mode",
                Component.translatable(modeKey(next))), true);
    }

    /** Commits a pending mode transition once its retract clip has actually finished. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) return;

        AidPendingModeData pending = stack.get(ModDataComponentTypes.AID_PENDING_MODE_DATA.get());
        if (pending == null || level.getGameTime() < pending.commitAtGameTime()) return;

        stack.set(ModDataComponentTypes.AID_MODE_DATA.get(), new AidModeData(pending.targetMode()));
        stack.remove(ModDataComponentTypes.AID_PENDING_MODE_DATA.get());

        if (level instanceof ServerLevel serverLevel) {
            fireDeploy(AidModeData.Mode.values()[pending.targetMode()], entity, stack, serverLevel);
        }
    }

    /** Only fires for a mode that actually has a registered controller -- see {@link #hasController}. */
    private void fireDeploy(AidModeData.Mode mode, Entity entity, ItemStack stack, ServerLevel level) {
        if (!hasController(mode)) return;
        triggerAnim(entity, GeoItem.getOrAssignId(stack, level), controllerName(mode), "deploy");
    }

    public static AidModeData modeData(ItemStack stack) {
        return stack.getOrDefault(ModDataComponentTypes.AID_MODE_DATA.get(), AidModeData.DEFAULT);
    }

    private static String modeKey(AidModeData.Mode mode) {
        return switch (mode) {
            case FORCEPS -> "tooltip.dermicraft.aid.mode.forceps";
            case SCALPEL -> "tooltip.dermicraft.aid.mode.scalpel";
            case SUTURE -> "tooltip.dermicraft.aid.mode.suture";
            case SYRINGE -> "tooltip.dermicraft.aid.mode.syringe";
        };
    }

    /** Which modes have an animation controller (deploy/retract) built so far. */
    private static boolean hasController(AidModeData.Mode mode) {
        return mode == AidModeData.Mode.FORCEPS || mode == AidModeData.Mode.SCALPEL
                || mode == AidModeData.Mode.SUTURE || mode == AidModeData.Mode.SYRINGE;
    }

    private static String controllerName(AidModeData.Mode mode) {
        return switch (mode) {
            case FORCEPS -> "Forceps";
            case SCALPEL -> "Scalpel";
            case SUTURE -> "Suture";
            case SYRINGE -> "Syringe";
        };
    }

    private static int retractTicks(AidModeData.Mode mode) {
        return switch (mode) {
            case FORCEPS -> FORCEPS_RETRACT_TICKS;
            case SCALPEL -> SCALPEL_RETRACT_TICKS;
            case SUTURE -> SUTURE_RETRACT_TICKS;
            case SYRINGE -> SYRINGE_RETRACT_TICKS;
        };
    }

    ////////////////////Target interactions\\\\\\\\\\\\\\\\\\\\

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        return switch (modeData(stack).modeEnum()) {
            case FORCEPS -> useForceps(stack, context, player);
            case SCALPEL -> useScalpel(stack, context, player);
            case SUTURE -> useSuture(stack, context, player);
            case SYRINGE -> useSyringe(stack, context, player);
        };
    }

    ////////////////////Forceps mode\\\\\\\\\\\\\\\\\\\\

    /**
     * Reuses ICollectBlocks' own logic wholesale, per the design notes ("any block picked up by it
     * will follow forceps' behavior") -- no internal storage, the block goes straight to the
     * player. Returns CONSUME rather than SUCCESS specifically to cancel the vanilla arm swing (see
     * DrinkerItem's onItemUseFirst for the same trick) -- the model's own "grab" trigger carries the
     * visual instead. No durability cost: unlike the standalone Forceps' Primitive alternate,
     * A.I.D.'s durability is Gadget HP (see IGadget), not per-use wear.
     */
    private InteractionResult useForceps(ItemStack stack, UseOnContext context, Player player) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (!canCollect(level, pos)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.CONSUME;

        collect(level, pos, player);
        triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel) level), "Forceps", "grab");
        return InteractionResult.CONSUME;
    }

    ////////////////////Scalpel mode\\\\\\\\\\\\\\\\\\\\

    /**
     * Harvest (deliberately NOT tag-driven): A.I.D. is not a member of {@code EXTRACTION_TOOLS},
     * because {@code TumorBlock.useItemOn} checks that tag with no notion of A.I.D.'s current mode
     * -- being permanently tagged would let a tumor get harvested while in any mode, not just
     * Scalpel. Instead this calls {@link IHarvestableBlock#harvest}/{@code changeState} directly,
     * gated on mode here, mirroring exactly what the tag-based path would have done. Same story for
     * cutting a Stitched Tumor's stitches ({@link ICutStitches}), also normally gated by
     * {@code EXTRACTION_TOOLS} on {@code StitchedTumorBlock}. Falls through to the free,
     * no-durability Innards Duct connection cycle (same as the standalone Scalpel) -- every branch
     * fires the "cut" trigger and returns CONSUME to cancel the vanilla swing in favor of it, same
     * reasoning as Forceps' "grab". Cutting stitches is the one action that keeps its real
     * durability cost (see {@link ICutStitches#cutStitches}'s own hurtAndBreak) -- unlike harvest/
     * duct-cycling/suture, this one genuinely costs the standalone Scalpel something too, so A.I.D.
     * inherits the same real Gadget-HP cost rather than being special-cased HP-free.
     */
    private InteractionResult useScalpel(ItemStack stack, UseOnContext context, Player player) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (isHarvestable(state) && state.getBlock() instanceof IHarvestableBlock harvestable) {
            if (level.isClientSide) return InteractionResult.CONSUME;

            harvestable.harvest(level, player, stack, pos);
            harvestable.changeState(level, pos, state);
            triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel) level), "Scalpel", "cut");
            return InteractionResult.CONSUME;
        }

        if (state.getBlock() instanceof ICutStitches cuttable
                && level.getBlockEntity(pos) instanceof StitchedTumorBlockEntity stitchedEntity) {
            if (level.isClientSide) return InteractionResult.CONSUME;

            cuttable.cutStitches(level, pos, player, stack, stitchedEntity);
            triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel) level), "Scalpel", "cut");
            return InteractionResult.CONSUME;
        }

        if (state.getBlock() instanceof AbstractInnardsDuctBlock duct
                && duct.cycleConnections(level, pos, state)) {
            if (level.isClientSide) return InteractionResult.CONSUME;

            triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel) level), "Scalpel", "cut");
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    ////////////////////Suture mode\\\\\\\\\\\\\\\\\\\\

    /**
     * Suture (deliberately NOT tag-driven, same reasoning as Scalpel's harvest): A.I.D. is not a
     * member of {@code SUTURE_TOOLS}, since {@code MarredTumorBlock.useItemOn} checks that tag with
     * no notion of A.I.D.'s current mode. Instead this calls {@link ISutableBlock#suture} directly,
     * gated on mode here -- that call internally routes back into A.I.D.'s own {@link #useMaterials}
     * exactly like it would for the standalone Suture Kit, so the string-slot gating below applies
     * automatically. Fires the "sow" trigger and returns CONSUME to cancel the vanilla swing, same
     * reasoning as Forceps' "grab" and Scalpel's "cut".
     *
     * <p>No self-suture heal gesture (the standalone Suture Kit's right-click-in-air-to-heal, see
     * {@link #suturePlayer}) -- same reason Scalpel dropped its self blood-let: that gesture is
     * claimed by the mode-cycle tap, and there's no other free gesture to give it.
     */
    private InteractionResult useSuture(ItemStack stack, UseOnContext context, Player player) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!isSutable(state) || !(state.getBlock() instanceof ISutableBlock sutable)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) return InteractionResult.CONSUME;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MarredTumorBlockEntity tumorEntity)) return InteractionResult.PASS;

        sutable.suture(level, pos, player, stack, tumorEntity, context.getHand());
        triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel) level), "Suture", "sow");
        return InteractionResult.CONSUME;
    }

    /**
     * Own string slot only ({@link ModDataComponentTypes#HELD_ITEM_DATA}) -- deliberately no
     * fallback search into the player's general inventory, unlike the standalone Suture Kit's
     * {@code consumeString(player)}. Empty (or non-string) slot hard-refuses with an action-bar
     * warning; no durability-damage fallback either, since A.I.D.'s durability is Gadget HP, not
     * per-use wear. The slot itself isn't fillable yet -- that's the not-yet-built GUI's job (see
     * the design notes) -- so this always refuses until that exists.
     */
    @Override
    public void useMaterials(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HeldItemData data = stack.getOrDefault(ModDataComponentTypes.HELD_ITEM_DATA.get(), HeldItemData.EMPTY);

        if (data.isEmpty() || !data.itemStack().is(Tags.Items.STRINGS)) {
            player.displayClientMessage(Component.translatable("tooltip.dermicraft.aid.no_string")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        ItemStack held = data.itemStack().copy();
        held.shrink(1);
        stack.set(ModDataComponentTypes.HELD_ITEM_DATA.get(), held.isEmpty() ? HeldItemData.EMPTY : new HeldItemData(held));
    }

    /** Unreachable via A.I.D.'s own gestures (see {@link #useSuture}'s class doc) -- implemented
     * for ISuture's contract only, matching the standalone Suture Kit's body should anything else
     * ever invoke it. */
    @Override
    public void suturePlayer(Level level, Player player, ItemStack stack) {
        useMaterials(player, player.getUsedItemHand());
        applySutureEffect(player, ModMath.Time.getSecondsToTicks(15), 0);
        playDefaultSutureSound(level, player);
    }

    ////////////////////Syringe mode\\\\\\\\\\\\\\\\\\\\

    /**
     * Inject (deliberately NOT tag-driven, same reasoning as Scalpel's harvest/Suture's sow):
     * A.I.D. is not a member of {@code INJECTION_TOOLS}, since {@code StitchedTumorBlock.useItemOn}
     * checks that tag with no notion of A.I.D.'s current mode. Instead this calls
     * {@link IInjectableBlock#inject} directly, gated on mode here -- that call internally checks
     * {@code stack.getItem() instanceof IInject}, which A.I.D. now is, and drains its own
     * {@link ModDataComponentTypes#FLUID_DATA} exactly like the standalone Syringe would. Fires the
     * "inject" trigger and returns CONSUME only when the injection actually took (a real recipe/
     * fluid match) -- an injection that didn't take plays no animation, same "nothing happened"
     * feel as the standalone Syringe's silent no-op.
     *
     * <p>Falls through to {@link FluidUtil#interactWithFluidHandler(Player, InteractionHand, Level,
     * BlockPos, Direction)} against a world fluid-handler block -- the exact same helper every
     * machine block in the mod (Masticator, Skin Tank, Render Kiln, ...) already uses against a
     * held bucket: tries filling A.I.D. from the block first, falling back to draining A.I.D. into
     * it. Uses A.I.D.'s own registered {@code FluidHandler.ITEM} capability, so it reads/writes
     * {@link ModDataComponentTypes#FLUID_DATA} the normal way. {@link #isValidFluidHandler}/
     * {@link #getTargetFluidHandler} are used purely as a cheap side-effect-free "is this actually a
     * relevant target" pre-check (safe on both sides), so a non-fluid block still correctly falls
     * through to PASS instead of swallowing the click. No dedicated clip for this (only deploy/
     * retract/inject were authored), so it's a plain vanilla-swing SUCCESS on an actual transfer,
     * same treatment as Scalpel's duct-cycling. Note: like any bucket-style container, creative-mode
     * players won't see the container side of the transfer persist (FluidUtil's own creative-mode
     * shortcut deliberately leaves a creative player's held item unchanged) -- this only affects the
     * block being drawn from/emptied into, not A.I.D. itself.
     */
    private InteractionResult useSyringe(ItemStack stack, UseOnContext context, Player player) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof IInjectableBlock injectable
                && level.getBlockEntity(pos) instanceof StitchedTumorBlockEntity stitchedEntity) {
            if (level.isClientSide) return InteractionResult.CONSUME;

            boolean injected = injectable.inject(level, player, stack, stitchedEntity);
            if (injected) {
                triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel) level), "Syringe", "inject");
            }
            return InteractionResult.CONSUME;
        }

        Direction face = context.getClickedFace();
        if (isValidFluidHandler(getTargetFluidHandler(level, pos, face))) {
            if (level.isClientSide) return InteractionResult.CONSUME;

            return FluidUtil.interactWithFluidHandler(player, context.getHand(), level, pos, face)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }

        return InteractionResult.PASS;
    }

    /** One injection's worth -- matches the standalone Syringe's own single-dose capacity. */
    private static final int DOSE_SIZE = 100;

    /**
     * {@code StitchedTumorBlock#inject} calls this polymorphically to consume whatever was just
     * injected -- the default ({@link IInject#emptyDataFluidIfSurvival}) wipes the whole tank, which
     * is only correct for the standalone Syringe because it never holds more than one dose anyway.
     * A.I.D.'s tank holds up to {@link #FLUID_CAPACITY}, so one injection should only cost one dose
     * ({@link #DOSE_SIZE}), leaving the rest loaded for the next one -- routes to {@link IInject}'s
     * new {@link IInject#useDoseIfSurvival} instead of the blanket-wipe default.
     */
    @Override
    public void emptyDataFluidIfSurvival(ItemStack stack, Player player) {
        useDoseIfSurvival(stack, player, DOSE_SIZE);
    }

    /**
     * Single-slot, string-only view over {@link ModDataComponentTypes#HELD_ITEM_DATA} -- the GUI's
     * item slot ({@link AidMenu}) and {@link #useMaterials} both read/write the same live component
     * on the same stack, so filling the slot in the GUI is immediately what Suture mode consumes
     * from. Reads the stack fresh via a supplier rather than capturing one at construction, matching
     * ScrenchMenu's own caution around a held item's identity changing hand/slot mid-session.
     */
    public static class StringSlotHandler implements IItemHandlerModifiable {

        private final java.util.function.Supplier<ItemStack> aidStack;

        public StringSlotHandler(java.util.function.Supplier<ItemStack> aidStack) {
            this.aidStack = aidStack;
        }

        private ItemStack held() {
            return aidStack.get().getOrDefault(ModDataComponentTypes.HELD_ITEM_DATA.get(), HeldItemData.EMPTY).itemStack();
        }

        private void setHeld(ItemStack stack) {
            aidStack.get().set(ModDataComponentTypes.HELD_ITEM_DATA.get(),
                    stack.isEmpty() ? HeldItemData.EMPTY : new HeldItemData(stack));
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return held();
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            setHeld(stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !isItemValid(slot, stack)) return stack;

            ItemStack current = held();
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) return stack;

            int space = getSlotLimit(slot) - current.getCount();
            int toInsert = Math.min(space, stack.getCount());
            if (toInsert <= 0) return stack;

            if (!simulate) {
                setHeld(current.isEmpty() ? stack.copyWithCount(toInsert) : current.copyWithCount(current.getCount() + toInsert));
            }

            ItemStack remainder = stack.copy();
            remainder.shrink(toInsert);
            return remainder;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack current = held();
            if (current.isEmpty() || amount <= 0) return ItemStack.EMPTY;

            int toExtract = Math.min(amount, current.getCount());
            ItemStack extracted = current.copyWithCount(toExtract);

            if (!simulate) {
                setHeld(current.copyWithCount(current.getCount() - toExtract));
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(Tags.Items.STRINGS);
        }
    }

    ////////////////////Animation\\\\\\\\\\\\\\\\\\\\

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "Forceps", 0, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            AidModeData.Mode mode = stack != null ? modeData(stack).modeEnum() : AidModeData.Mode.FORCEPS;
            if (mode != AidModeData.Mode.FORCEPS) return PlayState.STOP;

            // Deploy is its own explicit trigger (fired the instant Forceps mode commits, see
            // cycleMode/inventoryTick), not baked into this continuous state: GeckoLib caches an
            // animation by content, so returning the literal same "deploy then loop idle" descriptor
            // every re-entry would just resume mid-idle from a prior visit instead of replaying
            // deploy -- this steady state is deliberately idle-only so there's nothing to resume but
            // the loop, and deploy always genuinely restarts as a trigger.
            return state.setAndContinue(RawAnimation.begin().thenLoop("forceps_idle"));
        })
                .triggerableAnim("deploy", RawAnimation.begin().thenPlay("forceps_deploy"))
                .triggerableAnim("retract", RawAnimation.begin().thenPlay("forceps_retract"))
                .triggerableAnim("grab", RawAnimation.begin().thenPlay("forceps_grab")));

        // No idle clip (by design -- Scalpel holds its last pose after deploy rather than looping),
        // so the continuous state is just STOP whenever Scalpel is active: deploy/retract/cut are
        // all one-shot triggers, and STOP simply holds whatever frame the last one left behind.
        controllers.add(new AnimationController<>(this, "Scalpel", 0, state -> PlayState.STOP)
                .triggerableAnim("deploy", RawAnimation.begin().thenPlay("scalpel_deploy"))
                .triggerableAnim("retract", RawAnimation.begin().thenPlay("scalpel_retract"))
                .triggerableAnim("cut", RawAnimation.begin().thenPlay("scalpel_cut")));

        // Idle-only steady state, same reasoning as Forceps' controller above.
        controllers.add(new AnimationController<>(this, "Suture", 0, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            AidModeData.Mode mode = stack != null ? modeData(stack).modeEnum() : AidModeData.Mode.SUTURE;
            if (mode != AidModeData.Mode.SUTURE) return PlayState.STOP;

            return state.setAndContinue(RawAnimation.begin().thenLoop("suture_idle"));
        })
                .triggerableAnim("deploy", RawAnimation.begin().thenPlay("suture_deploy"))
                .triggerableAnim("retract", RawAnimation.begin().thenPlay("suture_retract"))
                .triggerableAnim("sow", RawAnimation.begin().thenPlay("suture_sow")));

        // No idle clip (by design -- same as Scalpel, Syringe holds its last pose after deploy).
        controllers.add(new AnimationController<>(this, "Syringe", 0, state -> PlayState.STOP)
                .triggerableAnim("deploy", RawAnimation.begin().thenPlay("syringe_deploy"))
                .triggerableAnim("retract", RawAnimation.begin().thenPlay("syringe_retract"))
                .triggerableAnim("inject", RawAnimation.begin().thenPlay("syringe_inject")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
