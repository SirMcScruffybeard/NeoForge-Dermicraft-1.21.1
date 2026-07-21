package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.MutatorBlock;
import net.scruffy.dermicraft.block.custom.MutatorVisualState;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.OneFluidOneItemRecipeInput;
import net.scruffy.dermicraft.recipe.mutating.MutatingRecipe;
import net.scruffy.dermicraft.screen.custom.mutator.MutatorMenu;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.tank.VulnerableTank;
import net.scruffy.dermicraft.util.ModFluidUtil;
import net.scruffy.dermicraft.util.ModItemUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Mutator: "apply a fluid to an item to change the item." Two modes, toggled by the player
 * (see {@link #setMode}):
 * <ul>
 *     <li>{@link Mode#MUTATE} -- fuel + reagent fluid (consumed) + item (consumed) -> a different,
 *     more complex item. Recipe-driven ({@link MutatingRecipe}), same shape as the Metastasizer's
 *     recipe but the input item is consumed rather than kept as a pattern.</li>
 *     <li>{@link Mode#FILL} -- no fuel/reagent cost beyond the fluid itself, which is packaged
 *     INTO the input item rather than consumed. Generic capability operation, not recipe-driven --
 *     see {@link #tickFill}.</li>
 * </ul>
 * Both modes are gated by the standard HP mechanic (see {@link AbstractFueledMachineBlockEntity}),
 * so a starved Mutator does neither.
 */
public class MutatorBlockEntity extends AbstractFueledMachineBlockEntity<MutatingRecipe>
        implements MenuProvider, IHaveInventory, IHasChannels {

    public static final int INPUT_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;

    private static final int FILL_RATE = 250; // mB per cycle, standard rate

    public enum Mode { MUTATE, FILL }

    private final VulnerableTank REAGENT_TANK = createReagentTank();

    private boolean isTransferringFluids = false;

    private final ItemStackHandler INVENTORY = createInventory(4);

    private ItemStack cachedResult = ItemStack.EMPTY;
    private int requiredFluid = 0;

    private Mode mode = Mode.MUTATE;

    // Fill-mode state -- see tickFill(). fillBudget is a rate-equivalent timer, not a real fluid
    // store (the reagent tank keeps holding the real fluid the whole time), so switching modes or
    // swapping the input item can just reset it to 0 with nothing to refund.
    private int fillBudget = 0;

    // Tracks whether the machine has bottomed out at 0 HP and not yet fully recovered -- fill is
    // blocked for the whole "recovering from 0" window, not just while HP == 0 (see setMode/tickFill
    // and the machine notes' HP-gated fill rule). Distinct from ordinary partial-HP recovery
    // (health < maxHealth but never hit 0), which only slows fill rather than blocking it.
    private boolean recoveringFromZero = false;

    public MutatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MUTATOR_BE.get(), pos, blockState);
    }

    @Override
    protected RecipeType<MutatingRecipe> getRecipeType() {
        return ModRecipes.MUTATING_TYPE.get();
    }

    public VulnerableTank getReagentTank() {
        return REAGENT_TANK;
    }

    public Mode getMode() {
        return mode;
    }

    /** Server-side mode toggle, invoked from {@code MutatorModeClickPayload}. */
    public void toggleMode() {
        setMode(mode == Mode.MUTATE ? Mode.FILL : Mode.MUTATE);
    }

    public void setMode(Mode mode) {
        if (this.mode == mode) return;
        this.mode = mode;
        fillBudget = 0;
        resetActiveRecipe();
        // Switching back to MUTATE with valid contents already in place: re-resolve immediately
        // rather than waiting for the next inventory/tank change to trigger it.
        if (mode == Mode.MUTATE) resolveRecipe();
        setChanged();
        updateBlock();
    }

    public FluidStack getFluid(int slot) {
        if (slot == FUEL_TANK.SLOT) return FUEL_TANK.getFluid();
        else if (slot == REAGENT_TANK.SLOT) return REAGENT_TANK.getFluid();
        else return FluidStack.EMPTY;
    }

    public IFluidHandler getTank(@Nullable Direction direction) {
        if (direction == Direction.UP) return FUEL_TANK;
        return REAGENT_TANK;
    }

    /** See {@link IHasChannels#describeFace} -- mirrors {@link #getTank}/{@link #getItemHandler} literally. */
    @Override
    public Component describeFace(Direction face) {
        return switch (face) {
            case UP -> Component.translatable("tooltip.dermicraft.idep.face.mutator_fuel");
            case DOWN -> Component.translatable("tooltip.dermicraft.idep.face.mutator_output");
            default -> Component.translatable("tooltip.dermicraft.idep.face.mutator_input");
        };
    }

    // Face routing: top = fuel (fluid + its bucket slot), bottom = result slot only,
    // sides = both the reagent fluid (via getTank's fluid capability) and the input item
    // (via this item capability) -- letting a hopper feed the input while a pipe fills the
    // reagent tank from the same side faces.
    public IItemHandler getItemHandler(@Nullable Direction direction) {
        if (direction == null) return INVENTORY;

        int targetSlot = switch (direction) {
            case UP -> FUEL_TANK.SLOT;
            case DOWN -> OUTPUT_SLOT;
            default -> INPUT_SLOT;
        };
        return new IItemHandlerModifiable() {
            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                INVENTORY.setStackInSlot(targetSlot, stack);
            }

            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            @NotNull
            public ItemStack getStackInSlot(int slot) {
                return INVENTORY.getStackInSlot(targetSlot);
            }

            @Override
            @NotNull
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                // The output slot is machine-produced only -- never accept automation inserts there.
                if (targetSlot == OUTPUT_SLOT) return stack;
                return INVENTORY.insertItem(targetSlot, stack, simulate);
            }

            @Override
            @NotNull
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return INVENTORY.extractItem(targetSlot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return INVENTORY.getSlotLimit(targetSlot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return targetSlot != OUTPUT_SLOT && INVENTORY.isItemValid(targetSlot, stack);
            }
        };
    }

    /**
     * Self-described channel list for the Gate multiblock -- see {@link IHasChannels}. Mirrors the
     * Metastasizer's scheme exactly (fuel = UP, reagent = everything except UP, input = sides,
     * output = DOWN), fuel listed first per the mod's starvation-risk-first channel-priority
     * convention.
     */
    @Override
    public List<Channel> getChannels() {
        List<Channel> channels = new ArrayList<>();

        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID, Direction.UP)) {
            channels.add(new Channel.FluidChannel("fuel", Component.literal("Fuel"), Channel.IO.IN, FUEL_TANK));
        }
        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID,
                Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            channels.add(new Channel.FluidChannel("reagent", Component.literal("Reagent"), Channel.IO.IN, REAGENT_TANK));
        }
        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.ITEM,
                Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            channels.add(new Channel.ItemChannel("input", Component.literal("Input"), Channel.IO.IN, singleSlotHandler(INPUT_SLOT, true)));
        }
        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.ITEM, Direction.DOWN)) {
            channels.add(new Channel.ItemChannel("output", Component.literal("Output"), Channel.IO.OUT, singleSlotHandler(OUTPUT_SLOT, false)));
        }

        return channels;
    }

    /** A single-slot view of INVENTORY, insert-gated by {@code allowInsert} -- mirrors the OUTPUT_SLOT
     * "machine-produced only, never accept automation inserts" rule already enforced in getItemHandler. */
    private IItemHandler singleSlotHandler(int slot, boolean allowInsert) {
        return new IItemHandlerModifiable() {
            @Override
            public void setStackInSlot(int i, ItemStack stack) {
                INVENTORY.setStackInSlot(slot, stack);
            }

            @Override
            public int getSlots() {
                return 1;
            }

            @NotNull
            @Override
            public ItemStack getStackInSlot(int i) {
                return INVENTORY.getStackInSlot(slot);
            }

            @NotNull
            @Override
            public ItemStack insertItem(int i, ItemStack stack, boolean simulate) {
                if (!allowInsert) return stack;
                return INVENTORY.insertItem(slot, stack, simulate);
            }

            @NotNull
            @Override
            public ItemStack extractItem(int i, int amount, boolean simulate) {
                return INVENTORY.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int i) {
                return INVENTORY.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int i, ItemStack stack) {
                return allowInsert && INVENTORY.isItemValid(slot, stack);
            }
        };
    }

    // Direct right-click helpers, mirroring the same face routing as getItemHandler:
    // sides expose the input slot, bottom exposes the (pull-only) result slot.
    public ItemStack insertInput(ItemStack stack) {
        return insertItemStack(INVENTORY, INPUT_SLOT, stack);
    }

    public ItemStack extractInput() {
        return extractItemStack(INVENTORY, INPUT_SLOT);
    }

    public ItemStack extractResult() {
        return extractItemStack(INVENTORY, OUTPUT_SLOT);
    }

    @Override
    public void drops() {
        super.drops(INVENTORY);
    }

    @Override
    protected void drainOutputs(Level level) {
        if (!INVENTORY.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            ModItemUtil.pushItemToBelowNeighbour(level, worldPosition, INVENTORY, OUTPUT_SLOT);
        }
    }

    // ---- Mode-branching engine hooks -----------------------------------------------------------
    //
    // FILL mode deliberately does NOT impersonate an active recipe to the base engine (an earlier
    // version overrode isRecipeValid() to return true in FILL mode, which also lied to the base
    // class's saveAdditional() guard -- `activeRecipe.id()` NPE on save). Instead, fill runs from
    // the tickHealing() hook (see the visual-state section below), which the base tick() calls
    // unconditionally every cycle; in FILL mode activeRecipe simply stays null and the base
    // recipe branch idles honestly.

    @Override
    protected boolean hasCraftingInputs() {
        if (mode == Mode.FILL) return hasFillCandidate(); // only reached via computeVisualState()
        return hasInput() && hasEnoughReagent();
    }

    @Override
    protected boolean hasCraftingOutputRoom() {
        if (mode == Mode.FILL) return true; // only reached via computeVisualState(); fill leaves the item in place if OUTPUT_SLOT is occupied
        return hasOutputRoom();
    }

    @Override
    protected void damageMachine(int amount) {
        super.damageMachine(amount);
        if (isStarved()) recoveringFromZero = true;
    }

    @Override
    protected void healMachine(int amount) {
        super.healMachine(amount);
        if (health >= maxHealth) recoveringFromZero = false;
    }

    // ---- Visual state (face texture) -------------------------------------------------------------
    // tickHealing() is the one hook the base tick() calls unconditionally every cycle regardless of
    // crafting state, so it doubles as the visual-state refresh point. Recovering (health < maxHealth)
    // takes priority over Running -- a damaged machine signals distress even mid-cycle.
    //
    // Debounced: a new state must be observed for VISUAL_STATE_STABLE_CYCLES consecutive cycles
    // (2 cycles = ~1s at CRAFT_TICKS=10) before the texture commits. Borderline conditions can
    // otherwise strobe -- e.g. a hopper feeding the input every 8 ticks against the 10-tick cycle
    // flaps RUNNING/IDLE -- and every commit is a setBlock (client update + chunk re-render), so
    // flapping is also wasted churn. Matching the current state resets any pending change, so a
    // one-cycle blip never flashes the face. Transient, deliberately not saved to NBT.

    private static final int VISUAL_STATE_STABLE_CYCLES = 2;

    @Nullable
    private MutatorVisualState pendingVisualState = null;
    private int pendingVisualCycles = 0;

    @Override
    protected boolean tickHealing(boolean fueled) {
        boolean healed = super.tickHealing(fueled);
        // FILL mode's work happens here rather than through the recipe branch -- tickHealing is
        // called unconditionally every cycle, and fill has no recipe for the base engine to track
        // (see the mode-branching note above). tickFill() gates itself on HP (fillBlocked()).
        if (mode == Mode.FILL) {
            tickFill(healed);
        }
        updateVisualState();
        return healed;
    }

    private void updateVisualState() {
        if (level == null) return;

        MutatorVisualState computed = computeVisualState();
        BlockState state = getBlockState();

        if (state.getValue(MutatorBlock.STATE) == computed) {
            pendingVisualState = null;
            pendingVisualCycles = 0;
            return;
        }

        if (pendingVisualState != computed) {
            pendingVisualState = computed;
            pendingVisualCycles = 1;
            return;
        }

        if (++pendingVisualCycles >= VISUAL_STATE_STABLE_CYCLES) {
            level.setBlock(worldPosition, state.setValue(MutatorBlock.STATE, computed), Block.UPDATE_CLIENTS);
            pendingVisualState = null;
            pendingVisualCycles = 0;
        }
    }

    private MutatorVisualState computeVisualState() {
        if (maxHealth > 0 && health < maxHealth) return MutatorVisualState.RECOVERING;
        if (!isStarved() && hasCraftingInputs() && hasCraftingOutputRoom()) return MutatorVisualState.RUNNING;
        return MutatorVisualState.IDLE;
    }

    // ---- Mutate mode (recipe-driven, mirrors the Metastasizer's resolve/complete shape) ---------

    @Override
    protected void onCraftComplete() {
        // Captured before draining: useFluid() fires REAGENT_TANK.onContentsChanged()
        // synchronously, which re-resolves the recipe and can clear requiredFluid/
        // cachedResult before this method finishes.
        int amount = requiredFluid;
        ItemStack output = cachedResult.copy();

        REAGENT_TANK.useFluid(amount);
        INVENTORY.extractItem(INPUT_SLOT, 1, false); // the input item IS consumed (unlike the Metastasizer's pattern)
        INVENTORY.insertItem(OUTPUT_SLOT, output, false);
    }

    // Restores the cached result/fluid amount when a saved recipe is reloaded from NBT.
    @Override
    protected void onRecipeResolved(RecipeHolder<MutatingRecipe> recipe) {
        this.cachedResult = recipe.value().getResult();
        this.requiredFluid = recipe.value().getFluidAmount();
    }

    private void resolveRecipe() {
        if (mode == Mode.FILL) return;

        Optional<RecipeHolder<MutatingRecipe>> opt = getRecipeOptional();
        if (opt.isPresent()) {
            this.activeRecipe = opt.get();
            this.maxProgress = activeRecipe.value().getCraftingTime();
            this.cachedResult = activeRecipe.value().getResult();
            this.requiredFluid = activeRecipe.value().getFluidAmount();
        } else {
            resetActiveRecipe();
        }
    }

    private Optional<RecipeHolder<MutatingRecipe>> getRecipeOptional() {
        if (level == null) return Optional.empty();

        RecipeManager recipeManager = level.getRecipeManager();
        return recipeManager.getRecipeFor(ModRecipes.MUTATING_TYPE.get(),
                new OneFluidOneItemRecipeInput(INVENTORY.getStackInSlot(INPUT_SLOT), REAGENT_TANK.getFluid()), this.level);
    }

    public void resetActiveRecipe() {
        activeRecipe = null;
        resetMaxProgress();
        resetProgress();
        cachedResult = ItemStack.EMPTY;
        requiredFluid = 0;
    }

    private boolean hasInput() {
        return !INVENTORY.getStackInSlot(INPUT_SLOT).isEmpty();
    }

    private boolean hasEnoughReagent() {
        return REAGENT_TANK.hasEnoughFluid(requiredFluid);
    }

    private boolean hasOutputRoom() {
        if (cachedResult.isEmpty()) return false;
        return INVENTORY.insertItem(OUTPUT_SLOT, cachedResult.copy(), true).isEmpty();
    }

    // ---- Fill mode (generic capability operation -- no recipe system involved) -------------------
    //
    // Rate-limited (FILL_RATE mB/cycle, 0.1x while recovering from partial HP, blocked entirely at
    // HP 0 or while recovering from a past 0 -- see damageMachine/healMachine and fillBlocked()).
    // Every container fills through the same budget flow: probe what a SINGLE unit of the input
    // stack would accept, accumulate a rate-equivalent "budget" across cycles until it covers that
    // amount, then complete the fill in one atomic action -- verify (simulate fill + simulate output
    // insert) BEFORE any fluid moves, drain, move the filled unit to the output, shrink the input
    // stack by 1. Rigid containers (bucket/bottle-style, exact-volume only) and flexible ones both
    // ride the same path; the budget just covers different amounts. The reagent tank holds the real
    // fluid the whole time (nothing is set aside early), so the budget is just a timer, not a real
    // fluid store -- toggling modes or swapping the input item can reset it to 0 with nothing to
    // refund.

    private boolean hasFillCandidate() {
        ItemStack target = INVENTORY.getStackInSlot(INPUT_SLOT);
        return !target.isEmpty() && !REAGENT_TANK.isEmpty()
                && target.getCapability(Capabilities.FluidHandler.ITEM) != null;
    }

    private boolean fillBlocked() {
        return isStarved() || recoveringFromZero;
    }

    private void tickFill(boolean healedThisCycle) {
        if (fillBlocked()) return;

        ItemStack target = INVENTORY.getStackInSlot(INPUT_SLOT);
        if (target.isEmpty() || REAGENT_TANK.isEmpty()) {
            fillBudget = 0;
            return;
        }

        // Always operate on a SINGLE unit off the (possibly stacked) input. Filling mutates the
        // item's data, so the whole stack must never be replaced with the fill result -- an earlier
        // version overwrote the input stack with the single filled item, silently destroying the
        // rest of a stack of empties. One unit fills (via the budget below), then that unit alone
        // moves to the output and the input stack shrinks by 1; the next unit starts fresh.
        //
        // `required` is probed against a synthetic, effectively-unlimited sample of the tank's fluid
        // TYPE -- never the tank's current amount. Probing with the current amount was a real bug:
        // a flexible container would report its available (partial) room as `required` whenever the
        // tank ran low, so the fill would "complete" and ship out a container that isn't actually
        // full; a rigid container would instead reject the too-small sample outright and get pushed
        // through the output UNFILLED, misread as "wrong fluid." Both were the same root cause. The
        // fix always waits for a genuinely FULL fill -- if the player wants to drain the reagent tank
        // to something less than full, the tank's own bucket slot is the tool for that, not this slot.
        ItemStack single = target.copyWithCount(1);
        FluidStack fullProbe = new FluidStack(REAGENT_TANK.getFluid().getFluid(), Integer.MAX_VALUE);
        int required = simulateFill(single, fullProbe);
        if (required <= 0) {
            // Wrong fluid, or the container is already full -- pass one unit through to the output
            // (when there's room) so a pre-filled/mismatched item can't jam the input slot forever.
            fillBudget = 0;
            passThroughToOutput();
            return;
        }

        int rate = Math.round(FILL_RATE * (healedThisCycle ? RECOVERY_SPEED_FACTOR : 1f));
        if (rate <= 0) return;

        fillBudget += rate;
        setChanged();
        if (fillBudget < required) return;
        if (REAGENT_TANK.getFluidAmount() < required) return; // budget's ready, but the tank itself doesn't hold enough yet -- keep waiting

        // Budget AND tank both cover the full requirement. Verify the whole completion BEFORE any
        // fluid actually moves: simulate the fill, then simulate the output insert -- if either fails
        // (tank drained by something else this same tick, output blocked), hold with the budget
        // pinned and retry next cycle. Nothing is drained or lost while holding.
        FluidActionResult simulated = FluidUtil.tryFillContainer(single, REAGENT_TANK, required, null, false);
        if (!simulated.isSuccess()) return;
        if (!INVENTORY.insertItem(OUTPUT_SLOT, simulated.getResult(), true).isEmpty()) return;

        FluidActionResult filled = FluidUtil.tryFillContainer(single, REAGENT_TANK, required, null, true);
        if (!filled.isSuccess()) return;

        fillBudget = 0;
        INVENTORY.extractItem(INPUT_SLOT, 1, false);
        INVENTORY.insertItem(OUTPUT_SLOT, filled.getResult(), false);
        setChanged();
        updateBlock();
    }

    /** Moves one unit of the input stack to the output unchanged (fill can't act on it). Only fires
     * when the output can take it; otherwise the item just waits in the input slot. */
    private void passThroughToOutput() {
        ItemStack single = INVENTORY.getStackInSlot(INPUT_SLOT).copyWithCount(1);
        if (INVENTORY.insertItem(OUTPUT_SLOT, single, true).isEmpty()) {
            INVENTORY.extractItem(INPUT_SLOT, 1, false);
            INVENTORY.insertItem(OUTPUT_SLOT, single, false);
            setChanged();
            updateBlock();
        }
    }

    private int simulateFill(ItemStack target, FluidStack sample) {
        IFluidHandlerItem handler = target.copy().getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) return 0;
        return handler.fill(sample, IFluidHandler.FluidAction.SIMULATE);
    }

    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.MUTATOR);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MutatorMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", INVENTORY.serializeNBT(registries));
        tag.put("reagent", REAGENT_TANK.writeToNBT(registries, new CompoundTag()));
        tag.putInt("requiredFluid", requiredFluid);
        tag.putInt("mode", mode.ordinal());
        tag.putInt("fillBudget", fillBudget);
        tag.putBoolean("recoveringFromZero", recoveringFromZero);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) INVENTORY.deserializeNBT(registries, tag.getCompound("inventory"));
        if (tag.contains("reagent")) REAGENT_TANK.readFromNBT(registries, tag.getCompound("reagent"));
        requiredFluid = tag.getInt("requiredFluid");
        mode = tag.getInt("mode") == Mode.FILL.ordinal() ? Mode.FILL : Mode.MUTATE;
        fillBudget = tag.getInt("fillBudget");
        recoveringFromZero = tag.getBoolean("recoveringFromZero");
    }

    private ItemStackHandler createInventory(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                if (level == null || level.isClientSide()) return;

                if (isTransferringFluids) return;

                if (slot == INPUT_SLOT && mode == Mode.MUTATE) resolveRecipe();

                biDirectionalFluidTransfer(FUEL_TANK, FUEL_TANK.SLOT);
                biDirectionalFluidTransfer(REAGENT_TANK, REAGENT_TANK.SLOT);

                isTransferringFluids = false;

                setChanged();
                updateBlock();
            }

            private void biDirectionalFluidTransfer(ModFluidTank tank, int tankSlot) {
                if (tank.hasFluidHandlerInSlot(this, tankSlot)) {
                    isTransferringFluids = true;
                    tank.transferFluidToTank(this, tankSlot);
                } else if (tank.hasEmptyFluidHandlerInSlot(this, tankSlot)) {
                    isTransferringFluids = true;
                    tank.transferFluidFromTankToHandler(this, tankSlot);
                }
            }

            // Fuel/reagent tank slots hold exactly one container at a time, unconditionally -- not
            // just once already occupied (the old `hasEmptyFluidHandlerInSlot` condition inspected
            // the slot's CURRENT contents, which never fires on a slot that starts empty, so a whole
            // stack of empty containers could be inserted in one shot; the auto-fill transfer then
            // only ever fills and returns a single container, silently collapsing the rest of the
            // stack -- the exact bug the fuel/reagent slots shared with every other machine's tank
            // slots). INPUT_SLOT deliberately stays stack-capable regardless of item type: FILL mode
            // processes a whole stack of containers one unit at a time via its own budget/pass-through
            // logic (see tickFill), and MUTATE mode's ingredient slot follows the Masticator's
            // convention of allowing a stack even though only one item is consumed per cycle.
            @Override
            public int getSlotLimit(int slot) {
                if (slot == FUEL_TANK.SLOT || slot == REAGENT_TANK.SLOT) return 1;
                return super.getSlotLimit(slot);
            }
        };
    }

    private VulnerableTank createReagentTank() {
        return new VulnerableTank(getTier().tankCapacity(), 1) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide()) {
                    if (mode == Mode.MUTATE) resolveRecipe();
                    setChanged();
                }
            }
        };
    }
}
