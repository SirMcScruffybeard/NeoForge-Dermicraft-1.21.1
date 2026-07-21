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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.RenderKilnBlock;
import net.scruffy.dermicraft.block.custom.RenderKilnVisualState;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.recipe.FluidOnlyRecipeInput;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.rendering.RenderingRecipe;
import net.scruffy.dermicraft.screen.custom.render_kiln.RenderKilnMenu;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.tank.VulnerableTank;
import net.scruffy.dermicraft.util.ModItemUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RenderKilnBlockEntity extends AbstractFueledMachineBlockEntity<RenderingRecipe>
        implements MenuProvider, IHaveInventory, IHasChannels {

    public static final int OUTPUT_SLOT = 2;

    private final VulnerableTank INPUT_TANK = createInputTank();

    private boolean isTransferringFluids = false;

    private final ItemStackHandler INVENTORY = createInventory(3);

    private ItemStack cachedResult = ItemStack.EMPTY;
    private int requiredFluid = 0;

    public RenderKilnBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.RENDER_KILN_BE.get(), pos, blockState);
    }

    @Override
    protected RecipeType<RenderingRecipe> getRecipeType() {
        return ModRecipes.RENDERING_TYPE.get();
    }

    public VulnerableTank getInputTank() {
        return INPUT_TANK;
    }

    // ---- Visual state (face texture) --------------------------------------------------------
    // tickHealing runs every cycle regardless of crafting state, so it doubles as the visual-state
    // refresh point. Recovering (health < maxHealth) takes priority over Running -- a damaged
    // machine signals distress even mid-cycle. Mirrors MasticatorBlockEntity/MutatorBlockEntity's
    // identical mechanism.
    //
    // Debounced: a new state must be observed for VISUAL_STATE_STABLE_CYCLES consecutive cycles
    // before the texture commits, avoiding strobing from a borderline condition flapping every cycle.
    private static final int VISUAL_STATE_STABLE_CYCLES = 2;

    @Nullable
    private RenderKilnVisualState pendingVisualState = null;
    private int pendingVisualCycles = 0;

    @Override
    protected boolean tickHealing(boolean fueled) {
        boolean healed = super.tickHealing(fueled);
        updateVisualState();
        return healed;
    }

    private void updateVisualState() {
        if (level == null) return;

        RenderKilnVisualState computed = computeVisualState();
        BlockState state = getBlockState();

        if (state.getValue(RenderKilnBlock.STATE) == computed) {
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
            level.setBlock(worldPosition, state.setValue(RenderKilnBlock.STATE, computed), Block.UPDATE_CLIENTS);
            pendingVisualState = null;
            pendingVisualCycles = 0;
        }
    }

    private RenderKilnVisualState computeVisualState() {
        if (maxHealth > 0 && health < maxHealth) return RenderKilnVisualState.RECOVERING;
        if (!isStarved() && isRecipeValid(activeRecipe) && hasCraftingInputs() && hasCraftingOutputRoom()) {
            return RenderKilnVisualState.RUNNING;
        }
        return RenderKilnVisualState.IDLE;
    }

    public FluidStack getFluid(int slot) {
        if (slot == FUEL_TANK.SLOT) return FUEL_TANK.getFluid();
        else if (slot == INPUT_TANK.SLOT) return INPUT_TANK.getFluid();
        else return FluidStack.EMPTY;
    }

    public IFluidHandler getTank(@Nullable Direction direction) {
        if (direction == Direction.UP) return FUEL_TANK;
        return INPUT_TANK;
    }

    /** See {@link IHasChannels#describeFace} -- mirrors {@link #getTank}/{@link #getItemHandler} literally. */
    @Override
    public Component describeFace(Direction face) {
        return switch (face) {
            case UP -> Component.translatable("tooltip.dermicraft.idep.face.render_kiln_fuel");
            case DOWN -> Component.translatable("tooltip.dermicraft.idep.face.render_kiln_output");
            default -> Component.translatable("tooltip.dermicraft.idep.face.render_kiln_input");
        };
    }

    // Face routing: top = fuel (fluid + its bucket slot), bottom = result slot only,
    // sides = the input fluid tank only -- no item slot on the sides at all, since there's no
    // item input (unlike the Metastasizer, whose sides also carry the pattern item).
    public IItemHandler getItemHandler(@Nullable Direction direction) {
        if (direction == null) return INVENTORY;

        int targetSlot = switch (direction) {
            case UP -> FUEL_TANK.SLOT;
            case DOWN -> OUTPUT_SLOT;
            default -> INPUT_TANK.SLOT;
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
     * Self-described channel list for the Gate multiblock -- see {@link IHasChannels}.
     * Native faces per {@link #getTank}/{@link #getItemHandler}: fuel = UP only; input =
     * everything EXCEPT UP (DOWN + all 4 sides); output = DOWN only. No item channel exists for the
     * input side (there is no item slot there at all) -- the Metastasizer's pattern channel has no
     * counterpart here.
     */
    @Override
    public List<Channel> getChannels() {
        List<Channel> channels = new ArrayList<>();

        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID, Direction.UP)) {
            channels.add(new Channel.FluidChannel("fuel", Component.literal("Fuel"), Channel.IO.IN, FUEL_TANK));
        }
        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID,
                Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            channels.add(new Channel.FluidChannel("input", Component.literal("Input"), Channel.IO.IN, INPUT_TANK));
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

    // Direct right-click helper, mirroring the same face routing as getItemHandler: bottom exposes
    // the (pull-only) result slot.
    public ItemStack extractResult() {
        return extractItemStack(INVENTORY, OUTPUT_SLOT);
    }

    @Override
    public void drops() {
        super.drops(INVENTORY);
    }

    @Override
    protected void drainOutputs(Level level) {
        // Mirrors the Metastasizer's item-slot drain cadence -- the Kiln's output is an item slot,
        // not a tank.
        if (!INVENTORY.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            ModItemUtil.pushItemToBelowNeighbour(level, worldPosition, INVENTORY, OUTPUT_SLOT);
        }
    }

    @Override
    protected boolean hasCraftingInputs() {
        return hasEnoughInput();
    }

    @Override
    protected boolean hasCraftingOutputRoom() {
        return hasOutputRoom();
    }

    @Override
    protected void onCraftComplete() {
        // Captured before draining: useFluid() fires INPUT_TANK.onContentsChanged() synchronously,
        // which re-resolves the recipe and can clear requiredFluid/cachedResult before this method
        // finishes (mirrors the same hazard documented in MetastasizerBlockEntity#onCraftComplete).
        int amount = requiredFluid;
        ItemStack output = cachedResult.copy();

        INPUT_TANK.useFluid(amount);
        INVENTORY.insertItem(OUTPUT_SLOT, output, false);
    }

    // Restores the cached result/fluid amount when a saved recipe is reloaded from NBT.
    @Override
    protected void onRecipeResolved(RecipeHolder<RenderingRecipe> recipe) {
        this.cachedResult = recipe.value().getResult();
        this.requiredFluid = recipe.value().getFluidAmount();
    }

    private void resolveRecipe() {
        Optional<RecipeHolder<RenderingRecipe>> opt = getRecipeOptional();
        if (opt.isPresent()) {
            this.activeRecipe = opt.get();
            this.maxProgress = activeRecipe.value().getCraftingTime();
            this.cachedResult = activeRecipe.value().getResult();
            this.requiredFluid = activeRecipe.value().getFluidAmount();
        } else {
            resetActiveRecipe();
        }
    }

    private Optional<RecipeHolder<RenderingRecipe>> getRecipeOptional() {
        if (level == null) return Optional.empty();

        RecipeManager recipeManager = level.getRecipeManager();
        return recipeManager.getRecipeFor(ModRecipes.RENDERING_TYPE.get(),
                new FluidOnlyRecipeInput(INPUT_TANK.getFluid()), this.level);
    }

    public void resetActiveRecipe() {
        activeRecipe = null;
        resetMaxProgress();
        resetProgress();
        cachedResult = ItemStack.EMPTY;
        requiredFluid = 0;
    }

    private boolean hasEnoughInput() {
        return INPUT_TANK.hasEnoughFluid(requiredFluid);
    }

    private boolean hasOutputRoom() {
        if (cachedResult.isEmpty()) return false;
        return INVENTORY.insertItem(OUTPUT_SLOT, cachedResult.copy(), true).isEmpty();
    }

    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.RENDER_KILN);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RenderKilnMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", INVENTORY.serializeNBT(registries));
        tag.put("input", INPUT_TANK.writeToNBT(registries, new CompoundTag()));
        tag.putInt("requiredFluid", requiredFluid);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) INVENTORY.deserializeNBT(registries, tag.getCompound("inventory"));
        if (tag.contains("input")) INPUT_TANK.readFromNBT(registries, tag.getCompound("input"));
        requiredFluid = tag.getInt("requiredFluid");
    }

    private ItemStackHandler createInventory(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                if (level == null || level.isClientSide()) return;

                if (isTransferringFluids) return;

                biDirectionalFluidTransfer(FUEL_TANK, FUEL_TANK.SLOT);
                biDirectionalFluidTransfer(INPUT_TANK, INPUT_TANK.SLOT);

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

            // Fuel/input tank slots hold exactly one container at a time, unconditionally -- see
            // MetastasizerBlockEntity's identical override for why (a whole stack of empty
            // containers could otherwise collapse to one silently).
            @Override
            public int getSlotLimit(int slot) {
                if (slot == FUEL_TANK.SLOT || slot == INPUT_TANK.SLOT) return 1;
                return super.getSlotLimit(slot);
            }
        };
    }

    private VulnerableTank createInputTank() {
        return new VulnerableTank(getTier().tankCapacity(), 1) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide()) {
                    resolveRecipe();
                    setChanged();
                }
            }
        };
    }
}
