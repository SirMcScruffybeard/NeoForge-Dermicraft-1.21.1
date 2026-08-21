package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.interfaces.IVagueRecipe;
import net.scruffy.dermicraft.tank.DroolingTank;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared base for the Drooling machine family (Cauldron, Crucible, and any future member with the
 * same shape: one ingredient/output item pair, one self-produced tank, passive generation plus an
 * optional food-boosted mode). Pulled out of what used to be {@code DroolingCauldronBlockEntity}
 * alone (dermicraft-machine-notes.md, Drooling Cauldron entry -> "Evolution Module family") --
 * everything here was already fluid-agnostic in that original class except two lines (the passive
 * fill's hardcoded {@code Fluids.WATER}/rate and the tank's fixed fluid filter), which are now the
 * hooks below instead.
 *
 * <p><b>{@code R} is the machine's own food-boost recipe type</b> (e.g. {@code VagueDroolingRecipe}
 * for Cauldron, {@code VagueDroolingCrucibleRecipe} for Crucible) -- deliberately a SEPARATE
 * {@link RecipeType} per machine, not one shared type both look up against. Two different recipes
 * matching the identical ingredient under one shared {@code RecipeType} would be ambiguous to
 * {@code RecipeManager#getRecipeFor} (it has no notion of "which block is asking"), so Wheat ->
 * Water and Wheat -> Lava can't coexist in the same type even though the ingredient list is
 * otherwise a straight copy. See {@code IVagueRecipe} for the shared formula both recipe classes
 * implement.
 *
 * <p><b>NBT tag keys are fixed here, not per-subclass</b> -- deliberately, so a future evolution
 * transform (Cauldron block swapping to Crucible) can read this class's own state and write it
 * straight into the new instance without a subclass-specific key mapping layer.
 */
public abstract class DroolingMachineBlockEntity<R extends Recipe<SingleRecipeInput> & IVagueRecipe>
        extends MachineBaseBlockEntity implements IHaveInventory, IHasChannels {

    public static final int INPUT = 0;
    public static final int OUTPUT = 1;
    public static final int MODULE = 2;
    /** Single source of truth for the handler's slot count -- also re-asserted after
     * {@code deserializeNBT}, see {@link #loadAdditional} for why that's load-bearing (a Cauldron/
     * Crucible saved before this slot existed carries Size=2). */
    public static final int INVENTORY_SIZE = 3;

    public final ItemStackHandler INVENTORY = createInventory();
    protected final DroolingTank TANK;

    private RecipeHolder<R> activeRecipe = null;
    private Item activeItem = Items.AIR;
    private int resultAmount = 0;

    @Nullable
    private ResourceLocation pendingRecipeId = null;

    // Which screen tab was last open -- same pattern as SkinTankBlockEntity's own
    // isModuleTabActive/setModuleTabActive, so reopening the screen returns to the tab last viewed.
    private boolean moduleTabActive = false;

    public boolean isModuleTabActive() {
        return moduleTabActive;
    }

    public void setModuleTabActive(boolean active) {
        this.moduleTabActive = active;
    }

    protected DroolingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        // currentTargetFluid() is a plain method reference here (lazy -- not invoked until the tank
        // actually fills/drains), so this is safe despite running before any subclass field
        // initializer has -- see the identical note on tankCapacity() below for the one hook that
        // genuinely IS called this early and therefore must stay a constant.
        this.TANK = createDroolingTank(tankCapacity(), -1, this::currentTargetFluid);
    }

    ////////////////////Hooks for subclasses\\\\\\\\\\\\\\\\\\\\

    /** Which fluid passive generation currently produces, and the only fluid {@link #TANK} will
     * accept a new fill of right now. Called fresh every time it's needed (never cached), so a
     * machine whose target can change post-construction (an evolving Cauldron, eventually) just
     * needs this to reflect current state -- no other hook or tank swap required. */
    protected abstract Fluid currentTargetFluid();

    /** mB added per passive-generation cycle (see {@link #tick}: gated on a 1-second timer, same
     * cadence the original Cauldron always used -- called once per fire of that gate, not once per
     * raw game tick). A hook rather than a constant specifically so a machine mid-evolution can
     * return a reduced rate and switch to full rate once evolution completes, per
     * dermicraft-machine-notes.md's Evolution Module design. */
    protected abstract int passiveYieldAmount();

    /** {@link #TANK}'s capacity. Called from the constructor, before any subclass field is
     * initialized -- MUST be a literal/constant expression, not something reading subclass state. */
    protected abstract int tankCapacity();

    /** This machine's own food-boost recipe type -- see the class javadoc for why this can't be
     * shared across Drooling machines. */
    protected abstract RecipeType<R> recipeType();

    ////////////////////Shared machinery, unchanged from the original Cauldron-only version\\\\\\\\\\\\\\\\\\\\

    @Override
    public boolean hasTank() {
        return true;
    }

    public FluidStack getFluid() {
        return TANK.getFluid();
    }

    @Override
    public Component describeFace(Direction face) {
        return Component.translatable("tooltip.dermicraft.idep.face.drooling_ingredient");
    }

    @Override
    public Component describeFluidFace(Direction face) {
        return Component.translatable("tooltip.dermicraft.tank.result");
    }

    public IFluidHandler getTank(@Nullable Direction face) {
        return TANK;
    }

    public IItemHandler getItemHandler(@Nullable Direction face) {
        if (face == null) return INVENTORY;
        return ingredientChannelHandler();
    }

    @Override
    public List<Channel> getChannels() {
        List<Channel> channels = new ArrayList<>();

        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.ITEM, Direction.values())) {
            channels.add(new Channel.ItemChannel("ingredient", Component.literal("Ingredient"), Channel.IO.IN, ingredientChannelHandler()));
        }
        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID, Direction.values())) {
            channels.add(new Channel.FluidChannel("result", Component.literal("Result"), Channel.IO.OUT, TANK));
        }

        return channels;
    }

    private IItemHandler ingredientChannelHandler() {
        return new IItemHandlerModifiable() {
            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                INVENTORY.setStackInSlot(INPUT, stack);
            }

            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return INVENTORY.getStackInSlot(INPUT);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return INVENTORY.insertItem(INPUT, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return INVENTORY.extractItem(INPUT, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return INVENTORY.getSlotLimit(INPUT);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return INVENTORY.isItemValid(INPUT, stack);
            }
        };
    }

    @Override
    public void drops() {
        dropItems(level, INVENTORY, worldPosition);
    }

    public ItemStack insertItemStack(ItemStack stack) {
        return insertItemStack(INVENTORY, INPUT, stack);
    }

    public ItemStack extractItemStack() {
        return extractItem(INVENTORY, INPUT, Integer.MAX_VALUE);
    }

    public void tick(Level level) {
        if (level.isClientSide) return;

        resolvePendingRecipe(level);

        //////////Every Second (20 ticks)\\\\\\\\\\
        if (ModMath.Time.hasTicksPassed(level, ModMath.Time.getSecondsToTicks(1))) {
            TANK.safeFill(new FluidStack(currentTargetFluid(), passiveYieldAmount()));
            setChanged();
            updateBlock();
        }

        //////////Craft\\\\\\\\\\
        if (ModMath.Time.hasTicksPassed(level, CRAFT_TICKS)) {
            if (!isRecipeValid(activeRecipe)) {
                if (progress > 0) {
                    resetProgress();
                }
                return;
            }

            if (isMaxProgressValid() && hasIngredients() && TANK.hasRoom(resultAmount)) {
                if (isStillCrafting()) {
                    incrementProgress();
                    setChanged();

                } else {
                    TANK.fill(activeRecipe.value().getResultFluidStack(resultAmount), IFluidHandler.FluidAction.EXECUTE);
                    INVENTORY.extractItem(INPUT, 1, false);
                    resetProgress();
                }
            } else {
                resetProgress();
            }
        }
    }

    private boolean hasIngredients() {
        return !INVENTORY.getStackInSlot(INPUT).isEmpty();
    }

    private void resolvePendingRecipe(Level level) {
        if (pendingRecipeId == null) return;

        level.getRecipeManager().byKey(pendingRecipeId).ifPresent(recipeHolder -> {
            // Type-token equality instead of instanceof R -- generics erase at runtime, so this is
            // the only way to check "is this actually one of MY recipes" rather than the sibling
            // machine's identically-shaped one.
            if (recipeHolder.value().getType() == recipeType()) {
                @SuppressWarnings("unchecked")
                RecipeHolder<R> typed = (RecipeHolder<R>) recipeHolder;
                this.activeRecipe = typed;
            }
        });
        pendingRecipeId = null;
    }

    private void setActiveRecipe() {
        if (level == null) {
            activeRecipe = null;
            return;
        }

        Optional<RecipeHolder<R>> opt = level.getRecipeManager()
                .getRecipeFor(recipeType(), new SingleRecipeInput(INVENTORY.getStackInSlot(INPUT)), level);

        opt.ifPresent(holder -> activeRecipe = holder);
    }

    public void setResultAmount() {
        resultAmount = activeRecipe.value().getCraftingAmount(INVENTORY.getStackInSlot(INPUT));
    }

    private void resetActiveRecipe() {
        activeRecipe = null;
    }

    private void resetActiveItem() {
        activeItem = Items.AIR;
    }

    private void resetResultAmount() {
        resultAmount = 0;
    }

    private void setMaxProgress() {
        maxProgress = activeRecipe.value().getCraftingTime(INVENTORY.getStackInSlot(INPUT));
    }

    private void incrementProgress() {
        progress += CRAFT_TICKS;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("drooling_items", INVENTORY.serializeNBT(registries));
        tag.put("drooling_tank", TANK.writeToNBT(registries, new CompoundTag()));
        tag.putInt("drooling_progress", progress);
        tag.putInt("drooling_max", maxProgress);
        tag.putInt("drooling_result_amount", resultAmount);
        if (isRecipeValid(activeRecipe)) tag.putString("drooling_saved_recipe", activeRecipe.id().toString());
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(this.activeItem);
        tag.putString("drooling_active_item", itemKey.toString());
        tag.putBoolean("drooling_module_tab_active", moduleTabActive);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // NOT a plain INVENTORY.deserializeNBT -- a Cauldron/Crucible saved before the Module slot
        // existed carries Size=2 and would shrink this handler back down, crashing on world load
        // when the menu adds its Module slot at index 2 (same bug MachineBaseBlockEntity#loadItemHandler
        // was generalized to fix, see SkinTankBlockEntity's own identical note).
        if (tag.contains("drooling_items")) loadItemHandler(INVENTORY, INVENTORY_SIZE, registries, tag.getCompound("drooling_items"));
        if (tag.contains("drooling_tank")) TANK.readFromNBT(registries, tag.getCompound("drooling_tank"));
        this.progress = tag.getInt("drooling_progress");
        this.maxProgress = tag.getInt("drooling_max");
        resultAmount = tag.getInt("drooling_result_amount");
        moduleTabActive = tag.getBoolean("drooling_module_tab_active");

        if (tag.contains("drooling_saved_recipe", CompoundTag.TAG_STRING)) {
            pendingRecipeId = ResourceLocation.parse(tag.getString("drooling_saved_recipe"));
        }
    }

    private ItemStackHandler createInventory() {
        return new ItemStackHandler(INVENTORY_SIZE) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                // Same tag every gadget's Module slot filters to -- a machine's Module slot is the
                // same "scarce, tag-identified" convention, not a bespoke allowlist. INPUT/OUTPUT
                // stay unrestricted, matching this class's own existing behavior.
                return slot != MODULE || stack.is(ModTags.Items.MODULES);
            }

            @Override
            protected void onContentsChanged(int slot) {
                if (level != null && !level.isClientSide()) {

                    if (slot == INPUT) {
                        ItemStack stack = getStackInSlot(INPUT);
                        Item currentItem = stack.getItem();

                        if (stack.isEmpty()) {
                            resetProgress();
                            resetMaxProgress();
                            resetActiveItem();
                            resetActiveRecipe();
                            resetResultAmount();
                        }

                        if (currentItem != activeItem) {
                            activeItem = currentItem;
                            resetProgress();
                            resetMaxProgress();

                            setActiveRecipe();

                            if (isRecipeValid(activeRecipe)) {
                                setMaxProgress();
                                setResultAmount();
                            } else {
                                resetActiveRecipe();
                                resetProgress();
                                resetMaxProgress();
                                resetResultAmount();
                            }
                        }
                    }

                    if (slot == OUTPUT) {
                        if (TANK.hasEmptyFluidHandlerInSlot(INVENTORY, OUTPUT)) {
                            TANK.transferFluidFromTankToHandler(INVENTORY, OUTPUT);
                        }
                    }

                    setChanged();
                    updateBlock();
                }
            }

            @Override
            public int getSlotLimit(int slot) {
                return (slot == OUTPUT || slot == MODULE) ? 1 : super.getSlotLimit(slot);
            }
        };
    }
}
