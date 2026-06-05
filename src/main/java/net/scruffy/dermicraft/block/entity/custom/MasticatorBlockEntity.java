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
import net.minecraft.world.inventory.ContainerData;
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
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.OneFluidOneItemRecipeInput;
import net.scruffy.dermicraft.recipe.masticating.MasticatingRecipe;
import net.scruffy.dermicraft.recipe.masticating.VagueMasticatingRecipe;
import net.scruffy.dermicraft.screen.custom.masticator.MasticatorMenu;
import net.scruffy.dermicraft.tank.FuelTank;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.tank.VulnerableTank;
import net.scruffy.dermicraft.util.ModFluidUtil;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("unchecked")
public class MasticatorBlockEntity extends MachineBaseBlockEntity implements MenuProvider, IHaveInventory {

    private final FuelTank FUEL_TANK = createFuelTank();
    private final VulnerableTank INGREDIENT_TANK = createIngredientTank();
    private final VulnerableTank RESULT_TANK = createVulnerableTank(FluidType.BUCKET_VOLUME * 5, 2);

    private boolean isTransferringFluids = false;

    private final ItemStackHandler INVENTORY = createItemHandler(3);

    public final Direction FUEL_FACE = Direction.UP;
    public final Direction OUTPUT_FACE = Direction.DOWN;
    public final Direction INGREDIENT_FACE = Direction.NORTH;

    private final int FUEL_USE_DEFAULT = 1;
    private int fuelUseRate = FUEL_USE_DEFAULT;
    private final float SPEED_DEFAULT = 1f;
    private float speed = SPEED_DEFAULT;
    private final int CRAFT_TICKS = 10; //How many ticks between processing logic firings

    private int resultAmount = 0;

    private RecipeHolder<MasticatingRecipe> activePreciseRecipe = null;
    private RecipeHolder<VagueMasticatingRecipe> activeVagueRecipe = null;
    private Item activeItem = Items.AIR;

    protected final ContainerData data;

