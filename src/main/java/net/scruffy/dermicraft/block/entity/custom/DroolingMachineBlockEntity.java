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
import net.scruffy.dermicraft.block.custom.DroolingMachineBlock;
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
        extends MachineBaseBlockEntity implements IHaveInventory, IHasChannels, net.scruffy.dermicraft.interfaces.IHaveModules {

    public static final int INPUT = 0;
    public static final int OUTPUT = 1;
    /** Single source of truth for the handler's slot count -- also re-asserted after
     * {@code deserializeNBT}, see {@link #loadAdditional} for why that's load-bearing (a Cauldron/
     * Crucible saved before OUTPUT existed carries Size=1). Module no longer counts toward this --
     * see {@link #MODULE_INVENTORY}, its own dedicated handler. */
    public static final int INVENTORY_SIZE = 2;

    public final ItemStackHandler INVENTORY = createInventory();

    /** Dedicated Module-only handler, not part of INVENTORY above -- see
     * {@code MachineBaseBlockEntity#createModuleInventory}. */
    public final ItemStackHandler MODULE_INVENTORY = createModuleInventory(moduleSlotCount());

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

    /** Called first thing every server tick, before anything else in {@link #tick} touches this
     * instance. Return {@code true} to skip the rest of this tick entirely -- for a subclass that
     * just replaced itself with a different block (Drooling Cauldron completing its evolution into
     * Drooling Crucible), so no further code runs against a now-stale block entity. No-op by
     * default (returns {@code false}): most Drooling machines never replace themselves. */
    protected boolean onTickStart(Level level) {
        return false;
    }

    /** Called after every passive-fill attempt (the once-a-second cycle), with how much actually
     * got added -- 0 if the tank had no room, or if the offered fluid didn't match what's already
     * in there (e.g. mid-transition: Module installed targeting a new fluid, but old contents
     * haven't fully drained yet, so nothing new can go in until they do). No-op by default; Drooling
     * Cauldron overrides this to advance evolution progress only on cycles where real production
     * actually happened, per direction -- not merely because a Module is installed. */
    protected void onPassiveFillResult(int filledAmount) {
    }

    /** Work Speed Module bonus in this machine's Module slot(s) -- see
     * IHaveModules#workSpeedMultiplier for the diminishing-return stacking rule. Applies ONLY to
     * the food-boost craft cycle (see {@link #incrementProgress}), deliberately not passive
     * generation -- see the design discussion for why passive yield stays module-independent. */
    protected float workSpeedMultiplier() {
        List<ItemStack> modules = new ArrayList<>();
        for (int i = 0; i < MODULE_INVENTORY.getSlots(); i++) {
            modules.add(MODULE_INVENTORY.getStackInSlot(i));
        }
        return net.scruffy.dermicraft.interfaces.IHaveModules.workSpeedMultiplier(modules);
    }

    /** Capacity Module bonus, summed over every installed Module. */
    @Override
    protected int capacityBonus() {
        int total = 0;
        for (int i = 0; i < MODULE_INVENTORY.getSlots(); i++) {
            total += capacityModuleBonus(MODULE_INVENTORY.getStackInSlot(i));
        }
        return total;
    }

    @Override
    protected void applyCapacityBonus() {
        TANK.setCapacity(tankCapacity() + capacityBonus());
    }

    @Override
    protected boolean canRemoveModule(int slot) {
        int thisBonus = capacityModuleBonus(MODULE_INVENTORY.getStackInSlot(slot));
        if (thisBonus == 0) return true;
        int newCapacity = tankCapacity() + (capacityBonus() - thisBonus);
        return TANK.getFluid().getAmount() <= newCapacity;
    }

    /**
     * Keeps this block's actual world light emission in sync with whatever's in {@link #TANK} --
     * same pattern {@code BeakerBlockEntity} already uses for the identical problem (a fluid-holding
     * block whose held fluid, and therefore how much it should glow, can change). Without this, a
     * Drooling Cauldron holding lava mid-evolution rendered exactly as dark as one holding water --
     * lava is supposed to glow regardless of ambient light, and nothing was telling the world (or
     * this block's own light value, which the tank-fluid renderer's packed light comes from) that it
     * should.
     */
    @Override
    protected void onTankContentsChanged() {
        if (level == null) return;

        FluidStack fluid = TANK.getFluid();
        int lightLevel = fluid.isEmpty() ? 0 : fluid.getFluid().getFluidType().getLightLevel(fluid);

        BlockState state = getBlockState();
        if (state.getValue(DroolingMachineBlock.LIGHT_LEVEL) != lightLevel) {
            level.setBlock(worldPosition, state.setValue(DroolingMachineBlock.LIGHT_LEVEL, lightLevel), 3);
        }
    }

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
        dropItems(level, MODULE_INVENTORY, worldPosition);
    }

    public ItemStack insertItemStack(ItemStack stack) {
        return insertItemStack(INVENTORY, INPUT, stack);
    }

    public ItemStack extractItemStack() {
        return extractItem(INVENTORY, INPUT, Integer.MAX_VALUE);
    }

    public void tick(Level level) {
        if (level.isClientSide) return;

        // Checked first, before anything else this tick touches `this` -- a subclass whose
        // evolution just completed (Drooling Cauldron) replaces the block here, which makes this
        // instance stale. Returning immediately means no further code this tick runs against a
        // block entity that's no longer the one at this position.
        if (onTickStart(level)) return;

        resolvePendingRecipe(level);

        //////////Every Second (20 ticks)\\\\\\\\\\
        if (ModMath.Time.hasTicksPassed(level, ModMath.Time.getSecondsToTicks(1))) {
            FluidStack offer = new FluidStack(currentTargetFluid(), passiveYieldAmount());
            int filled = TANK.hasRoom(offer) ? TANK.fill(offer, IFluidHandler.FluidAction.EXECUTE) : 0;
            onPassiveFillResult(filled);
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

            // The fluid added is ALWAYS currentTargetFluid() -- never the recipe's own stored
            // result field. A food-boost recipe only ever decided the ingredient/amount/time
            // formula; which fluid actually comes out has to track whatever the machine currently
            // produces, the same as passive generation does, or a Cauldron mid-evolution (tank
            // already holding the Module's target fluid) tries to add the recipe's water instead of
            // lava -- which the tank then silently refuses (can't mix two different fluids), while
            // the ingredient still got eaten for nothing. Checked via a SIMULATE first (matches
            // Masticator's own "won't even attempt it" convention) rather than the old
            // amount-only TANK.hasRoom check, which said yes even when the fluid didn't match.
            FluidStack offer = new FluidStack(currentTargetFluid(), resultAmount);
            boolean canComplete = isMaxProgressValid() && hasIngredients()
                    && TANK.fill(offer, IFluidHandler.FluidAction.SIMULATE) >= offer.getAmount();

            if (canComplete) {
                if (isStillCrafting()) {
                    incrementProgress();
                    setChanged();

                } else {
                    TANK.fill(offer, IFluidHandler.FluidAction.EXECUTE);
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
        progress += Math.max(1, Math.round(CRAFT_TICKS * workSpeedMultiplier()));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("drooling_items", INVENTORY.serializeNBT(registries));
        tag.put("drooling_module_inv", MODULE_INVENTORY.serializeNBT(registries));
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
        // NOT a plain INVENTORY.deserializeNBT -- a Cauldron/Crucible saved before OUTPUT existed
        // carries Size=1 and would shrink this handler back down, crashing on world load when the
        // menu adds its OUTPUT slot at index 1 (same bug MachineBaseBlockEntity#loadItemHandler was
        // generalized to fix, see SkinTankBlockEntity's own identical note).
        CompoundTag oldInventoryTag = tag.getCompound("drooling_items");
        if (tag.contains("drooling_items")) loadItemHandler(INVENTORY, INVENTORY_SIZE, registries, oldInventoryTag);

        if (tag.contains("drooling_module_inv")) {
            loadItemHandler(MODULE_INVENTORY, moduleSlotCount(), registries, tag.getCompound("drooling_module_inv"));
        } else {
            // Pre-split save: the Module item was the old combined INVENTORY's trailing slot
            // (index 2, back when INVENTORY_SIZE was 3) -- see
            // MachineBaseBlockEntity#extractLegacyModuleStack.
            ItemStack legacyModule = extractLegacyModuleStack(registries, oldInventoryTag, 2);
            if (!legacyModule.isEmpty()) MODULE_INVENTORY.setStackInSlot(0, legacyModule);
        }

        applyCapacityBonus();
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
                return slot == OUTPUT ? 1 : super.getSlotLimit(slot);
            }
        };
    }
}
