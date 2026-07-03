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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.OneFluidOneItemRecipeInput;
import net.scruffy.dermicraft.recipe.masticating.MasticatingRecipe;
import net.scruffy.dermicraft.screen.custom.masticator.MasticatorMenu;
import net.scruffy.dermicraft.tank.FuelTank;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.tank.VulnerableTank;
import net.scruffy.dermicraft.util.ModFluidUtil;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


public class MasticatorBlockEntity extends MachineBaseBlockEntity implements MenuProvider, IHaveInventory {

    private final FuelTank FUEL_TANK = createFuelTank();
    private final VulnerableTank INGREDIENT_TANK = createIngredientTank();
    private final VulnerableTank RESULT_TANK = createResultTank();

    private boolean isTransferringFluids = false;

    private final ItemStackHandler INVENTORY = createItemHandler(3);

    private final int FUEL_USE_DEFAULT = 1;
    private int fuelUseRate = FUEL_USE_DEFAULT;
    private final float SPEED_DEFAULT = 1f;
    private float speed = SPEED_DEFAULT;
    private int resultAmount = 0;

    private static final int MAX_HEALTH = 200;
    private static final int HUNGER_RATE = 1; // HP lost per cycle while unfueled and processing
    private static final float UNFUELED_SPEED_MODIFIER = 0.1f; // flat rate when running with no fuel at all
    private static final float RECOVERY_SPEED_FACTOR = 0.1f; // 10% of the fuel's own normal speed while healing
    private static final int BASE_HEAL_RATE = 2; // HP restored per cycle at a heal modifier of 1.0 (provisional)

    private RecipeHolder<MasticatingRecipe> activeRecipe = null;
    private Item activeItem = Items.AIR;

    @Nullable
    private ResourceLocation pendingRecipeId = null;

