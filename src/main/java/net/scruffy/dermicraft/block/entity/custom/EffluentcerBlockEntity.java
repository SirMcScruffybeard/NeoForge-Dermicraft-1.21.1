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
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.TwoFluidRecipeInput;
import net.scruffy.dermicraft.recipe.effluencing.EffluencingRecipe;
import net.scruffy.dermicraft.screen.custom.effluentcer.EffluentcerMenu;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.tank.VulnerableTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class EffluentcerBlockEntity extends AbstractFueledMachineBlockEntity<EffluencingRecipe>
        implements MenuProvider, IHaveInventory, IHasChannels {

    private final VulnerableTank INPUT_A_TANK = createInputTank(1);
    private final VulnerableTank INPUT_B_TANK = createInputTank(2);
    private final VulnerableTank RESULT_TANK = createResultTank();

    private boolean isTransferringFluids = false;

    private final ItemStackHandler INVENTORY = createItemHandler();

    // Set by the menu whenever a player opens the GUI -- used only as an eject target for
    // the fill-and-eject item-slot behavior (see createItemHandler()).
    @Nullable
    private Player interactingPlayer;

    private int resultAmount = 0;
    private int requiredAmountForA = 0;
    private int requiredAmountForB = 0;

    public EffluentcerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.EFFLUENTCER_BE.get(), pos, blockState);
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

    @Override
    protected boolean tickHealing(boolean fueled) {
        boolean healed = super.tickHealing(fueled);
        updateVisualState();
        return healed;
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

    public VulnerableTank getInputATank() {
        return INPUT_A_TANK;
    }

    public VulnerableTank getInputBTank() {
        return INPUT_B_TANK;
    }

    public VulnerableTank getResultTank() {
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
    }

    public void setInteractingPlayer(@Nullable Player player) {
        this.interactingPlayer = player;
    }

    @Override
    protected void drainOutputs(Level level) {
        if (!RESULT_TANK.isEmpty()) {
            RESULT_TANK.pushFluidToBelowNeighbour(level, worldPosition);
        }
    }

    @Override
    protected boolean hasCraftingInputs() {
        return INPUT_A_TANK.hasEnoughFluid(requiredAmountForA) && INPUT_B_TANK.hasEnoughFluid(requiredAmountForB);
    }

    @Override
    protected boolean hasCraftingOutputRoom() {
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

    private void resolveRecipe() {
        Optional<RecipeHolder<EffluencingRecipe>> recipeOpt = getRecipeOptional();
        setActiveRecipe(recipeOpt);

        if (isRecipeValid(activeRecipe)) {
            setOrientation();
            setMaxProgress();
            setResultAmount();
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
        tag.put("inputA", INPUT_A_TANK.writeToNBT(registries, new CompoundTag()));
        tag.put("inputB", INPUT_B_TANK.writeToNBT(registries, new CompoundTag()));
        tag.put("output", RESULT_TANK.writeToNBT(registries, new CompoundTag()));
        tag.putInt("resultFluid", resultAmount);
        tag.putInt("requiredA", requiredAmountForA);
        tag.putInt("requiredB", requiredAmountForB);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) INVENTORY.deserializeNBT(registries, tag.getCompound("inventory"));
        if (tag.contains("inputA")) INPUT_A_TANK.readFromNBT(registries, tag.getCompound("inputA"));
        if (tag.contains("inputB")) INPUT_B_TANK.readFromNBT(registries, tag.getCompound("inputB"));
        if (tag.contains("output")) RESULT_TANK.readFromNBT(registries, tag.getCompound("output"));
        resultAmount = tag.getInt("resultFluid");
        requiredAmountForA = tag.getInt("requiredA");
        requiredAmountForB = tag.getInt("requiredB");
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

    protected ItemStackHandler createItemHandler() {
        return new ItemStackHandler(4) {
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

    private VulnerableTank createInputTank(int slot) {
        return new VulnerableTank(getTier().tankCapacity(), slot) {
            @Override
            protected void onContentsChanged() {

                if (level != null && !level.isClientSide()) {
                    resolveRecipe();
                }
                setChanged();
            }
        };
    }

    private VulnerableTank createResultTank() {
        return new VulnerableTank(getTier().tankCapacity(), 3) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide()) {
                    this.pushFluidToBelowNeighbour(level, worldPosition);
                    setChanged();
                }
            }
        };
    }
}
