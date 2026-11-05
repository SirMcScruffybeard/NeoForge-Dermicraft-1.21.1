package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
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
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.EffluentcerBlock;
import net.scruffy.dermicraft.block.custom.EffluentcerVisualState;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IEvolvingMachine;
import net.scruffy.dermicraft.interfaces.IModuleTabMachine;
import net.scruffy.dermicraft.machine.MachineStatKey;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.interfaces.IHaveModules;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.TwoFluidRecipeInput;
import net.scruffy.dermicraft.recipe.effluencing.EffluencingRecipe;
import net.scruffy.dermicraft.screen.custom.effluentcer.EffluentcerMenu;
import net.scruffy.dermicraft.tank.ModFluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class EffluentcerBlockEntity extends AbstractFueledMachineBlockEntity<EffluencingRecipe>
        implements MenuProvider, IHaveInventory, IHasChannels, IEvolvingMachine, IModuleTabMachine {

    public static final int INVENTORY_SIZE = 4;

    private final ModFluidTank INPUT_A_TANK = createInputTank(1);
    private final ModFluidTank INPUT_B_TANK = createInputTank(2);
    private final ModFluidTank RESULT_TANK = createResultTank();

    private boolean isTransferringFluids = false;

    public final ItemStackHandler INVENTORY = createItemHandler(INVENTORY_SIZE);

    // General Module slot(s) -- own dedicated handler (see MachineBaseBlockEntity#createModuleInventory), not part of
    // INVENTORY above. Evolution Modules go in the dedicated Evolution slot (EVOLUTION_INVENTORY, in the engine), never here.
    public final ItemStackHandler MODULE_INVENTORY = createModuleInventory(getModuleSlotCount(),
            stack -> stack.is(net.scruffy.dermicraft.datagen.tag.ModTags.Items.MODULES) && !isEvolutionModule(stack));


    // Set by the menu whenever a player opens the GUI -- used only as an eject target for
    // the fill-and-eject item-slot behavior (see createItemHandler()).
    @Nullable
    private Player interactingPlayer;

    private int resultAmount = 0;
    private int requiredAmountForA = 0;
    private int requiredAmountForB = 0;

    // Which screen tab was last open -- same pattern as every other Module-tab machine, so
    // reopening the screen returns to the tab last viewed.
    private boolean moduleTabActive = false;

    @Override
    public ItemStackHandler getModuleInventory() {
        return MODULE_INVENTORY;
    }

    @Override
    public boolean isModuleTabActive() {
        return moduleTabActive;
    }

    @Override
    public void setModuleTabActive(boolean active) {
        this.moduleTabActive = active;
    }

    public EffluentcerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.EFFLUENTCER_BE.get(), pos, blockState);
    }

    // ---- Evolution ----------------------------------------------------------------------------
    // The Effluentcer evolves in place through its evolution state and the Evolution slot. The gradual-progress
    // machinery, the Evolution slot, the flourish and the stat defaults (tank capacity, HP, speed, fuel use, Module
    // slots) all live in AbstractFueledMachineBlockEntity; this class opts in and resizes what it owns.

    @Override
    protected boolean usesEvolutionState() {
        return true;
    }

    @Override
    protected MachineStatKey getStatKey() {
        return MachineStatKey.EFFLUENTCER;
    }

    @Override
    protected void onEvolutionStateChanged() {
        super.onEvolutionStateChanged();
        resizeModuleInventory(MODULE_INVENTORY, getModuleSlotCount());
        applyCapacityBonus();
    }

    @Override
    protected RecipeType<EffluencingRecipe> getRecipeType() {
        return ModRecipes.EFFLUENCING_TYPE.get();
    }

    // ---- Visual state (face texture) --------------------------------------------------------
    // tickHealing runs every cycle regardless of crafting state, so it doubles as the visual-state
    // refresh point. Recovering (health < maxHealth) takes priority over Running -- a damaged
    // machine signals distress even mid-cycle. Mirrors MutatorBlockEntity/MasticatorBlockEntity's
    // identical mechanism.
    //
    // Debounced: a new state must be observed for VISUAL_STATE_STABLE_CYCLES consecutive cycles
    // (2 cycles = ~1s at CRAFT_TICKS=10) before the texture commits, to avoid flicker on borderline
    // conditions; every commit is a setBlock (client update + chunk re-render), so flapping is also
    // wasted churn. Transient, deliberately not saved to NBT.
    //
    // isRecipeValid(activeRecipe) is required here, not just hasCraftingInputs()/hasCraftingOutputRoom()
    // -- those two are vacuously true while idle (requiredAmountForA/B and resultAmount default to 0,
    // so hasEnoughFluid(0)/hasRoom(0) are trivially satisfied), so without the recipe check RUNNING
    // would show whenever fueled, even with no actual recipe active.
    private static final int VISUAL_STATE_STABLE_CYCLES = 2;

    @Nullable
    private EffluentcerVisualState pendingVisualState = null;
    private int pendingVisualCycles = 0;

    /** The face texture is read at the end of the cycle, after progress and any hazard damage, so a machine that heals
     * and is hurt again every cycle still shows as recovering. */
    @Override
    protected void onCycleEnd() {
        updateVisualState();
    }

    private void updateVisualState() {
        if (level == null) return;

        EffluentcerVisualState computed = computeVisualState();
        BlockState state = getBlockState();

        if (state.getValue(EffluentcerBlock.STATE) == computed) {
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
            level.setBlock(worldPosition, state.setValue(EffluentcerBlock.STATE, computed), Block.UPDATE_CLIENTS);
            pendingVisualState = null;
            pendingVisualCycles = 0;
        }
    }

    private EffluentcerVisualState computeVisualState() {
        if (maxHealth > 0 && health < maxHealth) return EffluentcerVisualState.RECOVERING;
        if (!isStarved() && isRecipeValid(activeRecipe) && hasCraftingInputs() && hasCraftingOutputRoom()) {
            return EffluentcerVisualState.RUNNING;
        }
        return EffluentcerVisualState.IDLE;
    }

    // ---- Hazards ----------------------------------------------------------------------------------

    /** The hazards that matter when the Effluentcer processes: both input fluids it consumes and the fluid its recipe
     * produces (the fuel does not). */
    @Override
    protected List<FluidStack> getProcessingFluids() {
        return List.of(INPUT_A_TANK.getFluid(), INPUT_B_TANK.getFluid(), craftResult(1));
    }

    @Override
    protected HazardProfile getHazardProfile() {
        return installedHazardProfile();
    }

    /** The hazards this machine tolerates, which cancel the matching penalties: a Tier 1 base (none), plus the Safety
     * Modules in the general Module slots, plus the permanent tolerance it has earned by evolving, plus the tolerance
     * of a hazard Evolution Module that is evolving it right now. Recomputed on demand, never cached. */
    protected HazardProfile installedHazardProfile() {
        HazardProfile profile = HazardProfile.TIER_1;
        for (int slot = 0; slot < MODULE_INVENTORY.getSlots(); slot++) {
            profile = IHaveModules.installedHazardProfile(profile, MODULE_INVENTORY.getStackInSlot(slot));
        }
        return addEvolvingTolerance(addPermanentTolerance(profile));
    }

    /** Work Speed Module bonus across this machine's Module slot(s) -- see
     * IHaveModules#workSpeedMultiplier for the diminishing-return stacking rule. */
    @Override
    protected float workSpeedMultiplier() {
        List<ItemStack> modules = new ArrayList<>();
        for (int slot = 0; slot < MODULE_INVENTORY.getSlots(); slot++) {
            modules.add(MODULE_INVENTORY.getStackInSlot(slot));
        }
        return IHaveModules.workSpeedMultiplier(modules);
    }

    /** Capacity Module bonus, summed over every installed Module. */
    @Override
    protected int getCapacityBonus() {
        int total = 0;
        for (int i = 0; i < MODULE_INVENTORY.getSlots(); i++) {
            total += capacityModuleBonus(MODULE_INVENTORY.getStackInSlot(i));
        }
        return total;
    }

    @Override
    protected void applyCapacityBonus() {
        int capacity = getBaseTankCapacity() + getCapacityBonus();
        FUEL_TANK.setCapacity(capacity);
        INPUT_A_TANK.setCapacity(capacity);
        INPUT_B_TANK.setCapacity(capacity);
        RESULT_TANK.setCapacity(capacity);
    }

    @Override
    protected boolean canRemoveModule(int slot) {
        int thisBonus = capacityModuleBonus(MODULE_INVENTORY.getStackInSlot(slot));
        if (thisBonus == 0) return true;
        int newCapacity = getBaseTankCapacity() + (getCapacityBonus() - thisBonus);
        return FUEL_TANK.getFluid().getAmount() <= newCapacity
                && INPUT_A_TANK.getFluid().getAmount() <= newCapacity
                && INPUT_B_TANK.getFluid().getAmount() <= newCapacity
                && RESULT_TANK.getFluid().getAmount() <= newCapacity;
    }

    /** How far the pending evolution is, 0 to 1 (0 with no live Evolution Module). Read by
     * {@code EvolutionOverlayBlockEntityRenderer}'s creeping overlay. */
    @Override
    public float getEvolutionProgressFraction() {
        return getGradualEvolutionFraction();
    }

    /** See {@link IHasChannels#describeFluidFace} -- mirrors {@link #getTank} literally. */
    @Override
    public Component describeFluidFace(Direction face) {
        if (face == Direction.UP) return Component.translatable("tooltip.dermicraft.tank.fuel");
        if (face == Direction.DOWN) return Component.translatable("tooltip.dermicraft.tank.result");

        Direction facing = getBlockState().getValue(EffluentcerBlock.FACING);
        if (face == facing || face == facing.getOpposite()) {
            return Component.translatable("tooltip.dermicraft.tank.input_a");
        }
        return Component.translatable("tooltip.dermicraft.tank.input_b");
    }

    public IFluidHandler getTank(@Nullable Direction direction) {
        if (direction == null) return INPUT_A_TANK;

        if (direction == Direction.UP) return FUEL_TANK;
        if (direction == Direction.DOWN) return RESULT_TANK;

        Direction facing = getBlockState().getValue(EffluentcerBlock.FACING);
        if (direction == facing || direction == facing.getOpposite()) return INPUT_A_TANK;

        return INPUT_B_TANK;
    }

    /** See {@link IHasChannels#describeFace} -- mirrors {@link #getTank}/{@link #getItemHandler} literally. */
    @Override
    public Component describeFace(Direction face) {
        if (face == Direction.UP) return Component.translatable("tooltip.dermicraft.idep.face.effluentcer_fuel");
        if (face == Direction.DOWN) return Component.translatable("tooltip.dermicraft.idep.face.effluentcer_result");

        Direction facing = getBlockState().getValue(EffluentcerBlock.FACING);
        if (face == facing || face == facing.getOpposite()) return Component.translatable("tooltip.dermicraft.idep.face.effluentcer_input_a");
        return Component.translatable("tooltip.dermicraft.idep.face.effluentcer_input_b");
    }

    public ModFluidTank getInputATank() {
        return INPUT_A_TANK;
    }

    public ModFluidTank getInputBTank() {
        return INPUT_B_TANK;
    }

    public ModFluidTank getResultTank() {
        return RESULT_TANK;
    }

    /**
     * Self-described channel list for the Gate multiblock -- see {@link IHasChannels}.
     * Direction-unlocked from what {@code getTank(Direction)} currently hard-binds to UP/DOWN/the
     * facing-relative sides. All four of the Effluentcer's item slots are pure bucket-passthrough
     * for their matching tank (see {@code createItemHandler}'s biDirectionalFluidTransfer calls) --
     * there's no independent item-ingredient slot like the Masticator's, so only the four fluid
     * tanks need channels.
     *
     * <p>input_a/input_b's native faces depend on the block's own {@code FACING} state (read fresh
     * here, same as {@code getTank} does) -- input_a is {@code facing}+{@code facing.getOpposite()},
     * input_b is the other two horizontals. Each channel is omitted once its native face(s) already
     * have a direct connection (see {@link #isFaceServiced}).
     */
    @Override
    public List<Channel> getChannels() {
        List<Channel> channels = new ArrayList<>();

        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID, Direction.UP)) {
            channels.add(new Channel.FluidChannel("fuel", Component.literal("Fuel"), Channel.IO.IN, FUEL_TANK));
        }

        Direction facing = getBlockState().getValue(EffluentcerBlock.FACING);
        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID, facing, facing.getOpposite())) {
            channels.add(new Channel.FluidChannel("input_a", Component.literal("Input A"), Channel.IO.IN, INPUT_A_TANK));
        }
        Direction otherA = facing.getClockWise();
        Direction otherB = facing.getCounterClockWise();
        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID, otherA, otherB)) {
            channels.add(new Channel.FluidChannel("input_b", Component.literal("Input B"), Channel.IO.IN, INPUT_B_TANK));
        }

        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID, Direction.DOWN)) {
            channels.add(new Channel.FluidChannel("result", Component.literal("Result"), Channel.IO.OUT, RESULT_TANK));
        }

        return channels;
    }

    public FluidStack getFluid(int slot) {

        if (slot == FUEL_TANK.SLOT) return FUEL_TANK.getFluid();

        else if (slot == INPUT_A_TANK.SLOT) return INPUT_A_TANK.getFluid();

        else if (slot == INPUT_B_TANK.SLOT) return INPUT_B_TANK.getFluid();

        else if (slot == RESULT_TANK.SLOT) return RESULT_TANK.getFluid();

        else return FluidStack.EMPTY;
    }

    public IItemHandler getItemHandler(@Nullable Direction direction) {
        if (direction == null) return INVENTORY;

        int targetSlot;
        if (direction == Direction.UP) {
            targetSlot = FUEL_TANK.SLOT;
        } else if (direction == Direction.DOWN) {
            targetSlot = RESULT_TANK.SLOT;
        } else {
            Direction facing = getBlockState().getValue(EffluentcerBlock.FACING);
            targetSlot = (direction == facing || direction == facing.getOpposite())
                    ? INPUT_A_TANK.SLOT : INPUT_B_TANK.SLOT;
        }

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

            // Automation is deliberately capped at one container per slot (see getSlotLimit
            // below) -- a stack can never accumulate here, so there's nothing for the GUI's
            // fill-and-eject behavior to do on this path.
            @Override
            @NotNull
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!INVENTORY.getStackInSlot(targetSlot).isEmpty()) return stack;

                ItemStack single = stack.copyWithCount(1);
                ItemStack rejected = INVENTORY.insertItem(targetSlot, single, simulate);
                if (!rejected.isEmpty()) return stack;

                ItemStack remainder = stack.copy();
                remainder.shrink(1);
                return remainder;
            }

            @Override
            @NotNull
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return INVENTORY.extractItem(targetSlot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return true;
            }
        };
    }

    @Override
    public void drops() {
        super.drops(INVENTORY);
        super.drops(MODULE_INVENTORY);
        super.drops(EVOLUTION_INVENTORY);
    }

    public void setInteractingPlayer(@Nullable Player player) {
        this.interactingPlayer = player;
    }

    @Override
    protected void drainOutputs(Level level) {
        if (autoDrainEnabled && !RESULT_TANK.isEmpty()) {
            RESULT_TANK.pushFluidToBelowNeighbour(level, worldPosition);
        }
    }

    @Override
    protected boolean hasCraftingInputs() {
        return INPUT_A_TANK.hasEnoughFluid(requiredAmountForA) && INPUT_B_TANK.hasEnoughFluid(requiredAmountForB);
    }

    @Override
    protected boolean hasCraftingOutputRoom() {
        // hasRoom() alone only checks capacity, not fluid type -- if RESULT_TANK already holds a
        // DIFFERENT fluid (e.g. left over from a previous recipe), there can still be "room" by
        // amount, but RESULT_TANK.fill() would silently refuse it in onCraftComplete() (vanilla
        // FluidTank.fill() rejects a mismatched fluid), consuming both inputs for nothing.
        FluidStack pendingResult = craftResult(resultAmount);
        FluidStack current = RESULT_TANK.getFluid();
        if (!current.isEmpty() && !current.getFluid().isSame(pendingResult.getFluid())) return false;
        return RESULT_TANK.hasRoom(resultAmount);
    }

    @Override
    protected void onCraftComplete() {
        // Captured before draining: draining INPUT_A_TANK fires its onContentsChanged()
        // synchronously, which re-resolves the recipe against the now-partially-drained tanks and
        // can reset requiredAmountForB/resultAmount/activeRecipe before this method reaches them.
        int amountA = requiredAmountForA;
        int amountB = requiredAmountForB;
        FluidStack output = craftResult(resultAmount);

        INPUT_A_TANK.useFluid(amountA);
        INPUT_B_TANK.useFluid(amountB);
        RESULT_TANK.fill(output, IFluidHandler.FluidAction.EXECUTE);
    }

    private Optional<RecipeHolder<EffluencingRecipe>> getRecipeOptional() {
        if (level == null) return Optional.empty();

        RecipeManager recipeManager = level.getRecipeManager();
        return recipeManager.getRecipeFor(ModRecipes.EFFLUENCING_TYPE.get(),
                new TwoFluidRecipeInput(INPUT_A_TANK.getFluid(), INPUT_B_TANK.getFluid()), this.level);
    }

    // protected (not private) so a subclass can call it
    // directly from its own overridden createInputTank() -- see MasticatorBlockEntity's identical
    // getRecipeOptional()/setActiveRecipe() split for the same reason.
    protected void resolveRecipe() {
        // Captured before setActiveRecipe() overwrites it -- swapping either input fluid directly
        // for a different one that still resolves to SOME valid recipe must still restart progress;
        // comparing recipe identity (not just "a recipe is present") is what tells a genuine
        // ingredient swap apart from simply topping up a tank that still matches the same recipe,
        // which should NOT interrupt an in-progress craft.
        RecipeHolder<EffluencingRecipe> previousRecipe = this.activeRecipe;
        Optional<RecipeHolder<EffluencingRecipe>> recipeOpt = getRecipeOptional();
        setActiveRecipe(recipeOpt);

        if (isRecipeValid(activeRecipe)) {
            setOrientation();
            setMaxProgress();
            setResultAmount();
            if (previousRecipe == null || !previousRecipe.id().equals(activeRecipe.id())) {
                resetProgress();
            }
        } else {
            resetActiveRecipe();
            resetMaxProgress();
            resetProgress();
            resetResultAmount();
        }
    }

    private void setActiveRecipe(Optional<RecipeHolder<EffluencingRecipe>> opt) {
        if (opt.isPresent()) {
            this.activeRecipe = opt.get();
        } else {
            this.resetActiveRecipe();
            resetMaxProgress(); // Ensures the machine safely idles
        }
    }

    // Recipe matching is order-independent (either input tank can hold either half of the
    // pair), so once a match is found we still need to know which orientation matched, in
    // order to draw the correct required amount from each tank.
    private void setOrientation() {
        EffluencingRecipe recipe = activeRecipe.value();
        Fluid fluidInA = INPUT_A_TANK.getFluid().getFluid();
        Fluid fluidInB = INPUT_B_TANK.getFluid().getFluid();

        if (recipe.isStraightOrientation(fluidInA, fluidInB)) {
            requiredAmountForA = recipe.fluidAAmount();
            requiredAmountForB = recipe.fluidBAmount();
        } else {
            requiredAmountForA = recipe.fluidBAmount();
            requiredAmountForB = recipe.fluidAAmount();
        }
    }

    private void setResultAmount() {
        resultAmount = activeRecipe.value().getCraftingAmount();
    }

    private void resetResultAmount() {
        resultAmount = 0;
        requiredAmountForA = 0;
        requiredAmountForB = 0;
    }

    public void resetActiveRecipe() {
        activeRecipe = null;
    }

    private FluidStack craftResult(int craftAmount) {
        if (isRecipeValid(activeRecipe)) {
            return activeRecipe.value().getResultFluidStack(craftAmount);
        }
        return FluidStack.EMPTY;
    }

    private void setMaxProgress() {
        maxProgress = activeRecipe.value().getCraftingTime();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", INVENTORY.serializeNBT(registries));
        tag.put("module_inventory", MODULE_INVENTORY.serializeNBT(registries));
        tag.put("inputA", INPUT_A_TANK.writeToNBT(registries, new CompoundTag()));
        tag.put("inputB", INPUT_B_TANK.writeToNBT(registries, new CompoundTag()));
        tag.put("output", RESULT_TANK.writeToNBT(registries, new CompoundTag()));
        tag.putInt("resultFluid", resultAmount);
        tag.putInt("requiredA", requiredAmountForA);
        tag.putInt("requiredB", requiredAmountForB);
        tag.putBoolean("module_tab_active", moduleTabActive);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompoundTag oldInventoryTag = tag.getCompound("inventory");
        if (tag.contains("inventory")) {
            // Worlds saved before this handler existed at its current size have a smaller Size --
            // see MachineBaseBlockEntity#loadItemHandler for why a plain deserializeNBT would
            // shrink INVENTORY back down and crash the menu (slot out of range).
            loadItemHandler(INVENTORY, INVENTORY_SIZE, registries, oldInventoryTag);
        }

        if (tag.contains("module_inventory")) {
            loadItemHandler(MODULE_INVENTORY, getModuleSlotCount(), registries, tag.getCompound("module_inventory"));
        } else {
            // Pre-split save: the Module item was the old combined INVENTORY's trailing slot
            // (index 4, back when INVENTORY_SIZE was 5) -- see
            // MachineBaseBlockEntity#extractLegacyModuleStack.
            ItemStack legacyModule = extractLegacyModuleStack(registries, oldInventoryTag, 4);
            if (!legacyModule.isEmpty()) MODULE_INVENTORY.setStackInSlot(0, legacyModule);
        }

        applyCapacityBonus();
        if (tag.contains("inputA")) INPUT_A_TANK.readFromNBT(registries, tag.getCompound("inputA"));
        if (tag.contains("inputB")) INPUT_B_TANK.readFromNBT(registries, tag.getCompound("inputB"));
        if (tag.contains("output")) RESULT_TANK.readFromNBT(registries, tag.getCompound("output"));
        resultAmount = tag.getInt("resultFluid");
        requiredAmountForA = tag.getInt("requiredA");
        requiredAmountForB = tag.getInt("requiredB");
        moduleTabActive = tag.getBoolean("module_tab_active");
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.EFFLUENTCER);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new EffluentcerMenu(containerId, playerInventory, this);
    }

    protected ItemStackHandler createItemHandler(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                if (level == null || level.isClientSide()) return;

                if (isTransferringFluids) return;

                biDirectionalFluidTransfer(FUEL_TANK, FUEL_TANK.SLOT);
                biDirectionalFluidTransfer(INPUT_A_TANK, INPUT_A_TANK.SLOT);
                biDirectionalFluidTransfer(INPUT_B_TANK, INPUT_B_TANK.SLOT);
                biDirectionalFluidTransfer(RESULT_TANK, RESULT_TANK.SLOT);

                isTransferringFluids = false;

                setChanged();
                updateBlock();
            }

            private void biDirectionalFluidTransfer(ModFluidTank tank, int slot) {

                if (tank.hasFluidHandlerInSlot(this, slot)) {
                    isTransferringFluids = true;
                    tank.transferFluidToTank(this, slot);

                } else {
                    transferToHandlerWithEject(tank, slot);
                }

            }

            // Fills one empty container from the tank at a time. A stack of empties can only
            // ever reach this slot via the GUI (automation is capped at one item per slot by
            // getItemHandler(Direction)'s wrapper), so a stack here is by definition
            // player-placed -- the newly-filled container can't restack with the remaining
            // empties, so it's ejected to the player instead of being lost.
            private void transferToHandlerWithEject(ModFluidTank tank, int slot) {
                if (!tank.hasEmptyFluidHandlerInSlot(this, slot)) return;

                ItemStack stack = getStackInSlot(slot);
                if (stack.isEmpty()) return;

                isTransferringFluids = true;

                if (stack.getCount() > 1) {
                    ItemStack single = stack.copyWithCount(1);
                    ItemStack remainder = stack.copy();
                    remainder.shrink(1);
                    setStackInSlot(slot, remainder);

                    ItemStackHandler singleHandler = new ItemStackHandler(1);
                    singleHandler.setStackInSlot(0, single);
                    tank.transferFluidFromTankToHandler(singleHandler, 0);

                    ejectToPlayerOrDrop(singleHandler.getStackInSlot(0));
                } else {
                    tank.transferFluidFromTankToHandler(this, slot);
                }
            }
        };
    }

    private void ejectToPlayerOrDrop(ItemStack stack) {
        if (stack.isEmpty() || level == null) return;

        if (interactingPlayer != null && !interactingPlayer.isRemoved()) {
            if (!interactingPlayer.getInventory().add(stack)) {
                interactingPlayer.drop(stack, false);
            }
        } else {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), stack);
        }
    }

    protected ModFluidTank createInputTank(int slot) {
        return new ModFluidTank(getBaseTankCapacity(), slot) {
            @Override
            protected void onContentsChanged() {

                if (level != null && !level.isClientSide()) {
                    resolveRecipe();
                }
                setChanged();
            }
        };
    }

    protected ModFluidTank createResultTank() {
        return new ModFluidTank(getBaseTankCapacity(), 3) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide()) {
                    if (autoDrainEnabled) {
                        this.pushFluidToBelowNeighbour(level, worldPosition);
                    }
                    setChanged();
                }
            }
        };
    }
}