    public MasticatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MASTICATOR_BE.get(), pos, blockState);
        maxHealth = MAX_HEALTH;
        health = MAX_HEALTH;
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    public IFluidHandler getTank(@Nullable Direction direction) {
        if (direction == null) return INGREDIENT_TANK;

        return switch (direction) {
            case UP -> FUEL_TANK;
            case DOWN -> RESULT_TANK;
            default -> INGREDIENT_TANK;
        };
    }

    public FuelTank getFuelTank() {
        return FUEL_TANK;
    }

    public VulnerableTank getIngredientTank() {
        return INGREDIENT_TANK;
    }

    public VulnerableTank getResultTank() {
        return RESULT_TANK;
    }

    public FluidStack getFluid(int slot) {

        if (slot == FUEL_TANK.SLOT) return FUEL_TANK.getFluid();

        else if (slot == INGREDIENT_TANK.SLOT) return INGREDIENT_TANK.getFluid();

        else if (slot == RESULT_TANK.SLOT) return RESULT_TANK.getFluid();

        else return FluidStack.EMPTY;
    }

    public IItemHandler getItemHandler(@Nullable Direction direction) {
        if (direction == null) return INVENTORY;

        int targetSlot = switch (direction) {
            case UP -> FUEL_TANK.SLOT;
            case DOWN -> RESULT_TANK.SLOT;
            default -> INGREDIENT_TANK.SLOT;
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
    }

    public ItemStack insertItemStack(ItemStack stack) {
        return insertItemStack(INVENTORY, INGREDIENT_TANK.SLOT, stack);
    }

    public ItemStack extractIngredients() {
        return extractItemStack(INVENTORY, INGREDIENT_TANK.SLOT);
    }

    public void tick(Level level) {
        if (level.isClientSide) return;

        if (ModMath.Time.hasSecondsPassed(level, 5) && !RESULT_TANK.isEmpty()) {
            RESULT_TANK.pushFluidToBelowNeighbour(level, worldPosition);
        }

        resolvePendingRecipe(level);

        //INVENTORY'S onContentChange handles:
        // grabbing and setting the recipe
        // and setting maxProgress
        if (ModMath.Time.hasTicksPassed(level, CRAFT_TICKS)) {

            // Healing runs every cycle regardless of whether a recipe is active -- an idle,
            // fueled machine below max health should still recover.
            boolean fueled = FUEL_TANK.hasEnoughFuel(fuelUseRate);
            boolean healedThisCycle = tickHealing(fueled);

            if (!isRecipeValid(activeRecipe)) {
                if (progress > 0) {
                    resetProgress();
                }
            } else if (isMaxProgressValid() && hasIngredients() && RESULT_TANK.hasRoom(resultAmount)) {
                if (isStillCrafting()) {
                    tickProgress(fueled, healedThisCycle);
                } else {
                    INGREDIENT_TANK.useFluid(resultAmount);
                    RESULT_TANK.fill(craftResult(resultAmount), IFluidHandler.FluidAction.EXECUTE);
                    INVENTORY.extractItem(INGREDIENT_TANK.SLOT, 1, false);
                    resetProgress();
                }
            }
            // Always sync, even when idle -- otherwise healing that happens with no active
            // recipe never reaches the client, and the GUI health bar appears frozen.
            setChanged();
            updateBlock();
        }
    }


    private void resolvePendingRecipe(Level level) {
        if (pendingRecipeId == null) return;

        level.getRecipeManager().byKey(pendingRecipeId).ifPresent(recipeHolder -> {
            if (recipeHolder.value() instanceof MasticatingRecipe) {
                this.activeRecipe = (RecipeHolder<MasticatingRecipe>) recipeHolder;
            }
        });
        pendingRecipeId = null;
    }

    private void setSpeed() {
        // No floor here: an unfueled/starved machine's progress is handled explicitly
        // in tickHealthAndProgress(), not by falling back to this cached fueled-speed value.
        speed = FUEL_TANK.getSpeed() * CRAFT_TICKS;
    }

    private void setUseRate() {
        fuelUseRate = Math.max(FUEL_USE_DEFAULT, FUEL_TANK.getUseRate()) * CRAFT_TICKS;
    }

    private void useFuel() {
        FUEL_TANK.useFuel(fuelUseRate);
    }

    private boolean hasIngredients() {
        if (!isIngredientSlotEmpty()) return true;
        return hasEnoughIngredientFluid();
    }

    private boolean hasEnoughIngredientFluid() {
        return INGREDIENT_TANK.hasEnoughFluid(resultAmount);
    }

    private boolean isIngredientSlotEmpty() {
        return INVENTORY.getStackInSlot(INGREDIENT_TANK.SLOT).isEmpty();
    }

    private Optional<RecipeHolder<MasticatingRecipe>> getRecipeOptional() {
        if (level == null) return Optional.empty();

        RecipeManager recipeManager = level.getRecipeManager();
        ItemStack stack = INVENTORY.getStackInSlot(INGREDIENT_TANK.SLOT);
        FluidStack fluid = INGREDIENT_TANK.getFluid();

        return recipeManager.getRecipeFor(ModRecipes.MASTICATING_TYPE.get(),
                new OneFluidOneItemRecipeInput(stack, fluid), this.level);
    }

    private void setActiveRecipe(Optional<RecipeHolder<MasticatingRecipe>> opt) {
        if (opt.isPresent()) {
            this.activeRecipe = opt.get();
        } else {
            this.resetActiveRecipe();
            resetMaxProgress(); // Ensures the machine safely idles
        }
    }

    private void setResultAmount() {
        resultAmount = activeRecipe.value().getCraftingAmount(INVENTORY.getStackInSlot(INGREDIENT_TANK.SLOT));
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

    // NOTE: today's fueled full-speed path applies CRAFT_TICKS twice (once caching into
    // `speed` in setSpeed(), once here) -- for Crude Slurry (speed modifier 1.0) that's
    // invisible, but a future fuel with a different modifier will see this 10x scale-up.
    // Left as-is to avoid changing today's live balance; the unfueled/recovery paths below
    // are deliberately defined relative to this existing shape, not the "intended" formula.
    private void incrementProgress(float speedOverride) {
        int workDoneInCycle = Math.round(CRAFT_TICKS * speedOverride);
        progress += Math.max(1, workDoneInCycle);
    }

    private int getHealAmount() {
        return Math.round(BASE_HEAL_RATE * FUEL_TANK.getHeal());
    }

    // Runs every cycle regardless of recipe state -- heals an idle-but-fueled machine too.
    // Returns whether healing (and its fuel consumption) actually happened this cycle, so
    // tickProgress() knows not to consume fuel a second time for the same cycle.
    private boolean tickHealing(boolean fueled) {
        if (fueled && health < maxHealth) {
            useFuel();
            healMachine(getHealAmount());
            return true;
        }
        return false;
    }

    private void tickProgress(boolean fueled, boolean healedThisCycle) {
        if (isStarved()) {
            return; // fuel/heal already handled by tickHealing(); no progress while starved
        }

        if (fueled) {
            if (healedThisCycle) {
                incrementProgress(RECOVERY_SPEED_FACTOR * speed); // fuel already spent healing this cycle
            } else {
                useFuel();
                incrementProgress(speed);
            }
        } else {
            damageMachine(HUNGER_RATE);
            incrementProgress(UNFUELED_SPEED_MODIFIER * CRAFT_TICKS);
        }
    }

    private void setMaxProgress() {
        maxProgress = activeRecipe.value().getCraftingTime(INVENTORY.getStackInSlot(INGREDIENT_TANK.SLOT));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", INVENTORY.serializeNBT(registries));
        tag.put("fuel", FUEL_TANK.writeToNBT(registries, new CompoundTag()));
        tag.put("craft", INGREDIENT_TANK.writeToNBT(registries, new CompoundTag()));
        tag.put("output", RESULT_TANK.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("speed", speed);
        tag.putInt("use", fuelUseRate);
        tag.putInt("resultFluid", resultAmount);
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        tag.putInt("health", health);
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(this.activeItem);
        tag.putString("activeItem", itemKey.toString());
        if (isRecipeValid(activeRecipe)) tag.putString("saved_recipe", activeRecipe.id().toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) INVENTORY.deserializeNBT(registries, tag.getCompound("inventory"));
        if (tag.contains("fuel")) FUEL_TANK.readFromNBT(registries, tag.getCompound("fuel"));
        if (tag.contains("craft")) INGREDIENT_TANK.readFromNBT(registries, tag.getCompound("craft"));
        if (tag.contains("output")) RESULT_TANK.readFromNBT(registries, tag.getCompound("output"));
        speed = tag.getFloat("speed");
        fuelUseRate = tag.getInt("use");
        resultAmount = tag.getInt("resultFluid");
        this.progress = tag.getInt("progress");
        this.maxProgress = tag.getInt("maxProgress");
        this.health = tag.contains("health") ? tag.getInt("health") : maxHealth;

        if (tag.contains("saved_recipe", CompoundTag.TAG_STRING)) {
            pendingRecipeId = ResourceLocation.parse(tag.getString("saved_recipe"));
        }

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

                    if(slot == INGREDIENT_TANK.SLOT) {
                        ItemStack stack = getStackInSlot(INGREDIENT_TANK.SLOT);
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

            @Override
            public int getSlotLimit(int slot) {
                return ModFluidUtil.hasEmptyFluidHandlerInSlot(this, slot) ? 1 : super.getSlotLimit(slot);
            }

            @Override
            @NotNull
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (ModFluidUtil.hasFluidHandlerInSlot(this, slot)) {
                    if (!getStackInSlot(slot).isEmpty()) {
                        return stack;
                    }
                    if (stack.getCount() > 1) {
                        if (simulate) {
                            ItemStack remainder = stack.copy();
                            remainder.shrink(1);
                            return remainder;
                        } else {
                            ItemStack singleInsert = stack.copyWithCount(1);
                            super.insertItem(slot, singleInsert, false);
                            ItemStack remainder = stack.copy();
                            remainder.shrink(1);
                            return remainder;
                        }
                    }
                }
                return super.insertItem(slot, stack, simulate);
            }
        };
    }

    protected FuelTank createFuelTank() {
        return new FuelTank(FluidType.BUCKET_VOLUME * 5, 0) {
            @Override
            protected void onContentsChanged() {
                if (!level.isClientSide) {

                    setSpeed();
                    setUseRate();

                    setChanged();
                }
            }
        };
    }

    private VulnerableTank createIngredientTank() {
        return new VulnerableTank(FluidType.BUCKET_VOLUME * 5, 1) {
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

    private VulnerableTank createResultTank() {
        return new VulnerableTank(FluidType.BUCKET_VOLUME * 5, 2) {
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
