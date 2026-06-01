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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
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
import net.scruffy.dermicraft.recipe.masticating.MasticatingRecipe;
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

    private RecipeHolder<?> cachedRecipe = null;
    private RecipeHolder<MasticatingRecipe> activePreciseRecipe = null;
    private RecipeHolder<VagueMasticatingRecipe> activeVagueRecipe = null;

    private ItemStack cachedItem = ItemStack.EMPTY;
    private FluidStack cachedFluid = FluidStack.EMPTY;

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
        fuelUseRate = FUEL_USE_DEFAULT;
        fuelUseRate = FUEL_TANK.getUseRate() * CRAFT_TICKS;
    }

    private void useFuel() {
        FUEL_TANK.useFuel(fuelUseRate);
    }

    private boolean hasIngredients() {
        if (!INVENTORY.getStackInSlot(INGREDIENT_SLOT).isEmpty()) return true;
        return !INGREDIENT_TANK.isEmpty();
    }

    /***********************************************************************************
     * Checks if the cached recipe is a standard MasticatingRecipe and returns it typed.
     * @return MasticatingRecipe Optional or empty optional
     ***********************************************************************************/
    public Optional<RecipeHolder<MasticatingRecipe>> getPreciseRecipe() {
        if (super.isRecipeValid(cachedRecipe) && this.cachedRecipe.value() instanceof MasticatingRecipe) {
            @SuppressWarnings("unchecked")
            RecipeHolder<MasticatingRecipe> typedHolder = (RecipeHolder<MasticatingRecipe>) this.cachedRecipe;
            return Optional.of(typedHolder);
        }
        return Optional.empty();
    }
    /***********************************************************************************
     * Checks if the cached recipe is a VagueMasticatingRecipe and returns it typed.
     * @return VagueMasticatingRecipe Optional or empty optional
     ***********************************************************************************/
    public Optional<RecipeHolder<VagueMasticatingRecipe>> getVagueRecipe() {
        if (super.isRecipeValid(cachedRecipe) && this.cachedRecipe.value() instanceof VagueMasticatingRecipe) {
            @SuppressWarnings("unchecked")
            RecipeHolder<VagueMasticatingRecipe> typedHolder = (RecipeHolder<VagueMasticatingRecipe>) this.cachedRecipe;
            return Optional.of(typedHolder);
        }
        return Optional.empty();
    }


    private void incrementProgress() {
        progress += Math.round((progress + CRAFT_TICKS) * speed);
    }

    private void resetProgress() {
        progress = 0;
    }

    private boolean isMaxProgressValid() {
        return maxProgress > MAX_PROGRESS_DEFAULT;
    }

    private void resetMaxProgress() {
        maxProgress = MAX_PROGRESS_DEFAULT;
    }

    private void setMaxProgressRecipe(RecipeHolder<MasticatingRecipe> recipe) {
      maxProgress = recipe.value().ticks();
    }

    public boolean isStillCrafting() {
        return progress < MAX_PROGRESS_DEFAULT;
    }

    public int getScaledProgress(int scale) {
        return getScaledProgress(scale, progress, maxProgress);
    }

    private void setCachedItem() {
        cachedItem = INVENTORY.getStackInSlot(INGREDIENT_SLOT).copy();
    }

    public void setCachedFluid() {
        cachedFluid = INGREDIENT_TANK.getFluid().copy();
    }

    private void resetCachedRecipe() {
        cachedRecipe = null;
    }

    private void resetActiveRecipes() {
        activePreciseRecipe = null;
        activeVagueRecipe = null;
    }

    private void resetCachedItem() {
        cachedItem = ItemStack.EMPTY;
    }

    private void resetCachedFluid() {
        cachedFluid = FluidStack.EMPTY;
    }

    private void resetAllCache() {
        resetCachedItem();
        resetCachedFluid();
        resetActiveRecipes();
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
        if (isRecipeValid(cachedRecipe)) tag.putString("cache_recipe", cachedRecipe.id().toString());
        if (!cachedItem.isEmpty()) tag.put("cache_item", cachedItem.save(registries, new CompoundTag()));
        if (!cachedFluid.isEmpty()) tag.put("cache_fluid", cachedFluid.save(registries, new CompoundTag()));
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
        if (tag.contains("cache_item")) {
            cachedItem = ItemStack.parse(registries, tag.getCompound("cache_item")).orElse(ItemStack.EMPTY);
        } else resetCachedItem();
        if (tag.contains("cache_fluid")) {
            cachedFluid = FluidStack.parse(registries, tag.getCompound("cache_fluid")).orElse(FluidStack.EMPTY);
        } else resetCachedFluid();
        if (tag.contains("cache_recipe")) {
            ResourceLocation id = ResourceLocation.parse(tag.getString("cache_recipe"));
            if (level != null) {
               cachedRecipe = (RecipeHolder<MasticatingRecipe>) level.getRecipeManager().byKey(id).orElse(null);
            }
        } else resetCachedRecipe();
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
}