    public MasticatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MASTICATOR_BE.get(), pos, blockState);

        data = setContainerData();
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
            public ItemStack getStackInSlot(int slot) {
                return INVENTORY.getStackInSlot(targetSlot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return INVENTORY.insertItem(targetSlot, stack, simulate);
            }

            @Override
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

    public ItemStack insetItemStack(ItemStack stack) {
        return insertItemStack(INVENTORY, INGREDIENT_TANK.SLOT, stack);
    }

    public ItemStack extractIngredients() {
        return extractItemStack(INVENTORY, INGREDIENT_TANK.SLOT);
    }

    public void tick(Level level) {
        if (!level.isClientSide) {
            //INVENTORY'S onContentChange handles:
            // grabbing and setting the recipe
            // and setting maxProgress
            if (ModMath.Time.hasTicksPassed(level, CRAFT_TICKS)) {
                if (isMaxProgressValid() && hasIngredients() && RESULT_TANK.hasRoom(resultAmount)) {
                    if (isStillCrafting()) {
                        if (FUEL_TANK.hasEnoughFuel(fuelUseRate)) {
                            incrementProgress();
                            useFuel();
                            setChanged();
                        }
                    } else {
                        INGREDIENT_TANK.useFluid(resultAmount);
                        RESULT_TANK.fill(craftResult(resultAmount), IFluidHandler.FluidAction.EXECUTE);
                        INVENTORY.extractItem(INGREDIENT_TANK.SLOT, 1, false);
                        resetProgress();
                    }
                }

                setChanged();
                updateBlock();
            }
        }
    }

    private void setSpeed() {
        speed = Math.max(SPEED_DEFAULT, FUEL_TANK.getSpeed()) * CRAFT_TICKS;
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

    private Optional<RecipeHolder<?>> getRecipeOptional() {
        if (level == null) return Optional.empty();

        RecipeManager recipeManager = level.getRecipeManager();
        ItemStack stack = INVENTORY.getStackInSlot(INGREDIENT_TANK.SLOT);
        FluidStack fluid = INGREDIENT_TANK.getFluid();

        Optional<RecipeHolder<MasticatingRecipe>> preciseMatch =
                recipeManager.getRecipeFor(ModRecipes.MASTICATING_TYPE.get(),
                        new OneFluidOneItemRecipeInput(stack, fluid), this.level);

        if (preciseMatch.isPresent()) return Optional.of(preciseMatch.get());

        Optional<RecipeHolder<VagueMasticatingRecipe>> vagueMatch =
                recipeManager.getRecipeFor(ModRecipes.VAGUE_MASTICATING_TYPE.get(), new OneFluidOneItemRecipeInput(stack, fluid), this.level);

        if (vagueMatch.isPresent()) return Optional.of(vagueMatch.get());

        return Optional.empty();
    }

    private void setActiveRecipe(Optional<RecipeHolder<?>> opt) {
        if (opt.isPresent()) {
            RecipeHolder<?> holder = opt.get();
            Object recipeValue = holder.value();

            if (recipeValue instanceof MasticatingRecipe) {
                this.activePreciseRecipe = (RecipeHolder<MasticatingRecipe>) holder;
                this.resetActiveVagueRecipe(); // Sets vague to null
            } else if (recipeValue instanceof VagueMasticatingRecipe) {
                this.resetActivePreciseRecipe(); // Sets precise to null
                this.activeVagueRecipe = (RecipeHolder<VagueMasticatingRecipe>) holder;
            }
        }
        // SCENARIO 2: No recipe matched (Clear the machine's state completely!)
        else {
            this.resetActivePreciseRecipe();
            this.resetActiveVagueRecipe();
            resetMaxProgress(); // Ensures the machine safely idles
        }
    }

    private void setPreciseResultAmount() {
        resultAmount = activePreciseRecipe.value().resultAmount();
    }

    private void setVagueResultAmount() {
        resultAmount = activeVagueRecipe.value().getCraftingAmount(INVENTORY.getStackInSlot(INGREDIENT_TANK.SLOT));
    }

    private void resetResultAmount() {
        resultAmount = 0;
    }

    private void resetActiveItem() {
        activeItem = Items.AIR;
    }

    private void resetActivePreciseRecipe() {
        activePreciseRecipe = null;
    }

    public void resetActiveVagueRecipe() {
        activeVagueRecipe = null;
    }

    /**
     * ***************************
     * Resets both active recipes
     * ***************************
     */
    public void resetActiveRecipes() {
        resetActivePreciseRecipe();
        resetActiveVagueRecipe();
    }

    private FluidStack craftResult(int craftAmount) {
        if (isRecipeValid(activePreciseRecipe)) {
            return activePreciseRecipe.value().getResultFluidStack();
        } else if (isRecipeValid(activeVagueRecipe)) {
            return activeVagueRecipe.value().getResultStack(craftAmount);
        }
        return FluidStack.EMPTY;
    }

    private void incrementProgress() {
        int workDoneInCycle = Math.round(CRAFT_TICKS * speed);
        progress += Math.max(1, workDoneInCycle);
    }

    private void setMaxProgressPrecise() {
        maxProgress = activePreciseRecipe.value().ticks();
    }

    private void setMaxProgressVague() {
        maxProgress =
                activeVagueRecipe.value().getCraftingTime(INVENTORY.getStackInSlot(INGREDIENT_TANK.SLOT));
    }

    private boolean isMaxProgressValid() {
        return maxProgress > 0;
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
        tag.putInt("result", resultAmount);
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(this.activeItem);
        tag.putString("activeItem", itemKey.toString());
        if (isRecipeValid(activePreciseRecipe)) tag.putString("saved_recipe", activePreciseRecipe.id().toString());
        else if (isRecipeValid(activeVagueRecipe)) tag.putString("saved_recipe", activeVagueRecipe.id().toString());
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
        resultAmount = tag.getInt("result");
        this.progress = tag.getInt("progress");
        this.maxProgress = tag.getInt("maxProgress");

        if (this.level != null && tag.contains("SavedRecipeId", CompoundTag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.parse(tag.getString("SavedRecipeId"));
            this.level.getRecipeManager().byKey(id).ifPresent(recipeHolder -> {
                if (recipeHolder.value() instanceof MasticatingRecipe) {
                    this.activePreciseRecipe = (RecipeHolder<MasticatingRecipe>) recipeHolder;
                } else if (recipeHolder.value() instanceof VagueMasticatingRecipe) {
                    this.activeVagueRecipe = (RecipeHolder<VagueMasticatingRecipe>) recipeHolder;
                }
            });
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
        return new MasticatorMenu(containerId, playerInventory, this, this.data);
    }

    private ContainerData setContainerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> progress;
                    case 1 -> maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> progress = value;
                    case 1 -> maxProgress = value;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    protected ItemStackHandler createItemHandler(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                if (level != null && !level.isClientSide()) {

                    ItemStack stack = getStackInSlot(INGREDIENT_TANK.SLOT);
                    Item currentItem = stack.getItem();

                    if (stack.isEmpty()) {
                        resetActiveRecipes();
                        resetActiveItem();
                        resetProgress();
                        resetMaxProgress();
                        resetResultAmount();
                    }

                    if (currentItem != activeItem) {
                        activeItem = currentItem;
                        resetProgress();
                        resetMaxProgress();

                        Optional<RecipeHolder<?>> recipeOpt = getRecipeOptional();
                        setActiveRecipe(recipeOpt);

                        if (isRecipeValid(activePreciseRecipe)) {
                            setMaxProgressPrecise();
                            setPreciseResultAmount();

                        } else if (isRecipeValid(activeVagueRecipe)) {
                            setMaxProgressVague();
                            setVagueResultAmount();
                        } else {
                            resetActiveRecipes();
                            resetMaxProgress();
                            resetProgress();
                            resetResultAmount();
                        }
                    }

                    if (isTransferringFluids) {
                        return;
                    }

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
                    tank.transferFluidToTank(this, slot, tank);

                } else {
                    transferToHandler(tank, slot);
                }

            }

            private void transferToHandler(ModFluidTank tank, int slot) {
                if (tank.hasEmptyFluidHandlerInSlot(this, slot, tank)) {
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
                    tank.transferFluidFromTankToHandler(this, slot, tank);
                }
            }

            @Override
            public int getSlotLimit(int slot) {
                return ModFluidUtil.hasEmptyFluidHandlerInSlot(this, slot) ? 1 : super.getSlotLimit(slot);
            }

            @Override
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
                    // Bypass the item optimization check entirely because a fluid update occurred
                    Optional<RecipeHolder<?>> recipeOpt = getRecipeOptional();
                    setActiveRecipe(recipeOpt);

                    if (isRecipeValid(activePreciseRecipe)) {
                        setMaxProgressPrecise();
                    } else if (isRecipeValid(activeVagueRecipe)) {
                        setMaxProgressVague();
                    } else {
                        resetActivePreciseRecipe();
                        resetActiveVagueRecipe();
                        resetMaxProgress();
                        resetProgress();
                    }
                }
                setChanged();
            }
        };
    }
}
