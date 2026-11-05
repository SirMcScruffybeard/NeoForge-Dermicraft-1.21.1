package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.scruffy.dermicraft.block.custom.MasticatorBlock;
import net.scruffy.dermicraft.block.custom.MasticatorVisualState;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.interfaces.IHaveModules;
import net.scruffy.dermicraft.interfaces.IEvolvingMachine;
import net.scruffy.dermicraft.interfaces.IModuleTabMachine;
import net.scruffy.dermicraft.machine.MachineStatKey;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.OneFluidOneItemRecipeInput;
import net.scruffy.dermicraft.recipe.masticating.MasticatingRecipe;
import net.scruffy.dermicraft.screen.custom.masticator.MasticatorMenu;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.util.ModFluidUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class MasticatorBlockEntity extends AbstractFueledMachineBlockEntity<MasticatingRecipe>
        implements MenuProvider, IHaveInventory, IHasChannels, IEvolvingMachine, IModuleTabMachine {

    // The solid recipe ingredient (food item) lives in its own slot -- INGREDIENT_TANK.SLOT is
    // purely a fluid-container passthrough for filling/draining the Water tank (bucket in, bucket
    // out), same as FUEL_TANK.SLOT/RESULT_TANK.SLOT.
    public static final int INGREDIENT_ITEM_SLOT = 3;
    public static final int INVENTORY_SIZE = 4;

    private final ModFluidTank INGREDIENT_TANK = createIngredientTank();
    private final ModFluidTank RESULT_TANK = createResultTank();

    private boolean isTransferringFluids = false;

    public final ItemStackHandler INVENTORY = createItemHandler(INVENTORY_SIZE);

    // General Module slot(s) -- own dedicated handler (see MachineBaseBlockEntity#createModuleInventory), not part of
    // the general INVENTORY above; same tab-gated pattern as Skin Tank/Drooling Cauldron (see MasticatorMenu's
    // MAIN_TAB/MODULE_TAB). Evolution Modules go in the dedicated Evolution slot (EVOLUTION_INVENTORY, in the engine),
    // never here.
    public final ItemStackHandler MODULE_INVENTORY = createModuleInventory(getModuleSlotCount(),
            stack -> stack.is(net.scruffy.dermicraft.datagen.tag.ModTags.Items.MODULES) && !isEvolutionModule(stack));

    private int resultAmount = 0;

    private Item activeItem = Items.AIR;

    // Which screen tab was last open -- same pattern as SkinTankBlockEntity/DroolingMachineBlockEntity's
    // own isModuleTabActive/setModuleTabActive, so reopening the screen returns to the tab last viewed.
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

    public MasticatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MASTICATOR_BE.get(), pos, blockState);
    }

    // ---- Evolution ----------------------------------------------------------------------------
    // The Masticator evolves in place through its evolution state and the Evolution slot. The gradual-progress
    // machinery, the Evolution slot, the flourish and the stat defaults (tank capacity, HP, speed, fuel use, Module
    // slots) all live in AbstractFueledMachineBlockEntity; this class opts in and resizes what it owns.

    @Override
    protected boolean usesEvolutionState() {
        return true;
    }

    @Override
    protected MachineStatKey getStatKey() {
        return MachineStatKey.MASTICATOR;
    }

    @Override
    protected void onEvolutionStateChanged() {
        super.onEvolutionStateChanged();
        resizeModuleInventory(MODULE_INVENTORY, getModuleSlotCount());
        applyCapacityBonus();
    }

    @Override
    protected RecipeType<MasticatingRecipe> getRecipeType() {
        return ModRecipes.MASTICATING_TYPE.get();
    }

    // ---- Visual state (face texture) --------------------------------------------------------
    // tickHealing runs every cycle regardless of crafting state, so it doubles as the visual-state
    // refresh point. Recovering (health < maxHealth) takes priority over Running -- a damaged
    // machine signals distress even mid-cycle. Mirrors MutatorBlockEntity's identical mechanism.
    //
    // Debounced: a new state must be observed for VISUAL_STATE_STABLE_CYCLES consecutive cycles
    // (2 cycles = ~1s at CRAFT_TICKS=10) before the texture commits. Borderline conditions can
    // otherwise strobe -- e.g. a hopper feeding the ingredient slot every 8 ticks against the
    // 10-tick cycle flaps RUNNING/IDLE -- and every commit is a setBlock (client update + chunk
    // re-render), so flapping is also wasted churn. Matching the current state resets any pending
    // change, so a one-cycle blip never flashes the face. Transient, deliberately not saved to NBT.
    private static final int VISUAL_STATE_STABLE_CYCLES = 2;

    @Nullable
    private MasticatorVisualState pendingVisualState = null;
    private int pendingVisualCycles = 0;

    @Override
    protected boolean tickHealing(boolean fueled) {
        boolean healed = super.tickHealing(fueled);
        updateVisualState();
        return healed;
    }

    private void updateVisualState() {
        if (level == null) return;

        MasticatorVisualState computed = computeVisualState();
        BlockState state = getBlockState();

        if (state.getValue(MasticatorBlock.STATE) == computed) {
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
            level.setBlock(worldPosition, state.setValue(MasticatorBlock.STATE, computed), Block.UPDATE_CLIENTS);
            pendingVisualState = null;
            pendingVisualCycles = 0;
        }
    }

    private MasticatorVisualState computeVisualState() {
        if (maxHealth > 0 && health < maxHealth) return MasticatorVisualState.RECOVERING;
        // hasCraftingInputs()/hasCraftingOutputRoom() are vacuously true while idle (resultAmount
        // defaults to 0, so hasEnoughFluid(0)/hasRoom(0) are trivially satisfied) -- isRecipeValid
        // is the real "is there actually something to craft" signal, matching the base tick()'s
        // own crafting gate.
        if (!isStarved() && isRecipeValid(activeRecipe) && hasCraftingInputs() && hasCraftingOutputRoom()) {
            return MasticatorVisualState.RUNNING;
        }
        return MasticatorVisualState.IDLE;
    }

    /** See {@link IHasChannels#describeFace} -- mirrors {@link #getTank}/{@link #getItemHandler} literally. */
    @Override
    public Component describeFace(Direction face) {
        return switch (face) {
            case UP -> Component.translatable("tooltip.dermicraft.idep.face.masticator_fuel");
            case DOWN -> Component.translatable("tooltip.dermicraft.idep.face.masticator_result");
            default -> Component.translatable("tooltip.dermicraft.idep.face.masticator_ingredient");
        };
    }

    /** See {@link IHasChannels#describeFluidFace} -- mirrors {@link #getTank} literally. */
    @Override
    public Component describeFluidFace(Direction face) {
        return switch (face) {
            case UP -> Component.translatable("tooltip.dermicraft.tank.fuel");
            case DOWN -> Component.translatable("tooltip.dermicraft.tank.result");
            default -> Component.translatable("tooltip.dermicraft.tank.ingredient");
        };
    }

    public IFluidHandler getTank(@Nullable Direction direction) {
        if (direction == null) return INGREDIENT_TANK;

        return switch (direction) {
            case UP -> FUEL_TANK;
            case DOWN -> RESULT_TANK;
            default -> INGREDIENT_TANK;
        };
    }

    public ModFluidTank getIngredientTank() {
        return INGREDIENT_TANK;
    }

    public ModFluidTank getResultTank() {
        return RESULT_TANK;
    }

    /**
     * Self-described channel list for the Gate multiblock -- see {@link IHasChannels}. The three
     * tanks, direction-unlocked from what {@code getTank(Direction)} currently hard-binds to
     * UP/sides/DOWN, plus the solid-ingredient item slot ({@code INGREDIENT_ITEM_SLOT}) -- that
     * slot is the real recipe input (see {@code onCraftComplete}/{@code OneFluidOneItemRecipeInput}),
     * independent of the ingredient fluid tank and its own bucket-passthrough slot
     * ({@code INGREDIENT_TANK.SLOT}), so it gets its own item channel.
     *
     * <p>Each channel is omitted if its native face(s) -- the same faces {@code getTank}/
     * {@code getItemHandler} already bind it to -- already has a direct connection (see
     * {@link #isFaceServiced}). Ingredient's native faces are all 4 sides (the "default" branch of
     * both direction switches above), so either sub-channel is considered serviced if ANY side
     * already reaches it.
     */
    @Override
    public List<Channel> getChannels() {
        List<Channel> channels = new ArrayList<>();

        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID, Direction.UP)) {
            channels.add(new Channel.FluidChannel("fuel", Component.literal("Fuel"), Channel.IO.IN, FUEL_TANK));
        }
        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID,
                Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            channels.add(new Channel.FluidChannel("ingredient_fluid", Component.literal("Ingredient (fluid)"), Channel.IO.IN, INGREDIENT_TANK));
        }
        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.ITEM,
                Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            channels.add(new Channel.ItemChannel("ingredient_item", Component.literal("Ingredient (item)"), Channel.IO.IN, getIngredientItemChannelHandler()));
        }
        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID, Direction.DOWN)) {
            channels.add(new Channel.FluidChannel("result", Component.literal("Result"), Channel.IO.OUT, RESULT_TANK));
        }

        return channels;
    }

    /** The solid-ingredient slot, exposed standalone for the Gate's ingredient_item channel. */
    private IItemHandler getIngredientItemChannelHandler() {
        int slot = INGREDIENT_ITEM_SLOT;
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
                return INVENTORY.isItemValid(slot, stack);
            }
        };
    }

    public FluidStack getFluid(int slot) {

        if (slot == FUEL_TANK.SLOT) return FUEL_TANK.getFluid();

        else if (slot == INGREDIENT_TANK.SLOT) return INGREDIENT_TANK.getFluid();

        else if (slot == RESULT_TANK.SLOT) return RESULT_TANK.getFluid();

        else return FluidStack.EMPTY;
    }

    public IItemHandler getItemHandler(@Nullable Direction direction) {
        if (direction == null) return INVENTORY;

        // Sides expose the real recipe ingredient slot (INGREDIENT_ITEM_SLOT) to automation, not
        // INGREDIENT_TANK's bucket-passthrough slot -- that one stays GUI/player-only, since
        // automation should fill INGREDIENT_TANK via the fluid capability (getTank) directly
        // rather than shuttling buckets through an item slot.
        int targetSlot = switch (direction) {
            case UP -> FUEL_TANK.SLOT;
            case DOWN -> RESULT_TANK.SLOT;
            default -> INGREDIENT_ITEM_SLOT;
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
                return INVENTORY.isItemValid(targetSlot, stack);
            }
        };
    }

    @Override
    public void drops() {
        super.drops(INVENTORY);
        super.drops(MODULE_INVENTORY);
        super.drops(EVOLUTION_INVENTORY);
    }

    public ItemStack insertItemStack(ItemStack stack) {
        return insertItemStack(INVENTORY, INGREDIENT_ITEM_SLOT, stack);
    }

    public ItemStack extractIngredients() {
        return extractItemStack(INVENTORY, INGREDIENT_ITEM_SLOT);
    }

    @Override
    protected void drainOutputs(Level level) {
        if (autoDrainEnabled && !RESULT_TANK.isEmpty()) {
            RESULT_TANK.pushFluidToBelowNeighbour(level, worldPosition);
        }
    }

    @Override
    protected boolean hasCraftingInputs() {
        // Frozen during the evolution flourish, so the new state never lands mid-craft.
        return !isEvolutionFlourishing() && hasIngredients();
    }

    @Override
    protected boolean hasCraftingOutputRoom() {
        // Checked via a SIMULATE fill rather than hasRoom()+manual type comparison, so this also
        // catches a RESULT_TANK that can't accept the result's HAZARD kind (e.g. a Charred
        // Masticator, hazard-gated to TIER_2, asked to produce a RADIATION_MILD fluid like Molten
        // Glowstone without a Radiation Safety Module installed) -- not just amount/fluid-type
        // mismatches. Without this, onCraftComplete() still consumed the ingredient/reagent and
        // RESULT_TANK.fill() silently discarded the hazardous result (vanilla FluidTank.fill()
        // rejects anything its validator refuses), i.e. the craft appeared to eat the input and
        // produce nothing.
        FluidStack pendingResult = craftResult(resultAmount);
        return RESULT_TANK.fill(pendingResult, IFluidHandler.FluidAction.SIMULATE) >= pendingResult.getAmount();
    }

    @Override
    protected void onCraftComplete() {
        // Captured before draining: useFluid() fires INGREDIENT_TANK.onContentsChanged()
        // synchronously, which re-resolves the recipe against the now-drained fluid and can
        // null out activeRecipe before this method reaches it (mirrors the same hazard documented
        // in MetastasizerBlockEntity#onCraftComplete).
        int itemAmount = activeRecipe.value().itemAmount();
        int fluidCost = requiredIngredientFluidAmount();
        FluidStack result = craftResult(resultAmount);

        INGREDIENT_TANK.useFluid(fluidCost);
        RESULT_TANK.fill(result, IFluidHandler.FluidAction.EXECUTE);
        INVENTORY.extractItem(INGREDIENT_ITEM_SLOT, itemAmount, false);
    }

    private boolean hasIngredients() {
        return !isIngredientSlotEmpty() && hasEnoughIngredientFluid();
    }

    private boolean hasEnoughIngredientFluid() {
        return INGREDIENT_TANK.hasEnoughFluid(requiredIngredientFluidAmount());
    }

    // A recipe's ingredientFluidAmount is a fixed cost (e.g. raw iron's 250 mB Primitive Catalyst)
    // EXCEPT for vague recipes, which store -1 there deliberately -- their real input requirement
    // scales with the dynamic result amount (see IVagueRecipe/vagueMasticateWithTagAndWater, always
    // built at a 1:1 water-to-output ratio), so resultAmount is the correct stand-in only in that case.
    private int requiredIngredientFluidAmount() {
        int fixedAmount = activeRecipe.value().ingredientFluidAmount();
        return fixedAmount < 0 ? resultAmount : fixedAmount;
    }

    private boolean isIngredientSlotEmpty() {
        return INVENTORY.getStackInSlot(INGREDIENT_ITEM_SLOT).isEmpty();
    }

    protected Optional<RecipeHolder<MasticatingRecipe>> getRecipeOptional() {
        if (level == null) return Optional.empty();

        RecipeManager recipeManager = level.getRecipeManager();
        ItemStack stack = INVENTORY.getStackInSlot(INGREDIENT_ITEM_SLOT);
        FluidStack fluid = INGREDIENT_TANK.getFluid();
        OneFluidOneItemRecipeInput input = new OneFluidOneItemRecipeInput(stack, fluid);

        // Not a plain getRecipeFor -- two recipes can share the same ingredient tag at different
        // itemAmount thresholds (e.g. Fence's batch-of-3 vs its single-item alternate), and
        // MasticatingRecipe#matches is a >= check, so a stack of 3+ satisfies BOTH. Picking vanilla's
        // arbitrary "first match" would risk silently starving the bulkier recipe. Explicitly scan
        // every match and take the highest itemAmount the stack actually satisfies, so batching is
        // always preferred over singles whenever the stack is large enough for it.
        return recipeManager.getAllRecipesFor(ModRecipes.MASTICATING_TYPE.get()).stream()
                .filter(holder -> holder.value().matches(input, level))
                .max(java.util.Comparator.comparingInt(holder -> holder.value().itemAmount()));
    }

    protected void setActiveRecipe(Optional<RecipeHolder<MasticatingRecipe>> opt) {
        if (opt.isPresent()) {
            this.activeRecipe = opt.get();
        } else {
            this.resetActiveRecipe();
            resetMaxProgress(); // Ensures the machine safely idles
        }
    }

    protected void setResultAmount() {
        resultAmount = activeRecipe.value().getCraftingAmount(INVENTORY.getStackInSlot(INGREDIENT_ITEM_SLOT));
    }

    private void resetResultAmount() {
        resultAmount = 0;
    }

    private void resetActiveItem() {
        activeItem = Items.AIR;
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

    protected void setMaxProgress() {
        maxProgress = activeRecipe.value().getCraftingTime(INVENTORY.getStackInSlot(INGREDIENT_ITEM_SLOT));
    }

    /** The hazards that matter when the Masticator processes: the ingredient fluid it consumes and the fluid its recipe
     * produces (the fuel does not). */
    @Override
    protected List<FluidStack> getProcessingFluids() {
        return List.of(INGREDIENT_TANK.getFluid(), craftResult(1));
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
        INGREDIENT_TANK.setCapacity(capacity);
        RESULT_TANK.setCapacity(capacity);
    }

    @Override
    protected boolean canRemoveModule(int slot) {
        int thisBonus = capacityModuleBonus(MODULE_INVENTORY.getStackInSlot(slot));
        if (thisBonus == 0) return true;
        int newCapacity = getBaseTankCapacity() + (getCapacityBonus() - thisBonus);
        return FUEL_TANK.getFluid().getAmount() <= newCapacity
                && INGREDIENT_TANK.getFluid().getAmount() <= newCapacity
                && RESULT_TANK.getFluid().getAmount() <= newCapacity;
    }

    /** How far the pending evolution is, 0 to 1 (0 with no live Evolution Module). Read by
     * {@code EvolutionOverlayBlockEntityRenderer}'s creeping overlay. */
    @Override
    public float getEvolutionProgressFraction() {
        return getGradualEvolutionFraction();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", INVENTORY.serializeNBT(registries));
        tag.put("module_inventory", MODULE_INVENTORY.serializeNBT(registries));
        tag.put("craft", INGREDIENT_TANK.writeToNBT(registries, new CompoundTag()));
        tag.put("output", RESULT_TANK.writeToNBT(registries, new CompoundTag()));
        tag.putInt("resultFluid", resultAmount);
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(this.activeItem);
        tag.putString("activeItem", itemKey.toString());
        tag.putBoolean("module_tab_active", moduleTabActive);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompoundTag oldInventoryTag = tag.getCompound("inventory");
        if (tag.contains("inventory")) {
            // Worlds saved before INGREDIENT_ITEM_SLOT existed have a smaller Size -- see
            // MachineBaseBlockEntity#loadItemHandler for why a plain deserializeNBT would shrink
            // INVENTORY back down and crash the menu (slot out of range).
            loadItemHandler(INVENTORY, INVENTORY_SIZE, registries, oldInventoryTag);
        }

        if (tag.contains("module_inventory")) {
            loadItemHandler(MODULE_INVENTORY, getModuleSlotCount(), registries, tag.getCompound("module_inventory"));
        } else {
            // Pre-split save: the Module item was the old combined INVENTORY's trailing slot
            // (index 4, back when INVENTORY_SIZE was 5) -- recover it before that data's gone for
            // good. See MachineBaseBlockEntity#extractLegacyModuleStack.
            ItemStack legacyModule = extractLegacyModuleStack(registries, oldInventoryTag, 4);
            if (!legacyModule.isEmpty()) MODULE_INVENTORY.setStackInSlot(0, legacyModule);
        }

        applyCapacityBonus();
        if (tag.contains("craft")) INGREDIENT_TANK.readFromNBT(registries, tag.getCompound("craft"));
        if (tag.contains("output")) RESULT_TANK.readFromNBT(registries, tag.getCompound("output"));
        resultAmount = tag.getInt("resultFluid");
        moduleTabActive = tag.getBoolean("module_tab_active");

        if (tag.contains("activeItem", CompoundTag.TAG_STRING)) {
            String itemStringId = tag.getString("activeItem");
            ResourceLocation itemKey = ResourceLocation.parse(itemStringId);
            this.activeItem = BuiltInRegistries.ITEM.get(itemKey);
        } else resetActiveItem();
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.MASTICATOR);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MasticatorMenu(containerId, playerInventory, this);
    }

    protected ItemStackHandler createItemHandler(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                if (level != null && !level.isClientSide()) {

                    if(slot == INGREDIENT_ITEM_SLOT) {
                        ItemStack stack = getStackInSlot(INGREDIENT_ITEM_SLOT);
                        Item currentItem = stack.getItem();

                        if (stack.isEmpty()) {
                            resetActiveRecipe();
                            resetActiveItem();
                            resetProgress();
                            resetMaxProgress();
                            resetResultAmount();
                        }

                        if (currentItem != activeItem) {
                            activeItem = currentItem;
                            resetProgress();
                            resetMaxProgress();

                            Optional<RecipeHolder<MasticatingRecipe>> recipeOpt = getRecipeOptional();
                            setActiveRecipe(recipeOpt);

                            if (isRecipeValid(activeRecipe)) {
                                setMaxProgress();
                                setResultAmount();
                            } else {
                                resetActiveRecipe();
                                resetMaxProgress();
                                resetProgress();
                                resetResultAmount();
                            }
                        }
                    }

                    if (isTransferringFluids) return;

                    biDirectionalFluidTransfer(FUEL_TANK, FUEL_TANK.SLOT);
                    biDirectionalFluidTransfer(INGREDIENT_TANK, INGREDIENT_TANK.SLOT);
                    transferToHandler(RESULT_TANK, RESULT_TANK.SLOT);

                    isTransferringFluids = false;

                    setChanged();
                    updateBlock();
                }
            }

            private void biDirectionalFluidTransfer(ModFluidTank tank, int slot) {

                if (tank.hasFluidHandlerInSlot(this, slot)) {
                    isTransferringFluids = true;
                    tank.transferFluidToTank(this, slot);

                } else {
                    transferToHandler(tank, slot);
                }

            }

            private void transferToHandler(ModFluidTank tank, int slot) {
                if (tank.hasEmptyFluidHandlerInSlot(this, slot)) {
                    isTransferringFluids = true;

                    ItemStack stack = getStackInSlot(slot);
                    if (stack.isEmpty()) return;

                    // Defensive fallback only -- getSlotLimit now caps these slots to 1 unconditionally,
                    // so count > 1 shouldn't be reachable via normal insertion. Kept in case something
                    // ever forces a multi-count stack into the slot directly (NBT load, a command, etc.).
                    if (ModFluidUtil.hasEmptyFluidHandlerInSlot(this, slot) && stack.getCount() > 1) {
                        ItemStack extra = getStackInSlot(slot).copy();
                        extra.shrink(1);

                        stack.setCount(1);
                        setStackInSlot(slot, stack);

                        Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), extra);
                    } else {
                        setStackInSlot(slot, stack);
                    }
                    tank.transferFluidFromTankToHandler(this, slot);
                }
            }

            // Fuel/ingredient/result tank slots hold exactly one container at a time, unconditionally
            // -- not just once already occupied. The old condition (`hasEmptyFluidHandlerInSlot`,
            // which inspects the slot's CURRENT contents) never fires on a slot that starts empty, so
            // a whole stack of empty containers could be inserted in one shot; the auto-fill transfer
            // then only ever fills and returns a single container, silently collapsing the rest of the
            // stack. Capping unconditionally means vanilla's own insertItem correctly accepts just 1
            // and returns the remainder, so the custom insertItem override below is no longer needed.
            @Override
            public int getSlotLimit(int slot) {
                if (slot == FUEL_TANK.SLOT || slot == INGREDIENT_TANK.SLOT || slot == RESULT_TANK.SLOT) return 1;
                return super.getSlotLimit(slot);
            }
        };
    }

    protected ModFluidTank createIngredientTank() {
        return new ModFluidTank(getBaseTankCapacity(), 1) {
            @Override
            protected void onContentsChanged() {

                if (level != null && !level.isClientSide()) {
                    // Bypass the item optimization check entirely because a puddle update occurred
                    Optional<RecipeHolder<MasticatingRecipe>> recipeOpt = getRecipeOptional();
                    setActiveRecipe(recipeOpt);

                    if (isRecipeValid(activeRecipe)) {
                        setMaxProgress();
                        setResultAmount();
                    } else {
                        resetActiveRecipe();
                        resetMaxProgress();
                        resetProgress();
                    }
                }
                setChanged();
            }
        };
    }

    protected ModFluidTank createResultTank() {
        return new ModFluidTank(getBaseTankCapacity(), 2) {
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
