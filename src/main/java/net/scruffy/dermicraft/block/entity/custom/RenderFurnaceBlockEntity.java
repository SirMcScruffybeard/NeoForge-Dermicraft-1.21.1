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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.RenderFurnaceBlock;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IEvolvingMachine;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.interfaces.IHaveModules;
import net.scruffy.dermicraft.interfaces.IModuleTabMachine;
import net.scruffy.dermicraft.machine.MachineStatKey;
import net.scruffy.dermicraft.screen.custom.render_furnace.RenderFurnaceMenu;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.util.ModItemUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Render Furnace: automated smelting, resolved against VANILLA's own {@link SmeltingRecipe}
 * type -- no Dermicraft recipe authoring, works with any mod's smelting recipes for free (see the
 * machine notes for why this is the deliberate exception to the mod's own recipe types).
 *
 * <p>It has the standard HP mechanic like every other fueled machine, so an unfueled furnace limps along at the usual
 * 0.1x trickle and takes hunger damage rather than freezing. Its only tank is the fuel tank, so it processes no
 * hazardous fluid and the hazard effects never apply to it; its Module tab still takes Safety Modules, but they have
 * nothing to protect against here.
 */
public class RenderFurnaceBlockEntity extends AbstractFueledMachineBlockEntity<SmeltingRecipe>
        implements MenuProvider, IHaveInventory, IHasChannels, IEvolvingMachine, IModuleTabMachine {

    public static final int INPUT_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int INVENTORY_SIZE = 3;

    private boolean isTransferringFluids = false;

    private final ItemStackHandler INVENTORY = createInventory(INVENTORY_SIZE);

    // General Module slot(s) -- own dedicated handler (see MachineBaseBlockEntity#createModuleInventory), not part of
    // INVENTORY above. Evolution Modules go in the dedicated Evolution slot (EVOLUTION_INVENTORY, in the engine), never here.
    public final ItemStackHandler MODULE_INVENTORY = createModuleInventory(getModuleSlotCount(),
            stack -> stack.is(ModTags.Items.MODULES) && !isEvolutionModule(stack));

    private ItemStack cachedResult = ItemStack.EMPTY;

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

    public RenderFurnaceBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.RENDER_FURNACE_BE.get(), pos, blockState);
    }

    // ---- Evolution ----------------------------------------------------------------------------
    // The Render Furnace evolves in place through its evolution state and the Evolution slot. The gradual-progress
    // machinery, the Evolution slot, the flourish and the stat defaults (tank capacity, HP, speed, fuel use, Module
    // slots) all live in AbstractFueledMachineBlockEntity; this class opts in and resizes what it owns.

    @Override
    protected boolean usesEvolutionState() {
        return true;
    }

    @Override
    protected MachineStatKey getStatKey() {
        return MachineStatKey.RENDER_FURNACE;
    }

    @Override
    protected void onEvolutionStateChanged() {
        super.onEvolutionStateChanged();
        resizeModuleInventory(MODULE_INVENTORY, getModuleSlotCount());
        applyCapacityBonus();
    }

    @Override
    protected RecipeType<SmeltingRecipe> getRecipeType() {
        return RecipeType.SMELTING;
    }

    public FluidStack getFluid(int slot) {
        return slot == FUEL_TANK.SLOT ? FUEL_TANK.getFluid() : FluidStack.EMPTY;
    }

    // Only one tank exists here (no reagent tank), so it's returned regardless of face.
    /** See {@link IHasChannels#describeFluidFace} -- mirrors {@link #getTank} literally, which hands
     * out the fuel tank on EVERY face here (unlike describeFace, which names the item slots). */
    @Override
    public Component describeFluidFace(Direction face) {
        return Component.translatable("tooltip.dermicraft.tank.fuel");
    }

    public IFluidHandler getTank(@Nullable Direction direction) {
        return FUEL_TANK;
    }

    /** See {@link IHasChannels#describeFace} -- mirrors {@link #getItemHandler} literally. */
    @Override
    public Component describeFace(Direction face) {
        return switch (face) {
            case UP -> Component.translatable("tooltip.dermicraft.idep.face.render_furnace_fuel");
            case DOWN -> Component.translatable("tooltip.dermicraft.idep.face.render_furnace_output");
            default -> Component.translatable("tooltip.dermicraft.idep.face.render_furnace_input");
        };
    }

    // Face routing: top = fuel (fluid + its bucket slot), bottom = result slot only, sides = input.
    // MODULE is never exposed on any face -- GUI-only, same as every other machine's Module slot.
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

    @Override
    public List<Channel> getChannels() {
        List<Channel> channels = new ArrayList<>();

        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID, Direction.UP)) {
            channels.add(new Channel.FluidChannel("fuel", Component.literal("Fuel"), Channel.IO.IN, FUEL_TANK));
        }
        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.ITEM,
                Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            // Input and output share the "everything but the top" native face set (no dedicated
            // input-only side the way the Mutator has, since there's no reagent tank contending for
            // the sides here) -- listed as two channels regardless, so the Gate can service either
            // independently once a genuine face conflict exists.
            channels.add(new Channel.ItemChannel("input", Component.literal("Input"), Channel.IO.IN, singleSlotHandler(INPUT_SLOT, true)));
            channels.add(new Channel.ItemChannel("output", Component.literal("Output"), Channel.IO.OUT, singleSlotHandler(OUTPUT_SLOT, false)));
        }

        return channels;
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
    protected void drainOutputs(Level level) {
        if (autoDrainEnabled && !INVENTORY.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            ModItemUtil.pushItemToBelowNeighbour(level, worldPosition, INVENTORY, OUTPUT_SLOT);
        }
    }

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
        super.drops(MODULE_INVENTORY);
        super.drops(EVOLUTION_INVENTORY);
    }

    // Standard HP mechanic now, so an unfueled furnace takes the base engine's usual 0.1x-trickle-plus-hunger-damage
    // path instead of freezing. Frozen during the evolution flourish so the new state never lands mid-craft.
    @Override
    protected boolean hasCraftingInputs() {
        return !isEvolutionFlourishing() && hasInput();
    }

    @Override
    protected boolean hasCraftingOutputRoom() {
        return hasOutputRoom();
    }

    @Override
    protected void onCraftComplete() {
        ItemStack output = cachedResult.copy();

        INVENTORY.extractItem(INPUT_SLOT, 1, false);
        INVENTORY.insertItem(OUTPUT_SLOT, output, false);
    }

    @Override
    protected void onRecipeResolved(RecipeHolder<SmeltingRecipe> recipe) {
        this.cachedResult = recipe.value().assemble(currentInput(), registryAccessOrNull());
    }

    // ---- Modules ----------------------------------------------------------------------------------

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
        FUEL_TANK.setCapacity(getBaseTankCapacity() + getCapacityBonus());
    }

    @Override
    protected boolean canRemoveModule(int slot) {
        int thisBonus = capacityModuleBonus(MODULE_INVENTORY.getStackInSlot(slot));
        if (thisBonus == 0) return true;
        int newCapacity = getBaseTankCapacity() + (getCapacityBonus() - thisBonus);
        return FUEL_TANK.getFluid().getAmount() <= newCapacity;
    }

    /** How far the pending evolution is, 0 to 1 (0 with no live Evolution Module). Read by
     * {@code EvolutionOverlayBlockEntityRenderer}'s creeping overlay. */
    @Override
    public float getEvolutionProgressFraction() {
        return getGradualEvolutionFraction();
    }

    // ---- Recipe resolution -------------------------------------------------------------------

    private void resolveRecipe() {
        Optional<RecipeHolder<SmeltingRecipe>> opt = getRecipeOptional();
        if (opt.isPresent()) {
            this.activeRecipe = opt.get();
            this.maxProgress = activeRecipe.value().getCookingTime(); // vanilla's own duration -- no invented number needed
            this.cachedResult = activeRecipe.value().assemble(currentInput(), registryAccessOrNull());
        } else {
            resetActiveRecipe();
        }
    }

    private Optional<RecipeHolder<SmeltingRecipe>> getRecipeOptional() {
        if (level == null) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, currentInput(), level);
    }

    private SingleRecipeInput currentInput() {
        return new SingleRecipeInput(INVENTORY.getStackInSlot(INPUT_SLOT));
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

    private boolean hasInput() {
        return !INVENTORY.getStackInSlot(INPUT_SLOT).isEmpty();
    }

    private boolean hasOutputRoom() {
        if (cachedResult.isEmpty()) return false;
        return INVENTORY.insertItem(OUTPUT_SLOT, cachedResult.copy(), true).isEmpty();
    }

    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.RENDER_FURNACE);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RenderFurnaceMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", INVENTORY.serializeNBT(registries));
        tag.put("module_inventory", MODULE_INVENTORY.serializeNBT(registries));
        tag.putBoolean("module_tab_active", moduleTabActive);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Worlds saved at an older size have a smaller Size -- see
        // MachineBaseBlockEntity#loadItemHandler for why a plain deserializeNBT would shrink
        // INVENTORY back down and crash the menu (slot out of range).
        if (tag.contains("inventory")) loadItemHandler(INVENTORY, INVENTORY_SIZE, registries, tag.getCompound("inventory"));
        if (tag.contains("module_inventory")) loadItemHandler(MODULE_INVENTORY, getModuleSlotCount(), registries, tag.getCompound("module_inventory"));
        applyCapacityBonus();
        moduleTabActive = tag.getBoolean("module_tab_active");
    }

    private ItemStackHandler createInventory(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                if (level == null || level.isClientSide()) return;

                if (isTransferringFluids) return;

                if (slot == INPUT_SLOT) resolveRecipe();

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
            // tank-slot fix note in MutatorBlockEntity for why this must not be conditioned on the
            // slot's current contents. INPUT_SLOT/OUTPUT_SLOT keep the default stack limit.
            @Override
            public int getSlotLimit(int slot) {
                if (slot == FUEL_TANK.SLOT) return 1;
                return super.getSlotLimit(slot);
            }
        };
    }
}
