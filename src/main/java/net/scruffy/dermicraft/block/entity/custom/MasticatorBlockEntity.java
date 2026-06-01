package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.scruffy.dermicraft.interfaces.IProcessFood;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.masticating.MasticatingRecipe;
import net.scruffy.dermicraft.recipe.masticating.MasticatingRecipeInput;
import net.scruffy.dermicraft.recipe.masticating.VagueMasticatingRecipe;
import net.scruffy.dermicraft.screen.custom.masticator.MasticatorMenu;
import net.scruffy.dermicraft.tank.FuelTank;
import net.scruffy.dermicraft.tank.VulnerableTank;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("unchecked")
public class MasticatorBlockEntity extends MachineBaseBlockEntity implements MenuProvider, IProcessFood, IHaveInventory {

    public final int FUEL_CAPACITY = FluidType.BUCKET_VOLUME * 5;
    public final int INGREDIENT_CAPACITY = FluidType.BUCKET_VOLUME * 5;
    public final int RESULT_CAPACITY = FluidType.BUCKET_VOLUME * 5;
    private final FuelTank FUEL_TANK = createFuelTank(FUEL_CAPACITY);
    private final VulnerableTank INGREDIENT_TANK = createVulnerableTank(INGREDIENT_CAPACITY);
    private final VulnerableTank RESULT_TANK = createVulnerableTank(RESULT_CAPACITY);

    public final int FUEL_SLOT = 0;
    public final int INGREDIENT_SLOT = 1;
    public final int RESULT_SLOT = 2;
    private final ItemStackHandler INVENTORY = createItemHandler(3, RESULT_SLOT);

    public final Direction FUEL_FACE = Direction.UP;
    public final Direction OUTPUT_FACE = Direction.DOWN;

    private final int FUEL_USE_DEFAULT = 1;
    private int fuelUseRate = FUEL_USE_DEFAULT;
    private final float SPEED_DEFAULT = 1f;
    private float speed = SPEED_DEFAULT;
    private final int CRAFT_TICKS = 10;

    private int progress = 0;
    private final int MAX_PROGRESS_DEFAULT = 0;
    private int maxProgress = MAX_PROGRESS_DEFAULT;//Max Progress will be based on recipe

    private RecipeHolder<MasticatingRecipe> activePreciseRecipe = null;
    private RecipeHolder<VagueMasticatingRecipe> activeVagueRecipe = null;
    private Item activeItem = Items.AIR;

    protected final ContainerData data;

