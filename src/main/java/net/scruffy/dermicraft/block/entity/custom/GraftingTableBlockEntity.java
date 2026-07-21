package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.screen.custom.grafting_table.GraftingTableMenu;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.util.ModItemUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Grafting Table: automated crafting, resolved against VANILLA's own {@link CraftingRecipe}
 * type via a real 3x3 {@link CraftingInput} -- no Dermicraft recipe authoring, works with any
 * vanilla or other mod's crafting-table recipe for free (see the machine notes for why this is a
 * deliberate exception to the mod's own recipe types, same reasoning as the Render Furnace).
 *
 * <p>No HP mechanic ({@link net.scruffy.dermicraft.machine.MachineTier#NO_HEALTH}), same hard
 * fuel-gate philosophy as the Render Furnace: {@link #hasCraftingInputs()} requires fuel, so it
 * freezes rather than limping along unfueled.
 *
 * <p><b>Ghost/pattern model (the rework):</b> each grid slot tracks TWO separate things -- a
 * {@link #ghosts} entry (an item-type marker, no count, imprinted once and never returned to the
 * player) and its real stock (the actual {@link #INVENTORY} grid slot, freely deposited/extracted,
 * consumed per craft). Recipe resolution runs in two stages: {@link #resolveRecipe()} matches
 * against the GHOST types only (driving {@link #activeRecipe} and the output-slot ghost preview);
 * {@link #hasCraftingInputs()} separately requires the REAL stock to satisfy that same recipe
 * before any actual progress/consumption happens. Once a recipe is locked in, a slot's acceptance
 * becomes flexible -- see {@link #acceptsForSlot} -- governed by the recipe itself rather than the
 * literal imprinted item.
 *
 * <p>Unlike vanilla crafting (and the 1.21 Crafter block), this is NOT instant -- {@link CraftingRecipe}
 * carries no time field at all, so the duration is invented: {@link #BASE_TICKS} plus
 * {@link #PER_INGREDIENT_TICKS} per ghosted slot, scaled by fuel speed like every other machine's
 * recipe ticks. A tuning knob, not a fact.
 */
public class GraftingTableBlockEntity extends AbstractFueledMachineBlockEntity<CraftingRecipe>
        implements MenuProvider, IHaveInventory, IHasChannels {

    public static final int GRID_START = 1;
    public static final int GRID_SIZE = 9;
    public static final int GRID_WIDTH = 3;
    public static final int GRID_HEIGHT = 3;
    public static final int OUTPUT_SLOT = GRID_START + GRID_SIZE; // 10

    private static final int BASE_TICKS = 40;
    private static final int PER_INGREDIENT_TICKS = 20;

    private final ItemStackHandler INVENTORY = createInventory(GRID_START + GRID_SIZE + 1);

    // The permanent per-slot pattern markers -- count is meaningless here (always normalized to 1
    // on write), only the item/components identity matters. Separate from INVENTORY's real stock.
    private final NonNullList<ItemStack> ghosts = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);

    // Automation round-robins across every ghosted, currently-accepting slot rather than always
    // filling the first match -- this cursor is where the next scan starts. Not NBT-persisted:
    // losing the exact rotation on reload is harmless, it just restarts the round from slot 0.
    private int roundRobinCursor = 0;

    private ItemStack cachedResult = ItemStack.EMPTY;

    public GraftingTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.GRAFTING_TABLE_BE.get(), pos, blockState);
    }

    @Override
    protected RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeType.CRAFTING;
    }

    public FluidStack getFluid(int slot) {
        return slot == FUEL_TANK.SLOT ? FUEL_TANK.getFluid() : FluidStack.EMPTY;
    }

    // Only one tank exists here (no reagent tank), so it's returned regardless of face.
    public IFluidHandler getTank(@Nullable Direction direction) {
        return FUEL_TANK;
    }

    /** See {@link IHasChannels#describeFace} -- mirrors {@link #getItemHandler} literally. */
    @Override
    public Component describeFace(Direction face) {
        return switch (face) {
            case UP -> Component.translatable("tooltip.dermicraft.idep.face.grafting_table_fuel");
            case DOWN -> Component.translatable("tooltip.dermicraft.idep.face.grafting_table_output");
            default -> Component.translatable("tooltip.dermicraft.idep.face.grafting_table_grid");
        };
    }

    // Face routing: top = fuel, bottom = output (extract only), every other face = the flat 9-slot
    // grid (round-robin routed, see gridHandler()).
    public IItemHandler getItemHandler(@Nullable Direction direction) {
        if (direction == null) return INVENTORY;
        if (direction == Direction.UP) return singleSlotHandler(FUEL_TANK.SLOT, true);
        if (direction == Direction.DOWN) return singleSlotHandler(OUTPUT_SLOT, false);
        return gridHandler();
    }

    // Automation ONLY tops up slots that already have a ghost -- it never creates one (that's a
    // player-only, hand-click action). Every logical index in this handler routes through the same
    // round-robin scan regardless of which one a caller targets, so hopper-style "try slot 0, then
    // 1, then 2..." callers all converge on the same smart routing.
    private IItemHandler gridHandler() {
        return new IItemHandlerModifiable() {
            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                INVENTORY.setStackInSlot(GRID_START + slot, stack);
            }

            @Override
            public int getSlots() {
                return GRID_SIZE;
            }

            @NotNull
            @Override
            public ItemStack getStackInSlot(int slot) {
                return INVENTORY.getStackInSlot(GRID_START + slot);
            }

            @NotNull
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return insertRoundRobin(stack, simulate);
            }

            @NotNull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return INVENTORY.extractItem(GRID_START + slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return INVENTORY.getSlotLimit(GRID_START + slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return !ghosts.get(slot).isEmpty() && acceptsForSlot(slot, stack);
            }
        };
    }

    private ItemStack insertRoundRobin(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return stack;
        for (int attempt = 0; attempt < GRID_SIZE; attempt++) {
            int i = (roundRobinCursor + attempt) % GRID_SIZE;
            if (ghosts.get(i).isEmpty()) continue;
            if (!acceptsForSlot(i, stack)) continue;

            ItemStack remainder = INVENTORY.insertItem(GRID_START + i, stack, simulate);
            if (remainder.getCount() != stack.getCount()) {
                if (!simulate) roundRobinCursor = (i + 1) % GRID_SIZE;
                return remainder;
            }
        }
        return stack;
    }

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

    @Override
    public List<Channel> getChannels() {
        List<Channel> channels = new ArrayList<>();

        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID, Direction.UP)) {
            channels.add(new Channel.FluidChannel("fuel", Component.literal("Fuel"), Channel.IO.IN, FUEL_TANK));
        }
        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.ITEM,
                Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            channels.add(new Channel.ItemChannel("grid", Component.literal("Grid"), Channel.IO.IN, gridHandler()));
        }
        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.ITEM, Direction.DOWN)) {
            channels.add(new Channel.ItemChannel("output", Component.literal("Output"), Channel.IO.OUT, singleSlotHandler(OUTPUT_SLOT, false)));
        }

        return channels;
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

    // ---- Ghost/pattern access (used by the Menu's click handling and the Screen's rendering) -----

    public ItemStack getGhost(int gridIndex) {
        return ghosts.get(gridIndex);
    }

    public ItemStack getRealStock(int gridIndex) {
        return INVENTORY.getStackInSlot(GRID_START + gridIndex);
    }

    /** Player-only: imprints a slot's pattern from the given item (count normalized to 1). Does
     * NOT touch the caller's stack -- the Menu's click override is responsible for never letting
     * the cursor stack itself be consumed by this call. */
    public void setGhost(int gridIndex, ItemStack fromStack) {
        ghosts.set(gridIndex, fromStack.copyWithCount(1));
        resolveRecipe();
        setChanged();
        updateBlock();
    }

    /** Only valid once real stock is already at 0 -- the Menu enforces this before calling. */
    public void clearGhost(int gridIndex) {
        ghosts.set(gridIndex, ItemStack.EMPTY);
        resolveRecipe();
        setChanged();
        updateBlock();
    }

    /** The ghost preview to show in the output slot -- only meaningful while the real output slot
     * is empty (a real result sitting there takes visual priority; see the Screen). */
    public ItemStack getGhostResult() {
        return activeRecipe == null ? ItemStack.EMPTY : cachedResult;
    }

    /**
     * Whether {@code stack} may fill (or substitute into) a ghosted slot. Exact match to the
     * imprinted item always works. Once a recipe is locked in from the ghost pattern, also accepts
     * any substitute whose swap-in still resolves to that SAME recipe -- this reuses the recipe's
     * own {@code matches()} as the source of truth for "is this a valid substitute here" (e.g. a
     * tag-based "any planks" ingredient), rather than trying to hand-extract a per-position
     * Ingredient from a matched Shaped/ShapelessRecipe, which vanilla doesn't expose uniformly
     * (shaped patterns can match at multiple grid offsets; shapeless ingredients aren't positional
     * at all).
     */
    private boolean acceptsForSlot(int gridIndex, ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemStack ghost = ghosts.get(gridIndex);
        if (ghost.isEmpty()) return false;

        if (ItemStack.isSameItemSameComponents(ghost, stack.copyWithCount(1))) return true;
        if (activeRecipe == null || level == null) return false;

        List<ItemStack> trial = new ArrayList<>(GRID_SIZE);
        for (int j = 0; j < GRID_SIZE; j++) {
            trial.add(j == gridIndex ? stack.copyWithCount(1) : ghosts.get(j));
        }
        return activeRecipe.value().matches(CraftingInput.of(GRID_WIDTH, GRID_HEIGHT, trial), level);
    }

    // No HP mechanic (NO_HEALTH tier) -- requiring fuel here, same as the Render Furnace, is what
    // makes this STOP when fuel runs out instead of limping along at the standard 0.1x unfueled
    // trickle. Progress freezes (not reset) until fuel returns. Additionally requires the REAL
    // stock (not just the ghost pattern) to satisfy the locked recipe -- resolveRecipe() only ever
    // matches against ghost types, so this is the second stage that gates actual crafting.
    @Override
    protected boolean hasCraftingInputs() {
        return activeRecipe != null
                && FUEL_TANK.hasEnoughFuel(fuelUseRate)
                && level != null
                && activeRecipe.value().matches(realInput(), level);
    }

    @Override
    protected boolean hasCraftingOutputRoom() {
        if (cachedResult.isEmpty()) return false;
        return INVENTORY.insertItem(OUTPUT_SLOT, cachedResult.copy(), true).isEmpty();
    }

    @Override
    protected void onCraftComplete() {
        CraftingInput realInput = realInput();
        ItemStack output = activeRecipe.value().assemble(realInput, registryAccessOrNull());
        NonNullList<ItemStack> remainder = activeRecipe.value().getRemainingItems(realInput);

        // Shrink each real-stock slot by 1, then merge back whatever the recipe says should remain
        // (normally nothing; a returned container for bucket-style recipes etc.) -- NOT a blind
        // overwrite. Overwriting the slot with just the remainder would silently delete any extra
        // stacked stock beyond the 1 actually consumed this craft.
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack real = INVENTORY.getStackInSlot(GRID_START + i).copy();
            if (!real.isEmpty()) real.shrink(1);

            ItemStack returned = i < remainder.size() ? remainder.get(i) : ItemStack.EMPTY;
            if (!returned.isEmpty()) {
                if (real.isEmpty()) {
                    real = returned.copy();
                } else if (ItemStack.isSameItemSameComponents(real, returned)) {
                    real.grow(returned.getCount());
                } else {
                    // Different item returned than what's stacked (rare) -- push it to the output
                    // buffer rather than silently discarding it.
                    INVENTORY.insertItem(OUTPUT_SLOT, returned, false);
                }
            }
            INVENTORY.setStackInSlot(GRID_START + i, real);
        }

        INVENTORY.insertItem(OUTPUT_SLOT, output, false);
    }

    @Override
    protected void onRecipeResolved(RecipeHolder<CraftingRecipe> recipe) {
        this.cachedResult = recipe.value().assemble(ghostInput(), registryAccessOrNull());
    }

    // ---- Recipe resolution (GHOST-based only -- see hasCraftingInputs() for the real-stock stage) --

    private void resolveRecipe() {
        CraftingInput input = ghostInput();
        if (input.isEmpty()) {
            resetActiveRecipe();
            return;
        }

        Optional<RecipeHolder<CraftingRecipe>> opt = getRecipeOptional(input);
        if (opt.isPresent()) {
            this.activeRecipe = opt.get();
            this.maxProgress = BASE_TICKS + PER_INGREDIENT_TICKS * countGhosted();
            this.cachedResult = activeRecipe.value().assemble(input, registryAccessOrNull());
        } else {
            resetActiveRecipe();
        }
    }

    private Optional<RecipeHolder<CraftingRecipe>> getRecipeOptional(CraftingInput input) {
        if (level == null) return Optional.empty();

        RecipeManager recipeManager = level.getRecipeManager();
        return recipeManager.getRecipeFor(RecipeType.CRAFTING, input, level);
    }

    /** Synthetic grid built from the ghost TYPES (count 1 each), ignoring real stock entirely --
     * this is what determines the target recipe (and its output-slot ghost preview). */
    private CraftingInput ghostInput() {
        return CraftingInput.of(GRID_WIDTH, GRID_HEIGHT, new ArrayList<>(ghosts));
    }

    /** The real grid as it actually sits (real stock only) -- what actually gates crafting
     * ({@link #hasCraftingInputs()}) and what {@link #onCraftComplete()} consumes from. */
    private CraftingInput realInput() {
        List<ItemStack> grid = new ArrayList<>(GRID_SIZE);
        for (int i = 0; i < GRID_SIZE; i++) {
            grid.add(INVENTORY.getStackInSlot(GRID_START + i));
        }
        return CraftingInput.of(GRID_WIDTH, GRID_HEIGHT, grid);
    }

    private int countGhosted() {
        int count = 0;
        for (ItemStack ghost : ghosts) {
            if (!ghost.isEmpty()) count++;
        }
        return count;
    }

    @Nullable
    private HolderLookup.Provider registryAccessOrNull() {
        return level == null ? null : level.registryAccess();
    }

    public void resetActiveRecipe() {
        activeRecipe = null;
        resetMaxProgress();
        resetProgress();
        cachedResult = ItemStack.EMPTY;
    }

    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.GRAFTING_TABLE);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GraftingTableMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", INVENTORY.serializeNBT(registries));
        CompoundTag ghostTag = new CompoundTag();
        ContainerHelper.saveAllItems(ghostTag, ghosts, registries);
        tag.put("ghosts", ghostTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) INVENTORY.deserializeNBT(registries, tag.getCompound("inventory"));
        if (tag.contains("ghosts")) ContainerHelper.loadAllItems(tag.getCompound("ghosts"), ghosts, registries);
    }

    private ItemStackHandler createInventory(int size) {
        return new ItemStackHandler(size) {
            private boolean isTransferringFluids = false;

            @Override
            protected void onContentsChanged(int slot) {
                if (level == null || level.isClientSide()) return;

                if (isTransferringFluids) return;

                biDirectionalFluidTransfer(FUEL_TANK, FUEL_TANK.SLOT);

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

            // Fuel slot holds exactly one container at a time, unconditionally -- see the mod-wide
            // tank-slot fix note in MutatorBlockEntity. Grid slots are gated by the ghost/acceptance
            // rule (see gridHandler().isItemValid) rather than a stack-count limit; output keeps
            // the default limit (matches every other machine's "output slot has no manual-insert
            // restriction" pre-existing behavior).
            @Override
            public int getSlotLimit(int slot) {
                if (slot == FUEL_TANK.SLOT) return 1;
                return super.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot >= GRID_START && slot < GRID_START + GRID_SIZE) {
                    return acceptsForSlot(slot - GRID_START, stack);
                }
                return super.isItemValid(slot, stack);
            }
        };
    }
}