    public MasticatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MASTICATOR_BE.get(), pos, blockState);

        data = setContainerData();
    }

    public IFluidHandler getTank(@Nullable Direction direction) {
        if (direction == null) {
            return INGREDIENT_TANK;
        }
        return switch (direction) {
            case UP -> FUEL_TANK;
            case DOWN -> RESULT_TANK;
            default -> INGREDIENT_TANK;
        };
    }

    public FluidTank getFuelTank() {
        return FUEL_TANK;
    }

    public FluidTank getIngredientTank() {
        return INGREDIENT_TANK;
    }

    public FluidTank getResultTank() {
        return RESULT_TANK;
    }

    public FluidStack getFluid(int slot) {
        return switch (slot) {
            case FUEL_SLOT -> FUEL_TANK.getFluid();
            case INGREDIENT_SLOT -> INGREDIENT_TANK.getFluid();
            case RESULT_SLOT -> RESULT_TANK.getFluid();
            default -> FluidStack.EMPTY;
        };
    }

    public IItemHandler getItemHandler(@Nullable Direction direction) {
        if (direction == null) {
            return INVENTORY;
        }

        int targetSlot = switch (direction) {
            case UP -> FUEL_SLOT;
            case DOWN -> RESULT_SLOT;
            default -> INGREDIENT_SLOT;
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

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        if (level.isClientSide) return;

        if (ModMath.Time.hasTicksPassed(level, 5)) {
           handleFuel();
            INGREDIENT_TANK.internalFluidTransfer (INVENTORY, INGREDIENT_SLOT);
            RESULT_TANK.transferToHandler(INVENTORY, RESULT_SLOT);
        }

        if (ModMath.Time.hasTicksPassed(level, CRAFT_TICKS)) {


        }
    }

    private void handleFuel() {
        FUEL_TANK.internalFuelTransfer(INVENTORY, FUEL_SLOT);
        setSpeed();
        setUseRate();
    }

    private void setSpeed() {
        speed = SPEED_DEFAULT;
        speed = FUEL_TANK.getSpeed();
    }

    private void setUseRate() {
        fuelUseRate = FUEL_USE_DEFAULT * CRAFT_TICKS;
        fuelUseRate = FUEL_TANK.getUseRate() * CRAFT_TICKS;
    }

    private void useFuel() {
        FUEL_TANK.useFuel(fuelUseRate);
    }

    private boolean hasIngredients() {
        if (!isIngredientSlotEmpty()) return true;
        return !INGREDIENT_TANK.isEmpty();
    }

    private boolean isIngredientSlotEmpty() {
        return INVENTORY.getStackInSlot(INGREDIENT_SLOT).isEmpty();
    }

    private Optional<RecipeHolder<?>> getRecipeOptional() {
        if (level==null)return Optional.empty();

        RecipeManager recipeManager = level.getRecipeManager();
        ItemStack stack = INVENTORY.getStackInSlot(INGREDIENT_SLOT);
        FluidStack fluid = INGREDIENT_TANK.getFluid();

        Optional<RecipeHolder<MasticatingRecipe>> preciseMatch =
                 recipeManager.getRecipeFor(ModRecipes.MASTICATING_TYPE.get(),
                         new MasticatingRecipeInput(stack, fluid), this.level);

        if (preciseMatch.isPresent()) return Optional.of(preciseMatch.get());

        Optional<RecipeHolder<VagueMasticatingRecipe>> vagueMatch =
                recipeManager.getRecipeFor(ModRecipes.VAGUE_MASTICATING_TYPE.get(), new MasticatingRecipeInput(stack, fluid), this.level);

        if (vagueMatch.isPresent()) return Optional.of(vagueMatch.get());

        return Optional.empty();
    }

    private void setActiveRecipe(Optional<RecipeHolder<?>> opt) {

        if (opt.isPresent()) {
            if (opt.get().value() instanceof MasticatingRecipe) {
                activePreciseRecipe = (RecipeHolder<MasticatingRecipe>) opt.get();
                resetActiveVagueRecipe();
            }
            if (opt.get().value() instanceof VagueMasticatingRecipe) {
                resetActivePreciseRecipe();
                activeVagueRecipe = (RecipeHolder<VagueMasticatingRecipe>) opt.get();
            }
        }
    }


    private void resetActiveItem() {
        activeItem = Items.AIR;
    }

    private void incrementProgress() {
        progress += Math.round((progress + CRAFT_TICKS) * speed);
    }

    private void resetProgress() {
        progress = 0;
    }

    private void resetMaxProgress() {
        maxProgress = MAX_PROGRESS_DEFAULT;
    }

    private void setMaxProgressPrecise() {
      maxProgress = activePreciseRecipe.value().ticks();
    }

    public void setMaxProgressVague() {
        maxProgress =
                activeVagueRecipe.value().getCraftingTime(INVENTORY.getStackInSlot(INGREDIENT_SLOT));
    }

    public boolean isStillCrafting() {
        return progress < MAX_PROGRESS_DEFAULT;
    }

    public int getScaledProgress(int scale) {
        return getScaledProgress(scale, progress, maxProgress);
    }

    private void resetActivePreciseRecipe() {
        activePreciseRecipe = null;
    }

    public void resetActiveVagueRecipe() {
        activeVagueRecipe = null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", INVENTORY.serializeNBT(registries));
        tag.put("fuel", FUEL_TANK.writeToNBT(registries, new CompoundTag()));
        tag.put("craft", INGREDIENT_TANK.writeToNBT(registries, new CompoundTag()));
        tag.put("output", RESULT_TANK.writeToNBT(registries, new CompoundTag()));
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        if (isRecipeValid(activePreciseRecipe)) tag.putString("precise_recipe", activePreciseRecipe.id().toString());
        if (isRecipeValid(activeVagueRecipe)) tag.putString("vague_recipe", activeVagueRecipe.id().toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) INVENTORY.deserializeNBT(registries, tag.getCompound("inventory"));
        if (tag.contains("fuel")) FUEL_TANK.readFromNBT(registries, tag.getCompound("fuel"));
        if (tag.contains("craft")) INGREDIENT_TANK.readFromNBT(registries, tag.getCompound("craft"));
        if (tag.contains("output")) RESULT_TANK.readFromNBT(registries, tag.getCompound("output"));
        this.progress = tag.getInt("progress");
        this.maxProgress = tag.getInt("maxProgress");

        if (tag.contains("precise_recipe")) {
            ResourceLocation id = ResourceLocation.parse(tag.getString("precise_recipe"));
            if (level != null) {
              activePreciseRecipe = (RecipeHolder<MasticatingRecipe>) level.getRecipeManager().byKey(id).orElse(null);
            }
        } else resetActivePreciseRecipe();

        if (tag.contains("vague_recipe")) {
            ResourceLocation id = ResourceLocation.parse(tag.getString("vague_recipe"));
            if (level != null) {
                activeVagueRecipe = (RecipeHolder<VagueMasticatingRecipe>) level.getRecipeManager().byKey(id).orElse(null);
            }
        } else resetActiveVagueRecipe();
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

    @Override
    protected ItemStackHandler createItemHandler(int size, int limitedSlot) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {

                ItemStack stack = INVENTORY.getStackInSlot(INGREDIENT_SLOT);
                Item currentItem = stack.getItem();

                if(stack.isEmpty()) {
                    resetActivePreciseRecipe();
                    resetActiveVagueRecipe();
                    resetActiveItem();
                    resetProgress();
                    resetMaxProgress();
                }

                if (currentItem != activeItem) {
                    activeItem = currentItem;

                    Optional<RecipeHolder<?>> recipeOpt = getRecipeOptional();
                    setActiveRecipe(recipeOpt);

                    if (isRecipeValid(activePreciseRecipe)) {
                        setMaxProgressPrecise();

                    } else if (isRecipeValid(activeVagueRecipe)) {
                        setMaxProgressVague();
                    }

                    else {
                        resetActivePreciseRecipe();
                        resetActiveVagueRecipe();
                        resetMaxProgress();
                        resetProgress();
                    }

                    setChanged();
                    updateBlock();
                }


            }

            @Override
            public int getSlotLimit(int slot) {
                return slot == limitedSlot ? 1 : super.getSlotLimit(slot);
            }
        };
    }
}
